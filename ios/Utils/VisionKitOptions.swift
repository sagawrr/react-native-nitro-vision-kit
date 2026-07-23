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
    Int(options?.maxPixels ?? Double(VisionKitLimits.defaultMaxPixels))
  }

  static func maxResults(_ options: ClassificationOptions?) -> Int {
    Int(options?.maxResults ?? 0)
  }

  static func minConfidence(_ options: ClassificationOptions?) -> Double {
    options?.minConfidence ?? 0.5
  }
}
