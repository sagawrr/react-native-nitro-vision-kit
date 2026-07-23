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

  override val text: String
    get() = storedText

  override val blockCount: Double
    get() = storedBlocks.size.toDouble()

  override val blocks: Array<RecognizedTextBlock>
    get() = storedBlocks

  override fun blockAt(index: Double): RecognizedTextBlock {
    val i = index.toInt()
    if (i < 0 || i >= storedBlocks.size) {
      throw RuntimeException("blockAt index $i out of range (blockCount=${storedBlocks.size}).")
    }
    return storedBlocks[i]
  }

  override val memorySize: Long
    get() {
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
    storedText = ""
    storedBlocks = emptyArray()
  }
}
