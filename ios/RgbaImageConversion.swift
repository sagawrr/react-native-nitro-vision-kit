import Foundation
import CoreGraphics

/// Converts premultiplied RGBA byte buffers to Core Graphics image types for
/// file encoding. Kept separate from the `HybridSegmentationResult` so the
/// HybridObject file stays focused on its spec contract.
enum RgbaImageConversion {
  /// Builds a premultiplied-RGBA `CGImage` (alpha last) from [rgba] bytes.
  static func cgImage(fromPremultipliedRgba rgba: Data, width: Int, height: Int) -> CGImage? {
    guard let provider = CGDataProvider(data: rgba as CFData) else { return nil }
    return CGImage(
      width: width,
      height: height,
      bitsPerComponent: 8,
      bitsPerPixel: 32,
      bytesPerRow: width * 4,
      space: CGColorSpaceCreateDeviceRGB(),
      bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.premultipliedLast.rawValue),
      provider: provider,
      decode: nil,
      shouldInterpolate: false,
      intent: .defaultIntent
    )
  }
}
