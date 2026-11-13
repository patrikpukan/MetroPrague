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

## Task 4: Station Departures

- Status: done (reachable from Search in Task 5).
- Files: `ios/MetroPrague/Features/Departures/` (`DepartureRoute`, `StationDeparturesModel` +
  `StationDeparturesContent`, `StationDeparturesView`), `ios/MetroPrague/Data/FixedMetroDataSource.swift`
  (Debug-only fake source for previews and tests), the Search tab's
  `navigationDestination(for: DepartureRoute.self)`, model tests, and `docs/product/`.
- Verification: gates passed; `RenderPreview` rendered the Departures, Stale, Unavailable, and
  No service previews.
- Notes for later tasks: push a `DepartureRoute` onto the Search tab's stack to open the screen.

## Task 5: Station Search and Direction Picker

- Status: done.
- Files: `ios/MetroPrague/Features/Search/` (`SearchModel`, `SearchView` with private
  `SearchContent`, `StationRow`, `LineFilterBar`; `DirectionPickerSheet`),
  `ios/MetroPrague/Shared/DirectionRow.swift` (`DirectionRowContent`, generic `DirectionRow`,
  `FavoriteButton`), `SearchModelTests`, and `docs/product/`.
- Verification: gates passed. Simulator (mock build): "muz" found only Muzeum; the C chip
  narrowed the list and tapping C again cleared it; Muzeum's picker showed four directions with
  labels ("Line A, To Nemocnice Motol, 1 min", …); starring Háje changed its button to "Remove
  from favorites" [selected] and it stayed starred after reopening; choosing it pushed Muzeum
  departures (five rows); Back returned to Search. VoiceOver labels came from the accessibility
  snapshot.
- Deviation (platform-native, no Must match impact): the search field uses
  `.navigationBarDrawer(displayMode: .always)`. With the default placement on the search-role
  tab, iOS 27.2 showed no search field on iPhone.
- Notes for Task 6: reuse `DirectionRow` with `stationName:` and an accessory button.

## Task 6: Favorites on Home

- Status: done.
- Files: `ios/MetroPrague/Features/Home/` (`HomeModel` + `FavoriteRowContent`, `HomeView` with
  private `HomeContent`), `RootTabView` (passes `openSearch`), `HomeModelTests`, and
  `docs/product/`.
- Verification: gates passed. Simulator (mock build): the Muzeum → Háje favorite starred in Task
  5 survived a reinstall and relaunch; after adding Anděl → Zličín, Home listed both in insertion
  order; labels changed between snapshots ("1 min" → "At station"); swipe-delete removed
  Muzeum and the Remove button removed Anděl, neither confirmed; the empty state's
  **Search stations** opened the Search tab.

## Accessibility audits (all screens)

Run once after Task 6 with the three skills from `ios/AGENTS.md`, on Settings, Search, the
direction picker, departures, and Home. Fixes are in one commit on top of Task 6.

- Sufficient contrast: **FAIL → fixed.** White on line B's yellow was 2.0:1 and white on line
  A's green 3.2:1, which fails below 14 pt bold (the badge letter is 12–13 pt at the smallest
  Dynamic Type sizes; caught in the PR #20 review). A and B now use black (6.6:1 and 10.5:1); C
  keeps white (5.0:1).
  Departure time labels and Settings text used `.secondary` (≈3.4:1 on white rows) and now use
  the primary color. The selected line chip was white on blue (4.0:1, semibold) and now inverts
  primary/background. The red error text (≈3.6:1) is now primary text with a red warning symbol.
  The filled favorite star moved from yellow (≈1.6:1) to the accent color (4.0:1).
- VoiceOver: **PASS** after adding the header trait to the departures "Line X, To Y" header.
  The simulator's accessibility snapshots showed readable labels everywhere, "Add to favorites" /
  "Remove from favorites" with the selected trait, chips as "Line A/B/C", and App version as
  static text.
- Dynamic Type: **FAIL → fixed.** At AX 5 the line badge clipped its letter and Home rows broke
  station names mid-word. The badge now scales with `@ScaledMetric`, and direction and station
  rows stack the badge and text vertically at accessibility sizes (checked with `RenderPreview`
  at AX 5).
- Deviation: the plan says the badge letter is white on every line; lines A and B use black for
  contrast. Badge styling is platform-native, not a Must match rule. Recorded in
  `docs/product/features/station-search.md` (*Platform-native*) and `docs/product/decisions.md`.

## Definition of Done

- All five feature rows in `docs/product/parity.md` show iOS `done`.
- The four shared fixtures are asserted in `DepartureBoardParserTests`, and the Must match rules
  are covered by tests or by the simulator checks recorded above.
- Persistence matches Android's documented keys and formats (`favorites_v1` version-1 JSON, with
  the exact string asserted; `theme_preference` values).
- No request happens at launch (`MetroDataStoreTests.noRequestAtLaunch`); the only network path
  is `MetroDataStore.reload()` from Settings.
- Gates: `swift-format lint --strict` clean and `xcodebuild test` with
  `SWIFT_TREAT_WARNINGS_AS_ERRORS=YES` passing on every commit in the stack.
- No key, team ID, or live response is committed.

## Open items

- **Live-key check not run.** There is no `ios/Config/Secrets.xcconfig` in this checkout, so the
  Task 2/3 acceptance checks that need a real Golemio key were not performed: a reload saving 58
  stations (and recording station, platform, and departure counts), progress and counts in
  Settings, leaving Settings during a reload, and the offline error. The reload path is covered
  by `MetroDataStoreTests` with a fake transport.
- **Hosted iOS CI is not green for the stack yet.** `.github/workflows/ios.yml` runs only for
  pushes and pull requests targeting `main`, so only PR #15 gets a CI run. PRs #16–#20 target the
  previous branch in the stack and get no run until they are retargeted to `main` after the PR
  below them merges. Each PR's hosted result must be checked then. Local gates passed on every
  commit.
