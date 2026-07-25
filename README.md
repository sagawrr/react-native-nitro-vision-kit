<p align="center">
  <img src="assets/banner.png" alt="react-native-nitro-vision-kit" width="100%" />
</p>

<p align="center">
  <a href="https://www.npmjs.com/package/react-native-nitro-vision-kit"><img src="https://img.shields.io/npm/v/react-native-nitro-vision-kit?style=flat-square&logo=npm&label=npm" alt="npm" /></a>
  <a href="https://github.com/sagawrr/react-native-nitro-vision-kit/actions/workflows/ci.yml"><img src="https://img.shields.io/github/actions/workflow/status/sagawrr/react-native-nitro-vision-kit/ci.yml?branch=main&style=flat-square&label=ci" alt="CI" /></a>
  <a href="./LICENSE"><img src="https://img.shields.io/npm/l/react-native-nitro-vision-kit?style=flat-square" alt="MIT" /></a>
</p>

<h1 align="center">Nitro Vision</h1>

<p align="center">
  <strong>Subject cutouts · image labels · OCR</strong><br />
  On-device vision for React Native.<br />
  <sub>
    <a href="https://developer.apple.com/documentation/vision">Vision</a> on iOS ·
    <a href="https://developers.google.com/ml-kit">ML Kit</a> on Android ·
    <a href="https://nitro.margelo.com">Nitro Modules</a>
  </sub>
</p>

<p align="center">
  <a href="#install">Install</a> ·
  <a href="#quick-start">Quick start</a> ·
  <a href="#api">API</a> ·
  <a href="#platform-notes">Platform notes</a> ·
  <a href="#example-app">Example</a>
</p>

---

## Features

- **Cut out subjects** — `removeBackground` → transparent PNG + geometry
- **Label what’s in the frame** — `classifyImage` → `{ label, confidence }[]`
- **Read text on device** — `readText` → full string + blocks (Latin / CJK / Devanagari on Android)
- **One decode, many tasks** — `analyzeImage` runs any mix in one pass
- **Native memory control** — HybridObject results; `dispose()` when done

<p align="center">
  <img src="assets/demo.gif" alt="Example app running removeBackground, classifyImage, readText, and analyzeImage" width="280" />
</p>

<p align="center">
  <sub>Example app — <a href="./example"><code>example/</code></a></sub>
</p>

---

## Install

```bash
npm install react-native-nitro-vision-kit react-native-nitro-modules
cd ios && pod install
```

| | Minimum |
| --- | --- |
| React Native | ≥ 0.75 |
| Peer | [`react-native-nitro-modules`](https://nitro.margelo.com) ^0.36 |
| iOS | 13+ (cutouts 17+, OCR 18+) |
| Android | API 24+ |

> **New Architecture** recommended (Nitro / JSI).

---

## Quick start

```ts
import { VisionKit } from 'react-native-nitro-vision-kit'

// 1. Gate on capabilities
const { supportsBackgroundRemoval, supportsTextRecognition } = VisionKit.capabilities

// 2. Local path or file://  (Android also: content://)
const path = '/path/to/photo.jpg'

// 3. Run → export → dispose
if (supportsBackgroundRemoval) {
  const cutout = await VisionKit.removeBackground(path, { trim: true })
  const png = await cutout.saveToTemporaryFile('png', 100)
  cutout.dispose()
}
```

> Always **export before `dispose()`**. GC frees later; `dispose()` frees now.

---

## API

### Capabilities

```ts
const {
  supportsBackgroundRemoval,
  backgroundRemovalUnavailableReason,
  supportsImageClassification,
  supportsTextRecognition,
  supportedTextLanguages,
} = VisionKit.capabilities
```

| Flag | Meaning |
| --- | --- |
| `supportsBackgroundRemoval` | Cutouts available |
| `backgroundRemovalUnavailableReason` | Why cutouts are off (string) |
| `supportsImageClassification` | Labels available |
| `supportsTextRecognition` | OCR available |
| `supportedTextLanguages` | Tags this build can request (not “already downloaded”) |

---

### `removeBackground`

Transparent subject cutout.

```ts
const cutout = await VisionKit.removeBackground(path, { trim: true })
const png = await cutout.saveToTemporaryFile('png', 100)
cutout.dispose()
```

| | |
| --- | --- |
| **iOS** | 17+ |
| **Android** | API 24+ · Play Services · ML Kit subject segmentation **beta** |
| **Result** | HybridObject — export with `saveToTemporaryFile` / `toArrayBuffer` / `toMaskBuffer` |

<details>
<summary>Options</summary>

| Option | Default | Description |
| --- | --- | --- |
| `trim` | `true` | Crop to subject bounds |
| `maxPixels` | `6_000_000` | Decode pixel cap |
| `retainMask` | `false` | Keep mask for `toMaskBuffer()` |

</details>

<details>
<summary>Result fields</summary>

`width` · `height` · `bounds` · `pixelBounds` · `foregroundCoverage` · `centroid` · `instanceCount` · `hasMask`

Temp files from `saveToTemporaryFile` are yours to delete.

</details>

---

### `classifyImage`

Labels with confidence (high → low).

```ts
const labels = await VisionKit.classifyImage(path, {
  maxResults: 5,
  minConfidence: 0.5,
})
// → [{ label, confidence, index }, ...]
```

| | |
| --- | --- |
| **iOS** | 13+ |
| **Android** | Bundled ML Kit — works offline on first launch |

<details>
<summary>Options</summary>

| Option | Default | Description |
| --- | --- | --- |
| `maxResults` | `0` | `0` = all above threshold |
| `minConfidence` | `0.5` | Minimum score |
| `region` | full image | Normalized `0–1` rect |

</details>

---

### `readText`

On-device OCR.

```ts
if (!VisionKit.capabilities.supportsTextRecognition) return

const ocr = await VisionKit.readText(path, {
  languages: ['zh-Hans', 'en-US'],
})
console.log(ocr.text)       // full string
console.log(ocr.blockAt(0)) // one block (preferred)
ocr.dispose()
```

| | |
| --- | --- |
| **iOS** | 18+ · `RecognizeTextRequest` |
| **Android** | Play Services · Latin / Chinese / Japanese / Korean / Devanagari |
| **Result** | HybridObject — prefer `text` / `blockAt(i)`; `blocks` copies everything to JS |

<details>
<summary>Options</summary>

| Option | Default | Platform |
| --- | --- | --- |
| `languages` | auto / Latin | iOS auto-detect · Android script map |
| `recognitionLevel` | `accurate` | iOS |
| `region` | full image | Both |
| `minTextHeightFraction` | unset | Both |
| `usesLanguageCorrection` | `true` | iOS (forced off for Chinese-only) |
| `customWords` | unset | iOS |
| `maxCandidates` | `1` | iOS (`1–10`) |

Decode cap **4M px**. Android longest edge **2048**.

**Android `languages`:** picks script model(s). Non-Latin models also read Latin. Mixed non-Latin scripts run in parallel.

</details>

<details>
<summary>Block shape</summary>

Android: multi-line blocks. iOS: one line per observation.

Optional per line/block: `confidence`, `language`, `angleDegrees` (Android), `cornerPoints`, `candidates` (iOS).

</details>

---

### `analyzeImage`

One decode → any mix of cutout / labels / OCR.

```ts
const { segmentation, classifications, text } = await VisionKit.analyzeImage(path, {
  removeBackground: { trim: true },
  classify: { maxResults: 5, minConfidence: 0.5 },
  readText: {},
})

await segmentation?.saveToTemporaryFile('png', 100)
segmentation?.dispose()
text?.dispose()
```

| Rule | Behavior |
| --- | --- |
| Ops | Pass at least one of `removeBackground` / `classify` / `readText` |
| No subject | `segmentation` omitted; labels/OCR still run |
| ROI | Cutout + labels/OCR without `region` → uses subject bounds |

---

## Platform notes

### Android model download

| Method | First launch |
| --- | --- |
| `classifyImage` | Offline immediately (bundled) |
| `removeBackground` / `readText` | Downloads Play Services model once |

- **Online** — waits / prefetches (up to ~2 min), then works offline
- **Offline, model missing** — fails fast; connect once, open the app, retry
- **iOS** — models ship with the OS; no download step

### Paths

| Input | iOS | Android |
| --- | --- | --- |
| Absolute path | ✅ | ✅ |
| `file://` | ✅ | ✅ |
| `content://` | — | ✅ |

---

## Example app

```bash
cd example
npm install
cd ios && bundle install && bundle exec pod install && cd ..
npm run ios    # or: npm run android
```

Modes call the four APIs above. After a cutout, **Keep** saves to Photos.

---

## License

[MIT](./LICENSE) · [Security policy](./SECURITY.md) · [Changelog](./CHANGELOG.md)
