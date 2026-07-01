/**
 * A single label describing the contents of an image, produced by
 * `VisionKit.classifyImage`.
 */
export interface Classification {
  /** Human-readable label, e.g. `"Coffee mug"` or `"Plant"`. */
  readonly label: string
  /** Confidence score in `0`–`1`. Higher is more certain. */
  readonly confidence: number
  /** Model-specific index of the label, useful for de-duplication. */
  readonly index: number
}
