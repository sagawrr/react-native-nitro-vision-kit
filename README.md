<img src="assets/banner.png" alt="react-native-nitro-vision-kit" width="100%" />

[![npm version](https://img.shields.io/npm/v/react-native-nitro-vision-kit?logo=npm&label=npm)](https://www.npmjs.com/package/react-native-nitro-vision-kit)
[![CI](https://github.com/sagawrr/react-native-nitro-vision-kit/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/sagawrr/react-native-nitro-vision-kit/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/npm/l/react-native-nitro-vision-kit?color=blue)](https://github.com/sagawrr/react-native-nitro-vision-kit/blob/main/LICENSE)

Subject segmentation and image classification for React Native.

iOS: [Vision](https://developer.apple.com/documentation/vision). Android: [ML Kit](https://developers.google.com/ml-kit).

## Install

```sh
npm install react-native-nitro-vision-kit react-native-nitro-modules
cd ios && pod install
```

## Usage

```ts
import { VisionKit } from 'react-native-nitro-vision-kit'

if (!VisionKit.capabilities.supportsBackgroundRemoval) {
  throw new Error(VisionKit.capabilities.backgroundRemovalUnavailableReason)
}

const result = await VisionKit.removeBackground(photoPath, { trim: true })
const rgba = await result.toArrayBuffer()
```

```ts
const { segmentation, classifications } = await VisionKit.analyzeImage(photoPath, {
  removeBackground: { trim: true },
  classify: { maxResults: 5 },
})
```

Pass `removeBackground` and/or `classify` to `analyzeImage`. If both are set and `classify.region` is omitted, `segmentation.bounds` is used.

## API

### VisionKit

| Member | Returns |
| --- | --- |
| `capabilities` | `VisionCapabilities` |
| `removeBackground(path, options?)` | `SegmentationResult` |
| `classifyImage(path, options?)` | `Classification[]` |
| `analyzeImage(path, options)` | `ImageAnalysisResult` |

`path`: file path or `file://` URI.

### SegmentationResult

| Member | |
| --- | --- |
| `width`, `height` | Output size (px) |
| `bounds` | Foreground bounds (0–1) |
| `sourceWidth`, `sourceHeight` | Input size (px) |
| `foregroundCoverage` | Foreground ratio (0–1, threshold 0.5) |
| `centroid` | Foreground center (0–1) |
| `pixelBounds` | Foreground bounds (px) |
| `trimOrigin` | Output origin when `trim: true` |
| `instanceCount` | Detected instances |
| `hasMask` | `true` if `retainMask: true` was passed |
| `toArrayBuffer()` | RGBA buffer (async) |
| `toMaskBuffer()` | Float32 mask (async, `retainMask: true` required) |
| `saveToTemporaryFile(format, quality)` | Temp file path |

### Types

| Type | Fields |
| --- | --- |
| `BackgroundRemovalOptions` | `trim` (default `true`), `maxPixels` (default `6_000_000`), `retainMask` (default `false`) |
| `ClassificationOptions` | `maxResults`, `minConfidence` (default `0.5`), `region` |
| `AnalyzeImageOptions` | `removeBackground?`, `classify?` (one required) |
| `ImageAnalysisResult` | `segmentation?`, `classifications?` |
| `Classification` | `label`, `confidence`, `index` |
| `VisionCapabilities` | `supportsBackgroundRemoval`, `backgroundRemovalUnavailableReason?`, `supportsImageClassification` |
| `Rect`, `NormalizedPoint`, `PixelRect` | Geometry types |
| `ImageFormat` | `'png'` \| `'jpeg'` |

## Requirements

| | iOS | Android |
| --- | --- | --- |
| Segmentation | 17+, device only | ML Kit, Play services |
| Classification | 13+ | ML Kit |

Segmentation is not available on the iOS Simulator.

## License

MIT
