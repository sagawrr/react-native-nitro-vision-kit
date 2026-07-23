# Example

Demo for [react-native-nitro-vision-kit](../).

## Run

```bash
cd example
npm install
cd ios && bundle install && bundle exec pod install && cd ..
npm run ios      # or: npm run android
```

Root library uses Bun. This app uses npm.

## Use

1. Pick a photo
2. Tap **Lift**, **Read**, **Text**, or **All**
3. Tap **Keep** to save a cutout to Photos

OCR needs iOS 18+ or Android Play services (`supportsTextRecognition`).

### iOS device

Set your **Team** under Signing & Capabilities in Xcode (Automatic signing is already on).
