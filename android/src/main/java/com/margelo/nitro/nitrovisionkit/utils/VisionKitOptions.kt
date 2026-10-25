package com.margelo.nitro.nitrovisionkit

internal object VisionKitOptions {
  fun trim(options: BackgroundRemovalOptions?) = options?.trim ?: true

  fun retainMask(options: BackgroundRemovalOptions?) = options?.retainMask ?: false

  fun segmentMaxPixels(options: BackgroundRemovalOptions?): Int {
    val requested = options?.maxPixels
    if (requested == null || !requested.isFinite()) return VisionKitLimits.DEFAULT_MAX_PIXELS
    return requested.toInt().coerceIn(1, VisionKitLimits.MAX_SEGMENT_PIXELS)
  }

  fun maxResults(options: ClassificationOptions?): Int {
    val requested = options?.maxResults
    if (requested == null || !requested.isFinite()) return 0
    return requested.toInt().coerceIn(0, VisionKitLimits.MAX_CLASSIFICATION_RESULTS)
  }

  fun minConfidence(options: ClassificationOptions?): Float {
    val requested = options?.minConfidence
    if (requested == null || !requested.isFinite()) return 0.5f
    return requested.toFloat().coerceIn(0f, 1f)
  }

  fun requireAnalyzeOperations(options: AnalyzeImageOptions) {
    if (options.removeBackground == null && options.classify == null && options.readText == null) {
      throw RuntimeException("analyzeImage requires removeBackground, classify, and/or readText options.")
    }
  }
}
