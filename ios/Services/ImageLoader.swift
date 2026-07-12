import Foundation
import CoreImage
import ImageIO
import NitroModules

enum ImageLoader {
  /// Loads an image as an upright `CIImage`, downscaled so width×height ≤ `maxPixels`.
  ///
  /// Orientation follows ImageIO / Vision guidance:
  /// - Read EXIF/`kCGImagePropertyOrientation`
  /// - Bake it into pixel data via `kCGImageSourceCreateThumbnailWithTransform`
  /// - Callers pass `.up` to `VNImageRequestHandler` (pixels already match display)
  ///
  /// See: [CGImagePropertyOrientation](https://developer.apple.com/documentation/imageio/cgimagepropertyorientation),
  /// [kCGImageSourceCreateThumbnailWithTransform](https://developer.apple.com/documentation/imageio/kcgimagesourcecreatethumbnailwithtransform).
  static func loadCIImage(path: String, maxPixels: Int) throws -> CIImage {
    let filePath = path.hasPrefix("file://") ? String(path.dropFirst("file://".count)) : path
    let url = URL(fileURLWithPath: filePath)
    guard let source = CGImageSourceCreateWithURL(url as CFURL, nil) else {
      throw RuntimeError("Failed to load image at path: \(path)")
    }

    let maxDimension = thumbnailMaxPixelSize(source: source, maxPixels: maxPixels)
    let options: [CFString: Any] = [
      kCGImageSourceCreateThumbnailFromImageAlways: true,
      kCGImageSourceCreateThumbnailWithTransform: true,
      kCGImageSourceThumbnailMaxPixelSize: maxDimension,
    ]
    guard let cgImage = CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary) else {
      throw RuntimeError("Failed to load image at path: \(path)")
    }
    return CIImage(cgImage: cgImage)
  }

  private static func thumbnailMaxPixelSize(source: CGImageSource, maxPixels: Int) -> Int {
    guard
      let props = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any],
      let width = props[kCGImagePropertyPixelWidth] as? CGFloat,
      let height = props[kCGImagePropertyPixelHeight] as? CGFloat,
      width > 0, height > 0
    else {
      return Int(sqrt(Double(maxPixels)))
    }

    // PixelWidth/Height are the stored buffer size (pre-orientation). For 90°/270°
    // EXIF orientations the displayed size swaps; use oriented dimensions for the cap.
    let orientation = cgOrientation(from: props)
    let (displayW, displayH) = orientedSize(width: width, height: height, orientation: orientation)
    let longest = max(displayW, displayH)
    let total = displayW * displayH
    if total <= CGFloat(maxPixels) {
      return Int(longest.rounded(.up))
    }
    let scale = sqrt(Double(maxPixels) / Double(total))
    return max(1, Int(ceil(Double(longest) * scale)))
  }

  private static func cgOrientation(from props: [CFString: Any]) -> CGImagePropertyOrientation {
    if let raw = props[kCGImagePropertyOrientation] as? UInt32,
       let value = CGImagePropertyOrientation(rawValue: raw) {
      return value
    }
    if let raw = props[kCGImagePropertyOrientation] as? Int,
       let value = CGImagePropertyOrientation(rawValue: UInt32(raw)) {
      return value
    }
    return .up
  }

  private static func orientedSize(
    width: CGFloat,
    height: CGFloat,
    orientation: CGImagePropertyOrientation
  ) -> (CGFloat, CGFloat) {
    switch orientation {
    case .left, .leftMirrored, .right, .rightMirrored:
      return (height, width)
    default:
      return (width, height)
    }
  }
}
