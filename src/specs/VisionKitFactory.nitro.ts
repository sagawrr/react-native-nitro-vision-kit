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
  /** Device feature flags. Check before Lift / OCR. */
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
   * Recognizes text in the image (OCR).
   * iOS 18+: Vision `RecognizeTextRequest`. Android v1: ML Kit Latin + Play Services.
   * @param path File path or `file://` URI.
   */
  readText(
    path: string,
    options?: TextRecognitionOptions,
  ): Promise<TextRecognitionResult>
  /**
   * Decodes once and runs the requested operations.
   * At least one of `removeBackground`, `classify`, or `readText` is required.
   * When segment + classify/OCR run and `region` is omitted, those use `segmentation.bounds`.
   */
  analyzeImage(
    path: string,
    options: AnalyzeImageOptions,
  ): Promise<ImageAnalysisResult>
}
