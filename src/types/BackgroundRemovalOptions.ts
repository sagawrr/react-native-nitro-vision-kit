/**
 * Optional preferences for `VisionKit.removeBackground`.
 */
export interface BackgroundRemovalOptions {
  /**
   * When `true`, the returned `SegmentationResult` is cropped to the subject's
   * bounding box. Defaults to `true`.
   */
  readonly trim?: boolean
  /**
   * Upper bound on decoded pixel count (`width * height`) used while
   * segmenting. Larger values preserve detail at the cost of memory and time;
   * the native side may lower this under memory pressure. When omitted, uses a
   * platform default tuned for typical phone captures.
   */
  readonly maxPixels?: number
}
