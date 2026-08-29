package com.memorylane.app.model

import android.net.Uri

/**
 * A single photo or video found inside a project's folder.
 *
 * lastModified is the file's own modified timestamp (from the filesystem),
 * used to detect when a cached AI analysis is stale — e.g. if you replace
 * a file at the same path with a different photo.
 */
data class MediaItem(
    val uri: Uri,
    val name: String,
    val isVideo: Boolean,
    val lastModified: Long
)
