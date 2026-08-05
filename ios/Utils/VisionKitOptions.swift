import Foundation
import NitroModules

enum VisionKitOptions {
  static func requireAnalyzeOperations(_ options: AnalyzeImageOptions) throws {
    guard options.removeBackground != nil || options.classify != nil || options.readText != nil else {
      throw RuntimeError("analyzeImage requires removeBackground, classify, and/or readText options.")
    }
  }

  static func trim(_ options: BackgroundRemovalOptions?) -> Bool {
    options?.trim ?? true
  }

  static func retainMask(_ options: BackgroundRemovalOptions?) -> Bool {
    options?.retainMask ?? false
  }

  static func segmentMaxPixels(_ options: BackgroundRemovalOptions?) -> Int {
    let requested = options?.maxPixels ?? Double(VisionKitLimits.defaultMaxPixels)
    guard requested.isFinite else { return VisionKitLimits.defaultMaxPixels }
    let bounded = min(max(requested, 1), Double(VisionKitLimits.maxSegmentPixels))
    return Int(bounded)
  }

  static func maxResults(_ options: ClassificationOptions?) -> Int {
    let requested = options?.maxResults ?? 0
    guard requested.isFinite else { return 0 }
    return Int(min(max(requested, 0), Double(VisionKitLimits.maxClassificationResults)))
  }

  static func minConfidence(_ options: ClassificationOptions?) -> Double {
    guard let c = options?.minConfidence, c.isFinite, c >= 0, c <= 1 else { return 0.5 }
    return c
  }
}
