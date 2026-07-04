package com.margelo.nitro.nitrovisionkit

internal fun IntArray.cropArgbPixels(
  sourceWidth: Int,
  x: Int,
  y: Int,
  cropWidth: Int,
  cropHeight: Int,
): IntArray {
  val cropped = IntArray(cropWidth * cropHeight)
  for (row in 0 until cropHeight) {
    System.arraycopy(
      this,
      (y + row) * sourceWidth + x,
      cropped,
      row * cropWidth,
      cropWidth,
    )
  }
  return cropped
}
