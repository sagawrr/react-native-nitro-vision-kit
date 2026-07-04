package com.margelo.nitro.nitrovisionkit

internal data class MaskMetricsResult(
  val coverage: Double,
  val centroid: NormalizedPoint,
  val pixelBounds: PixelRect?,
)
