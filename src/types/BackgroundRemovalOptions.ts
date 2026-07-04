/** Options for `removeBackground` and `analyzeImage.removeBackground`. */
export interface BackgroundRemovalOptions {
  /** Crop to foreground bounds. @default true */
  readonly trim?: boolean
  /** Max decoded pixels (`width × height`). @default 6_000_000 */
  readonly maxPixels?: number
  /** Keep confidence mask for `toMaskBuffer`. @default false */
  readonly retainMask?: boolean
}
