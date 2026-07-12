# VisionKit Example

Demo for [`react-native-nitro-vision-kit`](../).

Pick a photo → **Lift**, **Read**, or **Both** → run on-device.

- **Lift** — cut the subject free
- **Read** — name what’s in the frame
- **Both** — one pass via `analyzeImage`
- **Keep** — save a cutout to your library

## Setup

```sh
cd example
npm install
cd ios && bundle install && bundle exec pod install && cd ..
npm run ios
# or
npm run android
```

The example app uses npm. The library at the repo root uses Bun.
