package com.memorylane.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.memorylane.app.data.MediaScanner
import com.memorylane.app.databinding.ActivityGalleryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var adapter: MediaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val projectName = intent.getStringExtra(EXTRA_PROJECT_NAME) ?: "Project"
        val folderUri = intent.getStringExtra(EXTRA_FOLDER_URI)

        supportActionBar?.title = projectName

        adapter = MediaAdapter { mediaItem, position ->
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

            if (items.isEmpty()) {
                binding.textEmpty.visibility = android.view.View.VISIBLE
                binding.textEmpty.text = "No photos or videos found in this folder."
            } else {
                binding.textEmpty.visibility = android.view.View.GONE
            }

            adapter.submitList(items)
        }
    }

    companion object {
        const val EXTRA_PROJECT_NAME = "extra_project_name"
        const val EXTRA_FOLDER_URI = "extra_folder_uri"
    }
}
