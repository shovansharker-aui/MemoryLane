package com.memorylane.app.data

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Reads a photo's capture date and GPS coordinates from its EXIF metadata
 * (the same data your phone's camera already embeds in every photo).
 *
 * Returns nulls where EXIF data is missing — not every photo has GPS on,
 * and screenshots/downloaded images usually have no EXIF at all.
 */
object PhotoMetadataReader {

    data class Metadata(
        val dateTakenMillis: Long?,
        val latitude: Double?,
        val longitude: Double?
    )

    private val EXIF_DATE_FORMAT = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)

    fun read(context: Context, uri: Uri): Metadata {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)

                val dateString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                val dateMillis = dateString?.let {
                    try {
                        EXIF_DATE_FORMAT.parse(it)?.time
                    } catch (e: Exception) {
                        null
                    }
                }

                val latLong = FloatArray(2)
                val hasLatLong = exif.getLatLong(latLong)

                Metadata(
                    dateTakenMillis = dateMillis,
                    latitude = if (hasLatLong) latLong[0].toDouble() else null,
                    longitude = if (hasLatLong) latLong[1].toDouble() else null
                )
            } ?: Metadata(null, null, null)
        } catch (e: Exception) {
            Metadata(null, null, null)
        }
    }
}
