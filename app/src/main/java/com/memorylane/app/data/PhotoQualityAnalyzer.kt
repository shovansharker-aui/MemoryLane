package com.memorylane.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri

/**
 * Two lightweight, fully on-device photo-quality checks:
 *
 * 1. sharpness() — a Laplacian-variance blur score. Lower = blurrier.
 *    We compare scores *relative to the rest of the project* (see
 *    GalleryActivity) rather than against a fixed number, since what
 *    counts as "sharp" varies a lot by camera and lighting.
 *
 * 2. averageHash() — a classic 64-bit "aHash" perceptual fingerprint.
 *    Near-duplicate photos (e.g. three shots of the same scene) end up
 *    with hashes that differ in only a handful of bits, so comparing
 *    Hamming distance between hashes finds duplicates without needing
 *    any AI model or network call.
 */
object PhotoQualityAnalyzer {

    data class Result(val uri: Uri, val sharpness: Double, val hash: Long)

    fun analyze(context: Context, uri: Uri): Result? {
        val bitmap = decodeDownsampled(context, uri, targetWidth = 120) ?: return null
        val sharpness = laplacianVariance(bitmap)
        val hash = averageHash(bitmap)
        bitmap.recycle()
        return Result(uri, sharpness, hash)
    }

    /** Hamming distance between two 64-bit hashes — number of differing bits. */
    fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

    private fun decodeDownsampled(context: Context, uri: Uri, targetWidth: Int): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(input, null, bounds)

                val sampleSize = (bounds.outWidth / targetWidth).coerceAtLeast(1)

                context.contentResolver.openInputStream(uri)?.use { input2 ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    BitmapFactory.decodeStream(input2, null, opts)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun laplacianVariance(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 3 || height < 3) return 0.0

        // Grayscale pixel grid
        val gray = Array(height) { y -> IntArray(width) { x -> luminance(bitmap.getPixel(x, y)) } }

        val laplacian = mutableListOf<Int>()
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val value = (-gray[y - 1][x] - gray[y][x - 1] + 4 * gray[y][x] -
                        gray[y][x + 1] - gray[y + 1][x])
                laplacian.add(value)
            }
        }

        if (laplacian.isEmpty()) return 0.0
        val mean = laplacian.average()
        val variance = laplacian.sumOf { (it - mean) * (it - mean) } / laplacian.size
        return variance
    }

    private fun averageHash(bitmap: Bitmap): Long {
        val small = Bitmap.createScaledBitmap(bitmap, 8, 8, true)
        val values = IntArray(64)
        var sum = 0
        var index = 0
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val l = luminance(small.getPixel(x, y))
                values[index++] = l
                sum += l
            }
        }
        small.recycle()
        val avg = sum / 64
        var hash = 0L
        for (i in 0 until 64) {
            if (values[i] >= avg) hash = hash or (1L shl i)
        }
        return hash
    }

    private fun luminance(pixel: Int): Int {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }
}
