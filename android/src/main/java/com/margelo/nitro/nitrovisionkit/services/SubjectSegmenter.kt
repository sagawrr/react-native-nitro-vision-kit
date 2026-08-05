package com.margelo.nitro.nitrovisionkit

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.common.api.OptionalModuleApi
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions

internal object SubjectSegmenter {
  private const val FEATURE = "subject segmentation"

  @Volatile
  private var cached: SubjectSegmenter? = null

  private fun client(): SubjectSegmenter =
    cached ?: synchronized(this) {
      cached ?: newClient().also { cached = it }
    }

  private fun newClient(): SubjectSegmenter {
    val options = SubjectSegmenterOptions.Builder()
      .enableForegroundConfidenceMask()
      .build()
    return SubjectSegmentation.getClient(options)
  }

  private fun discard(stale: SubjectSegmenter) {
    synchronized(this) {
      if (cached === stale) cached = null
    }
    try {
      stale.close()
    } catch (_: Exception) {
    }
  }

  fun optionalApi(): OptionalModuleApi = client()

  suspend fun segment(
    context: Context,
    bitmap: Bitmap,
    trim: Boolean,
    retainMask: Boolean,
  ): SegmentationOutput {
    val segmenter = awaitReady(context)

    val mlInput = bitmap.toMlKitInput()
    val ownsMlInput = mlInput !== bitmap
    try {
      return segmentInternal(segmenter, mlInput, bitmap, trim, retainMask)
    } catch (error: Exception) {
      if (error.message == "No foreground subject detected.") throw error
      throw RuntimeException(
        MlKitModuleInstaller.friendlyError(error, FEATURE),
        error,
      )
    } finally {
      if (ownsMlInput) {
        mlInput.recycle()
      }
    }
  }

  /**
   * Prefetch builds a client before the model is on the device, and that client
   * keeps its failed [SubjectSegmenter.initTask] for the life of the process.
   * Rebuild once so the first run does not have to wait for an app restart.
   */
  private suspend fun awaitReady(context: Context): SubjectSegmenter {
    val current = client()
    MlKitModuleInstaller.ensure(context, FEATURE, current)
    try {
      current.initTask.await()
      return current
    } catch (_: Exception) {
      discard(current)
    }

    val rebuilt = client()
    try {
      rebuilt.initTask.await()
      return rebuilt
    } catch (error: Exception) {
      throw RuntimeException(
        MlKitModuleInstaller.friendlyError(error, FEATURE),
        error,
      )
    }
  }

  private suspend fun segmentInternal(
    segmenter: SubjectSegmenter,
    mlInput: Bitmap,
    bitmap: Bitmap,
    trim: Boolean,
    retainMask: Boolean,
  ): SegmentationOutput {
    val result = segmenter.process(InputImage.fromBitmap(mlInput, 0)).await()
    val rawMask = result.foregroundConfidenceMask
      ?: throw RuntimeException("No foreground subject detected.")
    val mask = rawMask.upscaleMask(
      sourceWidth = mlInput.width,
      sourceHeight = mlInput.height,
      targetWidth = bitmap.width,
      targetHeight = bitmap.height,
    )

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

    val normalizedBounds = VisionRect(
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
    val cropRight =
      ((pixelBounds.x + pixelBounds.width - 1).toInt() + pad).coerceAtMost(sourceWidth - 1)
    val cropBottom =
      ((pixelBounds.y + pixelBounds.height - 1).toInt() + pad).coerceAtMost(sourceHeight - 1)
    val cropW = cropRight - cropX + 1
    val cropH = cropBottom - cropY + 1
    val cropped = colors.cropArgbPixels(sourceWidth, cropX, cropY, cropW, cropH)
    val croppedMask = if (processed.hasMask) {
      processed.mask.cropFloatMask(sourceWidth, cropX, cropY, cropW, cropH)
    } else {
      processed.mask
    }

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
      mask = croppedMask,
      hasMask = processed.hasMask,
    )
  }
}
