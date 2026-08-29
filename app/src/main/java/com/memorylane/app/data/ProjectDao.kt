package com.memorylane.app.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): LiveData<List<Project>>

    @Insert
    suspend fun insert(project: Project): Long

    @Delete
    suspend fun delete(project: Project)
}
