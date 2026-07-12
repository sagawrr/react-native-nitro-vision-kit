# Example

Demo app for [react-native-nitro-vision-kit](../).

Pick a photo, then **Lift**, **Read**, or **Both**. **Keep** saves a cutout to your library.

## Setup

```bash
cd example
npm install
cd ios && bundle install && bundle exec pod install && cd ..
npm run ios      # or: npm run android
```

The example uses npm. The library at the repo root uses Bun.

### iOS device

Select your **Team** under Signing & Capabilities in Xcode. The project already uses Automatic signing and a UIScene lifecycle (required on iOS 26+ SDKs).
