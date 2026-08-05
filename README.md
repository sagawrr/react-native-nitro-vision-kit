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
  <b>Subject cutouts · image labels · on-device OCR</b><br />
  React Native vision that stays on the device.<br />
  <sub>
    <a href="https://developer.apple.com/documentation/vision">Vision</a> ·
    <a href="https://developers.google.com/ml-kit">ML Kit</a> ·
    <a href="https://nitro.margelo.com">Nitro Modules</a>
  </sub>
</p>

<p align="center">
  <a href="#features">Features</a> ·
  <a href="#install">Install</a> ·
  <a href="#quick-start">Quick start</a> ·
  <a href="#api">API</a> ·
  <a href="#platform-notes">Platform</a> ·
  <a href="#example-app">Example</a>
</p>

---

## Features

- **Cutouts** — Isolate the subject (`removeBackground`). Trim, export PNG/JPEG, optional mask.
- **Labels** — Ranked classifications (`classifyImage`). Offline on both platforms.
- **OCR** — Read text (`readText`). Latin / CJK / Devanagari on Android; Vision on iOS 18+.
- **Compose** — One decode, many results (`analyzeImage`). Segment + classify + OCR together.
- **Native** — Nitro HybridObjects keep large results on the native side. Call `dispose()` when done.

<p align="center">
  <img src="assets/demo.gif" alt="Example app: cutout, labels, OCR, and analyze" width="280" />
</p>

---

## Install

```bash
npm install react-native-nitro-vision-kit react-native-nitro-modules
cd ios && pod install
```

| | Minimum |
| --- | --- |
| React Native | 0.75 |
| [`react-native-nitro-modules`](https://nitro.margelo.com) | 0.36.0 |
| iOS | 13 · cutouts **17+** · OCR **18+** |
| Android | API 24 |

> Prefer the New Architecture — Nitro is built for it.

---

## Quick start

```ts
import { VisionKit } from 'react-native-nitro-vision-kit'
```

**Cutout**

```ts
if (!VisionKit.capabilities.supportsBackgroundRemoval) {
  throw new Error(VisionKit.capabilities.backgroundRemovalUnavailableReason)
}

const cutout = await VisionKit.removeBackground(imagePath, { trim: true })
const path = await cutout.saveToTemporaryFile('png', 100)
cutout.dispose()
```

**Labels**

```ts
const labels = await VisionKit.classifyImage(imagePath, {
  maxResults: 5,
  minConfidence: 0.5,
})
```

**OCR**

```ts
const ocr = await VisionKit.readText(imagePath)
console.log(ocr.text)
ocr.dispose()
```

**Compose (one decode)**

```ts
const result = await VisionKit.analyzeImage(imagePath, {
  removeBackground: { trim: true },
  classify: { maxResults: 5 },
  readText: {},
})

result.segmentation?.dispose()
result.text?.dispose()
```

> [!NOTE]
> `removeBackground` and `readText` return HybridObjects. Export or read what you need, then call `dispose()`.

---

## API

### Capabilities

```ts
VisionKit.capabilities
```

| Field | Meaning |
| --- | --- |
| `supportsBackgroundRemoval` | Cutouts available |
| `backgroundRemovalUnavailableReason` | Why cutouts are off |
| `supportsImageClassification` | Labels available |
| `supportsTextRecognition` | OCR available |
| `supportedTextLanguages` | Language tags you can request |

### `removeBackground(path, options?)`

Returns a cutout HybridObject.

| Platform | Requirement |
| --- | --- |
| iOS | 17+ |
| Android | API 24+, Play Services, ML Kit subject segmentation (beta) |

**Export:** `saveToTemporaryFile(format, quality)` · `toArrayBuffer()` · `toMaskBuffer()`

<details>
<summary>Options &amp; result fields</summary>

| Option | Default | Meaning |
| --- | --- | --- |
| `trim` | `true` | Crop to the subject |
| `maxPixels` | `6_000_000` | Cap when loading the image |
| `retainMask` | `false` | Keep mask for `toMaskBuffer()` |

**Result:** `width`, `height`, `bounds` (`VisionRect`, 0–1), `pixelBounds`, `foregroundCoverage`, `centroid`, `instanceCount`, `hasMask`, `sourceWidth`, `sourceHeight`, `trimOrigin`

</details>

### `classifyImage(path, options?)`

Returns `{ label, confidence, index }[]`, highest confidence first.

| Platform | Notes |
| --- | --- |
| iOS | 13+ |
| Android | Model ships with the library — offline |

<details>
<summary>Options</summary>

| Option | Default | Meaning |
| --- | --- | --- |
| `maxResults` | `0` | `0` keeps all above the score floor |
| `minConfidence` | `0.5` | Lowest score to keep |
| `region` | full image | `VisionRect` (0–1) |

</details>

### `readText(path, options?)`

Returns a text HybridObject. Prefer `text` and `blockAt(i)` — `blocks` copies everything into JS.

| Platform | Notes |
| --- | --- |
| iOS | 18+ (`RecognizeTextRequest`) |
| Android | Play Services: Latin, Chinese, Japanese, Korean, Devanagari |

Image load cap: **4M pixels** (both platforms). On Android, longest side is also capped at **2048**.

<details>
<summary>Options &amp; platform quirks</summary>

| Option | Default | Platform |
| --- | --- | --- |
| `languages` | auto / Latin | Both |
| `recognitionLevel` | `accurate` | iOS |
| `region` | full image | Both (`VisionRect`) |
| `minTextHeightFraction` | unset | Both |
| `usesLanguageCorrection` | `true` | iOS |
| `customWords` | unset | iOS |
| `maxCandidates` | `1` | iOS (`1`–`10`) |

**Android:** `languages` selects script models. Non-Latin models also read Latin. Multiple non-Latin scripts can run together. A block may contain many lines.

**iOS:** Each block is one line.

</details>

### `analyzeImage(path, options)`

One decode. Pass at least one of `removeBackground`, `classify`, or `readText`.

- No subject found → `segmentation` is omitted; labels and text still run
- If classify/OCR omit `region` and a cutout ran → subject bounds are used

---

## Platform notes

### Model download (Android)

| Method | First use |
| --- | --- |
| `classifyImage` | Offline — model is packaged |
| `removeBackground` / `readText` | Downloads a Play Services model once |

Online: wait up to ~2 minutes for the first download, then offline. Offline with no model → fails immediately. iOS models ship with the system — no download step.

### Paths

| Input | iOS | Android |
| --- | --- | --- |
| Absolute path | yes | yes |
| `file://` | yes | yes |
| `content://` | no | yes |

> [!IMPORTANT]
> Only pass paths created by your app. Native code opens the path and returns pixels/text to JavaScript.

---

## Example app

```bash
cd example
npm install
cd ios && bundle install && bundle exec pod install && cd ..
npm run ios    # or: npm run android
```

After a cutout, **Keep** saves to Photos.

---

## License

[MIT](./LICENSE) · [Security](./SECURITY.md) · [Changelog](./CHANGELOG.md)
