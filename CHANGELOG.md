# Changelog

## [0.2.2](https://github.com/sagawrr/react-native-nitro-vision-kit/compare/v0.2.1...v0.2.2) (2026-07-12)

Photos and cutouts stay upright.

* Bake EXIF orientation when loading images (iOS + Android)
* Stop flipping cutout pixels after Vision renders on iOS
* Fix invalid `dispose()` override on iOS segmentation results

## [0.2.1](https://github.com/sagawrr/react-native-nitro-vision-kit/compare/v0.1.1...v0.2.1) (2026-07-04)

### Features

* add analyzeImage pipeline and reorganize native helpers ([bf19b2a](https://github.com/sagawrr/react-native-nitro-vision-kit/commit/bf19b2af33944566b53bd1b865bcdfbce6eefbb3))
* expand segmentation result metadata and async buffer exports ([e38957a](https://github.com/sagawrr/react-native-nitro-vision-kit/commit/e38957ae28fd427e732517ce081964a18982a2f8))

### Bug Fixes

* **android:** stabilize ML Kit segmentation and trim edge cases ([d3234cc](https://github.com/sagawrr/react-native-nitro-vision-kit/commit/d3234ccc15061e981a1103d4d984a1612f0498b1))
* **android:** use internal constructor for HybridSegmentationResult ([4459811](https://github.com/sagawrr/react-native-nitro-vision-kit/commit/44598111833117bd6b52c39226112df60f9d9425))

## [0.1.1](https://github.com/sagawrr/react-native-nitro-vision-kit/compare/v0.1.0...v0.1.1) (2026-07-01)

### Bug Fixes

* **ci:** repair release workflow + add supply-chain gates ([#3](https://github.com/sagawrr/react-native-nitro-vision-kit/issues/3)) ([6268993](https://github.com/sagawrr/react-native-nitro-vision-kit/commit/626899330bf271d8fead956282866b0a60cf51cf))
* **ci:** scorecard publish + SECURITY.md ([#10](https://github.com/sagawrr/react-native-nitro-vision-kit/issues/10)) ([ee58d42](https://github.com/sagawrr/react-native-nitro-vision-kit/commit/ee58d42f2730bd538f5ecf0466af165e0ba98623))
* **release:** drop --frozen-lockfile from release-it before:init hook ([4f837fe](https://github.com/sagawrr/react-native-nitro-vision-kit/commit/4f837fefe0ad076f7e4784345463611a3d2c3b92))
