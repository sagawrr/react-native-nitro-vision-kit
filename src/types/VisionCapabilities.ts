/** Device feature flags from `VisionKit.capabilities`. */
export interface VisionCapabilities {
  /** iOS 17+ device, or Android with Play services. Not available on Simulator. */
  readonly supportsBackgroundRemoval: boolean
  /** Set when `supportsBackgroundRemoval` is false. */
  readonly backgroundRemovalUnavailableReason?: string
  readonly supportsImageClassification: boolean
}
