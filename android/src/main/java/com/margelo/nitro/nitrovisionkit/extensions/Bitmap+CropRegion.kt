package com.margelo.nitro.nitrovisionkit

import android.graphics.Bitmap

internal fun Bitmap.cropToRegion(region: Rect): Bitmap {
  val x = (region.x * width).toInt().coerceIn(0, width - 1)
  val y = (region.y * height).toInt().coerceIn(0, height - 1)
  val cropW = (region.width * width).toInt().coerceAtLeast(1).coerceAtMost(width - x)
  val cropH = (region.height * height).toInt().coerceAtLeast(1).coerceAtMost(height - y)
  return Bitmap.createBitmap(this, x, y, cropW, cropH)
}
