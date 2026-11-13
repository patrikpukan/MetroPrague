# Android App

The Metro Prague Android app helps people find Prague public transport departures through a
modern, accessible interface. This document covers Android development and setup.

## Technology

- Kotlin
- Jetpack Compose
- Material 3 Expressive
- Navigation Compose with type-safe, serializable routes
- Kotlin Coroutines and Flow for asynchronous state
- MVVM with unidirectional data flow
- Hilt for dependency injection (planned)
- Gradle Kotlin DSL and a centralized version catalog

The project currently compiles against Android API 36, targets API 36, and has a minimum supported
SDK of API 36. App code targets Java 17; building it requires JDK 21, which the Gradle daemon is
pinned to.

## Architecture

The application is designed around a Compose-first MVVM architecture:

- Composables render immutable UI state and send user events upward.
- Screen-specific ViewModels expose lifecycle-aware `StateFlow` state.
- Repositories encapsulate transport data, caching, and remote API access.
- Hilt provides repositories and other dependencies to ViewModels.
- Navigation destinations use Kotlin serialization instead of string routes.

User-facing text belongs in Android string resources, and dynamic lists should use lazy Compose
components with stable keys.

## Project Structure

```text
android/
├── app/
│   └── src/
│       ├── main/          # Application code, manifest, and resources
│       ├── test/          # JVM unit tests
│       └── androidTest/   # Instrumentation and Compose UI tests
├── gradle/
│   └── libs.versions.toml # Dependency and plugin versions
├── build.gradle.kts
└── settings.gradle.kts
```

The Android application namespace and application ID are `dev.pukan.metroprague`.

## Getting Started

### Requirements

- A recent Android Studio release
- Android SDK 36
- JDK 21 (runs Gradle; the app itself targets Java 17)
- An API 36 emulator or physical device for instrumentation tests

Open the `android/` directory in Android Studio. Allow Gradle to sync, then run the `app`
configuration on an API 36 device or emulator. Store local SDK settings and the development
`GOLEMIO_API_KEY` in the gitignored `android/local.properties` file.

## Command-Line Tasks

Run Gradle commands from `android/`. From the repository root, prefix each command with
`cd android &&`.

```bash
./gradlew assembleDebug
```

Builds the debug APK.

```bash
./gradlew test
```

Runs JVM unit tests.

```bash
./gradlew connectedAndroidTest
```

Runs instrumentation and Compose UI tests on a connected device or emulator.

```bash
./gradlew clean
```

Removes generated build output.

## Testing and Quality

New features should include deterministic unit tests covering at least one happy path and one edge
case. User-facing flows should also receive Compose UI test coverage using semantics matchers and
synchronization APIs rather than fixed delays.

Before merging a code change, `./gradlew assembleDebug` should succeed and the change should avoid
introducing deprecated APIs or new lint warnings.

## Data and Privacy

Golemio is the timetable and real-time data provider. Debug builds use the direct Golemio integration
when `GOLEMIO_API_KEY` is set in the gitignored `android/local.properties`; otherwise they use mock
data. Release builds still use mocks. In development, open **Settings → Metro data → Reload data** to
download the full A/B/C catalogue and current departure snapshots. Home and Search read the saved
dataset from the device and make no API requests. Before the first reload, they list stations from
the built-in network topology and prompt you to reload for departures. Reloading is manual and
continues if you leave Settings; departure snapshots older than five minutes prompt you to reload. A
failed or cancelled reload preserves the previous dataset and your favorites. API findings and
implementation status are tracked in
[the Golemio API reference](../docs/api/golemio.md). Never commit API
keys or other secrets. For release, direct API access and a server proxy are both
under consideration; a compiled app key is recoverable from an APK.
