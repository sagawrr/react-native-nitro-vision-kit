import Foundation
import CoreImage
import CoreImage.CIFilterBuiltins
import Vision
import NitroModules

/// Runs a Vision `VNGenerateForegroundInstanceMaskRequest` (iOS 17+) and
/// composites the resulting matte onto the source image, preserving the soft
/// mask instead of a binary cut. Output is premultiplied RGBA to match the
/// `react-native-nitro-image` `loadFromRawPixelData` contract.
enum SubjectSegmenter {
  /// Segments `ciImage`, returning a [SegmentationOutput]. The image is first
  /// downscaled so its pixel count stays at or below `maxPixels`. When `trim`
  /// is true the returned pixels are cropped to the subject's bounding box;
  /// `bounds` always describes the subject in the pre-trim image.
  static func segment(ciImage: CIImage, maxPixels: Int, trim: Bool) throws -> SegmentationOutput {
    guard #available(iOS 17.0, *) else {
      throw RuntimeError("Background removal requires iOS 17 or newer.")
    }

    let scaled = downscaleIfNeeded(ciImage, maxPixels: maxPixels)
    let width = Int(scaled.extent.width.rounded())
    let height = Int(scaled.extent.height.rounded())
    if width <= 0 || height <= 0 {
      throw RuntimeError("Image has no readable pixels.")
    }

    let handler = VNImageRequestHandler(ciImage: scaled, orientation: .up)
    let request = VNGenerateForegroundInstanceMaskRequest()
    try handler.perform([request])

    guard let observation = request.results?.first else {
      throw RuntimeError("No foreground subject detected.")
    }

    let maskBuffer = try observation.generateScaledMaskForImage(
      forInstances: observation.allInstances,
      from: handler,
    )
    let maskImage = CIImage(cvPixelBuffer: maskBuffer)

    let blend = CIFilter.blendWithMask()
    blend.inputImage = scaled
    blend.maskImage = maskImage
    blend.backgroundImage = CIImage.empty()
    guard let composited = blend.outputImage else {
      throw RuntimeError("Failed to composite subject mask.")
    }

    guard
      let cgImage = CIContext().createCGImage(composited, from: CGRect(origin: .zero, size: scaled.extent.size))
    else {
      throw RuntimeError("Failed to render masked image.")
    }

    // Core Graphics renders premultiplied-last (RGBA) — exactly what nitro-image wants.
    let full = premultipliedRgbaBytes(from: cgImage, width: width, height: height)
    guard let subjectRect = boundsOfAlpha(full, width: width, height: height) else {
      throw RuntimeError("No foreground subject detected.")
    }

    let bounds = Rect(
      x: Double(subjectRect.minX) / Double(width),
      y: Double(subjectRect.minY) / Double(height),
      width: Double(subjectRect.maxX - subjectRect.minX + 1) / Double(width),
      height: Double(subjectRect.maxY - subjectRect.minY + 1) / Double(height),
    )

    guard trim else {
      return SegmentationOutput(rgba: Data(full), width: width, height: height, bounds: bounds)
    }

    let pad = 2
    let cropX = max(0, subjectRect.minX - pad)
    let cropY = max(0, subjectRect.minY - pad)
    let cropRight = min(width - 1, subjectRect.maxX + pad)
    let cropBottom = min(height - 1, subjectRect.maxY + pad)
    let cropped = cropRgba(
      full, width: width,
      x: cropX, y: cropY, right: cropRight, bottom: cropBottom
    )
    return SegmentationOutput(
      rgba: Data(cropped),
      width: cropRight - cropX + 1,
      height: cropBottom - cropY + 1,
      bounds: bounds,
    )
  }

  private static func downscaleIfNeeded(_ image: CIImage, maxPixels: Int) -> CIImage {
    let w = Double(image.extent.width)
    let h = Double(image.extent.height)
    let total = w * h
    if total <= Double(maxPixels) {
      return image
    }
    let scale = (Double(maxPixels) / total).squareRoot()
    return image.transformed(by: CGAffineTransform(scaleX: scale, y: scale))
  }

  /// Renders `cgImage` into a premultiplied RGBA byte array, row 0 = top.
  private static func premultipliedRgbaBytes(from cgImage: CGImage, width: Int, height: Int) -> [UInt8] {
    var bytes = [UInt8](repeating: 0, count: width * height * 4)
    guard let context = CGContext(
      data: &bytes,
      width: width,
      height: height,
      bitsPerComponent: 8,
      bytesPerRow: width * 4,
      space: CGColorSpaceCreateDeviceRGB(),
      bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
    ) else {
      return bytes
    }
    // CGContext origin is bottom-left; flip so the first row is the image's top.
    context.translateBy(x: 0, y: CGFloat(height))
    context.scaleBy(x: 1, y: -1)
    context.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))
    return bytes
  }

  private static func boundsOfAlpha(_ bytes: [UInt8], width: Int, height: Int) -> (minX: Int, minY: Int, maxX: Int, maxY: Int)? {
    var minX = width
    var minY = height
    var maxX = -1
    var maxY = -1
    for y in 0..<height {
      for x in 0..<width {
        let alpha = bytes[(y * width + x) * 4 + 3]
        if alpha > 0 {
          if x < minX { minX = x }
          if y < minY { minY = y }
          if x > maxX { maxX = x }
          if y > maxY { maxY = y }
        }
      }
    }
    guard maxX >= 0 else { return nil }
    return (minX, minY, maxX, maxY)
  }

  private static func cropRgba(_ bytes: [UInt8], width: Int, x: Int, y: Int, right: Int, bottom: Int) -> [UInt8] {
    let cropW = right - x + 1
    let cropH = bottom - y + 1
    var out = [UInt8](repeating: 0, count: cropW * cropH * 4)
    for row in 0..<cropH {
      let src = ((y + row) * width + x) * 4
      let dst = row * cropW * 4
      out.replaceSubrange(dst..<(dst + cropW * 4), with: bytes[src..<(src + cropW * 4)])
    }
    return out
  }
}
