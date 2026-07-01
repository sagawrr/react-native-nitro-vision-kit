import { NitroModules } from 'react-native-nitro-modules'
import type { VisionKitFactory } from './specs/VisionKitFactory.nitro'

/**
 * Singleton entry point for on-device vision. Use
 * {@linkcode VisionKit.capabilities}, {@linkcode VisionKit.removeBackground},
 * and {@linkcode VisionKit.classifyImage}.
 */
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
