package com.margelo.nitro.nitrovisionkit

internal object VisionKitLimits {
  const val DEFAULT_MAX_PIXELS = 6_000_000
  const val LABELING_MAX_PIXELS = 1_000_000
  /** ML Kit MediaPipe GPU path is unstable on some devices above ~512px. */
  const val ML_KIT_MAX_EDGE = 512
}
