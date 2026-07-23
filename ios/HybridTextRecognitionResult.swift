import Foundation
import NitroModules

final class HybridTextRecognitionResult: HybridTextRecognitionResultSpec {
  private var storedText: String
  private var storedBlocks: [RecognizedTextBlock]

  init(output: TextRecognitionOutput) {
    self.storedText = output.text
    self.storedBlocks = output.blocks
    super.init()
  }

  var text: String { storedText }

  var blockCount: Double { Double(storedBlocks.count) }

  var blocks: [RecognizedTextBlock] { storedBlocks }

  func blockAt(index: Double) throws -> RecognizedTextBlock {
    let i = Int(index)
    guard i >= 0, i < storedBlocks.count else {
      throw RuntimeError("blockAt index \(i) out of range (blockCount=\(storedBlocks.count)).")
    }
    return storedBlocks[i]
  }

  var memorySize: Int {
    storedText.utf8.count
      + storedBlocks.reduce(0) { partial, block in
        partial
          + block.text.utf8.count
          + (block.cornerPoints?.count ?? 0) * 16
          + block.lines.reduce(0) { linePartial, line in
            linePartial
              + line.text.utf8.count
              + (line.cornerPoints?.count ?? 0) * 16
              + (line.candidates?.reduce(0) { $0 + $1.text.utf8.count } ?? 0)
          }
      }
      + HybridMemorySize.overhead
  }

  func dispose() {
    storedText = ""
    storedBlocks = []
  }
}
