import type { HybridObject } from 'react-native-nitro-modules'
import type { RecognizedTextBlock } from '../types/RecognizedTextBlock'

/**
 * Prefer `text` / `blockAt(i)` so unused blocks stay native.
 * Call `dispose()` when done with large results.
 */
export interface TextRecognitionResult
  extends HybridObject<{ ios: 'swift'; android: 'kotlin' }> {
  readonly text: string
  readonly blockCount: number
  /** Throws if out of range. */
  blockAt(index: number): RecognizedTextBlock
  /** Copies all blocks into JS. Prefer `blockAt` for large docs. */
  readonly blocks: RecognizedTextBlock[]
}
