import type { HybridObject } from 'react-native-nitro-modules'
import type { ImageFormat } from '../types/ImageFormat'
import type { Rect } from '../types/Rect'

/**
 * Owns the masked subject pixels produced by
 * `VisionKit.removeBackground()`.
 *
 * The native bitmap lives on the native side; access its bytes lazily through
 * {@linkcode toArrayBuffer} (zero-copy RGBA) or
 * {@linkcode saveToTemporaryFile} (encoded file). Avoiding eager conversion is
 * what removes the encode/decode round-trips that dominated the old pipeline.
 *
 * Hold the reference for as long as you need the pixels; once it is
 * garbage-collected (or {@linkcode HybridObject.dispose dispose}d) the native
 * buffer is released.
 */
export interface SegmentationResult
  extends HybridObject<{ ios: 'swift'; android: 'kotlin' }> {
  /** Pixel width of the masked cutout this result holds. */
  readonly width: number
  /** Pixel height of the masked cutout this result holds. */
  readonly height: number
  /**
   * Where the subject sat inside the *original* image, normalized to `0`–`1`.
   * Useful for positioning fly-to animations. See {@linkcode Rect}.
   */
  readonly bounds: Rect
  /**
   * Returns the masked pixels as **premultiplied RGBA_8888** (alpha last),
   * zero-copy from the native buffer. Width and height come from
   * {@linkcode width} and {@linkcode height}. The alpha channel carries the
   * model's soft matte.
   *
   * Premultiplied to match `react-native-nitro-image`'s
   * `loadFromRawPixelData` contract — hand the buffer straight in without
   * re-encoding:
   * ```ts
   * const r = await VisionKit.removeBackground(path)
   * const image = ImageFactory.loadFromRawPixelData({
   *   buffer: r.toArrayBuffer(), width: r.width, height: r.height, pixelFormat: 'RGBA',
   * })
   * ```
   */
  toArrayBuffer(): ArrayBuffer
  /**
   * Encodes the masked pixels to a temporary file and returns its path.
   * `quality` is `0`–`100` (ignored for `'png'`). Use `'png'` to keep the alpha
   * matte, `'jpeg'` for smaller opaque output.
   */
  saveToTemporaryFile(format: ImageFormat, quality: number): Promise<string>
}
