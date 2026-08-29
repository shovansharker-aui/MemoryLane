package com.memorylane.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import com.memorylane.app.data.AppDatabase
import com.memorylane.app.data.MediaScanner
import com.memorylane.app.data.PhotoAnalysisCache
import com.memorylane.app.data.PhotoQualityAnalyzer
import com.memorylane.app.data.SmartTagger
import com.memorylane.app.databinding.ActivityGalleryBinding
import com.memorylane.app.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var adapter: MediaAdapter

    private var allItems: List<MediaItem> = emptyList()
    private val tagsByUri = mutableMapOf<Uri, List<String>>()
    private val blurryUris = mutableSetOf<Uri>()
    private val duplicateUris = mutableSetOf<Uri>()
    private var selectedFilter: String? = null // null = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val projectName = intent.getStringExtra(EXTRA_PROJECT_NAME) ?: "Project"
        val folderUri = intent.getStringExtra(EXTRA_FOLDER_URI)

        binding.collapsingToolbar.title = projectName
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        adapter = MediaAdapter { mediaItem, _ ->
            val viewerIntent = Intent(this, MediaViewerActivity::class.java).apply {
                putExtra(MediaViewerActivity.EXTRA_URI, mediaItem.uri.toString())
                putExtra(MediaViewerActivity.EXTRA_IS_VIDEO, mediaItem.isVideo)
            }
            startActivity(viewerIntent)
        }

        binding.recyclerMedia.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerMedia.adapter = adapter

        if (folderUri == null) {
            binding.textEmpty.visibility = android.view.View.VISIBLE
            binding.textEmpty.text = "No folder linked to this project."
            return
        }

        loadMedia(folderUri)
    }

    private fun loadMedia(folderUri: String) {
        binding.progressBar.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                MediaScanner.scan(this@GalleryActivity, folderUri)
            }

            binding.progressBar.visibility = android.view.View.GONE
            allItems = items

            if (items.isEmpty()) {
                binding.textEmpty.visibility = android.view.View.VISIBLE
                binding.textEmpty.text = "No photos or videos found in this folder."
            } else {
                binding.textEmpty.visibility = android.view.View.GONE
            }

            adapter.submitList(items)

            // AI tagging + blur/duplicate detection, using a persistent cache
            // so re-opening a project doesn't re-analyze photos you've
            // already seen. The grid is already usable while this runs.
            analyzeInBackground(items)
        }
    }

    private fun analyzeInBackground(items: List<MediaItem>) {
        lifecycleScope.launch {
            val imageItems = items.filter { !it.isVideo }
            if (imageItems.isEmpty()) return@launch

            val dao = AppDatabase.getDatabase(this@GalleryActivity).photoAnalysisDao()

            // 1) Pull every cached row for this batch in ONE query.
            val cached = withContext(Dispatchers.IO) {
                dao.getForUris(imageItems.map { it.uri.toString() })
            }.associateBy { it.uri }

            // 2) Split into "cache is valid" vs "needs (re-)analysis" —
            //    a cached entry is only trusted if the file hasn't changed
            //    since we last analyzed it.
            val needsAnalysis = mutableListOf<MediaItem>()

            for (item in imageItems) {
                val entry = cached[item.uri.toString()]
                if (entry != null && entry.fileLastModified == item.lastModified) {
                    applyCachedResult(item, entry)
                } else {
                    needsAnalysis.add(item)
                }
            }

            // Show whatever we already know from cache immediately.
            buildFilterChips()

            if (needsAnalysis.isNotEmpty()) {
                analyzeAndCache(needsAnalysis, dao)
            }
        }
    }

    private fun applyCachedResult(item: MediaItem, entry: PhotoAnalysisCache) {
        val tags = entry.tagList()
        if (tags.isNotEmpty()) tagsByUri[item.uri] = tags
        // Blur/duplicate flags get recomputed after all results are in
        // (see recomputeBlurAndDuplicates), since they're relative to the
        // whole project, not decidable from one cached row alone.
    }

    /**
     * Runs the actual ML Kit + pixel analysis for photos that weren't in
     * the cache (or had changed). Uses a Semaphore to cap how many photos
     * are processed at once — unlimited parallelism on a folder of
     * thousands of photos would spike memory and choke the device, so we
     * process a bounded number concurrently instead.
     */
    private suspend fun analyzeAndCache(items: List<MediaItem>, dao: com.memorylane.app.data.PhotoAnalysisDao) {
        val concurrency = Semaphore(4)
        val freshResults = mutableListOf<PhotoQualityAnalyzer.Result>()
        val cacheRows = mutableListOf<PhotoAnalysisCache>()

        withContext(Dispatchers.Default) {
            val jobs = items.map { item ->
                async {
                    concurrency.withPermit {
                        val tags = SmartTagger.tagsFor(this@GalleryActivity, item.uri)
                        val quality = PhotoQualityAnalyzer.analyze(this@GalleryActivity, item.uri)

                        if (tags.isNotEmpty()) {
                            synchronized(tagsByUri) { tagsByUri[item.uri] = tags }
                        }
                        if (quality != null) {
                            synchronized(freshResults) { freshResults.add(quality) }
                        }

                        synchronized(cacheRows) {
                            cacheRows.add(
                                PhotoAnalysisCache(
                                    uri = item.uri.toString(),
                                    tags = tags.joinToString(","),
                                    sharpness = quality?.sharpness ?: 0.0,
                                    hash = quality?.hash ?: 0L,
                                    fileLastModified = item.lastModified
                                )
                            )
                        }
                    }
                }
            }
            jobs.awaitAll()
        }

        // Persist everything we just computed, in one batch write.
        withContext(Dispatchers.IO) {
            dao.upsertAll(cacheRows)
        }

        recomputeBlurAndDuplicates()
        buildFilterChips()
    }

    /**
     * Re-derives which photos count as "blurry" or "possible duplicates"
     * using every cached + freshly-analyzed result for this project.
     * Pulls sharpness/hash back out of the cache table so this works even
     * for photos that were skipped this run (cache hit).
     */
    private fun recomputeBlurAndDuplicates() {
        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(this@GalleryActivity).photoAnalysisDao()
            val uris = allItems.filter { !it.isVideo }.map { it.uri.toString() }
            val rows = withContext(Dispatchers.IO) { dao.getForUris(uris) }
            if (rows.size < 3) return@launch

            blurryUris.clear()
            duplicateUris.clear()

            val sharpnessSorted = rows.map { it.sharpness }.sorted()
            val median = sharpnessSorted[sharpnessSorted.size / 2]
            val threshold = median * 0.35

            for (row in rows) {
                if (row.sharpness < threshold) {
                    blurryUris.add(Uri.parse(row.uri))
                }
            }

            // Bucket by the top 16 bits of the hash before comparing —
            // near-duplicates almost always share these bits, so this
            // avoids an O(n²) comparison across thousands of photos while
            // still catching the vast majority of true duplicates.
            val buckets = rows.groupBy { it.hash ushr 48 }
            for (bucket in buckets.values) {
                for (i in bucket.indices) {
                    for (j in i + 1 until bucket.size) {
                        val distance = PhotoQualityAnalyzer.hammingDistance(bucket[i].hash, bucket[j].hash)
                        if (distance <= 5) {
                            duplicateUris.add(Uri.parse(bucket[i].uri))
                            duplicateUris.add(Uri.parse(bucket[j].uri))
                        }
                    }
                }
            }

            buildFilterChips()
        }
    }

    private fun buildFilterChips() {
        val allTags = tagsByUri.values.flatten()
        if (allTags.isEmpty() && blurryUris.isEmpty() && duplicateUris.isEmpty()) return

        val topTags = allTags
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(6)
            .map { it.key }

        binding.chipGroupTags.removeAllViews()

        val allChip = Chip(this).apply {
            text = "All"
            isCheckable = true
            isChecked = selectedFilter == null
            setOnClickListener {
                selectedFilter = null
                applyFilter()
            }
        }
        binding.chipGroupTags.addView(allChip)

        if (blurryUris.isNotEmpty()) {
            val chip = Chip(this).apply {
                text = "Blurry (${blurryUris.size})"
                isCheckable = true
                isChecked = selectedFilter == FILTER_BLURRY
                setOnClickListener {
                    selectedFilter = FILTER_BLURRY
                    applyFilter()
                }
            }
            binding.chipGroupTags.addView(chip)
        }

        if (duplicateUris.isNotEmpty()) {
            val chip = Chip(this).apply {
                text = "Possible duplicates (${duplicateUris.size})"
                isCheckable = true
                isChecked = selectedFilter == FILTER_DUPLICATES
                setOnClickListener {
                    selectedFilter = FILTER_DUPLICATES
                    applyFilter()
                }
            }
            binding.chipGroupTags.addView(chip)
        }

        for (tag in topTags) {
            val chip = Chip(this).apply {
                text = tag
                isCheckable = true
                isChecked = selectedFilter == tag
                setOnClickListener {
                    selectedFilter = tag
                    applyFilter()
                }
            }
            binding.chipGroupTags.addView(chip)
        }

        binding.chipScroll.visibility = android.view.View.VISIBLE
    }

    private fun applyFilter() {
        val filter = selectedFilter
        val filtered = when (filter) {
            null -> allItems
            FILTER_BLURRY -> allItems.filter { it.uri in blurryUris }
            FILTER_DUPLICATES -> allItems.filter { it.uri in duplicateUris }
            else -> allItems.filter { tagsByUri[it.uri]?.contains(filter) == true }
        }
        adapter.submitList(filtered)
    }

    companion object {
        const val EXTRA_PROJECT_NAME = "extra_project_name"
        const val EXTRA_FOLDER_URI = "extra_folder_uri"
        private const val FILTER_BLURRY = "__blurry__"
        private const val FILTER_DUPLICATES = "__duplicates__"
    }
}
