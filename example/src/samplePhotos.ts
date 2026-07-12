import { Image } from 'react-native'

export type PhotoAsset = {
  uri?: string
  width?: number
  height?: number
  fileName?: string | null
}

export type SamplePhoto = {
  id: string
  source?: number
  asset: PhotoAsset
}

function bundled(n: number, source: number, fileName: string): SamplePhoto {
  const resolved = Image.resolveAssetSource(source)
  return {
    id: `bundled-${n}`,
    source,
    asset: {
      uri: resolved.uri,
      width: resolved.width,
      height: resolved.height,
      fileName,
    },
  }
}

/** Clear-subject Unsplash photos — good for lift + read demos. */
export const SAMPLE_PHOTOS: SamplePhoto[] = [
  bundled(1, require('../assets/images/portrait.jpg'), 'portrait.jpg'),
  bundled(2, require('../assets/images/product.jpg'), 'product.jpg'),
  bundled(3, require('../assets/images/pet.jpg'), 'pet.jpg'),
]

export function photoFromAsset(asset: PhotoAsset): SamplePhoto {
  return {
    id: `added-${asset.uri ?? Date.now()}`,
    asset,
  }
}

export function thumbSource(photo: SamplePhoto) {
  if (photo.source != null) {
    return photo.source
  }
  return { uri: photo.asset.uri }
}
