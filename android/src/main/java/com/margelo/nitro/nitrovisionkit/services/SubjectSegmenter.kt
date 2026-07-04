package com.margelo.nitro.nitrovisionkit

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions

internal object SubjectSegmenter {
  private val segmenter: SubjectSegmenter by lazy {
    val subjectResultOptions = SubjectSegmenterOptions.SubjectResultOptions.Builder()
      .enableConfidenceMask()
      .build()
    val options = SubjectSegmenterOptions.Builder()
      .enableForegroundConfidenceMask()
      .enableMultipleSubjects(subjectResultOptions)
      .build()
    SubjectSegmentation.getClient(options)
  }

  suspend fun segment(bitmap: Bitmap, trim: Boolean, retainMask: Boolean): SegmentationOutput {
    val result = segmenter.process(InputImage.fromBitmap(bitmap, 0)).await()
    val mask = result.foregroundConfidenceMask
      ?: throw RuntimeException("No foreground subject detected.")

    val sourceWidth = bitmap.width
    val sourceHeight = bitmap.height
    val colors = IntArray(sourceWidth * sourceHeight)
    bitmap.getPixels(colors, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight)

    val processed = MaskMetrics.processForeground(mask, colors, sourceWidth, sourceHeight, retainMask)
    val pixelBounds = processed.metrics.pixelBounds
      ?: throw RuntimeException("No foreground subject detected.")

    val subjects = result.subjects
    val instanceCount = when {
      subjects.isNotEmpty() -> subjects.size.toDouble()
      else -> 1.0
    }

    val normalizedBounds = Rect(
      x = pixelBounds.x / sourceWidth,
      y = pixelBounds.y / sourceHeight,
      width = pixelBounds.width / sourceWidth,
      height = pixelBounds.height / sourceHeight,
    )

    if (!trim) {
      return SegmentationOutput(
        pixels = RgbaConversions.colorsToPremultipliedRgba(colors, sourceWidth, sourceHeight),
        width = sourceWidth,
        height = sourceHeight,
        bounds = normalizedBounds,
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight,
        foregroundCoverage = processed.metrics.coverage,
        centroid = processed.metrics.centroid,
        instanceCount = instanceCount,
        pixelBounds = pixelBounds,
        trimOrigin = NormalizedPoint(0.0, 0.0),
        mask = processed.mask,
        hasMask = processed.hasMask,
      )
    }

    val pad = 2
    val cropX = (pixelBounds.x.toInt() - pad).coerceAtLeast(0)
    val cropY = (pixelBounds.y.toInt() - pad).coerceAtLeast(0)
    val cropRight = (pixelBounds.x + pixelBounds.width - 1).toInt().coerceAtMost(sourceWidth - 1) + pad
    val cropBottom = (pixelBounds.y + pixelBounds.height - 1).toInt().coerceAtMost(sourceHeight - 1) + pad
    val cropW = cropRight - cropX + 1
    val cropH = cropBottom - cropY + 1
    val cropped = colors.cropArgbPixels(sourceWidth, cropX, cropY, cropW, cropH)

    return SegmentationOutput(
      pixels = RgbaConversions.colorsToPremultipliedRgba(cropped, cropW, cropH),
      width = cropW,
      height = cropH,
      bounds = normalizedBounds,
      sourceWidth = sourceWidth,
      sourceHeight = sourceHeight,
      foregroundCoverage = processed.metrics.coverage,
      centroid = processed.metrics.centroid,
      instanceCount = instanceCount,
      pixelBounds = pixelBounds,
      trimOrigin = NormalizedPoint(
        x = cropX.toDouble() / sourceWidth,
        y = cropY.toDouble() / sourceHeight,
      ),
      mask = processed.mask,
      hasMask = processed.hasMask,
    )
  }
}
