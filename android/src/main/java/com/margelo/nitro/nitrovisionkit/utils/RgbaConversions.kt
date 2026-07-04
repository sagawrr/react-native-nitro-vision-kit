package com.margelo.nitro.nitrovisionkit

import android.graphics.Bitmap

/**
 * Converts masked subject pixels between the premultiplied RGBA byte layout
 * (the storage format for [HybridSegmentationResult], and the contract for
 * `react-native-nitro-image`'s `loadFromRawPixelData`) and Android [Bitmap]s
 * used for file encoding.
 */
internal object RgbaConversions {
  /**
   * Packs a masked ARGB [IntArray] (alpha already applied, straight) into
   * premultiplied RGBA bytes. Premultiplication matches the nitro-image
   * `loadFromRawPixelData` contract.
   */
  fun colorsToPremultipliedRgba(colors: IntArray, width: Int, height: Int): ByteArray {
    val out = ByteArray(width * height * 4)
    for (i in colors.indices) {
      val c = colors[i]
      val a = (c ushr 24 and 0xFF) / 255f
      val r = ((c shr 16 and 0xFF) * a).toInt()
      val g = ((c shr 8 and 0xFF) * a).toInt()
      val b = ((c and 0xFF) * a).toInt()
      out[i * 4] = r.toByte()
      out[i * 4 + 1] = g.toByte()
      out[i * 4 + 2] = b.toByte()
      out[i * 4 + 3] = (a * 255f).toInt().toByte()
    }
    return out
  }

  /** Builds an `ARGB_8888` [Bitmap] from premultiplied RGBA [rgba] bytes. */
  fun premultipliedRgbaToBitmap(rgba: ByteArray, width: Int, height: Int): Bitmap {
    val colors = IntArray(width * height)
    for (i in colors.indices) {
      val a = rgba[i * 4 + 3].toInt() and 0xFF
      val r = rgba[i * 4].toInt() and 0xFF
      val g = rgba[i * 4 + 1].toInt() and 0xFF
      val b = rgba[i * 4 + 2].toInt() and 0xFF
      // ARGB Int (premultiplied r/g/b); Android Bitmaps are premultiplied by default.
      colors[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
    }
    return Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888)
  }
}
