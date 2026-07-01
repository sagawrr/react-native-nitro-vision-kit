/**
 * A normalized rectangle describing where a region sits inside a source image.
 *
 * All fields are fractions of the source image's full dimensions (`0`–`1`),
 * so a value maps to the same visual region regardless of the image's pixel
 * resolution. Returned as `SegmentationResult.bounds`.
 */
export interface Rect {
  /**
   * Horizontal origin of the region, `0`–`1`, measured from the left edge of
   * the source image.
   */
  readonly x: number
  /**
   * Vertical origin of the region, `0`–`1`, measured from the top edge of the
   * source image.
   */
  readonly y: number
  /** Width of the region, `0`–`1`, as a fraction of the source image width. */
  readonly width: number
  /** Height of the region, `0`–`1`, as a fraction of the source image height. */
  readonly height: number
}
