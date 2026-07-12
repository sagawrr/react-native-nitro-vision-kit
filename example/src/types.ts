import type { Classification } from 'react-native-nitro-vision-kit'

export type Mode = 'cutout' | 'classify' | 'analyze'

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
  analyze: {
    id: 'analyze',
    label: 'Both',
    purpose: 'Lift and read in one pass.',
    runLabel: 'Lift & read',
  },
}

export const MODES: ModeInfo[] = [
  MODE_INFO.cutout,
  MODE_INFO.classify,
  MODE_INFO.analyze,
]

export const RUN_DEFAULTS = {
  trim: true,
  maxPixels: 12_000_000,
  maxResults: 6,
  minConfidence: 0,
} as const

export function modeAvailable(
  mode: Mode,
  canSegment: boolean,
  canClassify: boolean,
): boolean {
  if (mode === 'cutout') return canSegment
  if (mode === 'classify') return canClassify
  return canSegment && canClassify
}

export function firstAvailableMode(
  canSegment: boolean,
  canClassify: boolean,
): Mode {
  for (const mode of MODES) {
    if (modeAvailable(mode.id, canSegment, canClassify)) {
      return mode.id
    }
  }
  return 'cutout'
}

export function getMode(id: Mode): ModeInfo {
  return MODE_INFO[id]
}
