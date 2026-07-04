/** A single classification label. */
export interface Classification {
  readonly label: string
  /** Confidence score, 0–1. */
  readonly confidence: number
  readonly index: number
}
