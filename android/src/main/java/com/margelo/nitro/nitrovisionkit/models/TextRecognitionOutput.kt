package com.margelo.nitro.nitrovisionkit

internal data class TextRecognitionOutput(
  val text: String,
  val blocks: Array<RecognizedTextBlock>,
)
