package com.memorylane.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.memorylane.app.data.Project
import com.memorylane.app.databinding.ActivityCreateProjectBinding

class CreateProjectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateProjectBinding
    private lateinit var viewModel: ProjectViewModel
    private var pickedFolderUri: Uri? = null

    // Launches the system folder picker (Storage Access Framework).
    private val folderPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                // Keep permission to read this folder even after the app restarts.
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                pickedFolderUri = uri
                binding.textFolderPath.text = uri.path ?: uri.toString()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateProjectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[ProjectViewModel::class.java]

        supportActionBar?.title = "New Project"

        binding.buttonPickFolder.setOnClickListener {
            folderPicker.launch(null)
        }

        binding.buttonSaveProject.setOnClickListener {
            val name = binding.editProjectName.text.toString().trim()
            val folderUri = pickedFolderUri

            when {
                name.isEmpty() -> {
                    binding.editProjectName.error = "Give your project a name"
                }
                folderUri == null -> {
                    Toast.makeText(this, "Pick a folder first", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val project = Project(name = name, folderUri = folderUri.toString())
                    viewModel.insert(project) {
                        finish()
                    }
                }
            }
        }
    }
}
