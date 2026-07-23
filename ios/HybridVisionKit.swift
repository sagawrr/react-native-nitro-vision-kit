import Foundation
import NitroModules

final class HybridVisionKit: HybridVisionKitFactorySpec {
  var capabilities: VisionCapabilities {
    let supported = VisionAvailability.supportsBackgroundRemoval
    return VisionCapabilities(
      supportsBackgroundRemoval: supported,
      backgroundRemovalUnavailableReason: supported ? nil : VisionAvailability.backgroundRemovalUnavailableReason,
      supportsImageClassification: VisionAvailability.supportsImageClassification,
      supportsTextRecognition: VisionAvailability.supportsTextRecognition,
      supportedTextLanguages: TextRecognizer.supportedLanguages(),
    )
  }

  func removeBackground(path: String, options: BackgroundRemovalOptions?) throws -> Promise<any HybridSegmentationResultSpec> {
    try VisionAvailability.requireBackgroundRemoval()
    let trim = VisionKitOptions.trim(options)
    let retainMask = VisionKitOptions.retainMask(options)
    let maxPixels = VisionKitOptions.segmentMaxPixels(options)
    return Promise.parallel(VisionKitQueue.queue) {
      let ciImage = try ImageLoader.loadCIImage(path: path, maxPixels: maxPixels)
      let output = try SubjectSegmenter.segment(ciImage: ciImage, trim: trim, retainMask: retainMask)
      return HybridSegmentationResult(output: output) as any HybridSegmentationResultSpec
    }
  }

  func classifyImage(path: String, options: ClassificationOptions?) throws -> Promise<[Classification]> {
    let maxResults = VisionKitOptions.maxResults(options)
    let minConfidence = VisionKitOptions.minConfidence(options)
    let region = options?.region
    return Promise.parallel(VisionKitQueue.queue) {
      let ciImage = try ImageLoader.loadCIImage(path: path, maxPixels: VisionKitLimits.labelingMaxPixels)
      return try ImageClassifier.classify(
        ciImage: ciImage,
        maxResults: maxResults,
        minConfidence: minConfidence,
        region: region,
      )
    }
  }

  func readText(path: String, options: TextRecognitionOptions?) throws -> Promise<any HybridTextRecognitionResultSpec> {
    try VisionAvailability.requireTextRecognition()
    let languages = options?.languages
    let recognitionLevel = options?.recognitionLevel
    let region = options?.region
    let minTextHeightFraction = options?.minTextHeightFraction
    let usesLanguageCorrection = options?.usesLanguageCorrection
    let customWords = options?.customWords
    let maxCandidates = options?.maxCandidates
    return Promise.async(.userInitiated) {
      guard #available(iOS 18.0, *) else {
        throw RuntimeError(VisionAvailability.textRecognitionUnavailableReason)
      }
      let ciImage = try ImageLoader.loadCIImage(path: path, maxPixels: VisionKitLimits.textMaxPixels)
      let output = try await TextRecognizer.recognize(
        ciImage: ciImage,
        languages: languages,
        recognitionLevel: recognitionLevel,
        region: region,
        minTextHeightFraction: minTextHeightFraction,
        usesLanguageCorrection: usesLanguageCorrection,
        customWords: customWords,
        maxCandidates: maxCandidates,
      )
      return HybridTextRecognitionResult(output: output) as any HybridTextRecognitionResultSpec
    }
  }

  func analyzeImage(path: String, options: AnalyzeImageOptions) throws -> Promise<ImageAnalysisResult> {
    try VisionKitOptions.requireAnalyzeOperations(options)
    if options.removeBackground != nil {
      try VisionAvailability.requireBackgroundRemoval()
    }
    if options.readText != nil {
      try VisionAvailability.requireTextRecognition()
    }
    return Promise.async(.userInitiated) {
      try await ImageAnalyzer.analyze(path: path, options: options)
    }
  }
}
