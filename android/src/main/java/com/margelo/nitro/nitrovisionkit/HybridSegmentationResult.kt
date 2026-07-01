package com.margelo.nitro.nitrovisionkit

import android.graphics.Bitmap
import androidx.annotation.Keep
import com.facebook.proguard.annotations.DoNotStrip
import com.margelo.nitro.core.ArrayBuffer
import com.margelo.nitro.core.Promise
import java.io.File
import java.io.FileOutputStream

/**
 * Holds the masked subject pixels produced by a segmentation. The native byte
 * buffer stays on the native side and is accessed lazily through
 * [toArrayBuffer] (zero-copy, premultiplied RGBA) or [saveToTemporaryFile]
 * (encoded file), so the caller only pays for the conversion they need.
 *
 * The pixels are premultiplied RGBA so they can be handed straight to
 * `react-native-nitro-image`'s `loadFromRawPixelData` without re-encoding.
 */
@Keep
@DoNotStrip
class HybridSegmentationResult(
  private val rgba: ByteArray,
  pixelWidth: Int,
  pixelHeight: Int,
  override val bounds: Rect,
  private val tempDir: File,
) : HybridSegmentationResultSpec() {

  private val _width: Double = pixelWidth.toDouble()
  private val _height: Double = pixelHeight.toDouble()

  override val width: Double
    get() = _width

  override val height: Double
    get() = _height

  /**
   * Bytes owned by this HybridObject; reported so the JS VM can reclaim it
   * under memory pressure.
   */
  override val memorySize: Long
    get() = rgba.size.toLong() + 128L

  override fun toArrayBuffer(): ArrayBuffer {
    return ArrayBuffer.copy(rgba)
  }

  override fun saveToTemporaryFile(format: ImageFormat, quality: Double): Promise<String> {
    val q = quality.toInt().coerceIn(0, 100)
    val w = width.toInt()
    val h = height.toInt()
    val pixels = rgba
    return Promise.async {
      val bitmap = RgbaConversions.premultipliedRgbaToBitmap(pixels, w, h)
      try {
        val compressed = if (format == ImageFormat.PNG) {
          Bitmap.CompressFormat.PNG
        } else {
          Bitmap.CompressFormat.JPEG
        }
        val ext = if (format == ImageFormat.PNG) "png" else "jpg"
        val file = File.createTempFile("visionkit-", ".$ext", tempDir)
        FileOutputStream(file).use { out -> bitmap.compress(compressed, q, out) }
        file.absolutePath
      } finally {
        bitmap.recycle()
      }
    }
  }
}
