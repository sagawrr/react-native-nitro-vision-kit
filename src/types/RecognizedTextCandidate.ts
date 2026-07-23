/** Alternate OCR reading from Vision `topCandidates` (iOS). */
export interface RecognizedTextCandidate {
  readonly text: string
  /** Confidence 0–1. */
  readonly confidence: number
}
