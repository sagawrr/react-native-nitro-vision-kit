import Foundation
import CoreImage
import NitroModules

final class HybridVisionKit: HybridVisionKitFactorySpec {
  var capabilities: VisionCapabilities {
    let supported = VisionAvailability.supportsBackgroundRemoval
    return VisionCapabilities(
      supportsBackgroundRemoval: supported,
      backgroundRemovalUnavailableReason: supported ? nil : VisionAvailability.backgroundRemovalUnavailableReason,
      supportsImageClassification: VisionAvailability.supportsImageClassification,
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

  func analyzeImage(path: String, options: AnalyzeImageOptions) throws -> Promise<ImageAnalysisResult> {
    try VisionKitOptions.requireAnalyzeOperations(options)
    if options.removeBackground != nil {
      try VisionAvailability.requireBackgroundRemoval()
    }
    return Promise.parallel(VisionKitQueue.queue) {
      try ImageAnalyzer.analyze(path: path, options: options)
    }
  }
}
