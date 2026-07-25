import type { Classification } from 'react-native-nitro-vision-kit'

export type Mode = 'cutout' | 'classify' | 'ocr' | 'analyze'

export type StageView = 'original' | 'result'

export type SegmentationMeta = {
  sourceWidth: number
  sourceHeight: number
}

export type RunResult = {
  mode: Mode
  originalUri: string
  cutoutUri: string | null
  classifications: Classification[]
  /** Full OCR string when scanned. */
  ocrText: string | null
  meta: SegmentationMeta | null
}

export type ModeInfo = {
  id: Mode
  label: string
  purpose: string
  runLabel: string
}

export const MODE_INFO: Record<Mode, ModeInfo> = {
  cutout: {
    id: 'cutout',
    label: 'Lift',
    purpose: 'Cut the subject free of its background.',
    runLabel: 'Lift subject',
  },
  classify: {
    id: 'classify',
    label: 'Read',
    purpose: 'Name what’s in the frame.',
    runLabel: 'Read photo',
  },
  ocr: {
    id: 'ocr',
    label: 'Text',
    purpose: 'Pull words from the photo.',
    runLabel: 'Scan text',
  },
  analyze: {
    id: 'analyze',
    label: 'All',
    purpose: 'Lift, read, and scan in one pass.',
    runLabel: 'Run all',
  },
}

export const MODES: ModeInfo[] = [
  MODE_INFO.cutout,
  MODE_INFO.classify,
  MODE_INFO.ocr,
  MODE_INFO.analyze,
]

export const RUN_DEFAULTS = {
  trim: true,
  maxPixels: 12_000_000,
  maxResults: 6,
  minConfidence: 0,
} as const

/**
 * OCR languages for Text / All (BCP-47).
 * Android: Chinese model also reads English — no second Latin pass.
 */
export const OCR_LANGUAGES = ['zh-Hans', 'zh-Hant', 'en-US'] as const

export function modeAvailable(
  mode: Mode,
  canSegment: boolean,
  canClassify: boolean,
  canOcr: boolean,
): boolean {
  switch (mode) {
    case 'cutout':
      return canSegment
    case 'classify':
      return canClassify
    case 'ocr':
      return canOcr
    case 'analyze':
      return canSegment && canClassify && canOcr
    default: {
      const _exhaustive: never = mode
      void _exhaustive
      return false
    }
  }
}

export function firstAvailableMode(
  canSegment: boolean,
  canClassify: boolean,
  canOcr: boolean,
): Mode {
  for (const mode of MODES) {
    if (modeAvailable(mode.id, canSegment, canClassify, canOcr)) {
      return mode.id
    }
  }
  return 'cutout'
}

export function getMode(id: Mode): ModeInfo {
  return MODE_INFO[id]
}
