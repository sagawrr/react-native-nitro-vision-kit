/** Device feature flags from `VisionKit.capabilities`. */
export interface VisionCapabilities {
  /** iOS 17+ device, or Android with Play services. Not available on Simulator. */
  readonly supportsBackgroundRemoval: boolean
  /** Set when `supportsBackgroundRemoval` is false. */
  readonly backgroundRemovalUnavailableReason?: string
  readonly supportsImageClassification: boolean
  /** On-device OCR (Vision RecognizeTextRequest on iOS 18+; ML Kit Latin + Play Services on Android). */
  readonly supportsTextRecognition: boolean
  /**
   * Languages the current build can request.
   * iOS 18+: Vision `supportedRecognitionLanguages` for accurate/revision 3.
   * Android v1: Latin-script tags covered by the ML Kit Latin model.
   */
  readonly supportedTextLanguages: string[]
}
