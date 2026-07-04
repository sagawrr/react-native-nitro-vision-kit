import type { BackgroundRemovalOptions } from './BackgroundRemovalOptions'
import type { ClassificationOptions } from './ClassificationOptions'

/**
 * Options for `analyzeImage`.
 * At least one of `removeBackground` or `classify` is required.
 */
export interface AnalyzeImageOptions {
  readonly removeBackground?: BackgroundRemovalOptions
  readonly classify?: ClassificationOptions
}
