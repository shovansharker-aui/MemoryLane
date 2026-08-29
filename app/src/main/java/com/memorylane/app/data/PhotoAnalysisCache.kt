package com.memorylane.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached AI analysis for one photo, keyed by its content URI (as a String,
 * since Room can't store Uri directly).
 *
 * fileLastModified lets us detect a stale cache entry: if the file at this
 * URI has since changed (different lastModified than when we analyzed it),
 * we know to re-run analysis instead of trusting the old cached result.
 */
@Entity(tableName = "photo_analysis_cache")
data class PhotoAnalysisCache(
    @PrimaryKey val uri: String,
    val tags: String,          // comma-separated, e.g. "Beach,People"
    val sharpness: Double,
    val hash: Long,
    val fileLastModified: Long,
    val dateTaken: Long? = null,   // from EXIF, null if unavailable
    val latitude: Double? = null,  // from EXIF GPS, null if unavailable
    val longitude: Double? = null,
    val analyzedAt: Long = System.currentTimeMillis()
) {
    fun tagList(): List<String> =
        if (tags.isBlank()) emptyList() else tags.split(",")
}
