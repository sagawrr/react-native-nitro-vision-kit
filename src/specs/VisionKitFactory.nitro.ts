import type { HybridObject } from 'react-native-nitro-modules'
import type { AnalyzeImageOptions } from '../types/AnalyzeImageOptions'
import type { BackgroundRemovalOptions } from '../types/BackgroundRemovalOptions'
import type { Classification } from '../types/Classification'
import type { ClassificationOptions } from '../types/ClassificationOptions'
import type { ImageAnalysisResult } from '../types/ImageAnalysisResult'
import type { TextRecognitionOptions } from '../types/TextRecognitionOptions'
import type { TextRecognitionResult } from './TextRecognitionResult.nitro'
import type { SegmentationResult } from './SegmentationResult.nitro'
import type { VisionCapabilities } from '../types/VisionCapabilities'

/** On-device vision — segmentation, classification, and text recognition. */
export interface VisionKitFactory
  extends HybridObject<{ ios: 'swift'; android: 'kotlin' }> {
  readonly capabilities: VisionCapabilities
  /**
   * Segments foreground instances in the image.
   * @param path File path or `file://` URI.
   */
  removeBackground(
    path: string,
    options?: BackgroundRemovalOptions,
  ): Promise<SegmentationResult>
  /**
   * Classifies the image. Returns labels sorted by confidence.
   * @param path File path or `file://` URI.
   */
  classifyImage(
    path: string,
    options?: ClassificationOptions,
  ): Promise<Classification[]>
  /**
   * On-device OCR.
   * iOS 18+: Vision `RecognizeTextRequest`.
   * Android: ML Kit v2 (Latin / CJK / Devanagari) via Play Services.
   * @param path File path or `file://` URI.
   */
  readText(
    path: string,
    options?: TextRecognitionOptions,
  ): Promise<TextRecognitionResult>
  /**
   * One decode → any mix of Lift / Read / Text.
   * At least one option required. Omit `region` with Lift → uses subject bounds.
   */
  analyzeImage(
    path: string,
    options: AnalyzeImageOptions,
  ): Promise<ImageAnalysisResult>
}
