package com.memorylane.app.data

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Runs Google ML Kit's on-device models against a single photo to produce
 * a short list of human-friendly tags (e.g. "Beach", "Food", "People").
 *
 * Both models run fully offline once downloaded by Play Services the first
 * time your app uses them — no server calls, no account, no cost.
 *
 * Only works on images. Skip video URIs before calling this.
 */
object SmartTagger {

    private const val MIN_CONFIDENCE = 0.65f
    private const val MAX_TAGS = 3

    private val labeler by lazy {
        ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    }

    private val faceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
        FaceDetection.getClient(options)
    }

    suspend fun tagsFor(context: Context, uri: Uri): List<String> {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            return emptyList()
        }

        val labels = labelImage(image)
        val hasFaces = detectFaces(image)

        val tags = mutableListOf<String>()
        if (hasFaces) tags.add("People")
        tags.addAll(labels)

        return tags.distinct().take(MAX_TAGS)
    }

    private suspend fun labelImage(image: InputImage): List<String> =
        suspendCancellableCoroutine { cont ->
            labeler.process(image)
                .addOnSuccessListener { result ->
                    val names = result
                        .filter { it.confidence >= MIN_CONFIDENCE }
                        .sortedByDescending { it.confidence }
                        .map { it.text }
                    if (cont.isActive) cont.resume(names)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(emptyList())
                }
        }

    private suspend fun detectFaces(image: InputImage): Boolean =
        suspendCancellableCoroutine { cont ->
            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    if (cont.isActive) cont.resume(faces.isNotEmpty())
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(false)
                }
        }
}
