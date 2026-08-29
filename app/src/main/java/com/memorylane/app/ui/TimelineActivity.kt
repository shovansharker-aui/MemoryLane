package com.memorylane.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.memorylane.app.data.AppDatabase
import com.memorylane.app.data.MediaScanner
import com.memorylane.app.data.PhotoMetadataReader
import com.memorylane.app.databinding.ActivityTimelineBinding
import com.memorylane.app.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TimelineActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTimelineBinding
    private lateinit var adapter: TimelineAdapter

    private val dayFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimelineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val projectName = intent.getStringExtra(GalleryActivity.EXTRA_PROJECT_NAME) ?: "Timeline"
        val folderUri = intent.getStringExtra(GalleryActivity.EXTRA_FOLDER_URI)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.title = "$projectName · Timeline"
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        adapter = TimelineAdapter { item ->
            val intent = android.content.Intent(this, MediaViewerActivity::class.java).apply {
                putExtra(MediaViewerActivity.EXTRA_URI, item.uri.toString())
                putExtra(MediaViewerActivity.EXTRA_IS_VIDEO, item.isVideo)
            }
            startActivity(intent)
        }

        val layoutManager = GridLayoutManager(this, 3)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int =
                if (adapter.getItemViewType(position) == 0) 3 else 1
        }
        binding.recyclerTimeline.layoutManager = layoutManager
        binding.recyclerTimeline.adapter = adapter

        if (folderUri == null) {
            binding.progressBar.visibility = android.view.View.GONE
            binding.textEmpty.visibility = android.view.View.VISIBLE
            return
        }

        loadTimeline(folderUri)
    }

    private fun loadTimeline(folderUri: String) {
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                MediaScanner.scan(this@TimelineActivity, folderUri).filter { !it.isVideo }
            }

            val dao = AppDatabase.getDatabase(this@TimelineActivity).photoAnalysisDao()
            val cached = withContext(Dispatchers.IO) {
                dao.getForUris(items.map { it.uri.toString() })
            }.associateBy { it.uri }

            // For anything not already analyzed/cached, read EXIF directly —
            // this is cheap (no ML models involved) so Timeline works even
            // if you haven't opened the gallery grid for this project yet.
            val datedItems = withContext(Dispatchers.Default) {
                items.mapNotNull { item ->
                    val cachedDate = cached[item.uri.toString()]?.dateTaken
                    val date = cachedDate
                        ?: PhotoMetadataReader.read(this@TimelineActivity, item.uri).dateTakenMillis
                    if (date != null) item to date else null
                }
            }

            binding.progressBar.visibility = android.view.View.GONE

            if (datedItems.isEmpty()) {
                binding.textEmpty.visibility = android.view.View.VISIBLE
                return@launch
            }

            binding.textEmpty.visibility = android.view.View.GONE
            adapter.submitRows(buildRows(datedItems))
        }
    }

    private fun buildRows(datedItems: List<Pair<MediaItem, Long>>): List<TimelineRow> {
        val sorted = datedItems.sortedBy { it.second }
        val rows = mutableListOf<TimelineRow>()
        var currentDayKey: String? = null

        for ((item, dateMillis) in sorted) {
            val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
            val dayKey = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"

            if (dayKey != currentDayKey) {
                rows.add(TimelineRow.Header(dayFormat.format(cal.time)))
                currentDayKey = dayKey
            }
            rows.add(TimelineRow.Photo(item))
        }

        return rows
    }
}
