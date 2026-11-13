# iOS Android Parity Plan Implementation

Progress log for [`ios-android-parity-plan.md`](../plans/ios-android-parity-plan.md). Each task is
one commit (one PR) in plan order.

Baseline before Task 1: `xcodebuild test` passed on `main` (`fa84bc4`).

## Task 1: Domain and Parsing

- Status: done.
- Files: `ios/MetroPrague/Domain/` (`Line`, `Station`/`LinePosition`, `Direction`, `FavoriteKey`,
  `Departure`, `DepartureBoard`, `ThemePreference`, `StationDirections`, `DepartureDisplay`),
  `ios/MetroPrague/Data/Network/` (`MetroNetwork`, `MetroStopIDs`, `MetroPlatformFallbacks`,
  `MetroCatalogue`/`SavedStation`/`MetroPlatform`, `DepartureBoardParser`, `ISO8601`),
  `ios/MetroPrague/Shared/` (`LineBadge`, `DepartureDisplay.label`), tests under
  `ios/MetroPragueTests/Domain/` and `ios/MetroPragueTests/Data/Network/`, and `ios/AGENTS.md`.
- Verification: `swift-format lint --strict` clean; `xcodebuild test` with
  `SWIFT_TREAT_WARNINGS_AS_ERRORS=YES` passed (70 test cases).
- Notes for later tasks:
  - `DepartureBoard.evaluated(at:)` returns a board without `fetchedAt` unchanged, so unavailable
    and mock boards pass through.
  - `SavedStation.lineOrders` is `[Line: Int]` (`Line` is `CodingKeyRepresentable`), encoded as a
    JSON object keyed by `A`/`B`/`C`.
  - `ISO8601.date(from:)` parses `Z`/offset timestamps with or without fractional seconds.

## Task 2: Golemio Sync and Storage

- Status: done, except the live-key acceptance check (no `ios/Config/Secrets.xcconfig` in this
  checkout); see *Open items*.
- Files: `ios/MetroPrague/Data/Network/` (`GolemioHTTPClient` + `GolemioError` + `HTTPTransport`,
  `GolemioEndpoints`, `CatalogueLoader` + GTFS response types, `LenientValues`),
  `ios/MetroPrague/Data/Storage/` (`MetroSnapshot`, `SavedBoard`, `LoadedSnapshot`,
  `SnapshotFileStore`), `ios/MetroPrague/Data/` (`MetroDataSource`, `GolemioMetroDataSource`,
  `MockMetroDataSource` + `MockDepartures`, `MetroDataStore`, `FavoritesStore`, `AppSettings`),
  `ios/MetroPrague/App/` (`MetroPragueApp`, `ThemePreference+ColorScheme`), tests and the
  `TestClock`, `FakeTransport`, and `MetroTestData` helpers, `docs/product/parity.md`, and
  `docs/product/features/metro-data-sync.md`.
- Verification: `swift-format lint --strict` clean; `xcodebuild test` with warnings as errors
  passed (124 test cases).
- Notes for later tasks:
  - Screens read boards with `MetroDataStore.board(for:at:)`, which already applies
    `evaluated(at:)` (and generates mock boards at `now`). The plan's `board(for:)` takes `at:`
    for that reason.
  - `MetroDataStore.status` (`MetroDataStatus`) has `canReload`, `phase`, `stationCount`,
    `departureCount`, `reloadedAt`, and `error` (`MetroReloadError`).
  - The data source returns `LoadedSnapshot` (the snapshot plus its parsed boards) rather than a
    bare `MetroSnapshot`, so parsing stays in the `@concurrent` functions.
  - `FavoritesStore` has `favorites`, `contains`, `add`, `remove`, `toggle`. `AppSettings.theme`
    is settable and persists on set.

## Task 3: Settings

- Status: done, except the live-key reload checks; see *Open items*.
- Files: `ios/MetroPrague/Features/Settings/` (`SettingsView` with a private `MetroDataSection`,
  `ChangelogView` + `ChangelogEntry`, `AppVersion`, `MetroDataStatus+Text`), the
  `RootTabView` preview, tests in `ios/MetroPragueTests/Features/Settings/`, and
  `docs/product/` (`parity.md`, `settings.md`, `metro-data-sync.md`, `decisions.md`).
- Verification: gates passed. Simulator (iPhone 18 Pro, iOS 27.2, mock build): choosing Dark
  returned to Settings and switched the app at once; Dark survived a relaunch; Changelog opened
  with the three 1.0 notes; the App version row is exposed as static text ("App version, 1.0
  (1)"), not a button; "Data reload is unavailable in this build." shows without a key. The
  theme was set back to System afterwards.
- The error message is also announced to VoiceOver when it appears (Android uses a live region).
