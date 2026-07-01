import Foundation
import CoreImage
import NitroModules

/// Root entry point for on-device vision. Orchestrates `SubjectSegmenter` and
/// `ImageClassifier`; all native work runs off the caller thread inside the
/// returned `Promise`s. Platform availability comes from `VisionAvailability`,
/// image loading from `ImageLoader`.
final class HybridVisionKit: HybridVisionKitFactorySpec {
  private static let queue = DispatchQueue(label: "com.margelo.nitro.visionkit", qos: .userInitiated)

  var capabilities: VisionCapabilities {
    return VisionCapabilities(
      supportsBackgroundRemoval: VisionAvailability.supportsBackgroundRemoval,
      supportsImageClassification: VisionAvailability.supportsImageClassification,
    )
  }

  func removeBackground(path: String, options: BackgroundRemovalOptions?) throws -> Promise<any HybridSegmentationResultSpec> {
    let trim = options?.trim ?? true
    let maxPixels = Int(options?.maxPixels ?? Double(Self.defaultMaxPixels))
    return Promise.parallel(Self.queue) {
      let ciImage = try ImageLoader.loadCIImage(path: path)
      let output = try SubjectSegmenter.segment(ciImage: ciImage, maxPixels: maxPixels, trim: trim)
      return HybridSegmentationResult(
        rgba: output.rgba,
        width: output.width,
        height: output.height,
        bounds: output.bounds,
      ) as any HybridSegmentationResultSpec
    }
  }

  func classifyImage(path: String, options: ClassificationOptions?) throws -> Promise<[Classification]> {
    let maxResults = Int(options?.maxResults ?? 0)
    let minConfidence = options?.minConfidence ?? 0.5
    return Promise.parallel(Self.queue) {
      let ciImage = try ImageLoader.loadCIImage(path: path)
      return try ImageClassifier.classify(
        ciImage: ciImage,
        maxResults: maxResults,
        minConfidence: minConfidence,
      )
    }
  }

  private static let defaultMaxPixels = 6_000_000
}
