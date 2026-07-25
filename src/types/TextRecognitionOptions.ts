import type { Rect } from './Rect'
import type { TextRecognitionLevel } from './TextRecognitionLevel'

/** Options for `readText`. */
export interface TextRecognitionOptions {
  /**
   * BCP-47 tags, priority order.
   * iOS: Vision `recognitionLanguages` (omit → auto-detect).
   * Android: maps to ML Kit v2 script clients. Omit → Latin.
   * Chinese/Japanese/Korean/Devanagari models also read Latin — no second Latin pass.
   * Distinct non-Latin scripts run in parallel and merge.
   */
  readonly languages?: string[]
  /** @default 'accurate' — iOS Vision only; Android ignores. */
  readonly recognitionLevel?: TextRecognitionLevel
  /** Normalized ROI (0–1). iOS: Vision ROI. Android: crop before OCR. */
  readonly region?: Rect
  /**
   * Minimum text height as a fraction of image height (0–1).
   * iOS: Vision `minimumTextHeight`. Android: post-filter on line bounds.
   */
  readonly minTextHeightFraction?: number
  /**
   * iOS language correction (`usesLanguageCorrection`).
   * Ignored on Android. Forced off when languages are Chinese-only (Apple).
   * @default true
   */
  readonly usesLanguageCorrection?: boolean
  /**
   * iOS `customWords` — domain terms for language correction.
   * Ignored unless language correction is on; ignored on Android.
   */
  readonly customWords?: string[]
  /**
   * Max Vision `topCandidates` per observation (1–10). Apple caps at 10.
   * When > 1, alternatives appear on each line as `candidates`.
   * Android always returns a single reading — ignored.
   * @default 1
   */
  readonly maxCandidates?: number
}
