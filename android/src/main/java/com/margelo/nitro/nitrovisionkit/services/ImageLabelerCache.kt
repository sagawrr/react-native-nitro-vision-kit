package com.margelo.nitro.nitrovisionkit

import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

internal object ImageLabelerCache {
  private val labelers = mutableMapOf<Float, ImageLabeler>()

  fun labeler(minConfidence: Float): ImageLabeler {
    synchronized(labelers) {
      return labelers.getOrPut(minConfidence) {
        val options = ImageLabelerOptions.Builder()
          .setConfidenceThreshold(minConfidence)
          .build()
        ImageLabeling.getClient(options)
      }
    }
  }
}
