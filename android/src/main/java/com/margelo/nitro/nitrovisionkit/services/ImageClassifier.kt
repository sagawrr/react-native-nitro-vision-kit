package com.margelo.nitro.nitrovisionkit

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel

internal object ImageClassifier {
  suspend fun classify(
    bitmap: Bitmap,
    maxResults: Int,
    minConfidence: Float,
    region: Rect?,
  ): List<Classification> {
    val cropped = if (region != null) bitmap.cropToRegion(region) else null
    val source = cropped ?: bitmap
    try {
      val labeler = ImageLabelerCache.labeler(minConfidence)
      val labels = labeler.process(InputImage.fromBitmap(source, 0)).await()
      val sorted = labels.sortedByDescending(ImageLabel::getConfidence)
      val limited = if (maxResults > 0) sorted.take(maxResults) else sorted
      return limited.mapIndexed { index, label ->
        Classification(
          label = label.text,
          confidence = label.confidence.toDouble(),
          index = index.toDouble(),
        )
      }
    } finally {
      if (cropped != null && cropped !== bitmap) {
        cropped.recycle()
      }
    }
  }
}
