import type { Rect } from './Rect'

/** Options for `classifyImage` and `analyzeImage.classify`. */
export interface ClassificationOptions {
  /** @default 0 (all labels above minConfidence) */
  readonly maxResults?: number
  /** @default 0.5 */
  readonly minConfidence?: number
  /** Normalized ROI (0–1). iOS: Vision ROI. Android: crop before classify. */
  readonly region?: Rect
}
