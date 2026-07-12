import Foundation
import CoreGraphics
import ImageIO
import UniformTypeIdentifiers
import NitroModules

final class HybridSegmentationResult: HybridSegmentationResultSpec {
  private var rgba: Data
  private var mask: Data
  private let pixelWidth: Int
  private let pixelHeight: Int

  init(output: SegmentationOutput) {
    self.rgba = output.rgba
    self.pixelWidth = output.width
    self.pixelHeight = output.height
    self.bounds = output.bounds
    self.sourceWidth = Double(output.sourceWidth)
    self.sourceHeight = Double(output.sourceHeight)
    self.foregroundCoverage = output.foregroundCoverage
    self.centroid = output.centroid
    self.instanceCount = output.instanceCount
    self.pixelBounds = output.pixelBounds
    self.trimOrigin = output.trimOrigin
    self.mask = output.mask
    self.hasMask = output.hasMask
    super.init()
  }

  let bounds: Rect
  let sourceWidth: Double
  let sourceHeight: Double
  let foregroundCoverage: Double
  let centroid: NormalizedPoint
  let instanceCount: Double
  let pixelBounds: PixelRect
  let trimOrigin: NormalizedPoint
  let hasMask: Bool

  var width: Double { Double(pixelWidth) }
  var height: Double { Double(pixelHeight) }

  var memorySize: Int {
    rgba.count + mask.count + HybridMemorySize.overhead
  }

  func dispose() {
    rgba = Data()
    mask = Data()
  }

  func toMaskBuffer() throws -> Promise<ArrayBuffer> {
    guard hasMask else {
      throw RuntimeError("No mask retained. Pass retainMask: true to removeBackground.")
    }
    let maskCopy = mask
    return Promise.parallel(VisionKitQueue.queue) {
      try ArrayBuffer.copy(data: maskCopy)
    }
  }

  func toArrayBuffer() throws -> Promise<ArrayBuffer> {
    let rgbaCopy = rgba
    return Promise.parallel(VisionKitQueue.queue) {
      try ArrayBuffer.copy(data: rgbaCopy)
    }
  }

  func saveToTemporaryFile(format: ImageFormat, quality: Double) throws -> Promise<String> {
    let qualityClamped = min(100, max(0, Int(quality.rounded())))
    let width = pixelWidth
    let height = pixelHeight
    let rgbaCopy = rgba
    let formatCopy = format
    return Promise.parallel(VisionKitQueue.queue) {
      guard let cgImage = RgbaImageConversion.cgImage(fromPremultipliedRgba: rgbaCopy, width: width, height: height) else {
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
