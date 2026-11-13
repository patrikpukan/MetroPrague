# Metro Prague for iOS

The native SwiftUI app. Feature behavior is specified in
[`../docs/product/features/`](../docs/product/features/), and platform status is in
[`../docs/product/parity.md`](../docs/product/parity.md).

## Requirements

- Xcode 27 or later. The project uses Xcode 27's JSON project format.
- An iPhone simulator running iOS 26 or later. The deployment target is iOS 26.0.

## First-Time Setup

1. Copy `Config/Secrets.example.xcconfig` to `Config/Secrets.xcconfig` (gitignored).
2. Set `GOLEMIO_API_KEY` to your Golemio key. See [`../docs/api/golemio.md`](../docs/api/golemio.md)
   for how to get one. The app builds without a key.
3. Optional: set `DEVELOPMENT_TEAM` to run on a physical device.

## Run

Open `MetroPrague.xcodeproj` in Xcode and run the `MetroPrague` scheme on an iPhone simulator.

From the command line, in this directory:

```sh
DEST="$(scripts/simulator-destination.sh)"
xcodebuild build -project MetroPrague.xcodeproj -scheme MetroPrague -destination "$DEST" -derivedDataPath build
xcodebuild test  -project MetroPrague.xcodeproj -scheme MetroPrague -destination "$DEST" -derivedDataPath build
xcrun swift-format lint --strict --recursive MetroPrague MetroPragueTests
```

## Security

Debug builds contain the Golemio key from `Config/Secrets.xcconfig`, and anyone with the app can
recover it. Do not distribute Debug builds. Release builds never contain the key.
