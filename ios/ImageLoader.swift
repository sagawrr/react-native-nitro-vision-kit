import Foundation
import CoreImage
import UIKit
import NitroModules

/// Loads images from disk and caps their resolution for vision processing.
enum ImageLoader {
  /// Loads a `CIImage` from a filesystem path or a `file://` URI.
  static func loadCIImage(path: String) throws -> CIImage {
    let filePath = path.hasPrefix("file://") ? String(path.dropFirst("file://".count)) : path
    guard let uiImage = UIImage(contentsOfFile: filePath), let cgImage = uiImage.cgImage else {
      throw RuntimeError("Failed to load image at path: \(path)")
    }
    return CIImage(cgImage: cgImage)
  }
}
