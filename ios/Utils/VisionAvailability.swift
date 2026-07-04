import Foundation
import NitroModules

enum VisionAvailability {
  static var supportsBackgroundRemoval: Bool {
    if #available(iOS 17.0, *) {
#if targetEnvironment(simulator)
      return false
#else
      return true
#endif
    }
    return false
  }

  static let supportsImageClassification = true

  static var backgroundRemovalUnavailableReason: String {
    if #available(iOS 17.0, *) {
#if targetEnvironment(simulator)
      return "Background removal requires a physical iOS device; the iOS Simulator cannot run Vision foreground masking."
#endif
      return "Background removal is unavailable on this device."
    }
    return "Background removal requires iOS 17 or newer."
  }

  static func requireBackgroundRemoval() throws {
    guard supportsBackgroundRemoval else {
      throw RuntimeError(backgroundRemovalUnavailableReason)
    }
  }
}
