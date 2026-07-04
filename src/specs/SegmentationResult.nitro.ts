import type { HybridObject } from 'react-native-nitro-modules'
import type { ImageFormat } from '../types/ImageFormat'
import type { NormalizedPoint } from '../types/NormalizedPoint'
import type { PixelRect } from '../types/PixelRect'
import type { Rect } from '../types/Rect'

/** Segmentation result from `removeBackground` or `analyzeImage`. */
export interface SegmentationResult
  extends HybridObject<{ ios: 'swift'; android: 'kotlin' }> {
  /** Output width in pixels. Affected by `trim`. */
  readonly width: number
  /** Output height in pixels. Affected by `trim`. */
  readonly height: number
  /** Foreground bounds in normalized coordinates (0–1). */
  readonly bounds: Rect
  /** Width of the input image in pixels. */
  readonly sourceWidth: number
  /** Height of the input image in pixels. */
  readonly sourceHeight: number
  /** Foreground pixel ratio. Threshold: 0.5. */
  readonly foregroundCoverage: number
  /** Foreground center in normalized coordinates (0–1). */
  readonly centroid: NormalizedPoint
  /** Number of detected foreground instances. */
  readonly instanceCount: number
  /** Foreground bounds in pixel coordinates. */
  readonly pixelBounds: PixelRect
  /** Output origin in source space when `trim` is enabled. */
  readonly trimOrigin: NormalizedPoint
  /** Whether `toMaskBuffer` is available. Requires `retainMask: true`. */
  readonly hasMask: boolean
  /** Float32 confidence mask, row-major. Requires `retainMask: true`. */
  toMaskBuffer(): Promise<ArrayBuffer>
  /** Premultiplied RGBA_8888. `width × height × 4` bytes. */
  toArrayBuffer(): Promise<ArrayBuffer>
  /** Writes the result to a temp file. `quality` is 0–100 (JPEG only). */
  saveToTemporaryFile(format: ImageFormat, quality: number): Promise<string>
}
