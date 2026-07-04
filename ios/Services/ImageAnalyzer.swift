import Foundation
import CoreImage
import NitroModules

enum ImageAnalyzer {
  static func analyze(path: String, options: AnalyzeImageOptions) throws -> ImageAnalysisResult {
    let segmentOptions = options.removeBackground
    let classifyOptions = options.classify
    let trim = VisionKitOptions.trim(segmentOptions)
    let retainMask = VisionKitOptions.retainMask(segmentOptions)
    let maxPixels = VisionKitOptions.segmentMaxPixels(segmentOptions)
    let maxResults = VisionKitOptions.maxResults(classifyOptions)
    let minConfidence = VisionKitOptions.minConfidence(classifyOptions)
    var region = classifyOptions?.region

    let maxLoad = segmentOptions != nil ? maxPixels : VisionKitLimits.labelingMaxPixels
    let ciImage = try ImageLoader.loadCIImage(path: path, maxPixels: maxLoad)

    var segmentation: (any HybridSegmentationResultSpec)?
    if segmentOptions != nil {
      let output = try SubjectSegmenter.segment(ciImage: ciImage, trim: trim, retainMask: retainMask)
      segmentation = HybridSegmentationResult(output: output)
      if region == nil, classifyOptions != nil {
        region = output.bounds
      }
    }

    var classifications: [Classification]?
    if classifyOptions != nil {
      classifications = try ImageClassifier.classify(
        ciImage: ciImage,
        maxResults: maxResults,
        minConfidence: minConfidence,
        region: region,
      )
    }

    return ImageAnalysisResult(
      segmentation: segmentation,
      classifications: classifications,
    )
  }
}
