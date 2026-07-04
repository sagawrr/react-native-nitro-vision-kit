package com.margelo.nitro.nitrovisionkit

internal class SegmentationOutput(
  val pixels: ByteArray,
  val width: Int,
  val height: Int,
  val bounds: Rect,
  val sourceWidth: Int,
  val sourceHeight: Int,
  val foregroundCoverage: Double,
  val centroid: NormalizedPoint,
  val instanceCount: Double,
  val pixelBounds: PixelRect,
  val trimOrigin: NormalizedPoint,
  val mask: ByteArray,
  val hasMask: Boolean,
)
