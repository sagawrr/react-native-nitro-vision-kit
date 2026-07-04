package com.margelo.nitro.nitrovisionkit

internal object VisionKitOptions {
  fun trim(options: BackgroundRemovalOptions?) = options?.trim ?: true

  fun retainMask(options: BackgroundRemovalOptions?) = options?.retainMask ?: false

  fun segmentMaxPixels(options: BackgroundRemovalOptions?) =
    options?.maxPixels?.toInt()?.coerceAtLeast(1) ?: VisionKitLimits.DEFAULT_MAX_PIXELS

  fun maxResults(options: ClassificationOptions?) = options?.maxResults?.toInt() ?: 0

  fun minConfidence(options: ClassificationOptions?) = options?.minConfidence?.toFloat() ?: 0.5f

  fun requireAnalyzeOperations(options: AnalyzeImageOptions) {
    if (options.removeBackground == null && options.classify == null) {
      throw RuntimeException("analyzeImage requires removeBackground and/or classify options.")
    }
  }
}
