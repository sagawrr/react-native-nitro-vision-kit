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
  Subject cutouts, image labels, and OCR on device for React Native.<br />
  <sub>Vision on iOS · ML Kit on Android · <a href="https://nitro.margelo.com">Nitro Modules</a></sub>
</p>

---

## Install

```bash
npm install react-native-nitro-vision-kit react-native-nitro-modules
cd ios && pod install
```

---

## API

| Method | Returns | Platform |
| --- | --- | --- |
| `removeBackground` | Transparent PNG cutout (HybridObject) | iOS 17+ · Android API 24+ (Play Services, **beta**) |
| `classifyImage` | `{ label, confidence, index }[]` | iOS 13+ · Android (bundled ML Kit) |
| `readText` | OCR HybridObject (`text`, `blockAt`, …) | iOS 18+ · Android (Play Services) |
| `analyzeImage` | Any mix of the three, one decode | same as the ops you pass |

**Image path:** local path or `file://`. Android also accepts `content://`.

**Memory:** `removeBackground` / `readText` results stay native until you `dispose()`. Call `saveToTemporaryFile` / `toArrayBuffer` **before** `dispose()`.

<table>
  <tr>
    <td width="42%" align="center" valign="top">
      <img src="assets/demo.gif" alt="Example playground: removeBackground, classifyImage, readText, analyzeImage" width="260" />
    </td>
    <td valign="middle">
      <p><strong>Example app</strong></p>
      <p>Modes map to the four methods above. After a cutout, <strong>Keep</strong> writes to Photos.</p>
      <p><a href="./example"><code>example/</code></a></p>
    </td>
  </tr>
</table>

---

## Capabilities

```ts
import { VisionKit } from 'react-native-nitro-vision-kit'

const {
  supportsBackgroundRemoval,
  backgroundRemovalUnavailableReason,
  supportsImageClassification,
  supportsTextRecognition,
  supportedTextLanguages, // tags this build can request — not “already downloaded”
} = VisionKit.capabilities
```

---

## Examples

### `removeBackground`

```ts
const cutout = await VisionKit.removeBackground(path, { trim: true })
const png = await cutout.saveToTemporaryFile('png', 100)
cutout.dispose()
```

### `classifyImage`

```ts
const labels = await VisionKit.classifyImage(path, {
  maxResults: 5,
  minConfidence: 0.5,
})
```

### `readText`

```ts
if (!VisionKit.capabilities.supportsTextRecognition) return

const ocr = await VisionKit.readText(path, {
  languages: ['zh-Hans', 'en-US'],
})
console.log(ocr.text)
ocr.dispose()
```

### `analyzeImage`

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

Rules:

1. Pass at least one of `removeBackground` / `classify` / `readText`
2. No subject → `segmentation` omitted; classify/OCR still run
3. Cutout + classify/OCR without `region` → ROI = subject bounds

---

## Android model download

| Method | First launch |
| --- | --- |
| `classifyImage` | Offline OK (bundled) |
| `removeBackground` / `readText` | Downloads Play Services model once |

- Online: waits/prefetches (up to ~2 min), then offline forever
- Offline with no model: fails fast — connect once, open the app, retry
- iOS: models ship with the OS

---

## Options

<details>
<summary><code>removeBackground</code></summary>

| Option | Default | |
| --- | --- | --- |
| `trim` | `true` | Crop to subject |
| `maxPixels` | `6_000_000` | Decode cap |
| `retainMask` | `false` | Keep mask for `toMaskBuffer()` |

</details>

<details>
<summary><code>classifyImage</code></summary>

| Option | Default | |
| --- | --- | --- |
| `maxResults` | `0` | `0` = all above threshold |
| `minConfidence` | `0.5` | |
| `region` | full image | Normalized `0–1` |

</details>

<details>
<summary><code>readText</code></summary>

| Option | Default | Who |
| --- | --- | --- |
| `languages` | auto / Latin | iOS auto · Android script map |
| `recognitionLevel` | `accurate` | iOS only |
| `region` | full image | Both |
| `minTextHeightFraction` | unset | Both |
| `usesLanguageCorrection` | `true` | iOS (off for Chinese-only) |
| `customWords` | unset | iOS |
| `maxCandidates` | `1` | iOS (`1–10`) |

Decode cap **4M px**. Android longest edge **2048**.

Android `languages` selects script model(s): Latin / Chinese / Japanese / Korean / Devanagari. Non-Latin models also read Latin. Mixed non-Latin scripts run in parallel.

</details>

---

## Results

**`removeBackground`** — `saveToTemporaryFile` / `toArrayBuffer` / `toMaskBuffer`, then `dispose()`. Also: `width`, `height`, `bounds`, `pixelBounds`, `foregroundCoverage`, `centroid`, `instanceCount`.

**`classifyImage`** — `{ label, confidence, index }[]`, high → low.

**`readText`** — `text`, `blockAt(i)` (preferred), `blocks` (copies all to JS), then `dispose()`. Android: multi-line blocks. iOS: one line per observation.

Temp files from `saveToTemporaryFile` are yours to delete.

---

## Run the example

```bash
cd example
npm install
cd ios && bundle install && bundle exec pod install && cd ..
npm run ios   # or: npm run android
```

---

## Requirements

- React Native ≥ 0.75 and [react-native-nitro-modules](https://nitro.margelo.com)
- iOS Simulator: `removeBackground` unavailable (no subject segmentation)
- New Architecture recommended (Nitro / JSI)

## License

[MIT](./LICENSE) · [Security](./SECURITY.md)
