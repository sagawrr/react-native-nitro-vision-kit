import Foundation
import CoreImage
import Vision
import NitroModules

/// Runs a Vision `VNClassifyImageRequest` to label the contents of an image.
enum ImageClassifier {
  /// Classifies `ciImage`, returning labels ranked by confidence. Labels below
  /// `minConfidence` are dropped, and at most `maxResults` are returned (when
  /// `maxResults` is positive).
  static func classify(
    ciImage: CIImage,
    maxResults: Int,
    minConfidence: Double
  ) throws -> [Classification] {
    let handler = VNImageRequestHandler(ciImage: ciImage, orientation: .up)
    let request = VNClassifyImageRequest()
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
