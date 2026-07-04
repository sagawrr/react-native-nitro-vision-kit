import Foundation
import CoreImage
import Vision
import NitroModules

enum ImageClassifier {
  static func classify(
    ciImage: CIImage,
    maxResults: Int,
    minConfidence: Double,
    region: Rect?,
  ) throws -> [Classification] {
    let handler = VNImageRequestHandler(ciImage: ciImage, orientation: .up)
    let request = VNClassifyImageRequest()
    if let region {
      request.regionOfInterest = CGRect(
        x: region.x,
        y: 1.0 - region.y - region.height,
        width: region.width,
        height: region.height,
      )
    }
    try handler.perform([request])

    let observations = (request.results ?? [])
      .filter { Double($0.confidence) >= minConfidence }
      .sorted { $0.confidence > $1.confidence }

    let limited = maxResults > 0 ? Array(observations.prefix(maxResults)) : observations
    return limited.enumerated().map { index, observation in
      Classification(
        label: observation.identifier,
        confidence: Double(observation.confidence),
        index: Double(index)
      )
    }
  }
}
