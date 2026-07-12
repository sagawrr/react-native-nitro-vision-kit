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

<table>
  <tr>
    <td width="42%" align="center" valign="top">
      <img src="assets/demo.gif" alt="Playground: Lift subject, see the cutout, read labels, keep to Photos" width="260" />
    </td>
    <td valign="middle">
      <p><strong>The playground in motion</strong></p>
      <p>
        <code>Lift</code> frees the subject from its background.<br />
        <code>Read</code> names what’s in the frame.<br />
        <code>Both</code> does it in one pass — then <strong>Keep</strong> saves the cutout.
      </p>
      <p>
        All on-device. Orientation handled for you.<br />
        Run it yourself in <a href="./example"><code>example/</code></a>.
      </p>
    </td>
  </tr>
</table>

---

## Install

```bash
npm install react-native-nitro-vision-kit react-native-nitro-modules
cd ios && pod install
```

## Quick start

Pass a **local** path or `file://` URI (cache remote images first).

```ts
import { VisionKit } from 'react-native-nitro-vision-kit'

const { segmentation, classifications } = await VisionKit.analyzeImage(path, {
  removeBackground: { trim: true },
  classify: { maxResults: 5, minConfidence: 0.5 },
})

const png = await segmentation?.saveToTemporaryFile('png', 100)
segmentation?.dispose()
```

Or call them apart:

```ts
const cutout = await VisionKit.removeBackground(path, { trim: true })
const labels = await VisionKit.classifyImage(path, { maxResults: 5 })

await cutout.saveToTemporaryFile('png', 100)
cutout.dispose()
```

> Always `dispose()` a segmentation result when you’re done with it.

## API

Three verbs. Same kit.

| | Method | What it does |
| --- | --- | --- |
| **Lift** | `removeBackground` | Transparent subject cutout |
| **Read** | `classifyImage` | Labels with confidence |
| **Both** | `analyzeImage` | One decode — segment and/or classify |

```ts
const { supportsBackgroundRemoval, backgroundRemovalUnavailableReason } =
  VisionKit.capabilities
```

| Platform | Segment | Classify |
| --- | --- | --- |
| iOS | 17.0+ | 13.0+ |
| Android | ML Kit + Play services | ML Kit |

## Options

<details>
<summary><strong>removeBackground</strong> / <code>analyzeImage.removeBackground</code></summary>

| Option | Default | |
| --- | --- | --- |
| `trim` | `true` | Crop to the subject |
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

When `analyzeImage` runs both and you omit `region`, classification uses the subject bounds.

</details>

## Segmentation result

| | |
| --- | --- |
| `saveToTemporaryFile(format, quality)` | Write a PNG or JPEG |
| `toArrayBuffer()` | Premultiplied RGBA bytes |
| `toMaskBuffer()` | Float32 mask (needs `retainMask`) |
| `dispose()` | Free native memory |
| `width` / `height` | Output size |
| `bounds` | Subject box, normalized `0–1` |
| `foregroundCoverage` | Foreground pixel ratio |

## Playground

```bash
cd example
npm install
cd ios && bundle install && bundle exec pod install && cd ..
npm run ios   # or: npm run android
```

Pick a photo → **Lift**, **Read**, or **Both** → **Keep** to Photos.

## License

[MIT](./LICENSE)
