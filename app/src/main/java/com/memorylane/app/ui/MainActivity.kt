package com.memorylane.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.memorylane.app.data.Project
import com.memorylane.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ProjectViewModel
    private lateinit var adapter: ProjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[ProjectViewModel::class.java]

        adapter = ProjectAdapter(
            onClick = { project -> openProject(project) },
            onLongClick = { project -> viewModel.delete(project) }
        )

        binding.recyclerProjects.layoutManager = LinearLayoutManager(this)
        binding.recyclerProjects.adapter = adapter

        viewModel.allProjects.observe(this) { projects ->
            adapter.submitList(projects)
            binding.textEmpty.visibility =
                if (projects.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }

        binding.fabAddProject.setOnClickListener {
            startActivity(Intent(this, CreateProjectActivity::class.java))
        }
    }

    private fun openProject(project: Project) {
        val intent = Intent(this, GalleryActivity::class.java).apply {
            putExtra(GalleryActivity.EXTRA_PROJECT_NAME, project.name)
            putExtra(GalleryActivity.EXTRA_FOLDER_URI, project.folderUri)
        }
        startActivity(intent)
    }
}
