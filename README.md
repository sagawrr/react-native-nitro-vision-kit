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
  <strong>Lift the subject. Read the frame. Keep the cutout.</strong><br />
  On-device vision for React Native — nothing leaves the phone.<br />
  <sub>Vision on iOS · ML Kit on Android · <a href="https://nitro.margelo.com">Nitro Modules</a></sub>
</p>

---

## What it does

| Verb | Method | Output |
| --- | --- | --- |
| **Lift** | `removeBackground` | Transparent subject cutout + bounds |
| **Read** | `classifyImage` | Labels with confidence |
| **Text** | `readText` | On-device OCR (blocks / lines) |
| **Compose** | `analyzeImage` | One decode → any mix of the three |

All on-device. Local path or `file://` only (cache remotes first). Orientation handled for you.

| | Lift | Read | Text |
| --- | --- | --- | --- |
| **iOS** | 17+ | 13+ | 18+ |
| **Android** | API 24+ · Play services | ML Kit | ML Kit Latin · Play services |

---

<table>
  <tr>
    <td width="42%" align="center" valign="top">
      <img src="assets/demo.gif" alt="Playground: Lift, Read, Text, All" width="260" />
    </td>
    <td valign="middle">
      <p><strong>Playground</strong></p>
      <p>
        <code>Lift</code> · <code>Read</code> · <code>Text</code> · <code>All</code><br />
        then <strong>Keep</strong> saves the cutout.
      </p>
      <p><a href="./example"><code>example/</code></a></p>
    </td>
  </tr>
</table>

---

## 1. Install

```bash
npm install react-native-nitro-vision-kit react-native-nitro-modules
cd ios && pod install
```

## 2. Check capabilities

```ts
import { VisionKit } from 'react-native-nitro-vision-kit'

const {
  supportsBackgroundRemoval,
  backgroundRemovalUnavailableReason,
  supportsImageClassification,
  supportsTextRecognition,
  supportedTextLanguages,
} = VisionKit.capabilities
```

| Flag | Gate |
| --- | --- |
| `supportsBackgroundRemoval` | Lift |
| `backgroundRemovalUnavailableReason` | Why Lift is off (when false) |
| `supportsImageClassification` | Read |
| `supportsTextRecognition` | Text |
| `supportedTextLanguages` | OCR language tags (empty if Text off) |

## 3. Call what you need

### Lift

```ts
const cutout = await VisionKit.removeBackground(path, { trim: true })
const png = await cutout.saveToTemporaryFile('png', 100)
cutout.dispose() // always
```

### Read

```ts
const labels = await VisionKit.classifyImage(path, {
  maxResults: 5,
  minConfidence: 0.5,
})
// [{ label, confidence }, ...]
```

### Text

```ts
if (!VisionKit.capabilities.supportsTextRecognition) return

const ocr = await VisionKit.readText(path)
console.log(ocr.text)
ocr.dispose() // prefer for large docs
```

### Compose (one decode)

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

Pass at least one of `removeBackground` / `classify` / `readText`.

No subject found → `segmentation` omitted; Read/Text still run when requested.

When Lift + Read/Text run and you omit `region`, Read/Text use `segmentation.bounds`.

---

## Options

<details>
<summary><strong>removeBackground</strong> / <code>analyzeImage.removeBackground</code></summary>

| Option | Default | |
| --- | --- | --- |
| `trim` | `true` | Crop to subject |
| `maxPixels` | `6_000_000` | Decode cap (`width × height`) |
| `retainMask` | `false` | Keep mask for `toMaskBuffer()` |

</details>

<details>
<summary><strong>classifyImage</strong> / <code>analyzeImage.classify</code></summary>

| Option | Default | |
| --- | --- | --- |
| `maxResults` | `0` | Cap labels (`0` = all above threshold) |
| `minConfidence` | `0.5` | Minimum score |
| `region` | full image | Normalized ROI (`0–1`) |

</details>

<details>
<summary><strong>readText</strong> / <code>analyzeImage.readText</code></summary>

| Option | Default | Who |
| --- | --- | --- |
| `languages` | platform default | iOS BCP-47 · Android ignores (Latin) |
| `recognitionLevel` | `accurate` | iOS only (`accurate` \| `fast`) |
| `region` | full image | Both |
| `minTextHeightFraction` | unset | Both |
| `usesLanguageCorrection` | `true` | iOS only |
| `customWords` | unset | iOS only (needs correction) |
| `maxCandidates` | `1` | iOS only (`1–10`) |

Decode cap: **4M pixels**. Android longest edge also capped at **2048**.

</details>

---

## Results

### Segmentation (`removeBackground` / `analyzeImage.segmentation`)

| | |
| --- | --- |
| `saveToTemporaryFile(format, quality)` | Write PNG or JPEG |
| `toArrayBuffer()` | Premultiplied RGBA |
| `toMaskBuffer()` | Float32 mask (`retainMask: true`) |
| `dispose()` | Free native memory |
| `width` / `height` | Output size |
| `bounds` / `pixelBounds` | Subject box |
| `foregroundCoverage` / `centroid` / `instanceCount` | Mask stats |
| `sourceWidth` / `sourceHeight` / `trimOrigin` | Source mapping |

### Classification (`classifyImage` / `analyzeImage.classifications`)

Array of `{ label: string, confidence: number }`, sorted high → low.

### Text (`readText` / `analyzeImage.text`)

| | |
| --- | --- |
| `text` | Full string (blocks joined by newlines) |
| `blockAt(i)` | One block (preferred) |
| `blocks` | All blocks (copies into JS) |
| `dispose()` | Free native memory early |

Prefer `text` / `blockAt` over `blocks` for large docs.

Android blocks can hold many lines. iOS maps each observation to a one-line block. Optional line fields: `confidence`, `language` (ML Kit / iOS 26+), `angleDegrees` (Android), `cornerPoints`, `candidates` (iOS when `maxCandidates` > 1).

---

## Playground

```bash
cd example
npm install
cd ios && bundle install && bundle exec pod install && cd ..
npm run ios   # or: npm run android
```

Photo → **Lift** / **Read** / **Text** / **All** → **Keep**.

## License

[MIT](./LICENSE)
