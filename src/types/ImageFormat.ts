/**
 * The encoded file formats supported by `SegmentationResult.saveToTemporaryFile`.
 *
 * - `'png'` — lossless, preserves the alpha matte. Use for stickers/cutouts.
 * - `'jpeg'` — lossy, smaller. Use for thumbnails where transparency is unwanted.
 */
export type ImageFormat = 'png' | 'jpeg'
