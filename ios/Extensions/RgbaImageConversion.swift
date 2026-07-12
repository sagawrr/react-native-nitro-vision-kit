import Foundation
import CoreGraphics

enum RgbaImageConversion {
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

  static func premultipliedRgbaBytes(from cgImage: CGImage, width: Int, height: Int) -> [UInt8] {
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
    // CGImage / ImageIO use top-left row order. Do not apply a Quartz Y-flip here —
    // that inverts cutouts relative to the source and to React Native's Image display.
    // See CGImagePropertyOrientation / kCGImageSourceCreateThumbnailWithTransform:
    // orientation is baked at load time; this path must preserve upright pixels.
    context.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))
    return bytes
  }

  static func cropRgba(_ bytes: [UInt8], width: Int, x: Int, y: Int, right: Int, bottom: Int) -> [UInt8] {
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

  static func cropFloatMask(_ mask: Data, width: Int, x: Int, y: Int, right: Int, bottom: Int) -> Data {
    let cropW = right - x + 1
    let cropH = bottom - y + 1
    var out = Data(count: cropW * cropH * MemoryLayout<Float32>.size)
    guard !mask.isEmpty else { return out }
    mask.withUnsafeBytes { src in
      out.withUnsafeMutableBytes { dst in
        let srcFloats = src.bindMemory(to: Float32.self)
        let dstFloats = dst.bindMemory(to: Float32.self)
        for row in 0..<cropH {
          let srcRow = (y + row) * width + x
          let dstRow = row * cropW
          for col in 0..<cropW {
            dstFloats[dstRow + col] = srcFloats[srcRow + col]
          }
        }
      }
    }
    return out
  }
}
