package com.margelo.nitro.nitrovisionkit

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Crops a row-major Float32 mask stored as little-endian bytes. */
internal fun ByteArray.cropFloatMask(
  sourceWidth: Int,
  x: Int,
  y: Int,
  cropWidth: Int,
  cropHeight: Int,
): ByteArray {
  if (isEmpty()) {
    return this
  }
  val out = ByteArray(cropWidth * cropHeight * 4)
  val source = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
  val dest = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
  for (row in 0 until cropHeight) {
    val srcRow = (y + row) * sourceWidth
    for (col in 0 until cropWidth) {
      dest.put(source.get(srcRow + x + col))
    }
  }
  return out
}
