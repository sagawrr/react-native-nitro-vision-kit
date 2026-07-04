package com.margelo.nitro.nitrovisionkit

import android.content.Context

internal object ImageAnalyzer {
  suspend fun analyze(context: Context, path: String, options: AnalyzeImageOptions): ImageAnalysisResult {
    val segmentOptions = options.removeBackground
    val classifyOptions = options.classify
    val trim = VisionKitOptions.trim(segmentOptions)
    val retainMask = VisionKitOptions.retainMask(segmentOptions)
    val maxPixels = VisionKitOptions.segmentMaxPixels(segmentOptions)
    val maxResults = VisionKitOptions.maxResults(classifyOptions)
    val minConfidence = VisionKitOptions.minConfidence(classifyOptions)
    var region = classifyOptions?.region

    val maxLoad = if (segmentOptions != null) maxPixels else VisionKitLimits.LABELING_MAX_PIXELS
    val loaded = ImageLoader.load(context, path, maxLoad)
    try {
      var segmentation: HybridSegmentationResultSpec? = null
      if (segmentOptions != null) {
        val output = SubjectSegmenter.segment(loaded, trim, retainMask)
        segmentation = HybridSegmentationResult(output, ImageLoader.cacheDir(context))
        if (region == null && classifyOptions != null) {
          region = output.bounds
        }
      }

      var classifications: Array<Classification>? = null
      if (classifyOptions != null) {
        classifications = ImageClassifier.classify(
          loaded,
          maxResults,
          minConfidence,
          region,
        ).toTypedArray()
      }

      return ImageAnalysisResult(
        segmentation = segmentation,
        classifications = classifications,
      )
    } finally {
      loaded.recycle()
    }
  }
}
