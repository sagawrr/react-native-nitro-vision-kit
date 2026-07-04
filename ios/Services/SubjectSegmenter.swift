import Foundation
import CoreImage
import CoreImage.CIFilterBuiltins
import Vision
import NitroModules

enum SubjectSegmenter {
  private static let renderContext = CIContext()

  static func segment(ciImage: CIImage, trim: Bool, retainMask: Bool) throws -> SegmentationOutput {
    guard #available(iOS 17.0, *) else {
      throw RuntimeError(VisionAvailability.backgroundRemovalUnavailableReason)
    }

    let sourceWidth = Int(ciImage.extent.width.rounded())
    let sourceHeight = Int(ciImage.extent.height.rounded())
    if sourceWidth <= 0 || sourceHeight <= 0 {
      throw RuntimeError("Image has no readable pixels.")
    }

    let handler = VNImageRequestHandler(ciImage: ciImage, orientation: .up)
    let request = VNGenerateForegroundInstanceMaskRequest()
    try handler.perform([request])

    guard let observation = request.results?.first else {
      throw RuntimeError("No foreground subject detected.")
    }

    let maskBuffer = try observation.generateScaledMaskForImage(
      forInstances: observation.allInstances,
      from: handler,
    )
    let analysis = try MaskMetrics.analyze(
      pixelBuffer: maskBuffer,
      width: sourceWidth,
      height: sourceHeight,
      retainMask: retainMask,
    )
    guard let pixelBounds = analysis.pixelBounds else {
      throw RuntimeError("No foreground subject detected.")
    }

    let instanceCount = observation.allInstances.isEmpty ? 1.0 : Double(observation.allInstances.count)
    let normalizedBounds = Rect(
      x: pixelBounds.x / Double(sourceWidth),
      y: pixelBounds.y / Double(sourceHeight),
      width: pixelBounds.width / Double(sourceWidth),
      height: pixelBounds.height / Double(sourceHeight),
    )

    let maskImage = CIImage(cvPixelBuffer: maskBuffer)
    let blend = CIFilter.blendWithMask()
    blend.inputImage = ciImage
    blend.maskImage = maskImage
    blend.backgroundImage = CIImage.empty()
    guard let composited = blend.outputImage else {
      throw RuntimeError("Failed to composite subject mask.")
    }

    guard
      let cgImage = renderContext.createCGImage(composited, from: CGRect(origin: .zero, size: ciImage.extent.size))
    else {
      throw RuntimeError("Failed to render masked image.")
    }

    let full = RgbaImageConversion.premultipliedRgbaBytes(from: cgImage, width: sourceWidth, height: sourceHeight)

    guard trim else {
      return SegmentationOutput(
        rgba: Data(full),
        width: sourceWidth,
        height: sourceHeight,
        bounds: normalizedBounds,
        sourceWidth: sourceWidth,
        sourceHeight: sourceHeight,
        foregroundCoverage: analysis.coverage,
        centroid: analysis.centroid,
        instanceCount: instanceCount,
        pixelBounds: pixelBounds,
        trimOrigin: NormalizedPoint(x: 0, y: 0),
        mask: analysis.mask,
        hasMask: retainMask,
      )
    }

    let pad = 2
    let cropX = max(0, Int(pixelBounds.x) - pad)
    let cropY = max(0, Int(pixelBounds.y) - pad)
    let cropRight = min(sourceWidth - 1, Int(pixelBounds.x + pixelBounds.width) - 1 + pad)
    let cropBottom = min(sourceHeight - 1, Int(pixelBounds.y + pixelBounds.height) - 1 + pad)
    let cropped = RgbaImageConversion.cropRgba(
      full, width: sourceWidth,
      x: cropX, y: cropY, right: cropRight, bottom: cropBottom
    )
    let cropW = cropRight - cropX + 1
    let cropH = cropBottom - cropY + 1
    let croppedMask = retainMask
      ? RgbaImageConversion.cropFloatMask(
        analysis.mask,
        width: sourceWidth,
        x: cropX,
        y: cropY,
        right: cropRight,
        bottom: cropBottom
      )
      : analysis.mask
    return SegmentationOutput(
      rgba: Data(cropped),
      width: cropW,
      height: cropH,
      bounds: normalizedBounds,
      sourceWidth: sourceWidth,
      sourceHeight: sourceHeight,
      foregroundCoverage: analysis.coverage,
      centroid: analysis.centroid,
      instanceCount: instanceCount,
      pixelBounds: pixelBounds,
      trimOrigin: NormalizedPoint(
        x: Double(cropX) / Double(sourceWidth),
        y: Double(cropY) / Double(sourceHeight),
      ),
      mask: croppedMask,
      hasMask: retainMask,
    )
  }
}
