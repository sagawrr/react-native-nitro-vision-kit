/**
 * Optional preferences for `VisionKit.classifyImage`.
 */
export interface ClassificationOptions {
  /** Maximum number of labels to return. When omitted, returns all labels. */
  readonly maxResults?: number
  /** Omit labels whose confidence is below this `0`–`1` threshold. */
  readonly minConfidence?: number
}
