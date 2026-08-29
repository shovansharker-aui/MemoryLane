package com.memorylane.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.memorylane.app.data.AppDatabase
import com.memorylane.app.data.Project
import com.memorylane.app.data.ProjectRepository
import kotlinx.coroutines.launch

class ProjectViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProjectRepository
    val allProjects: LiveData<List<Project>>

    init {
        val dao = AppDatabase.getDatabase(application).projectDao()
        repository = ProjectRepository(dao)
        allProjects = repository.allProjects
    }

    fun insert(project: Project, onDone: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = repository.insert(project)
        onDone(id)
    }

    fun delete(project: Project) = viewModelScope.launch {
        repository.delete(project)
    }
}
