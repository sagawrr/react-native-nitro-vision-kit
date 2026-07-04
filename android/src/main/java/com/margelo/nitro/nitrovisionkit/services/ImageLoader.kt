package com.margelo.nitro.nitrovisionkit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.File
import kotlin.math.sqrt

/**
 * Loads and resolution-caps images from a filesystem path or `file://`/`content://` URI.
 *
 * The [maxPixels] cap bounds peak memory (raw decode + ML Kit intermediate buffers).
 * ML Kit's mask is upsampled from an internal ~512px model, so capping the input
 * trades no mask detail for a large memory/time win.
 */
internal object ImageLoader {
  /**
   * Loads [path] as a software `ARGB_8888` [Bitmap], downscaling so the pixel
   * count does not exceed [maxPixels]. Software-allocated to avoid GPU/ashmem
   * pressure during pixel reads.
   */
  fun load(context: Context, path: String, maxPixels: Int): Bitmap {
    val uri = toUri(path)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      val source = ImageDecoder.createSource(context.contentResolver, uri)
      val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val total = info.size.width.toLong() * info.size.height.toLong()
        if (total > maxPixels) {
          val scale = sqrt(total.toDouble() / maxPixels.toDouble())
          decoder.setTargetSize(
            (info.size.width / scale).toInt().coerceAtLeast(1),
            (info.size.height / scale).toInt().coerceAtLeast(1),
          )
        }
        decoder.isMutableRequired = true
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
      }
      return decoded.copy(Bitmap.Config.ARGB_8888, false)
        ?: throw RuntimeException("Failed to copy decoded image to ARGB_8888.")
    }
    return decodeLegacy(context, uri, maxPixels)
  }

  /** Cache directory used for [HybridSegmentationResult] temp-file output. */
  fun cacheDir(context: Context): File = context.cacheDir

  private fun toUri(path: String): Uri {
    return if (path.startsWith("file://") || path.startsWith("content://")) {
      Uri.parse(path)
    } else {
      Uri.fromFile(File(path))
    }
  }

  private fun decodeLegacy(context: Context, uri: Uri, maxPixels: Int): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val rawW = bounds.outWidth
    val rawH = bounds.outHeight
    var sampleSize = 1
    if (rawW > 0 && rawH > 0) {
      val total = rawW.toLong() * rawH.toLong()
      if (total > maxPixels) {
        val ratio = sqrt(total.toDouble() / maxPixels.toDouble()).coerceAtLeast(1.0)
        while ((rawW / (sampleSize * 2)) * (rawH / (sampleSize * 2)) > maxPixels && sampleSize * 2 <= ratio) {
          sampleSize *= 2
        }
      }
    }
    val opts = BitmapFactory.Options().apply {
      inPreferredConfig = Bitmap.Config.ARGB_8888
      inSampleSize = sampleSize
    }
    return context.contentResolver.openInputStream(uri)?.use {
      BitmapFactory.decodeStream(it, null, opts)
    } ?: throw RuntimeException("Failed to decode image: $uri")
  }
}
