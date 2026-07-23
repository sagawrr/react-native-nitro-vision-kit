import type { BackgroundRemovalOptions } from './BackgroundRemovalOptions'
import type { ClassificationOptions } from './ClassificationOptions'
import type { TextRecognitionOptions } from './TextRecognitionOptions'

/**
 * Options for `analyzeImage`.
 * At least one of `removeBackground`, `classify`, or `readText` is required.
 * If no subject is found, `segmentation` is omitted and other ops still run.
 */
export interface AnalyzeImageOptions {
  readonly removeBackground?: BackgroundRemovalOptions
  readonly classify?: ClassificationOptions
  /** Run OCR in the same decode pass. */
  readonly readText?: TextRecognitionOptions
}
