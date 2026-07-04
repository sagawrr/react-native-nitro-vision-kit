import Foundation
import CoreVideo
import NitroModules

enum MaskMetrics {
  static let threshold: Float = 0.5

  static func analyze(
    pixelBuffer: CVPixelBuffer,
    width: Int,
    height: Int,
    retainMask: Bool,
  ) throws -> (coverage: Double, centroid: NormalizedPoint, pixelBounds: PixelRect?, mask: Data) {
    CVPixelBufferLockBaseAddress(pixelBuffer, .readOnly)
    defer { CVPixelBufferUnlockBaseAddress(pixelBuffer, .readOnly) }

    guard let base = CVPixelBufferGetBaseAddress(pixelBuffer) else {
      throw RuntimeError("Mask buffer has no base address.")
    }

    let bytesPerRow = CVPixelBufferGetBytesPerRow(pixelBuffer)
    let rowFloats = bytesPerRow / MemoryLayout<Float32>.size
    let src = base.assumingMemoryBound(to: Float32.self)
    let count = width * height
    var mask = retainMask ? Data(count: count * MemoryLayout<Float32>.size) : Data()

    var foreground = 0
    var sumX = 0.0
    var sumY = 0.0
    var minX = width
    var minY = height
    var maxX = -1
    var maxY = -1

    if retainMask {
      try mask.withUnsafeMutableBytes { raw in
        guard let destBase = raw.baseAddress?.assumingMemoryBound(to: Float32.self) else {
          throw RuntimeError("Mask buffer has no writable address.")
        }
        scanMask(
          src: src,
          destBase: destBase,
          rowFloats: rowFloats,
          width: width,
          height: height,
          retainMask: true,
          foreground: &foreground,
          sumX: &sumX,
          sumY: &sumY,
          minX: &minX,
          minY: &minY,
          maxX: &maxX,
          maxY: &maxY,
        )
      }
    } else {
      scanMask(
        src: src,
        destBase: nil,
        rowFloats: rowFloats,
        width: width,
        height: height,
        retainMask: false,
        foreground: &foreground,
        sumX: &sumX,
        sumY: &sumY,
        minX: &minX,
        minY: &minY,
        maxX: &maxX,
        maxY: &maxY,
      )
    }

    guard foreground > 0, maxX >= 0 else {
      return (0, NormalizedPoint(x: 0.5, y: 0.5), nil, mask)
    }

    let coverage = Double(foreground) / Double(count)
    let centroid = NormalizedPoint(
      x: (sumX / Double(foreground) + 0.5) / Double(width),
      y: (sumY / Double(foreground) + 0.5) / Double(height),
    )
    let pixelBounds = PixelRect(
      x: Double(minX),
      y: Double(minY),
      width: Double(maxX - minX + 1),
      height: Double(maxY - minY + 1),
    )
    return (coverage, centroid, pixelBounds, mask)
  }

  private static func scanMask(
    src: UnsafePointer<Float32>,
    destBase: UnsafeMutablePointer<Float32>?,
    rowFloats: Int,
    width: Int,
    height: Int,
    retainMask: Bool,
    foreground: inout Int,
    sumX: inout Double,
    sumY: inout Double,
    minX: inout Int,
    minY: inout Int,
    maxX: inout Int,
    maxY: inout Int,
  ) {
    for y in 0..<height {
      let srcRow = src.advanced(by: y * rowFloats)
      let destRow = retainMask ? destBase!.advanced(by: y * width) : nil
      for x in 0..<width {
        let confidence = srcRow[x]
        if retainMask {
          destRow![x] = confidence
        }
        if confidence > threshold {
          foreground += 1
          sumX += Double(x)
          sumY += Double(y)
          if x < minX { minX = x }
          if y < minY { minY = y }
          if x > maxX { maxX = x }
          if y > maxY { maxY = y }
        }
      }
    }
  }
}
