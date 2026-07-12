import { CameraRoll } from '@react-native-camera-roll/camera-roll'
import { toFileUri } from './format'

export async function exportImage(uri: string): Promise<void> {
  await CameraRoll.saveAsset(toFileUri(uri), { type: 'photo' })
}
