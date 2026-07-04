import Foundation
import CoreImage
import ImageIO
import NitroModules

enum ImageLoader {
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
    let longest = max(width, height)
    let total = width * height
    if total <= CGFloat(maxPixels) {
      return Int(longest.rounded(.up))
    }
    let scale = sqrt(Double(maxPixels) / Double(total))
    return max(1, Int(ceil(Double(longest) * scale)))
  }
}
