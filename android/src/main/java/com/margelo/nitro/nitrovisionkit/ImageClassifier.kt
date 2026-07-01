package com.margelo.nitro.nitrovisionkit

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

/**
 * Runs ML Kit [ImageLabeling] to classify the contents of a [Bitmap].
 *
 * The labeler is rebuilt on demand because [ImageLabelerOptions] carry the
 * confidence threshold, which can vary per call. The underlying model is cached
 * by ML Kit across labeler instances.
 */
internal object ImageClassifier {
  /**
   * Classifies [bitmap], returning labels ranked by confidence. Labels below
   * [minConfidence] are dropped, and at most [maxResults] are returned
   * (when [maxResults] is positive).
   */
  suspend fun classify(
    bitmap: Bitmap,
    maxResults: Int,
    minConfidence: Float,
  ): List<Classification> {
    val options = ImageLabelerOptions.Builder()
      .setConfidenceThreshold(minConfidence)
      .build()
    val labeler: ImageLabeler = ImageLabeling.getClient(options)
    val labels = labeler.process(InputImage.fromBitmap(bitmap, 0)).await()
    val sorted = labels.sortedByDescending(ImageLabel::getConfidence)
    val limited = if (maxResults > 0) sorted.take(maxResults) else sorted
    return limited.mapIndexed { index, label ->
      Classification(
        label = label.text,
        confidence = label.confidence.toDouble(),
        index = index.toDouble(),
      )
    }
  }
}
