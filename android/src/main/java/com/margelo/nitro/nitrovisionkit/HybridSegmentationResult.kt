package com.margelo.nitro.nitrovisionkit

import android.graphics.Bitmap
import androidx.annotation.Keep
import com.facebook.proguard.annotations.DoNotStrip
import com.margelo.nitro.core.ArrayBuffer
import com.margelo.nitro.core.Promise
import java.io.File
import java.io.FileOutputStream

@Keep
@DoNotStrip
class HybridSegmentationResult internal constructor(
  output: SegmentationOutput,
  private val tempDir: File,
) : HybridSegmentationResultSpec() {

  private var rgba = output.pixels
  private var mask = output.mask
  private val pixelWidth = output.width
  private val pixelHeight = output.height

  override val bounds: Rect = output.bounds
  override val sourceWidth: Double = output.sourceWidth.toDouble()
  override val sourceHeight: Double = output.sourceHeight.toDouble()
  override val foregroundCoverage: Double = output.foregroundCoverage
  override val centroid: NormalizedPoint = output.centroid
  override val instanceCount: Double = output.instanceCount
  override val pixelBounds: PixelRect = output.pixelBounds
  override val trimOrigin: NormalizedPoint = output.trimOrigin
  override val hasMask: Boolean = output.hasMask

  override val width: Double
    get() = pixelWidth.toDouble()

  override val height: Double
    get() = pixelHeight.toDouble()

  override val memorySize: Long
    get() = rgba.size.toLong() + mask.size.toLong() + HybridMemorySize.OVERHEAD

  override fun dispose() {
    rgba = ByteArray(0)
    mask = ByteArray(0)
  }

  override fun toMaskBuffer(): Promise<ArrayBuffer> {
    if (!hasMask) {
      return Promise.rejected(
        RuntimeException("No mask retained. Pass retainMask: true to removeBackground."),
      )
    }
    val maskCopy = mask
    return Promise.async {
      ArrayBuffer.copy(maskCopy)
    }
  }

  override fun toArrayBuffer(): Promise<ArrayBuffer> {
    val rgbaCopy = rgba
    return Promise.async {
      ArrayBuffer.copy(rgbaCopy)
    }
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
