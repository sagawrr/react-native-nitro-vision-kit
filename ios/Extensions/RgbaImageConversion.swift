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
    context.translateBy(x: 0, y: CGFloat(height))
    context.scaleBy(x: 1, y: -1)
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
}
