package com.margelo.nitro.nitrovisionkit

import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

internal object ImageLabelerCache {
  private const val MAX_ENTRIES = 8
  private val labelers = LinkedHashMap<Float, ImageLabeler>(MAX_ENTRIES, 0.75f, true)

  fun labeler(minConfidence: Float): ImageLabeler {
    val key = (minConfidence * 100f).toInt().coerceIn(0, 100) / 100f
    synchronized(labelers) {
      labelers[key]?.let { return it }
      while (labelers.size >= MAX_ENTRIES) {
        val eldest = labelers.entries.iterator().next()
        eldest.value.close()
        labelers.remove(eldest.key)
      }
      val created = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
          .setConfidenceThreshold(key)
          .build(),
      )
      labelers[key] = created
      return created
    }
  }
}
