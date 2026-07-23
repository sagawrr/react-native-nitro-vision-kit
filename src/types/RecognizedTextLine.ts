import type { NormalizedPoint } from './NormalizedPoint'
import type { Rect } from './Rect'
import type { RecognizedTextCandidate } from './RecognizedTextCandidate'

/** A single line of recognized text. */
export interface RecognizedTextLine {
  readonly text: string
  /** Normalized bounds, origin top-left (0–1). */
  readonly bounds: Rect
  /** Confidence 0–1 when the engine provides it. */
  readonly confidence?: number
  /** BCP-47 language when reported (ML Kit; Vision on iOS 26+). */
  readonly language?: string
  /**
   * Clockwise rotation in degrees (−180…180) when reported.
   * ML Kit only; omitted on Vision.
   */
  readonly angleDegrees?: number
  /**
   * Four corners, clockwise from top-left, normalized (0–1).
   * ML Kit when present; Vision `RecognizeTextRequest` quads on iOS 18+.
   */
  readonly cornerPoints?: NormalizedPoint[]
  /**
   * Extra Vision `topCandidates` when `maxCandidates` > 1.
   * Index 0 matches `text` / `confidence`. Omitted when only one candidate.
   */
  readonly candidates?: RecognizedTextCandidate[]
}
