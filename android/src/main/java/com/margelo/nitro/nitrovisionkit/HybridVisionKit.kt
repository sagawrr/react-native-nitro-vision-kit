package com.margelo.nitro.nitrovisionkit

import androidx.annotation.Keep
import com.facebook.proguard.annotations.DoNotStrip
import com.margelo.nitro.NitroModules
import com.margelo.nitro.core.Promise
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Keep
@DoNotStrip
class HybridVisionKit : HybridVisionKitFactorySpec() {

  private val context
    get() = NitroModules.applicationContext
      ?: throw RuntimeException("No ApplicationContext set!")

  override val capabilities: VisionCapabilities
    get() {
      val supported = VisionAvailability.supportsBackgroundRemoval(context)
      val textSupported = VisionAvailability.supportsTextRecognition(context)
      scheduleModulePrefetch(supported, textSupported)
      return VisionCapabilities(
        supportsBackgroundRemoval = supported,
        backgroundRemovalUnavailableReason = if (supported) {
          null
        } else {
          VisionAvailability.backgroundRemovalUnavailableReason(context)
        },
        supportsImageClassification = VisionAvailability.supportsImageClassification,
        supportsTextRecognition = textSupported,
        supportedTextLanguages = if (textSupported) {
          TextRecognizer.supportedLanguages
        } else {
          emptyArray()
        },
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
        val output = SubjectSegmenter.segment(context, loaded, trim, retainMask)
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

  override fun readText(
    path: String,
    options: TextRecognitionOptions?,
  ): Promise<HybridTextRecognitionResultSpec> {
    VisionAvailability.requireTextRecognition(context)
    val languages = options?.languages
    val region = options?.region
    val minTextHeightFraction = options?.minTextHeightFraction
    return Promise.async {
      val loaded = ImageLoader.load(context, path, VisionKitLimits.TEXT_MAX_PIXELS)
      try {
        val output = TextRecognizer.recognize(
          context = context,
          bitmap = loaded,
          languages = languages,
          region = region,
          minTextHeightFraction = minTextHeightFraction,
        )
        HybridTextRecognitionResult(output)
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
    if (options.readText != null) {
      VisionAvailability.requireTextRecognition(context)
    }
    return Promise.async {
      ImageAnalyzer.analyze(context, path, options)
    }
  }

  private fun scheduleModulePrefetch(lift: Boolean, text: Boolean) {
    if (!lift && !text) return
    if (!prefetchScheduled.compareAndSet(false, true)) return
    val appContext = context.applicationContext
    prefetchExecutor.execute {
      try {
        if (lift) {
          MlKitModuleInstaller.prefetch(appContext, SubjectSegmenter.optionalApi())
        }
        if (text) {
          val textApis = TextRecognitionScript.entries
            .map { TextRecognizer.optionalApi(it) }
            .toTypedArray()
          MlKitModuleInstaller.prefetch(appContext, *textApis)
        }
      } catch (_: Exception) {
      }
    }
  }

  private companion object {
    private val prefetchScheduled = AtomicBoolean(false)
    private val prefetchExecutor = Executors.newSingleThreadExecutor { runnable ->
      Thread(runnable, "nitro-vision-kit-prefetch").apply { isDaemon = true }
    }
  }
}
