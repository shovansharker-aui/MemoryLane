package com.memorylane.app.data

import androidx.lifecycle.LiveData

class ProjectRepository(private val dao: ProjectDao) {

    val allProjects: LiveData<List<Project>> = dao.getAllProjects()

    suspend fun insert(project: Project): Long = dao.insert(project)

    suspend fun delete(project: Project) = dao.delete(project)
}
