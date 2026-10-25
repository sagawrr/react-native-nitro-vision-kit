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
        let output = try SubjectSegmenter.segment(
          ciImage: segmentImage(ciImage, maxPixels: maxPixels),
          trim: trim,
          retainMask: retainMask,
        )
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
    var text: (any HybridTextRecognitionResultSpec)?

    if classifyOptions != nil, textOptions != nil {
      async let labelsTask: [Classification] = ImageClassifier.classify(
        ciImage: ciImage,
        maxResults: maxResults,
        minConfidence: minConfidence,
        region: classifyRegion,
      )
      async let textTask: (any HybridTextRecognitionResultSpec) = {
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
        return HybridTextRecognitionResult(output: output) as any HybridTextRecognitionResultSpec
      }()
      classifications = try await labelsTask
      text = try await textTask
    } else if classifyOptions != nil {
      classifications = try ImageClassifier.classify(
        ciImage: ciImage,
        maxResults: maxResults,
        minConfidence: minConfidence,
        region: classifyRegion,
      )
    } else if textOptions != nil {
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

  /// Shared decode may be larger than `maxPixels` so labels and OCR stay sharp.
  /// The segmenter still sees an image within the caller's cap.
  private static func segmentImage(_ image: CIImage, maxPixels: Int) -> CIImage {
    let extent = image.extent.integral
    let width = extent.width
    let height = extent.height
    guard width > 1, height > 1 else { return image }
    let count = width * height
    guard count > CGFloat(maxPixels) else { return image }
    let scale = sqrt(CGFloat(maxPixels) / count)
    let scaled = image.transformed(by: CGAffineTransform(scaleX: scale, y: scale))
    let origin = scaled.extent.origin
    return scaled.transformed(by: CGAffineTransform(translationX: -origin.x, y: -origin.y))
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
