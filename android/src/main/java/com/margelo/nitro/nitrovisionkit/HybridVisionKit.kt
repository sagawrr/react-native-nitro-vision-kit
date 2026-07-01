package com.margelo.nitro.nitrovisionkit

import androidx.annotation.Keep
import com.facebook.proguard.annotations.DoNotStrip
import com.margelo.nitro.NitroModules
import com.margelo.nitro.core.Promise

/**
 * Root entry point for on-device vision. Orchestrates [SubjectSegmenter] and
 * [ImageClassifier]; all native work runs off the caller thread inside the
 * returned [Promise]s.
 */
@Keep
@DoNotStrip
class HybridVisionKit : HybridVisionKitFactorySpec() {

  private val context
    get() = NitroModules.applicationContext
      ?: throw Error("No ApplicationContext set!")

  override val capabilities: VisionCapabilities =
    VisionCapabilities(
      supportsBackgroundRemoval = true,
      supportsImageClassification = true,
    )

  override fun removeBackground(
    path: String,
    options: BackgroundRemovalOptions?,
  ): Promise<HybridSegmentationResultSpec> {
    val trim = options?.trim ?: true
    val maxPixels = options?.maxPixels?.toInt()?.coerceAtLeast(1) ?: DEFAULT_MAX_PIXELS
    return Promise.async {
      val loaded = ImageLoader.load(context, path, maxPixels)
      try {
        val output = SubjectSegmenter.segment(loaded, trim)
        HybridSegmentationResult(
          rgba = output.pixels,
          pixelWidth = output.width,
          pixelHeight = output.height,
          bounds = output.bounds,
          tempDir = ImageLoader.cacheDir(context),
        )
      } finally {
        loaded.recycle()
      }
    }
  }

  override fun classifyImage(
    path: String,
    options: ClassificationOptions?,
  ): Promise<Array<Classification>> {
    val maxResults = options?.maxResults?.toInt() ?: 0
    val minConfidence = options?.minConfidence?.toFloat() ?: 0.5f
    return Promise.async {
      val loaded = ImageLoader.load(context, path, LABELING_MAX_PIXELS)
      try {
        ImageClassifier.classify(loaded, maxResults, minConfidence).toTypedArray()
      } finally {
        loaded.recycle()
      }
    }
  }

  private companion object {
    // ML Kit's mask is upsampled from an internal ~512px model, so capping input
    // trades no mask detail for memory. Tunable via BackgroundRemovalOptions.maxPixels.
    const val DEFAULT_MAX_PIXELS = 6_000_000
    const val LABELING_MAX_PIXELS = 1_000_000
  }
}
