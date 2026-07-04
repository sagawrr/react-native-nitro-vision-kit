package com.margelo.nitro.nitrovisionkit

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

internal object MaskMetrics {
  const val THRESHOLD = 0.5f

  fun processForeground(
    mask: FloatBuffer,
    colors: IntArray,
    width: Int,
    height: Int,
    retainMask: Boolean,
  ): ProcessedForeground {
    val count = width * height
    val maskBytes = if (retainMask) ByteArray(count * 4) else ByteArray(0)
    val maskOut = if (retainMask) ByteBuffer.wrap(maskBytes).order(ByteOrder.LITTLE_ENDIAN) else null

    var foreground = 0
    var sumX = 0.0
    var sumY = 0.0
    var minX = width
    var minY = height
    var maxX = -1
    var maxY = -1

    mask.rewind()
    for (i in 0 until count) {
      val confidence = mask.get().coerceIn(0f, 1f)
      if (retainMask) {
        maskOut?.putFloat(confidence)
      }
      val alpha = (confidence * 255f).toInt()
      colors[i] = (alpha shl 24) or (colors[i] and 0x00FFFFFF)
      if (confidence > THRESHOLD) {
        foreground += 1
        val x = i % width
        val y = i / width
        sumX += x.toDouble()
        sumY += y.toDouble()
        if (x < minX) minX = x
        if (y < minY) minY = y
        if (x > maxX) maxX = x
        if (y > maxY) maxY = y
      }
    }

    if (foreground <= 0 || maxX < 0) {
      return ProcessedForeground(
        metrics = MaskMetricsResult(0.0, NormalizedPoint(0.5, 0.5), null),
        mask = maskBytes,
        hasMask = retainMask,
      )
    }

    val coverage = foreground.toDouble() / count.toDouble()
    val centroid = NormalizedPoint(
      x = (sumX / foreground + 0.5) / width,
      y = (sumY / foreground + 0.5) / height,
    )
    val pixelBounds = PixelRect(
      x = minX.toDouble(),
      y = minY.toDouble(),
      width = (maxX - minX + 1).toDouble(),
      height = (maxY - minY + 1).toDouble(),
    )
    return ProcessedForeground(
      metrics = MaskMetricsResult(coverage, centroid, pixelBounds),
      mask = maskBytes,
      hasMask = retainMask,
    )
  }
}
