import Foundation

/// Reports which on-device vision features the current iOS version supports.
enum VisionAvailability {
  /// Vision subject lifting (`VNGenerateForegroundInstanceMaskRequest`) needs
  /// iOS 17+. Used to populate `VisionCapabilities`.
  static var supportsBackgroundRemoval: Bool {
    if #available(iOS 17.0, *) {
      return true
    }
    return false
  }

  /// Vision image classification (`VNClassifyImageRequest`) is available on all
  /// supported iOS versions.
  static let supportsImageClassification = true
}
