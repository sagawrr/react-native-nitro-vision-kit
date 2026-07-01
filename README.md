# react-native-nitro-vision-kit

**On-device computer vision for React Native** — background removal and image classification, powered by native Vision (iOS) and ML Kit (Android), with zero-copy pixel access. Built with [Nitro Modules](https://nitro.margelo.com).

- 🎯 **Subject segmentation** — lift the primary subject with a soft alpha matte, no server round-trip.
- 🏷️ **Image classification** — rank what's in the frame.
- ⚡ **Zero-copy pixels** — masked output stays native; grab an `ArrayBuffer` only when you need it, or hand it straight to [`react-native-nitro-image`](https://github.com/margelo/react-native-nitro-image) without a PNG round-trip.
- 🤖 **Native engines** — Apple Vision `VNGenerateForegroundInstanceMaskRequest` (iOS 17+) and Google ML Kit Subject Segmentation (Android, via Google Play services).

## Platform Support

| Feature | iOS | Android |
| --- | --- | --- |
| Background removal | iOS 17+ | ✅ (ML Kit, unbundled model) |
| Image classification | iOS 13+ | ✅ (ML Kit) |

On iOS < 17, `capabilities.supportsBackgroundRemoval` reports `false` and `removeBackground(...)` rejects with a descriptive error.

## Installation

```sh
npm install react-native-nitro-vision-kit react-native-nitro-modules
```

iOS:

```sh
cd ios && pod install
```

Android: ML Kit downloads its model after install via Google Play services (the required `<meta-data>` is shipped by this library's manifest).

## Usage

### Remove the background (cutout / sticker)

```ts
import { VisionKit } from 'react-native-nitro-vision-kit'

if (VisionKit.capabilities.supportsBackgroundRemoval) {
  const result = await VisionKit.removeBackground('/path/to/photo.jpg', { trim: true })

  // Zero-copy premultiplied RGBA — hand it straight to nitro-image, no PNG encode/decode.
  const rgba = result.toArrayBuffer()
}
```

The returned `SegmentationResult` is a native `HybridObject`: the masked bytes live on the native side and are read lazily through `toArrayBuffer()` or `saveToTemporaryFile(format, quality)`. Dispose it (or let it be garbage-collected) when done.

### Compose with `react-native-nitro-image`

`toArrayBuffer()` returns **premultiplied RGBA**, exactly what `loadFromRawPixelData` expects:

```ts
import { Images } from 'react-native-nitro-image'

const r = await VisionKit.removeBackground(path)
const image = Images.loadFromRawPixelData({
  buffer: r.toArrayBuffer(),
  width: r.width,
  height: r.height,
  pixelFormat: 'RGBA',
})
```

### Classify an image

```ts
const labels = await VisionKit.classifyImage('/path/to/photo.jpg', {
  maxResults: 5,
  minConfidence: 0.5,
})
// [{ label: 'Coffee mug', confidence: 0.92, index: 0 }, ...]
```

## API

### `VisionKit`

The singleton entry point.

| Member | Returns | Description |
| --- | --- | --- |
| `capabilities` | `VisionCapabilities` | Which features the current device supports. |
| `removeBackground(path, options?)` | `Promise<SegmentationResult>` | Lifts the primary subject; rejects if unsupported. |
| `classifyImage(path, options?)` | `Promise<Classification[]>` | Ranks labels for the image; rejects if unsupported. |

### `SegmentationResult`

| Member | Description |
| --- | --- |
| `width`, `height` | Pixel dimensions of the masked cutout. |
| `bounds` | Normalized (`0`–`1`) rect of the subject within the original image. |
| `toArrayBuffer()` | Zero-copy **premultiplied RGBA**. |
| `saveToTemporaryFile(format, quality)` | Encodes to a temp file (`'png'` keeps alpha). |

## License

MIT
