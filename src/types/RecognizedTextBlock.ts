import type { NormalizedPoint } from './NormalizedPoint'
import type { Rect } from './Rect'
import type { RecognizedTextLine } from './RecognizedTextLine'

/**
 * A contiguous text region.
 * Android (ML Kit): paragraph-like block with one or more lines.
 * iOS 18+ (RecognizeTextRequest): each observation is a one-line block.
 */
export interface RecognizedTextBlock {
  readonly text: string
  /** Normalized bounds, origin top-left (0–1). */
  readonly bounds: Rect
  readonly lines: RecognizedTextLine[]
  /** Prevailing BCP-47 language when reported (ML Kit; Vision on iOS 26+). */
  readonly language?: string
  /**
   * Four corners, clockwise from top-left, normalized (0–1).
   * ML Kit when present; Vision `RecognizeTextRequest` quads on iOS 18+.
   */
  readonly cornerPoints?: NormalizedPoint[]
}
