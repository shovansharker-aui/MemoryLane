package com.memorylane.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PhotoAnalysisDao {

    // Fetch every cached row whose URI is in this batch — one query instead
    // of one-per-photo, which matters a lot once you're dealing with
    // thousands of files.
    @Query("SELECT * FROM photo_analysis_cache WHERE uri IN (:uris)")
    suspend fun getForUris(uris: List<String>): List<PhotoAnalysisCache>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: PhotoAnalysisCache)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<PhotoAnalysisCache>)
}
