/**
 * Reports which on-device vision features the current platform supports.
 *
 * Returned by `VisionKit.capabilities`. Inspect it before calling
 * `VisionKit.removeBackground` or `VisionKit.classifyImage`; calling an
 * unsupported capability rejects with a descriptive error.
 */
export interface VisionCapabilities {
  /**
   * Whether `VisionKit.removeBackground` can run on this device. Requires
   * iOS 17+ (Vision) or Android with Google Play Services (ML Kit).
   */
  readonly supportsBackgroundRemoval: boolean
  /** Whether `VisionKit.classifyImage` can run on this device. */
  readonly supportsImageClassification: boolean
}
