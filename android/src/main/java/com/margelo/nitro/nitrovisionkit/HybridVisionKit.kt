package com.margelo.nitro.nitrovisionkit

import androidx.annotation.Keep
import com.facebook.proguard.annotations.DoNotStrip
import com.margelo.nitro.NitroModules
import com.margelo.nitro.core.Promise

@Keep
@DoNotStrip
class HybridVisionKit : HybridVisionKitFactorySpec() {

  private val context
    get() = NitroModules.applicationContext
      ?: throw RuntimeException("No ApplicationContext set!")

  override val capabilities: VisionCapabilities
    get() {
      val supported = VisionAvailability.supportsBackgroundRemoval(context)
      return VisionCapabilities(
        supportsBackgroundRemoval = supported,
        backgroundRemovalUnavailableReason = if (supported) {
          null
        } else {
          VisionAvailability.backgroundRemovalUnavailableReason(context)
        },
        supportsImageClassification = VisionAvailability.supportsImageClassification,
      )
    }

  override fun removeBackground(
    path: String,
    options: BackgroundRemovalOptions?,
  ): Promise<HybridSegmentationResultSpec> {
    VisionAvailability.requireBackgroundRemoval(context)
    val trim = VisionKitOptions.trim(options)
    val retainMask = VisionKitOptions.retainMask(options)
    val maxPixels = VisionKitOptions.segmentMaxPixels(options)
    return Promise.async {
      val loaded = ImageLoader.load(context, path, maxPixels)
      try {
        val output = SubjectSegmenter.segment(loaded, trim, retainMask)
        HybridSegmentationResult(output, ImageLoader.cacheDir(context))
      } finally {
        loaded.recycle()
      }
    }
  }

  override fun classifyImage(
    path: String,
    options: ClassificationOptions?,
  ): Promise<Array<Classification>> {
    val maxResults = VisionKitOptions.maxResults(options)
    val minConfidence = VisionKitOptions.minConfidence(options)
    val region = options?.region
    return Promise.async {
      val loaded = ImageLoader.load(context, path, VisionKitLimits.LABELING_MAX_PIXELS)
      try {
        ImageClassifier.classify(loaded, maxResults, minConfidence, region).toTypedArray()
      } finally {
        loaded.recycle()
      }
    }
  }

  override fun analyzeImage(
    path: String,
    options: AnalyzeImageOptions,
  ): Promise<ImageAnalysisResult> {
    VisionKitOptions.requireAnalyzeOperations(options)
    if (options.removeBackground != null) {
      VisionAvailability.requireBackgroundRemoval(context)
    }
    return Promise.async {
      ImageAnalyzer.analyze(context, path, options)
    }
  }
}
