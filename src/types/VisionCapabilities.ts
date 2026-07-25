/** Device feature flags from `VisionKit.capabilities`. */
export interface VisionCapabilities {
  readonly supportsBackgroundRemoval: boolean
  /** Set when Lift is off. */
  readonly backgroundRemovalUnavailableReason?: string
  readonly supportsImageClassification: boolean
  /** iOS 18+ · Android Play Services. */
  readonly supportsTextRecognition: boolean
  /** Empty when Text is off. */
  readonly supportedTextLanguages: string[]
}
