import { Platform } from 'react-native'
import ImageCropPicker from 'react-native-image-crop-picker'
import {
  PERMISSIONS,
  RESULTS,
  check,
  request,
  type Permission,
} from 'react-native-permissions'
import type { PhotoAsset } from './samplePhotos'

function photoLibraryPermission(): Permission | null {
  if (Platform.OS === 'ios') return PERMISSIONS.IOS.PHOTO_LIBRARY
  if (Platform.OS === 'android') {
    return typeof Platform.Version === 'number' && Platform.Version >= 33
      ? null
      : PERMISSIONS.ANDROID.READ_EXTERNAL_STORAGE
  }
  return null
}

async function ensurePhotoLibraryPermission(): Promise<void> {
  const permission = photoLibraryPermission()
  if (!permission) return
  const status = await check(permission)
  if (status === RESULTS.GRANTED || status === RESULTS.LIMITED) return
  const next = await request(permission)
  if (next === RESULTS.GRANTED || next === RESULTS.LIMITED) return
  throw new Error(
    'Photo library permission was denied. Enable it in system settings and try again.',
  )
}

function isCancel(err: unknown): boolean {
  if (!err || typeof err !== 'object') return false
  const code = 'code' in err ? String(err.code) : ''
  const message = 'message' in err ? String(err.message) : ''
  return (
    code === 'E_PICKER_CANCELLED' || message.toLowerCase().includes('cancel')
  )
}

export async function pickPhotoFromLibrary(): Promise<{
  uri: string
  asset: PhotoAsset
} | null> {
  await ensurePhotoLibraryPermission()
  try {
    const image = await ImageCropPicker.openPicker({
      mediaType: 'photo',
      cropping: false,
      includeBase64: false,
      compressImageQuality: 1,
      forceJpg: false,
    })
    const uri = image.path.startsWith('file://')
      ? image.path
      : `file://${image.path}`
    return {
      uri,
      asset: {
        uri,
        width: image.width,
        height: image.height,
        fileName: uri.split('/').pop() ?? 'photo.jpg',
      },
    }
  } catch (err) {
    if (isCancel(err)) return null
    throw err instanceof Error ? err : new Error(String(err))
  }
}
