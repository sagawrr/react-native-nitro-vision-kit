package com.margelo.nitro.nitrovisionkit

/**
 * Output of a subject segmentation: the masked subject pixels (RGBA,
 * premultiplied alpha carries the matte) plus where the subject sat in the
 * source image. Premultiplied to match `react-native-nitro-image`'s
 * `loadFromRawPixelData` contract.
 *
 * @property pixels premultiplied RGBA bytes, `width * height * 4` long.
 * @property width pixel width of [pixels].
 * @property height pixel height of [pixels].
 * @property bounds subject rectangle inside the *original* (pre-trim, full)
 *  bitmap, normalized to `0`–`1`.
 */
internal data class SegmentationOutput(
  val pixels: ByteArray,
  val width: Int,
  val height: Int,
  val bounds: Rect,
)
