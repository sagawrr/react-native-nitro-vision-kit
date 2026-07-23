import { NitroModules } from 'react-native-nitro-modules'
import type { VisionKitFactory } from './specs/VisionKitFactory.nitro'

export const VisionKit =
  NitroModules.createHybridObject<VisionKitFactory>('VisionKitFactory')

export type { VisionKitFactory } from './specs/VisionKitFactory.nitro'
export type { SegmentationResult } from './specs/SegmentationResult.nitro'
export type { Rect } from './types/Rect'
export type { ImageFormat } from './types/ImageFormat'
export type { VisionCapabilities } from './types/VisionCapabilities'
export type { BackgroundRemovalOptions } from './types/BackgroundRemovalOptions'
export type { ClassificationOptions } from './types/ClassificationOptions'
export type { Classification } from './types/Classification'
export type { NormalizedPoint } from './types/NormalizedPoint'
export type { PixelRect } from './types/PixelRect'
export type { AnalyzeImageOptions } from './types/AnalyzeImageOptions'
export type { ImageAnalysisResult } from './types/ImageAnalysisResult'
export type { TextRecognitionLevel } from './types/TextRecognitionLevel'
export type { TextRecognitionOptions } from './types/TextRecognitionOptions'
export type { TextRecognitionResult } from './specs/TextRecognitionResult.nitro'
export type { RecognizedTextBlock } from './types/RecognizedTextBlock'
export type { RecognizedTextLine } from './types/RecognizedTextLine'
export type { RecognizedTextCandidate } from './types/RecognizedTextCandidate'
