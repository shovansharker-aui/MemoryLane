package com.memorylane.app.model

import android.net.Uri

/**
 * A single photo or video found inside a project's folder.
 */
data class MediaItem(
    val uri: Uri,
    val name: String,
    val isVideo: Boolean
)
