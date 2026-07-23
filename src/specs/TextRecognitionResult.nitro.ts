import type { HybridObject } from 'react-native-nitro-modules'
import type { RecognizedTextBlock } from '../types/RecognizedTextBlock'

/**
 * OCR result from `readText` / `analyzeImage`.
 * Prefer `text` or `blockAt(i)` so unused blocks stay native.
 */
export interface TextRecognitionResult
  extends HybridObject<{ ios: 'swift'; android: 'kotlin' }> {
  /** Full recognized text (blocks joined by newlines). */
  readonly text: string
  /** Number of text blocks held natively. */
  readonly blockCount: number
  /** One block by index (`0 .. blockCount-1`). Throws if out of range. */
  blockAt(index: number): RecognizedTextBlock
  /** All blocks (copied into JS). Prefer `blockAt` for large documents. */
  readonly blocks: RecognizedTextBlock[]
}
