package com.margelo.nitro.nitrovisionkit

import androidx.annotation.Keep
import com.facebook.proguard.annotations.DoNotStrip

@Keep
@DoNotStrip
class HybridTextRecognitionResult internal constructor(
  output: TextRecognitionOutput,
) : HybridTextRecognitionResultSpec() {

  private var storedText: String = output.text
  private var storedBlocks: Array<RecognizedTextBlock> = output.blocks
  private var disposed = false

  override val text: String
    get() {
      ensureNotDisposed()
      return storedText
    }

  override val blockCount: Double
    get() {
      ensureNotDisposed()
      return storedBlocks.size.toDouble()
    }

  override val blocks: Array<RecognizedTextBlock>
    get() {
      ensureNotDisposed()
      return storedBlocks
    }

  override fun blockAt(index: Double): RecognizedTextBlock {
    ensureNotDisposed()
    val i = index.toInt()
    if (i < 0 || i >= storedBlocks.size) {
      throw RuntimeException("blockAt index $i out of range (blockCount=${storedBlocks.size}).")
    }
    return storedBlocks[i]
  }

  override val memorySize: Long
    get() {
      if (disposed) return HybridMemorySize.OVERHEAD
      var size = storedText.length.toLong() + HybridMemorySize.OVERHEAD
      for (block in storedBlocks) {
        size += block.text.length
        size += (block.cornerPoints?.size ?: 0) * 16L
        for (line in block.lines) {
          size += line.text.length
          size += (line.cornerPoints?.size ?: 0) * 16L
          line.candidates?.forEach { size += it.text.length }
        }
      }
      return size
    }

  override fun dispose() {
    disposed = true
    storedText = ""
    storedBlocks = emptyArray()
  }

  private fun ensureNotDisposed() {
    if (disposed) {
      throw RuntimeException(
        "TextRecognitionResult already disposed. Read text/blocks before dispose().",
      )
    }
  }
}
