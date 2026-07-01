package com.margelo.nitro.nitrovisionkit

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions

/**
 * Runs ML Kit [SubjectSegmentation] and composites the float confidence mask
 * onto the source [Bitmap], preserving the soft matte instead of ML Kit's
 * pre-baked binary alpha. Output is premultiplied RGBA to match the
 * `react-native-nitro-image` `loadFromRawPixelData` contract. A single
 * segmenter is reused across calls.
 */
internal object SubjectSegmenter {
  private val segmenter: SubjectSegmenter by lazy {
    val options = SubjectSegmenterOptions.Builder()
      .enableForegroundConfidenceMask()
      .build()
    SubjectSegmentation.getClient(options)
  }

  /**
   * Segments [bitmap], returning a [SegmentationOutput]. When [trim] is true the
   * returned pixels are cropped to the subject's bounding box (padded); bounds
   * still describe the subject's position in the original [bitmap].
   */
  suspend fun segment(bitmap: Bitmap, trim: Boolean): SegmentationOutput {
    val result = segmenter.process(InputImage.fromBitmap(bitmap, 0)).await()
    val mask = result.foregroundConfidenceMask
      ?: throw RuntimeException("No foreground subject detected.")

    val w = bitmap.width
    val h = bitmap.height
    val colors = IntArray(w * h)
    bitmap.getPixels(colors, 0, w, 0, 0, w, h)

    var minX = w
    var minY = h
    var maxX = -1
    var maxY = -1
    mask.rewind()
    for (i in 0 until w * h) {
      val confidence = mask.get().coerceIn(0f, 1f)
      val alpha = (confidence * 255f).toInt()
      // Apply straight alpha now; premultiplication happens in colorsToPremultipliedRgba.
      colors[i] = (alpha shl 24) or (colors[i] and 0x00FFFFFF)
      if (alpha > 0) {
        val x = i % w
        val y = i / w
        if (x < minX) minX = x
        if (y < minY) minY = y
        if (x > maxX) maxX = x
        if (y > maxY) maxY = y
      }
    }

    if (maxX < 0) {
      throw RuntimeException("No foreground subject detected.")
    }

    val bounds = Rect(
      x = minX.toDouble() / w,
      y = minY.toDouble() / h,
      width = (maxX - minX + 1).toDouble() / w,
      height = (maxY - minY + 1).toDouble() / h,
    )

    if (!trim) {
      return SegmentationOutput(
        pixels = RgbaConversions.colorsToPremultipliedRgba(colors, w, h),
        width = w,
        height = h,
        bounds = bounds,
      )
    }

    val pad = 2
    val cropX = (minX - pad).coerceAtLeast(0)
    val cropY = (minY - pad).coerceAtLeast(0)
    val cropRight = (maxX + pad).coerceAtMost(w - 1)
    val cropBottom = (maxY + pad).coerceAtMost(h - 1)
    val cropW = cropRight - cropX + 1
    val cropH = cropBottom - cropY + 1
    val cropped = IntArray(cropW * cropH)
    for (y in 0 until cropH) {
      for (x in 0 until cropW) {
        cropped[y * cropW + x] = colors[(cropY + y) * w + (cropX + x)]
      }
    }
    return SegmentationOutput(
      pixels = RgbaConversions.colorsToPremultipliedRgba(cropped, cropW, cropH),
      width = cropW,
      height = cropH,
      bounds = bounds,
    )
  }
}
