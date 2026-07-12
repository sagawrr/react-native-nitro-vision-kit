import { Platform } from 'react-native'

/** Cool paper — not cream, not near-black. */
export const paper = '#EEF1EF'
export const ink = '#141C19'
export const mute = '#6B7571'
export const line = '#C5CDC8'
/** Optical orange — CTA / live signal */
export const signal = '#E85D04'
/** Cutout / success */
export const aqua = '#0F7A6C'
export const danger = '#C62828'
/** Stage well under the image */
export const well = '#0E1512'

export const display = Platform.select({
  ios: 'AvenirNext-Heavy',
  android: 'sans-serif-condensed',
  default: 'System',
})

export const body = Platform.select({
  ios: 'AvenirNext-Medium',
  android: 'sans-serif-medium',
  default: 'System',
})
