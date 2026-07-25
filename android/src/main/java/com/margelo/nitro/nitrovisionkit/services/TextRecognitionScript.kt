package com.margelo.nitro.nitrovisionkit

/**
 * ML Kit Text Recognition v2 script models.
 * @see https://developers.google.com/ml-kit/vision/text-recognition/v2/android
 * @see https://developers.google.com/ml-kit/vision/text-recognition/v2/languages
 */
internal enum class TextRecognitionScript {
  LATIN,
  CHINESE,
  JAPANESE,
  KOREAN,
  DEVANAGARI,
  ;

  companion object {
    /**
     * Maps BCP-47 tags to unique script models (priority order preserved).
     * Empty / null → Latin only (ML Kit default).
     * Chinese/Japanese/Korean/Devanagari models also read Latin — skip a second Latin pass.
     */
    fun fromLanguages(languages: Array<String>?): List<TextRecognitionScript> {
      if (languages.isNullOrEmpty()) return listOf(LATIN)
      val scripts = LinkedHashSet<TextRecognitionScript>()
      for (raw in languages) {
        val tag = raw.trim()
        if (tag.isEmpty()) continue
        scripts.add(fromTag(tag))
      }
      if (scripts.isEmpty()) return listOf(LATIN)
      val hasScriptModel = scripts.any { it != LATIN }
      return if (hasScriptModel) {
        scripts.filter { it != LATIN }
      } else {
        scripts.toList()
      }
    }

    fun fromTag(tag: String): TextRecognitionScript {
      val normalized = tag.trim().lowercase().replace('_', '-')
      val parts = normalized.split('-')
      val primary = parts.firstOrNull().orEmpty()
      val has = { token: String -> parts.any { it == token } || normalized.contains(token) }

      return when {
        primary == "zh" || primary == "yue" || has("hans") || has("hant") -> CHINESE
        primary == "ja" || has("jpan") -> JAPANESE
        primary == "ko" || has("kore") -> KOREAN
        primary == "hi" || primary == "mr" || primary == "ne" || primary == "sa" || has("deva") ->
          DEVANAGARI
        else -> LATIN
      }
    }
  }
}
