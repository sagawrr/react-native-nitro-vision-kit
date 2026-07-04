import Foundation

struct SegmentationOutput {
  let rgba: Data
  let width: Int
  let height: Int
  let bounds: Rect
  let sourceWidth: Int
  let sourceHeight: Int
  let foregroundCoverage: Double
  let centroid: NormalizedPoint
  let instanceCount: Double
  let pixelBounds: PixelRect
  let trimOrigin: NormalizedPoint
  let mask: Data
  let hasMask: Bool
}
