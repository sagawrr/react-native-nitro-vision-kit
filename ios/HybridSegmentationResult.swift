import Foundation
import CoreGraphics
import ImageIO
import UniformTypeIdentifiers
import UIKit
import NitroModules

/// Holds the masked subject pixels produced by a segmentation. The native byte
/// buffer stays on the native side and is accessed lazily through
/// `toArrayBuffer` (zero-copy, premultiplied RGBA) or `saveToTemporaryFile`
/// (encoded file), so the caller only pays for the conversion they need.
///
/// The pixels are premultiplied RGBA so they can be handed straight to
/// `react-native-nitro-image`'s `loadFromRawPixelData` without re-encoding.
final class HybridSegmentationResult: HybridSegmentationResultSpec {
  private let rgba: Data
  private let pixelWidth: Int
  private let pixelHeight: Int

  init(rgba: Data, width: Int, height: Int, bounds: Rect) {
    self.rgba = rgba
    self.pixelWidth = width
    self.pixelHeight = height
    self.bounds = bounds
    super.init()
  }

  let bounds: Rect

  var width: Double { Double(pixelWidth) }
  var height: Double { Double(pixelHeight) }

  /// Bytes owned by this HybridObject; reported so the JS VM can reclaim it
  /// under memory pressure.
  var memorySize: Int {
    return rgba.count + 128
  }

  func toArrayBuffer() throws -> ArrayBuffer {
    return try ArrayBuffer.copy(data: rgba)
  }

  func saveToTemporaryFile(format: ImageFormat, quality: Double) throws -> Promise<String> {
    let qualityClamped = min(100, max(0, Int(quality.rounded())))
    let width = pixelWidth
    let height = pixelHeight
    let bytes = rgba
    let formatCopy = format
    return Promise.async {
      guard let cgImage = RgbaImageConversion.cgImage(fromPremultipliedRgba: bytes, width: width, height: height) else {
        throw RuntimeError("Failed to build image from masked pixels.")
      }
      let ext = formatCopy == .png ? "png" : "jpg"
      let utType = formatCopy == .png ? UTType.png : UTType.jpeg
      let url = FileManager.default.temporaryDirectory
        .appendingPathComponent("visionkit-\(UUID().uuidString).\(ext)")
      guard let destination = CGImageDestinationCreateWithURL(url as CFURL, utType.identifier as CFString, 1, nil) else {
        throw RuntimeError("Failed to create image destination.")
      }
      let options: [CFString: Any] = [
        kCGImageDestinationLossyCompressionQuality: Double(qualityClamped) / 100.0,
      ]
      CGImageDestinationAddImage(destination, cgImage, options as CFDictionary)
      guard CGImageDestinationFinalize(destination) else {
        throw RuntimeError("Failed to encode image.")
      }
      return url.path
    }
  }
}
