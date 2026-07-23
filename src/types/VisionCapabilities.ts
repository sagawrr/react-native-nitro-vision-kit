/** Device feature flags from `VisionKit.capabilities`. */
export interface VisionCapabilities {
  /** Lift / subject segmentation. Check `backgroundRemovalUnavailableReason` when false. */
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
