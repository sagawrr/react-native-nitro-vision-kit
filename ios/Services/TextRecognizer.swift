import Foundation
import CoreImage
import Vision
import NitroModules

enum TextRecognizer {
  private static let languageCacheLock = NSLock()
  private static var cachedAccurateLanguages: [String]?
  private static var cachedFastLanguages: [String]?
  private static let maxCandidateCap = 10
  private static let prefetchOnce: Void = {
    prefetchSupportedLanguages()
  }()

  /// Prefetched off the JS thread; fills sync once if the first read races the warm-up.
  static func supportedLanguages(level: TextRecognitionLevel = .accurate) -> [String] {
    _ = prefetchOnce
    guard #available(iOS 18.0, *) else { return [] }
    ensureLanguageCacheFilled()
    languageCacheLock.lock()
    defer { languageCacheLock.unlock() }
    switch level {
    case .fast:
      return cachedFastLanguages ?? []
    case .accurate:
      return cachedAccurateLanguages ?? []
    @unknown default:
      return cachedAccurateLanguages ?? []
    }
  }

  private static func prefetchSupportedLanguages() {
    guard #available(iOS 18.0, *) else { return }
    Task.detached(priority: .utility) {
      fillLanguageCacheIfNeeded()
    }
  }

  @available(iOS 18.0, *)
  private static func ensureLanguageCacheFilled() {
    languageCacheLock.lock()
    let ready = cachedAccurateLanguages != nil && cachedFastLanguages != nil
    languageCacheLock.unlock()
    if ready { return }
    fillLanguageCacheIfNeeded()
  }

  @available(iOS 18.0, *)
  private static func fillLanguageCacheIfNeeded() {
    languageCacheLock.lock()
    let needsAccurate = cachedAccurateLanguages == nil
    let needsFast = cachedFastLanguages == nil
    languageCacheLock.unlock()
    if !needsAccurate && !needsFast { return }

    let accurate = needsAccurate ? fetchSupportedLanguages(level: .accurate) : nil
    let fast = needsFast ? fetchSupportedLanguages(level: .fast) : nil

    languageCacheLock.lock()
    if let accurate, cachedAccurateLanguages == nil {
      cachedAccurateLanguages = accurate
    }
    if let fast, cachedFastLanguages == nil {
      cachedFastLanguages = fast
    }
    languageCacheLock.unlock()
  }

  @available(iOS 18.0, *)
  private static func fetchSupportedLanguages(level: RecognizeTextRequest.RecognitionLevel) -> [String] {
    var request = RecognizeTextRequest(.revision3)
    request.recognitionLevel = level
    return request.supportedRecognitionLanguages.map(\.maximalIdentifier)
  }

  @available(iOS 18.0, *)
  static func recognize(
    ciImage: CIImage,
    languages: [String]?,
    recognitionLevel: TextRecognitionLevel?,
    region: Rect?,
    minTextHeightFraction: Double?,
    usesLanguageCorrection: Bool?,
    customWords: [String]?,
    maxCandidates: Double?,
  ) async throws -> TextRecognitionOutput {
    var request = RecognizeTextRequest(.revision3)
    request.recognitionLevel = recognitionLevel == .fast ? .fast : .accurate

    let resolvedLanguages = languages?.filter { !$0.isEmpty }
    if let resolvedLanguages, !resolvedLanguages.isEmpty {
      request.recognitionLanguages = resolvedLanguages.map { Locale.Language(identifier: $0) }
      request.automaticallyDetectsLanguage = false
    } else {
      request.automaticallyDetectsLanguage = true
    }

    if let minTextHeightFraction {
      request.minimumTextHeightFraction = Float(minTextHeightFraction.clamped(to: 0...1))
    }

    let chineseOnly = resolvedLanguages.map { langs in
      !langs.isEmpty && langs.allSatisfy { $0.hasPrefix("zh") || $0.hasPrefix("yue") }
    } ?? false
    let correctionEnabled = chineseOnly ? false : (usesLanguageCorrection ?? true)
    request.usesLanguageCorrection = correctionEnabled
    if correctionEnabled, let customWords, !customWords.isEmpty {
      request.customWords = customWords
    }

    if let region {
      request.regionOfInterest = Vision.NormalizedRect(
        x: region.x,
        y: 1.0 - region.y - region.height,
        width: region.width,
        height: region.height
      )
    }

    let candidateCount = resolvedCandidateCount(maxCandidates)
    let observations = try await request.perform(on: ciImage, orientation: .up)

    var blocks: [RecognizedTextBlock] = []
    blocks.reserveCapacity(observations.count)
    var texts: [String] = []
    texts.reserveCapacity(observations.count)

    for observation in observations {
      let ranked = observation.topCandidates(candidateCount)
      guard let primary = ranked.first else { continue }

      let stringRange = primary.string.startIndex..<primary.string.endIndex
      let quad: any QuadrilateralProviding = primary.boundingBox(for: stringRange) ?? observation
      let geometry = topLeftGeometry(from: quad)
      let language = observationLanguage(observation)
      let alternatives: [RecognizedTextCandidate]? = candidateCount > 1
        ? ranked.map {
          RecognizedTextCandidate(text: $0.string, confidence: Double($0.confidence))
        }
        : nil

      let line = RecognizedTextLine(
        text: primary.string,
        bounds: geometry.bounds,
        confidence: Double(primary.confidence),
        language: language,
        angleDegrees: nil,
        cornerPoints: geometry.corners,
        candidates: alternatives,
      )
      blocks.append(
        RecognizedTextBlock(
          text: primary.string,
          bounds: geometry.bounds,
          lines: [line],
          language: language,
          cornerPoints: geometry.corners,
        )
      )
      texts.append(primary.string)
    }

    return TextRecognitionOutput(
      text: texts.joined(separator: "\n"),
      blocks: blocks,
    )
  }

  private static func resolvedCandidateCount(_ maxCandidates: Double?) -> Int {
    guard let maxCandidates else { return 1 }
    let n = Int(maxCandidates.rounded())
    return min(max(n, 1), maxCandidateCap)
  }

  @available(iOS 18.0, *)
  private static func observationLanguage(_ observation: RecognizedTextObservation) -> String? {
    if #available(iOS 26.0, *) {
      return observation.recognitionLanguages.first?.maximalIdentifier
    }
    return nil
  }

  @available(iOS 18.0, *)
  private static func topLeftGeometry(
    from quad: any QuadrilateralProviding
  ) -> (bounds: Rect, corners: [NormalizedPoint]) {
    let flippedBox = quad.boundingBox.verticallyFlipped()
    let bounds = Rect(
      x: Double(flippedBox.origin.x),
      y: Double(flippedBox.origin.y),
      width: Double(flippedBox.width),
      height: Double(flippedBox.height)
    )
    let corners = [
      topLeftPoint(quad.topLeft),
      topLeftPoint(quad.topRight),
      topLeftPoint(quad.bottomRight),
      topLeftPoint(quad.bottomLeft),
    ]
    return (bounds, corners)
  }

  @available(iOS 18.0, *)
  private static func topLeftPoint(_ point: Vision.NormalizedPoint) -> NormalizedPoint {
    let flipped = point.verticallyFlipped()
    return NormalizedPoint(x: Double(flipped.x), y: Double(flipped.y))
  }
}

private extension Double {
  func clamped(to range: ClosedRange<Double>) -> Double {
    min(max(self, range.lowerBound), range.upperBound)
  }
}
