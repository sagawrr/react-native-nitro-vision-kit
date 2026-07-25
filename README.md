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
  On-device vision for React Native — nothing leaves the phone.<br />
  <sub>Vision on iOS · ML Kit on Android · <a href="https://nitro.margelo.com">Nitro Modules</a></sub>
</p>

---

## Do this first

```bash
npm install react-native-nitro-vision-kit react-native-nitro-modules
cd ios && pod install
```

Then pick one call below. Always **export → then `dispose()`**.

---

## What you get

| Call | Method | Needs |
| --- | --- | --- |
| Lift | `removeBackground` | iOS 17+ · Android Play Services (beta model) |
| Read | `classifyImage` | iOS 13+ · Android bundled ML Kit |
| Text | `readText` | iOS 18+ · Android Play Services |
| Compose | `analyzeImage` | same as the ops you pass |

**Paths:** local file path or `file://`. Android also accepts `content://`.

**Memory:** Lift/Text return Nitro HybridObjects. Save or copy first, then `dispose()`. GC frees later if you forget; dispose frees now.

---

## 1. Capabilities

```ts
import { VisionKit } from 'react-native-nitro-vision-kit'

const c = VisionKit.capabilities
// c.supportsBackgroundRemoval
// c.supportsImageClassification
// c.supportsTextRecognition
// c.supportedTextLanguages  // tags this build can request (not “already downloaded”)
```

---

## 2. Lift

```ts
const cutout = await VisionKit.removeBackground(path, { trim: true })
const png = await cutout.saveToTemporaryFile('png', 100)
cutout.dispose()
```

---

## 3. Read

```ts
const labels = await VisionKit.classifyImage(path, {
  maxResults: 5,
  minConfidence: 0.5,
})
```

---

## 4. Text

```ts
if (!VisionKit.capabilities.supportsTextRecognition) return

const ocr = await VisionKit.readText(path, {
  languages: ['zh-Hans', 'en-US'],
})
console.log(ocr.text)
ocr.dispose()
```

---

## 5. Compose (one decode)

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

1. Pass at least one of `removeBackground` / `classify` / `readText`
2. No subject → `segmentation` omitted; Read/Text still run
3. Lift + Read/Text without `region` → uses subject bounds

---

## Android first run

| Feature | First launch |
| --- | --- |
| **Read** | Works offline immediately (bundled) |
| **Lift / Text** | Needs Play Services model download once |

- **Online:** kit waits / prefetches (can take up to ~2 min), then works offline forever
- **Offline, model missing:** fails fast — connect once, open the app, retry
- **iOS:** models ship with the OS — no download

Android Lift uses ML Kit subject segmentation **beta**.

---

## Options (skim)

<details>
<summary><strong>removeBackground</strong></summary>

| Option | Default | |
| --- | --- | --- |
| `trim` | `true` | Crop to subject |
| `maxPixels` | `6_000_000` | Decode cap |
| `retainMask` | `false` | Keep mask for `toMaskBuffer()` |

</details>

<details>
<summary><strong>classifyImage</strong></summary>

| Option | Default | |
| --- | --- | --- |
| `maxResults` | `0` | `0` = all above threshold |
| `minConfidence` | `0.5` | |
| `region` | full image | Normalized `0–1` |

</details>

<details>
<summary><strong>readText</strong></summary>

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

**Android `languages`:** picks script model(s) — Latin / Chinese / Japanese / Korean / Devanagari. Non-Latin models also read Latin. Mixed non-Latin scripts run in parallel.

</details>

---

## Results

**Lift** — `saveToTemporaryFile` / `toArrayBuffer` / `toMaskBuffer` → then `dispose()`. Geometry: `width`, `height`, `bounds`, `pixelBounds`, `foregroundCoverage`, `centroid`, `instanceCount`.

**Read** — `{ label, confidence, index }[]` high → low.

**Text** — `text`, `blockAt(i)` (preferred), `blocks` (copies all to JS), then `dispose()`. Android: multi-line blocks. iOS: one line per observation.

Temp files from `saveToTemporaryFile` are yours to delete.

---

## Playground

```bash
cd example
npm install
cd ios && bundle install && bundle exec pod install && cd ..
npm run ios   # or: npm run android
```

Demo: `Lift` · `Read` · `Text` · `All` → **Keep**. See [`example/`](./example).

---

## Requirements

- React Native ≥ 0.75 · [Nitro Modules](https://nitro.margelo.com)
- iOS Simulator: Lift unavailable (no subject segmentation)
- New Architecture recommended (Nitro / JSI)

## License

[MIT](./LICENSE) · [Security](./SECURITY.md)
