import Foundation
import CoreImage
import NitroModules

enum ImageAnalyzer {
  private static let noForeground = "No foreground subject detected."

  static func analyze(path: String, options: AnalyzeImageOptions) async throws -> ImageAnalysisResult {
    let segmentOptions = options.removeBackground
    let classifyOptions = options.classify
    let textOptions = options.readText
    let trim = VisionKitOptions.trim(segmentOptions)
    let retainMask = VisionKitOptions.retainMask(segmentOptions)
    let maxPixels = VisionKitOptions.segmentMaxPixels(segmentOptions)
    let maxResults = VisionKitOptions.maxResults(classifyOptions)
    let minConfidence = VisionKitOptions.minConfidence(classifyOptions)
    var classifyRegion = classifyOptions?.region
    var textRegion = textOptions?.region

    let maxLoad = resolveMaxLoad(
      segmentMaxPixels: segmentOptions != nil ? maxPixels : nil,
      classify: classifyOptions != nil,
      readText: textOptions != nil,
    )
    let ciImage = try ImageLoader.loadCIImage(path: path, maxPixels: maxLoad)

    var segmentation: (any HybridSegmentationResultSpec)?
    if segmentOptions != nil {
      do {
        let output = try SubjectSegmenter.segment(ciImage: ciImage, trim: trim, retainMask: retainMask)
        segmentation = HybridSegmentationResult(output: output)
        if classifyRegion == nil, classifyOptions != nil {
          classifyRegion = output.bounds
        }
        if textRegion == nil, textOptions != nil {
          textRegion = output.bounds
        }
      } catch {
        let hasOtherOps = classifyOptions != nil || textOptions != nil
        if !hasOtherOps || !isNoForeground(error) {
          throw error
        }
      }
    }

    var classifications: [Classification]?
    if classifyOptions != nil {
      classifications = try ImageClassifier.classify(
        ciImage: ciImage,
        maxResults: maxResults,
        minConfidence: minConfidence,
        region: classifyRegion,
      )
    }

    var text: (any HybridTextRecognitionResultSpec)?
    if textOptions != nil {
      guard #available(iOS 18.0, *) else {
        throw RuntimeError(VisionAvailability.textRecognitionUnavailableReason)
      }
      let output = try await TextRecognizer.recognize(
        ciImage: ciImage,
        languages: textOptions?.languages,
        recognitionLevel: textOptions?.recognitionLevel,
        region: textRegion,
        minTextHeightFraction: textOptions?.minTextHeightFraction,
        usesLanguageCorrection: textOptions?.usesLanguageCorrection,
        customWords: textOptions?.customWords,
        maxCandidates: textOptions?.maxCandidates,
      )
      text = HybridTextRecognitionResult(output: output)
    }

    return ImageAnalysisResult(
      segmentation: segmentation,
      classifications: classifications,
      text: text,
    )
  }

  private static func isNoForeground(_ error: Error) -> Bool {
    error.localizedDescription.contains(noForeground)
  }

  private static func resolveMaxLoad(
    segmentMaxPixels: Int?,
    classify: Bool,
    readText: Bool,
  ) -> Int {
    var maxLoad = 0
    if let segmentMaxPixels {
      maxLoad = max(maxLoad, segmentMaxPixels)
    }
    if classify {
      maxLoad = max(maxLoad, VisionKitLimits.labelingMaxPixels)
    }
    if readText {
      maxLoad = max(maxLoad, VisionKitLimits.textMaxPixels)
    }
    return maxLoad > 0 ? maxLoad : VisionKitLimits.defaultMaxPixels
  }
}
