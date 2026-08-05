import type { HybridObject } from 'react-native-nitro-modules'
import type { VisionImageFormat } from '../types/VisionImageFormat'
import type { NormalizedPoint } from '../types/NormalizedPoint'
import type { PixelRect } from '../types/PixelRect'
import type { VisionRect } from '../types/VisionRect'

/** HybridObject — call `dispose()` when done. */
export interface SegmentationResult
  extends HybridObject<{ ios: 'swift'; android: 'kotlin' }> {
  /** Affected by `trim`. */
  readonly width: number
  readonly height: number
  /** Normalized 0–1. */
  readonly bounds: VisionRect
  readonly sourceWidth: number
  readonly sourceHeight: number
  /** Foreground pixel ratio (threshold 0.5). */
  readonly foregroundCoverage: number
  readonly centroid: NormalizedPoint
  readonly instanceCount: number
  readonly pixelBounds: PixelRect
  /** Output origin in source space when `trim` is on. */
  readonly trimOrigin: NormalizedPoint
  /** Requires `retainMask: true`. */
  readonly hasMask: boolean
  /** Float32, row-major. Requires `retainMask: true`. */
  toMaskBuffer(): Promise<ArrayBuffer>
  /** Premultiplied RGBA_8888 (`width × height × 4`). */
  toArrayBuffer(): Promise<ArrayBuffer>
  /** `quality` 0–100 (JPEG only). */
  saveToTemporaryFile(
    format: VisionImageFormat,
    quality: number
  ): Promise<string>
}
