import Foundation

/// Output of a subject segmentation: the masked subject pixels (RGBA,
/// premultiplied alpha carries the matte) plus where the subject sat in the
/// source image. Premultiplied to match `react-native-nitro-image`'s
/// `loadFromRawPixelData` contract.
struct SegmentationOutput {
  /// Premultiplied RGBA bytes, `width * height * 4` long.
  let rgba: Data
  /// Pixel width of `rgba`.
  let width: Int
  /// Pixel height of `rgba`.
  let height: Int
  /// Subject rectangle inside the *original* (pre-trim, full) image, `0`–`1`.
  let bounds: Rect
}
