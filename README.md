# Metro Prague

Metro Prague is an Android application intended to make Prague public transport
timetables easy to find and use. The project focuses on metro and tram
schedules, presented through a modern, accessible interface built with Jetpack
Compose and Material 3.

The application is currently in an early stage of development. Its main goal is
to provide accurate, up-to-date departure and arrival information that helps
residents and visitors navigate Prague efficiently.

## Planned Features

### Timetables and Search

- Browse and search metro lines and their schedules.
- Browse and search tram lines and their schedules.
- View departure and arrival times for individual stations and stops.
- Show real-time updates when a suitable public transport API is available.

### Navigation and User Experience

The app will use a bottom navigation bar with three primary sections:

1. **Timetables/Search** for finding lines, stations, stops, and schedules.
2. **To be defined**, potentially a map, route planner, or nearby stops view.
3. **To be defined**, potentially favorites, service alerts, or settings.

Additional UX goals include:

- Material You styling with Material 3 Expressive components.
- Light and dark theme support.
- Accessible controls, descriptions, and touch targets.
- A responsive, Compose-first user interface.

### Offline Access and Personalization

- Cache timetable data for offline use where the data source permits it.
- Save frequently used lines, stations, or stops as favorites.
- Display service disruptions and schedule changes when such data is available.

## Technology

- Kotlin
- Jetpack Compose
- Material 3 Expressive
- Navigation Compose with type-safe, serializable routes
- Kotlin Coroutines and Flow for asynchronous state
- MVVM with unidirectional data flow
- Hilt for dependency injection (planned)
- Gradle Kotlin DSL and a centralized version catalog

The project currently compiles against Android API 36, targets API 36, and has
a minimum supported SDK of API 36. Java 17 is required.

## Architecture

The application is designed around a Compose-first MVVM architecture:

- Composables render immutable UI state and send user events upward.
- Screen-specific ViewModels expose lifecycle-aware `StateFlow` state.
- Repositories encapsulate transport data, caching, and remote API access.
- Hilt provides repositories and other dependencies to ViewModels.
- Navigation destinations use Kotlin serialization instead of string routes.

User-facing text belongs in Android string resources, and dynamic lists should
use lazy Compose components with stable keys.

## Project Structure

```text
MetroPrague/
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

The Android application namespace and application ID are
`dev.pukan.metroprague`.

## Getting Started

### Requirements

- A recent Android Studio release
- Android SDK 36
- JDK 17
- An API 36 emulator or physical device for instrumentation tests

Clone the repository and open its root directory in Android Studio. Allow
Gradle to sync, then run the `app` configuration on an API 36 device or
emulator.

### Command-Line Tasks

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

New features should include deterministic unit tests covering at least one
happy path and one edge case. User-facing flows should also receive Compose UI
test coverage using semantics matchers and synchronization APIs rather than
fixed delays.

Before merging a code change, `./gradlew assembleDebug` should succeed and the
change should avoid introducing deprecated APIs or new lint warnings.

## Data and Privacy

The timetable and real-time data provider has not yet been selected. API keys
and other secrets must not be committed to the repository; local values belong
in `local.properties` or Gradle properties and should be exposed to the app
through build configuration or dependency injection.

## Status

Metro Prague is under active development. Planned functionality and navigation
sections may change as transport data sources and product requirements are
defined.

## License

This project is licensed under the Apache License, Version 2.0.
