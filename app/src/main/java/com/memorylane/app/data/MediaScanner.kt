package com.memorylane.app.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.memorylane.app.model.MediaItem

/**
 * Scans a folder (picked via Storage Access Framework) and returns
 * every image/video inside it as a MediaItem.
 *
 * This does NOT recurse into sub-folders on purpose — each project
 * is meant to map to one flat folder of photos/videos. If you want
 * sub-folder support later, make scan() recursive.
 */
object MediaScanner {

    private val IMAGE_TYPES = setOf("jpg", "jpeg", "png", "webp", "heic", "gif", "bmp")
    private val VIDEO_TYPES = setOf("mp4", "mkv", "3gp", "mov", "webm", "avi")

    fun scan(context: Context, folderUriString: String): List<MediaItem> {
        val treeUri = Uri.parse(folderUriString)
        val folder = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        if (!folder.exists() || !folder.isDirectory) return emptyList()

        val items = mutableListOf<MediaItem>()

        for (file in folder.listFiles()) {
            if (!file.isFile) continue
            val name = file.name ?: continue
            val extension = name.substringAfterLast('.', "").lowercase()

            when {
                extension in IMAGE_TYPES -> items.add(MediaItem(file.uri, name, isVideo = false, lastModified = file.lastModified()))
                extension in VIDEO_TYPES -> items.add(MediaItem(file.uri, name, isVideo = true, lastModified = file.lastModified()))
            }
        }

        // Newest-looking file names first is not reliable, so just sort alphabetically.
        return items.sortedBy { it.name.lowercase() }
    }
}
