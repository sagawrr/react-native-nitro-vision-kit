import type { HybridObject } from 'react-native-nitro-modules'
import type { BackgroundRemovalOptions } from '../types/BackgroundRemovalOptions'
import type { Classification } from '../types/Classification'
import type { ClassificationOptions } from '../types/ClassificationOptions'
import type { SegmentationResult } from './SegmentationResult.nitro'
import type { VisionCapabilities } from '../types/VisionCapabilities'

/**
 * Root entry point for on-device computer vision (subject segmentation,
 * background removal, image classification).
 *
 * Implemented natively with Vision on iOS and ML Kit on Android, exposed with
 * zero-copy pixel access so callers can composite results without PNG
 * round-trips. Construct the singleton via the package's `VisionKit` export.
 *
 * @example
 * ```ts
 * import { VisionKit } from 'react-native-nitro-vision-kit'
 *
 * if (VisionKit.capabilities.supportsBackgroundRemoval) {
 *   const result = await VisionKit.removeBackground(photoPath, { trim: true })
 *   const rgba = result.toArrayBuffer() // zero-copy, no PNG encode
 * }
 * ```
 */
export interface VisionKitFactory
  extends HybridObject<{ ios: 'swift'; android: 'kotlin' }> {
  /**
   * Which vision features are available on this device. Check before calling
   * the corresponding method.
   */
  readonly capabilities: VisionCapabilities
  /**
   * Lifts the primary subject out of the image at `path` and returns a
   * {@linkcode SegmentationResult} holding the masked pixels and the subject's
   * bounds. Rejects if {@linkcode VisionCapabilities.supportsBackgroundRemoval}
   * is `false`.
   *
   * `path` accepts a filesystem path or a `file://` URI.
   */
  removeBackground(
    path: string,
    options?: BackgroundRemovalOptions,
  ): Promise<SegmentationResult>
  /**
   * Classifies the image at `path` and returns ranked
   * {@linkcode Classification} labels. Rejects if
   * {@linkcode VisionCapabilities.supportsImageClassification} is `false`.
   *
   * `path` accepts a filesystem path or a `file://` URI.
   */
  classifyImage(
    path: string,
    options?: ClassificationOptions,
  ): Promise<Classification[]>
}
