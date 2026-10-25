package com.margelo.nitro.nitrovisionkit

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.min
import kotlin.math.sqrt

/**
 * ML Kit's subject-segmentation module (GMS dynamite / MediaPipe) reads the
 * bitmap native pointer from a worker thread. Mutable, hardware, or aliased
 * bitmaps can SIGSEGV when marshalled across the GMS binder boundary.
 */
internal fun Bitmap.toMlKitInput(maxEdge: Int = VisionKitLimits.ML_KIT_MAX_EDGE): Bitmap {
  val scaled = downscaleToMaxEdge(maxEdge)
  val ownsScaled = scaled !== this
  val needsCopy =
    scaled.config == Bitmap.Config.HARDWARE ||
      scaled.isRecycled ||
      scaled.isMutable ||
      scaled.config != Bitmap.Config.ARGB_8888
  if (!needsCopy) {
    return scaled
  }
  try {
    return scaled.copy(Bitmap.Config.ARGB_8888, false)
      ?: throw RuntimeException("Failed to copy bitmap for ML Kit input.")
  } finally {
    if (ownsScaled) {
      scaled.recycle()
    }
  }
}

internal fun Bitmap.downscaleToMaxPixels(maxPixels: Int): Bitmap {
  if (maxPixels <= 0) return this
  val count = width.toLong() * height.toLong()
  if (count <= maxPixels.toLong()) return this
  val scale = sqrt(maxPixels.toDouble() / count.toDouble())
  val targetW = (width * scale).toInt().coerceAtLeast(1)
  val targetH = (height * scale).toInt().coerceAtLeast(1)
  if (targetW >= width && targetH >= height) return this
  return Bitmap.createScaledBitmap(this, targetW, targetH, true)
}

internal fun Bitmap.immutableArgbCopy(): Bitmap =
  copy(Bitmap.Config.ARGB_8888, false)
    ?: throw RuntimeException("Failed to copy bitmap.")

private fun Bitmap.downscaleToMaxEdge(maxEdge: Int): Bitmap {
  val longest = maxOf(width, height)
  if (longest <= maxEdge) {
    return this
  }
  val scale = maxEdge.toFloat() / longest.toFloat()
  val targetW = (width * scale).toInt().coerceAtLeast(1)
  val targetH = (height * scale).toInt().coerceAtLeast(1)
  return Bitmap.createScaledBitmap(this, targetW, targetH, true)
}

internal fun FloatBuffer.upscaleMask(
  sourceWidth: Int,
  sourceHeight: Int,
  targetWidth: Int,
  targetHeight: Int,
): FloatBuffer {
  if (sourceWidth == targetWidth && sourceHeight == targetHeight) {
    rewind()
    return this
  }

  val source = FloatArray(sourceWidth * sourceHeight)
  rewind()
  get(source)

  val out = ByteBuffer
    .allocateDirect(targetWidth * targetHeight * 4)
    .order(ByteOrder.nativeOrder())
    .asFloatBuffer()

  for (y in 0 until targetHeight) {
    val srcY = min(sourceHeight - 1, y * sourceHeight / targetHeight)
    for (x in 0 until targetWidth) {
      val srcX = min(sourceWidth - 1, x * sourceWidth / targetWidth)
      out.put(source[srcY * sourceWidth + srcX])
    }
  }
  out.rewind()
  return out
}
