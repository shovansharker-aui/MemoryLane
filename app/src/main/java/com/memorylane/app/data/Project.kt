package com.memorylane.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One "project" = a name the user gave (e.g. "Cox's Bazar 2025")
 * plus the folder URI on the phone where those photos/videos live.
 */
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    // Stored as a String because Room can't store Uri directly.
    // This is a SAF "tree" URI, e.g. content://com.android.externalstorage.documents/tree/...
    val folderUri: String,
    val createdAt: Long = System.currentTimeMillis()
)
