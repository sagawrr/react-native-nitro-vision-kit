package com.margelo.nitro.nitrovisionkit

import android.content.Context

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal object ImageAnalyzer {
  private const val NO_FOREGROUND = "No foreground subject detected."

  suspend fun analyze(context: Context, path: String, options: AnalyzeImageOptions): ImageAnalysisResult {
    val segmentOptions = options.removeBackground
    val classifyOptions = options.classify
    val textOptions = options.readText
    val trim = VisionKitOptions.trim(segmentOptions)
    val retainMask = VisionKitOptions.retainMask(segmentOptions)
    val maxPixels = VisionKitOptions.segmentMaxPixels(segmentOptions)
    val maxResults = VisionKitOptions.maxResults(classifyOptions)
    val minConfidence = VisionKitOptions.minConfidence(classifyOptions)
    var classifyRegion = classifyOptions?.region
    var textRegion = textOptions?.region

    val maxLoad = resolveMaxLoad(
      segmentMaxPixels = if (segmentOptions != null) maxPixels else null,
      classify = classifyOptions != null,
      readText = textOptions != null,
    )
    val loaded = ImageLoader.load(context, path, maxLoad)
    try {
      var segmentation: HybridSegmentationResultSpec? = null
      if (segmentOptions != null) {
        val segmentSource = loaded.downscaleToMaxPixels(maxPixels)
        try {
          try {
            val output = SubjectSegmenter.segment(context, segmentSource, trim, retainMask)
            segmentation = HybridSegmentationResult(output, ImageLoader.cacheDir(context))
            if (classifyRegion == null && classifyOptions != null) {
              classifyRegion = output.bounds
            }
            if (textRegion == null && textOptions != null) {
              textRegion = output.bounds
            }
          } catch (error: Exception) {
            val hasOtherOps = classifyOptions != null || textOptions != null
            if (!hasOtherOps || error.message?.contains(NO_FOREGROUND) != true) {
              throw error
            }
          }
        } finally {
          if (segmentSource !== loaded) segmentSource.recycle()
        }
      }

      var classifications: Array<Classification>? = null
      var text: HybridTextRecognitionResultSpec? = null

      if (classifyOptions != null && textOptions != null) {
        // ML Kit reads pixels off the calling thread. Each task gets its own copy.
        val labelBitmap = loaded.immutableArgbCopy()
        val textBitmap = loaded.immutableArgbCopy()
        try {
          coroutineScope {
            val labelsDeferred = async {
              ImageClassifier.classify(
                labelBitmap,
                maxResults,
                minConfidence,
                classifyRegion,
              ).toTypedArray()
            }
            val textDeferred = async {
              val output = TextRecognizer.recognize(
                context = context,
                bitmap = textBitmap,
                languages = textOptions.languages,
                region = textRegion,
                minTextHeightFraction = textOptions.minTextHeightFraction,
              )
              HybridTextRecognitionResult(output)
            }
            classifications = labelsDeferred.await()
            text = textDeferred.await()
          }
        } finally {
          labelBitmap.recycle()
          textBitmap.recycle()
        }
      } else if (classifyOptions != null) {
        classifications = ImageClassifier.classify(
          loaded,
          maxResults,
          minConfidence,
          classifyRegion,
        ).toTypedArray()
      } else if (textOptions != null) {
        val output = TextRecognizer.recognize(
          context = context,
          bitmap = loaded,
          languages = textOptions.languages,
          region = textRegion,
          minTextHeightFraction = textOptions.minTextHeightFraction,
        )
        text = HybridTextRecognitionResult(output)
      }

      return ImageAnalysisResult(
        segmentation = segmentation,
        classifications = classifications,
        text = text,
      )
    } finally {
      loaded.recycle()
    }
  }

  private fun resolveMaxLoad(
    segmentMaxPixels: Int?,
    classify: Boolean,
    readText: Boolean,
  ): Int {
    var maxLoad = 0
    if (segmentMaxPixels != null) {
      maxLoad = maxOf(maxLoad, segmentMaxPixels)
    }
    if (classify) {
      maxLoad = maxOf(maxLoad, VisionKitLimits.LABELING_MAX_PIXELS)
    }
    if (readText) {
      maxLoad = maxOf(maxLoad, VisionKitLimits.TEXT_MAX_PIXELS)
    }
    return if (maxLoad > 0) maxLoad else VisionKitLimits.DEFAULT_MAX_PIXELS
  }
}
