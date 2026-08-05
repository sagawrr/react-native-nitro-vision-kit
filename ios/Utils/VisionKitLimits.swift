enum VisionKitLimits {
  static let defaultMaxPixels = 6_000_000
  static let labelingMaxPixels = 1_000_000
  /// Decode budget for OCR — enough for letter-size pages (~720×1280+).
  static let textMaxPixels = 4_000_000
  static let maxSegmentPixels = 25_000_000
  static let maxClassificationResults = 50
}
