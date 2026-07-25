import type { Classification } from './Classification'
import type { SegmentationResult } from '../specs/SegmentationResult.nitro'
import type { TextRecognitionResult } from '../specs/TextRecognitionResult.nitro'

export interface ImageAnalysisResult {
  readonly segmentation?: SegmentationResult
  readonly classifications?: Classification[]
  readonly text?: TextRecognitionResult
}
