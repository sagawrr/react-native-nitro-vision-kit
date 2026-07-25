import type { BackgroundRemovalOptions } from './BackgroundRemovalOptions'
import type { ClassificationOptions } from './ClassificationOptions'
import type { TextRecognitionOptions } from './TextRecognitionOptions'

/**
 * At least one of `removeBackground`, `classify`, or `readText` is required.
 * No subject → `segmentation` omitted; other ops still run.
 */
export interface AnalyzeImageOptions {
  readonly removeBackground?: BackgroundRemovalOptions
  readonly classify?: ClassificationOptions
  readonly readText?: TextRecognitionOptions
}
