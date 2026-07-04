<img src="assets/banner.png" alt="react-native-nitro-vision-kit" width="100%" />

[![npm version](https://img.shields.io/npm/v/react-native-nitro-vision-kit?logo=npm&label=npm)](https://www.npmjs.com/package/react-native-nitro-vision-kit)
[![CI](https://github.com/sagawrr/react-native-nitro-vision-kit/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/sagawrr/react-native-nitro-vision-kit/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/npm/l/react-native-nitro-vision-kit?color=blue)](https://github.com/sagawrr/react-native-nitro-vision-kit/blob/main/LICENSE)

**react-native-nitro-vision-kit** runs subject segmentation and image classification on-device in React Native. It uses [Nitro Modules](https://nitro.margelo.com) for a type-safe bridge to Vision on iOS and ML Kit on Android.

## Overview

- **Segmentation** — cut out foreground subjects with transparent backgrounds
- **Classification** — label images with on-device confidence scores
- **Single decode** — run both operations in one pass with `analyzeImage`

| Feature | iOS | Android |
| --- | --- | --- |
| Segmentation | 17.0+ | ML Kit + Play services |
| Classification | 13.0+ | ML Kit |

Check `VisionKit.capabilities` before calling segmentation APIs.

## Installation

Install the package and its peer dependency, then run CocoaPods:

```sh
npm install react-native-nitro-vision-kit react-native-nitro-modules
cd ios && pod install
```

## Usage

Pass a local file path or `file://` URI to every method.

### Analyze an image

Decode once and optionally segment and classify together. When both run, classification uses the subject bounds unless you pass `region`.

```ts
import { VisionKit } from 'react-native-nitro-vision-kit'

const { segmentation, classifications } = await VisionKit.analyzeImage(photoPath, {
  removeBackground: { trim: true },
  classify: { maxResults: 5, minConfidence: 0.5 },
})

const pngPath = await segmentation?.saveToTemporaryFile('png', 100)
segmentation?.dispose()
```

### Remove background

```ts
const result = await VisionKit.removeBackground(photoPath, { trim: true })

const pngPath = await result.saveToTemporaryFile('png', 100)
result.dispose()
```

### Classify an image

```ts
const labels = await VisionKit.classifyImage(photoPath, {
  maxResults: 5,
  minConfidence: 0.5,
})
```

## API Reference

### `VisionKit.capabilities`

| Field | Description |
| --- | --- |
| `supportsBackgroundRemoval` | Whether segmentation is available |
| `supportsImageClassification` | Whether classification is available |
| `backgroundRemovalUnavailableReason` | Set when background removal is unavailable |

### `removeBackground(path, options?)`

Returns a `SegmentationResult`. Call `dispose()` when finished.

| Option | Default | Description |
| --- | --- | --- |
| `trim` | `true` | Crop to foreground bounds |
| `maxPixels` | `6_000_000` | Max decoded pixels |
| `retainMask` | `false` | Keep mask for `toMaskBuffer()` |

### `classifyImage(path, options?)`

Returns `{ label, confidence, index }[]` sorted by confidence.

| Option | Default | Description |
| --- | --- | --- |
| `maxResults` | `0` | Max labels (`0` = all above threshold) |
| `minConfidence` | `0.5` | Minimum confidence |
| `region` | full image | Normalized ROI (`0–1`) |

### `analyzeImage(path, options)`

Pass `removeBackground`, `classify`, or both. Returns `{ segmentation?, classifications? }`.

### `SegmentationResult`

| Method | Description |
| --- | --- |
| `saveToTemporaryFile(format, quality)` | Write PNG or JPEG to a temp file |
| `toArrayBuffer()` | Premultiplied RGBA bytes |
| `toMaskBuffer()` | Float32 mask (`retainMask: true`) |
| `dispose()` | Release native memory |

| Property | Description |
| --- | --- |
| `width`, `height` | Output size in pixels |
| `bounds` | Subject bounds, normalized `0–1` |
| `foregroundCoverage` | Foreground pixel ratio |
| `hasMask` | Whether `toMaskBuffer()` is available |

## License

MIT
