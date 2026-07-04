import type { HybridObject } from 'react-native-nitro-modules'
import type { AnalyzeImageOptions } from '../types/AnalyzeImageOptions'
import type { BackgroundRemovalOptions } from '../types/BackgroundRemovalOptions'
import type { Classification } from '../types/Classification'
import type { ClassificationOptions } from '../types/ClassificationOptions'
import type { ImageAnalysisResult } from '../types/ImageAnalysisResult'
import type { SegmentationResult } from './SegmentationResult.nitro'
import type { VisionCapabilities } from '../types/VisionCapabilities'

/** On-device vision — segmentation and classification. */
export interface VisionKitFactory
  extends HybridObject<{ ios: 'swift'; android: 'kotlin' }> {
  /** Device feature flags. Check before calling segmentation APIs. */
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
   * Decodes once and runs the requested operations.
   * At least one of `removeBackground` or `classify` is required.
   * When both run, classification uses `segmentation.bounds` if `region` is omitted.
   */
  analyzeImage(
    path: string,
    options: AnalyzeImageOptions,
  ): Promise<ImageAnalysisResult>
}
