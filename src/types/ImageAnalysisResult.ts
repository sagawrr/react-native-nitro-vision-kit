import type { Classification } from './Classification'
import type { SegmentationResult } from '../specs/SegmentationResult.nitro'
import type { TextRecognitionResult } from '../specs/TextRecognitionResult.nitro'

/** Result of `analyzeImage`. Unrequested fields are omitted. */
export interface ImageAnalysisResult {
  readonly segmentation?: SegmentationResult
  readonly classifications?: Classification[]
  readonly text?: TextRecognitionResult
}
