# iOS Feature Parity Plan

## Goal

Bring the iOS app to feature parity with the current Android app, following steps 2–4 of
[the iOS quickstart](../quickstart-ios.md). When the plan is complete, iOS:

- loads metro data from Golemio through a user-started reload in Settings, stores one complete
  snapshot on the device, and keeps the previous snapshot when a reload fails;
- falls back to mock departures in builds without a Golemio key, as Android does;
- has Settings (theme, metro data reload, About), station search with a direction picker, a station
  departures screen, and favorites on Home;
- satisfies every **Must match** rule in the five feature specs in `docs/product/features/`;
- marks every feature `done` for iOS in `docs/product/parity.md`.

The work ships as **six PRs**, one per task below. Each PR changes one platform (plus its shared
doc lines), updates `parity.md` and the spec's *iOS reference* section, and leaves `main` green.

## Repository Context

- The iOS scaffold is done: see [`ios-scaffold-plan.md`](ios-scaffold-plan.md),
  [the scaffold report](../reports/ios-scaffold-plan-implementation.md), and `ios/AGENTS.md`.
  The app has an empty Home / Settings / Search tab shell, `AppConfiguration.main.golemioAPIKey`
  (non-nil only in Debug builds with `Config/Secrets.xcconfig`), the `Fixtures` test helper, and
  iOS CI on the `xcode-27` runner.
- iOS conventions are in `ios/AGENTS.md`: Swift 6, default `MainActor` isolation, Approachable
  Concurrency, `@Observable`, Swift Testing, Apple frameworks only, Xcode plugin for project work.
- Feature specs (the contract): `docs/product/features/metro-data-sync.md`, `settings.md`,
  `station-search.md`, `station-departures.md`, `favorites-home.md`. API facts:
  `docs/api/golemio.md`. Fixtures and their required assertions: `fixtures/README.md`.
- Android reference implementation (read for **rules**, not structure). All paths are under
  `android/app/src/main/java/dev/pukan/metroprague/`:

  | Concern | Android files |
  | --- | --- |
  | Domain models | `domain/model/{Line,Station,Direction,FavoriteKey,Departure,ThemePreference}.kt` |
  | Direction derivation | `domain/model/StationDirections.kt` (`terminiOf`, `directionsAt`) |
  | Direction filter | `domain/model/Departure.kt` (`forDirection`) |
  | Time labels | `ui/model/DepartureDisplay.kt` (`toDisplay`) |
  | HTTP pacing and 429 | `data/golemio/GolemioBoardService.kt` (`GolemioHttpClient`, `boardUrl`) |
  | Board parsing and direction assignment | `data/golemio/GolemioDepartureBoardParser.kt` |
  | Static topology and station IDs | `data/golemio/MetroNetwork.kt` |
  | Stop IDs for parsing without a catalogue | `data/golemio/MetroStopIds.kt` |
  | Platform fallback list | `data/golemio/MetroPlatformFallbacks.kt` |
  | GTFS catalogue download and parsing | `data/golemio/GolemioCatalogueLoader.kt`, `GolemioCatalogueService.kt` |
  | Snapshot shape | `data/golemio/MetroDataSnapshot.kt` |
  | Reload orchestration and errors | `data/repository/GolemioMetroDataRepository.kt` |
  | Saved boards: staleness and past filter | `data/repository/GolemioDepartureRepository.kt` |
  | Stations before the first reload | `data/repository/GolemioStationRepository.kt` |
  | Mock departures | `data/repository/MockDepartureRepository.kt` (`buildDepartureBoard`) |
  | Live-or-mock selection | `di/RepositoryModule.kt` (`LiveDeparturesConfig`) |
  | Favorites persistence | `data/favorites/DataStoreFavoritesRepository.kt` |
  | Theme persistence | `data/preferences/DataStoreUserPreferencesRepository.kt` |
  | Screen logic | `ui/screens/{settings,search,departures,home}/*ViewModel.kt` |
  | English copy | `android/app/src/main/res/values/strings.xml` |

- The Android test suite in `android/app/src/test/` documents edge cases. Each iOS task names the
  Android tests to mirror.

## Decisions

### Delivery

- **Six PRs, in this order:** (1) domain and parsing, (2) Golemio sync and storage,
  (3) Settings, (4) station departures, (5) station search and direction picker, (6) favorites on
  Home.
- **Departures before Search.** The quickstart lists Search before departures, but the direction
  picker must open the departures screen (`station-search.md`, Must match 4). Building departures
  first means Search links to a finished screen instead of a throwaway placeholder. Until Search
  lands, the departures screen is reachable only from previews and tests.
- Each PR runs the iOS quality gates from `ios/AGENTS.md` (build, test, `swift-format lint`).
  Each screen PR also runs the three accessibility skills and records a simulator check (T3 device
  tools) in its PR description.
- A builder may write a narrower plan for a single PR (`docs/ai/plans/<feature>-ios-plan.md`) if
  it needs one. That plan must not reopen decisions made here.

### Architecture

- **Folders.** `MetroPrague/Domain/` holds pure value types and rules with no I/O.
  `MetroPrague/Data/` holds networking, parsing, storage, and the app-level stores.
  `MetroPrague/Features/<Feature>/` holds views and screen models. `MetroPrague/Shared/` holds
  views and helpers used by more than one feature (line badge, departure label). Add `Domain/`
  and `Shared/` to the structure in `ios/AGENTS.md` in PR 1.
- **Value types are `nonisolated` and `Sendable`.** Domain types and parsers carry no state and
  are used from background work, so they are marked `nonisolated`. That opts them out of the
  default `MainActor` isolation.
- **App-level state lives in three `@Observable` stores** (main-actor by default), created once in
  `MetroPragueApp` as `@State` and passed into the view tree with `.environment(_:)`:
  - `MetroDataStore`: the stations list, the saved snapshot, reload status, and `reload()`;
  - `FavoritesStore`: the ordered favorites, `add`, `remove`, `toggle`;
  - `AppSettings`: the theme preference.

  This replaces Android's repositories, `Flow`s, and Hilt. Views read store properties directly,
  and `@Observable` re-renders only views that read a changed property, so no publisher pipelines
  are needed.
- **Screen models** (`SearchModel` and others) are `@Observable` classes owned with `@State` by
  their screen. They receive the stores through their initializer, keep only screen-local state
  (query, selected station), and expose computed properties derived from the stores. A screen with
  no local state (Settings) reads the stores directly and has no model.
- **Live or mock.** The app uses `GolemioMetroDataSource` when
  `AppConfiguration.main.golemioAPIKey != nil`, otherwise `MockMetroDataSource`. This matches
  Android, where only debug builds with a key go live, because only Debug builds can contain the
  key on iOS. Both conform to one protocol, so stores and screens do not branch.
- **Mock stations use the full bundled network** (58 stations from `MetroNetwork`), not Android's
  20-station mock list. The mock list is test scaffolding, not spec behavior, and a single topology
  is simpler. Record this in the `metro-data-sync.md` *Platform-native* section.

### Time and the 15-second tick

- Stores hold **dates, not labels.** A saved board keeps its departures (with `Date`s) and
  `fetchedAt`.
- One pure function, `DepartureBoard.evaluated(at now: Date)`, applies the time-based rules:
  staleness (whole-second age 0…299 is fresh), removal of past departures, and the fresh at-stop
  exception (`metro-data-sync.md` 3, `station-departures.md` 4). `DepartureDisplay.make(for:now:timeZone:)`
  turns a departure into a label (`station-departures.md` 3).
- Screens wrap time-dependent content in `TimelineView(.periodic(from: .now, by: 15))` and pass
  `context.date` as `now`. The 15-second refresh is then a view concern (as `golemio.md`
  describes) and the rules stay deterministic: tests pass fixed dates and no timer runs in tests.
- Hour-and-minute labels use the device time zone and a fixed 24-hour `HH:mm` format with zero
  padding, matching Android's `%02d:%02d`, because the format is a Must match rule.

### Networking and concurrency

- **`actor GolemioHTTPClient`** owns pacing state and wraps an injected
  `HTTPTransport` protocol (`func data(for: URLRequest) async throws -> (Data, HTTPURLResponse)`).
  `URLSession` conforms in the app; tests use a fake. It sends `X-Access-Token` and
  `Accept: application/json` with a 10-second timeout, and never logs the key or URLs that carry
  it.
- **Serialize requests explicitly.** Actors are *reentrant*: while a request awaits the network,
  another call can enter the actor. The client therefore keeps a `lastRequest: Task<Void, Never>?`
  and makes each new request await it before starting, which gives one-at-a-time behavior like
  Android's `Mutex`. Explain this in a code comment because it is easy to miss.
- **Pacing (`metro-data-sync.md` 5).** After each response, the next request may start no sooner
  than 600 ms later. On HTTP 429, a numeric `Retry-After` is clamped to 1…3600 s; a missing or
  non-numeric value uses 8 s. If a call would have to wait more than 600 ms, it throws
  `GolemioError.http(429)` immediately instead of waiting. Time comes from an injected
  `now: @Sendable () -> ContinuousClock.Instant` and `sleep: @Sendable (Duration) async throws -> Void`,
  so tests check the waits without sleeping.
- **Heavy work leaves the main actor with `@concurrent`.** Under Approachable Concurrency, a
  `nonisolated async` function runs on its caller's actor. Downloading and parsing run inside
  `@concurrent` functions of the data source, so a 1.5 MB snapshot never blocks the UI. The
  stores (main actor) only `await` the results.
- **The reload is app-scoped.** `MetroDataStore.reload()` starts an unstructured `Task` that the
  store owns and returns immediately. Leaving Settings never cancels it. A second call while
  reloading is ignored (`metro-data-sync.md` 1).
- **Errors (`metro-data-sync.md` 4).** HTTP 401/403 → `.authentication`, 429 → `.rateLimited`,
  anything else → `.networkOrData`.

### Parsing

- Departure boards are decoded with `Codable` into private response types that use **lenient
  helpers**, matching Android: `route.type` accepts a number or a numeric string; booleans accept
  `true`/`false` or the strings `"true"`/`"false"`; missing optional objects (`delay`, `stop`,
  `last_stop`, `infotexts`) are allowed. Timestamps are ISO 8601 with `Z` or an offset, with or
  without fractional seconds (fixtures use both `Z` and `+02:00`).
- Non-metro routes (type ≠ 1) and lines other than A/B/C are dropped. Departures are sorted by
  effective time (predicted, otherwise scheduled). Info texts keep non-blank Czech `text` values.
- **Direction assignment** follows Android exactly: with a catalogue, use the headsign's position
  on the line, then the platform's direction, then the previous stop. Without a catalogue, use the
  stop-ID table ported from `MetroStopIds`, which is needed for the shared fixture assertions.
- The GTFS catalogue (routes, stops GeoJSON, trip pages, trip details) is decoded with small
  `Codable` types holding only the fields `parseMetroCatalogue` reads. Port every `require` as a
  thrown `GolemioError.invalidData`, so malformed metadata fails the reload instead of saving bad
  data.

### Persistence

- **Snapshot.** One JSON file, `Application Support/metro-data.json`, encoded with `Codable`:
  `version` (1), `catalogue` (stations with line orders, platforms with optional direction),
  `boards` (station ID → raw response `String` + `fetchedAt`), `reloadedAt`, and
  `departureCount`. It is written with `Data.write(to:options: .atomic)`, so a reader sees either
  the old or the new file, never a partial one (`metro-data-sync.md` 2). The file is marked
  `isExcludedFromBackup`. A missing or unreadable file means no snapshot. Raw board responses are
  stored and re-parsed on load, so parser fixes apply to saved data. Raw GTFS routes, stops, and
  patterns are not stored, because nothing reads them (*Platform-native* persistence).
- **Favorites.** The `UserDefaults` key `favorites_v1` holds the same version-1 JSON string as
  Android: `{"version":1,"favorites":[{"stationId":…,"line":"A","terminusStationId":…}]}`.
  Missing, blank, or malformed data → empty list; entries with an unknown line are skipped
  (`favorites-home.md` 2–3).
- **Theme.** The `UserDefaults` key `theme_preference` holds `system`, `light`, or `dark`;
  anything else resolves to System (`settings.md` 2). The app applies it with
  `.preferredColorScheme` at the root (`nil` for System).
- Stores take their `UserDefaults` and file URL through their initializers, so tests use a
  throwaway suite and a temporary directory.

### UI (Platform-native choices)

- **Settings:** a `Form` with three sections.
  - Appearance: a theme `Picker` in `.navigationLink` style. Choosing an option returns to
    Settings, which satisfies "choosing closes the selection" (`settings.md` 3).
  - Metro data: description, saved counts, last reload, progress, a **Reload data** button, and
    error text.
  - About: a **Changelog** `NavigationLink` and a non-interactive **App version** `LabeledContent`
    showing `CFBundleShortVersionString (CFBundleVersion)`.

  **No Language row:** Android's is an unpersisted mock, the spec does not require it, and iOS
  has per-app language in the system Settings app. Record this divergence in `settings.md`
  *Platform-native* and in `decisions.md`.
- **Search:** `.searchable` on a `List` of stations, with a row of four toggle chips (All, A, B, C)
  above it. Tapping the selected line clears the filter (`station-search.md` 1); `.searchScopes`
  cannot clear on a second tap, so it is not used. Station rows show the name and line badges.
- **Direction picker:** `.sheet(item:)` with `.presentationDetents([.medium, .large])` and a
  **Close** toolbar button. Each row shows the line badge, "To <destination>", the next-departure
  label, and a star button labeled **Add to favorites** / **Remove from favorites**. Tapping a row
  dismisses the sheet and pushes the departures screen onto the Search tab's `NavigationStack`.
- **Departures:** a `List` of up to five rows (destination + time label). The navigation title is
  the station name, with the line badge and "To <terminus>" in a header. Stale, unavailable, and
  no-service states use `ContentUnavailableView` with distinct copy. Back is the system back
  button.
- **Home:** a `List` of favorite rows (station, line badge, "To <destination>", next-departure
  label) with a **Remove from favorites** button and a swipe-to-delete action, neither confirmed
  (`favorites-home.md` 5). The empty state's **Search stations** button switches to the Search
  tab: `RootTabView` passes a closure that sets `selectedTab`. Rows do not open departures,
  matching Android.
- **Copy:** user-visible English text is copied from Android's `strings.xml` wherever a string
  exists (for example "Reload departures in Settings", "Live departures unavailable",
  "No service", "Cancelled", "At station", "Now", "%d min"). `Localizable.xcstrings` stays
  English-only.
- **Line colors:** A `#00A562`, B `#F6A800`, C `#E00034`, from `Line.colorHex`. The badge draws
  the letter in white on the line color, and its accessibility label is "Line A" (the
  `line_label` string).

## Out of Scope

- Changing any **Must match** rule or Android behavior. Gaps found on Android are reported, not
  fixed here.
- Diacritic-insensitive search (`mustek` → Můstek). It would diverge from the spec's
  case-insensitive rule; raise it as a spec change for both platforms.
- Service-text (info text) display, trams, maps, route planning, widgets, Live Activities, App
  Intents, iPad layouts, Czech localization, Language setting.
- Tapping a Home favorite to open departures (Android does not do this).
- Background refresh, push, or any automatic network request (`metro-data-sync.md` 1).
- The direct-vs-proxy key decision, TestFlight, App Store, and `ios-v*` release tags.
- Third-party packages.

## Task Dependency Order

1. Task 1: Domain and parsing (no UI, no network).
2. Task 2: Golemio sync and storage. Depends on 1.
3. Task 3: Settings. Depends on 2.
4. Task 4: Station departures. Depends on 2.
5. Task 5: Station search and direction picker. Depends on 2 and 4.
6. Task 6: Favorites on Home. Depends on 2. Reuses the shared row pieces from 4 and 5.

Tasks 3 and 4 are independent and may be built in parallel worktrees, but merge them one at a time.

---

## Task 1: Domain and Parsing

**Objective:** Pure, fully tested Swift versions of the shared rules. Nothing user-visible changes.

**Work:**

- `Domain/`:
  - `Line` (`String` raw values `A`, `B`, `C`; `colorHex`), `LinePosition`, `Station`,
    `Direction`, `FavoriteKey` (`Codable`, `Hashable`), `Departure` (`effectiveTime`),
    `DepartureBoard` (`departures`, `infoTexts`, `isUnavailable`, `isStale`, `fetchedAt`),
    `ThemePreference` (`storedValue`, `init(storedValue:)` falling back to `.system`).
  - `StationDirections.swift`: `termini(of:in:)` and `directions(at:in:)`, ported from
    `StationDirections.kt`. Lines are ordered A, B, C; a terminus equal to the station is skipped.
  - `DepartureBoard.departures(for direction:)`, ported from `forDirection`: match by
    `directionTerminusStationId` when present, otherwise by headsign = terminus name.
  - `DepartureBoard.evaluated(at:)`: stale = whole-second age not in 0…299; keep departures whose
    effective time is not before `now`, plus at-stop departures while the board is fresh.
  - `DepartureDisplay` enum (`noService`, `unavailable`, `stale`, `cancelled`, `atStation`,
    `now`, `inMinutes(Int)`, `atTime(hour:minute:)`) and its factory, ported from `toDisplay`.
- `Data/Network/` (static data and parsing only):
  - `MetroNetwork`: `namesByLine`, `stations` (58 stations, line orders), `seedPlatforms`, and
    `stationID(for:)`. The ID strips diacritics, lowercases, and turns runs of non-`[a-z0-9]`
    characters into `-`, trimmed, with the special case `I. P. Pavlova` → `ip-pavlova`.
  - `MetroStopIDs` and `MetroPlatformFallbacks`: tables copied exactly from the Android files.
  - `MetroCatalogue`, `SavedStation`, `MetroPlatform` (`Codable`).
  - `DepartureBoardParser.parse(stationID:response:catalogue:) throws -> DepartureBoard`, with the
    lenient decoding and direction assignment from *Decisions → Parsing*.
- Shared view pieces in `Shared/`: `LineBadge` view and `DepartureDisplay.label`
  (`LocalizedStringResource`), so later PRs reuse them. Each gets a `#Preview`.
- Update `ios/AGENTS.md` *Project Structure* with `Domain/` and `Shared/`.
- Tests (Swift Testing, one file per type), mirroring `StationDirectionsTest`,
  `DepartureDisplayTest`, `GolemioDepartureBoardParserTest`, and the staleness cases in
  `GolemioDepartureRepositoryTest`:
  - all four fixtures in `fixtures/README.md`, asserting exactly what the table documents, read
    through `Fixtures`;
  - `MetroNetwork`: 58 unique stations; memberships A = 17, B = 24, C = 20; the IDs `ip-pavlova`,
    `nadrazi-holesovice`, `prazskeho-povstani`, `cerny-most`, and `zelivskeho`;
  - directions at a terminus, a middle station, and an interchange (Muzeum: A both ways, C both
    ways);
  - display boundaries: 59 s → Now, 60 s → 1 min, 3599 s → 59 min, 3600 s → `HH:mm`;
    cancelled wins over at-stop, and at-stop wins over time;
  - staleness at ages 0, 299, 300, and negative; a past departure removed; a past at-stop departure
    kept while fresh and removed when stale;
  - parser: catalogue direction by headsign, by platform, by previous stop, and the Skalka
    shared platform (no direction on the platform, resolved by headsign).

**Acceptance Criteria:**

- Quality gates pass. No app behavior change.
- Every fixture assertion in `fixtures/README.md` is covered by a passing iOS test.
- `parity.md` unchanged (no feature is complete yet).

---

## Task 2: Golemio Sync and Storage

**Objective:** Download, store, and expose metro data and user preferences. The UI still shows
the scaffold placeholders.

**Work:**

- `Data/Network/GolemioHTTPClient.swift`: the actor from *Decisions → Networking*, plus
  `GolemioError` (`.http(Int)`, `.invalidData`, `.transport`).
- `Data/Network/GolemioEndpoints.swift`: URL builders that encode `ids[]` and `names[]` like
  Android:
  - boards: `/v2/pid/departureboards?ids[]=…&minutesAfter=60&limit=30&mode=departures&order=real`
    (`metro-data-sync.md` 6);
  - `/v2/gtfs/routes?limit=10000`;
  - `/v2/gtfs/stops?names[]=…&limit=10000`;
  - `/v2/gtfs/trips?stopId=…&limit=1000&offset=…`;
  - `/v2/gtfs/trips/{id}?includeStops=true&includeStopTimes=true`.
- `Data/Network/CatalogueLoader.swift`: port of `GolemioCatalogueLoader.load` and
  `parseMetroCatalogue`. Keep the same page loop (stop when a page has fewer than 1000 trips; fail
  above 10 000), pattern key `route_id:shape_id:direction_id`, arrival-only platforms, shared
  reversing platforms, fallback lookup, and every validation.
- `Data/MetroDataSource.swift`: protocol with `loadSnapshot() async -> MetroSnapshot?` and
  `reload(progress:) async throws -> MetroSnapshot`, plus `var canReload: Bool`.
  - `GolemioMetroDataSource`: catalogue, then one board request per station (its platform IDs),
    with progress `(completed, total)` after each. The departure count is the sum of parsed
    departures. It saves the snapshot only after every board succeeded, as one atomic write.
  - `MockMetroDataSource`: `canReload == false`. It builds boards with the ported
    `buildDepartureBoard` rules (headways: none 00:30–04:30, 2 min 07–09 and 15–19, 5 min
    09–15 and 19–20, 8 min otherwise; five departures per direction; every fifth trip 60 s late;
    at-stop within 30 s), regenerated whenever a screen asks at a new `now`.
- `Data/Storage/SnapshotFileStore.swift`: read and atomically write `metro-data.json` as described
  in *Decisions → Persistence*.
- `Data/MetroDataStore.swift` (`@Observable`): `stations` (catalogue stations, else
  `MetroNetwork.stations`; `metro-data-sync.md` 7), `board(for stationID:) -> DepartureBoard`
  (catalogue nil → unavailable + stale; board missing or unparsable → unavailable), and `status`:
  `canReload`, `phase` (`.idle`, `.loadingCatalogue`, `.loadingBoards(completed:total:)`),
  `stationCount`, `departureCount`, `reloadedAt`, `error`. Plus `reload()` with the app-scoped,
  duplicate-ignoring behavior. A failure keeps the previous snapshot and sets `error`; a new
  reload clears `error`. It loads the saved snapshot once at launch and never makes a request at
  launch.
- `Data/FavoritesStore.swift` and `Data/AppSettings.swift`: as in *Decisions → Persistence*.
  `toggle` and `add` are no-ops for keys already present (add) and keep insertion order.
- `MetroPragueApp`: create the three stores (choosing the data source from `AppConfiguration`),
  inject them with `.environment`, and apply `.preferredColorScheme(settings.theme.colorScheme)`.
- Tests, mirroring `GolemioBoardServiceTest`, `GolemioCatalogueLoaderTest`,
  `MetroSnapshotStoreTest`, `GolemioMetroDataRepositoryTest`,
  `DataStoreFavoritesRepositoryTest`, `DataStoreUserPreferencesRepositoryTest`, and
  `MockDepartureRepositoryTest`:
  - client: 600 ms spacing; serialization of two concurrent calls; Retry-After `5`, `0` (→ 1),
    `99999` (→ 3600), `soon` (→ 8), missing (→ 8); prompt 429 during a cooldown; the key header is
    sent;
  - loader: a small hand-built routes/stops/patterns set produces the expected platforms and
    directions; a missing station and a conflicting platform throw;
  - store: a successful reload replaces the snapshot and counts; a failed board keeps the old file
    byte-for-byte; 401, 403, 429, and other errors map correctly; a second `reload()` while
    reloading is ignored; nothing is requested at init;
  - favorites: order, duplicate add, toggle, remove, malformed JSON → empty, unknown line skipped,
    and the exact stored JSON string;
  - theme: round-trip, and an unknown value → System;
  - mock: no departures at 02:00; the peak headway at 08:00.

**Acceptance Criteria:**

- Quality gates pass. The app still launches to the scaffold shell; no network request happens at
  launch (checked in a test with a recording fake transport).
- With a real key in `Secrets.xcconfig`, a temporary debug call to `reload()` (removed before
  commit), or the Settings button once Task 3 lands, saves 58 stations. Record the station,
  platform, and departure counts in the PR description. Never commit the key or responses.
- `parity.md`: Metro data sync iOS → `in progress`.

---

## Task 3: Settings

**Objective:** The Settings tab described in `settings.md` and the Settings parts of
`metro-data-sync.md`.

**Work:**

- `Features/Settings/SettingsView.swift`: replace the placeholder with the `Form` from
  *Decisions → UI*. Copy the English text from Android's `settings_*` strings. Metro data shows:
  - the description, including the five-minute expiry (`metro-data-sync.md` 3);
  - "No metro data saved yet…" when there is no snapshot;
  - "%d stations · %d saved departures";
  - "Last reload:" with `reloadedAt.formatted(date: .abbreviated, time: .shortened)`;
  - catalogue / "Loading departures: %d of %d stations…" progress;
  - the Reload button, disabled while reloading, or "Data reload is unavailable in this build."
    when `canReload` is false;
  - the error message for each error kind, each stating that saved data was kept.
- `Features/Settings/ChangelogView.swift`: a list grouped by version. The 1.0 notes are "Initial
  release.", "Metro Prague line and station search.", "Theme preferences." (iOS has no language
  preference).
- `Features/Settings/AppVersion.swift`: reads the version and build from the bundle; tested with
  an injected info dictionary.
- Tests: theme selection persists and is reflected by `AppSettings`; the version string format
  `1.0 (1)`; status text selection for each phase and error (pure function); the changelog
  content.
- Docs: fill the *iOS reference* in `settings.md`. Add the "no Language row" divergence to
  `settings.md` *Platform-native* and to `decisions.md`.

**Acceptance Criteria:**

- On the simulator: choosing Light or Dark switches the whole app immediately and survives a
  relaunch; System follows the device. The Changelog opens. VoiceOver reads App version as
  static text with no button trait.
- With a key: Reload shows progress, then counts and last-reload time. Leaving Settings during a
  reload does not stop it. With networking off, Reload shows the network error, and saved data and
  counts are unchanged.
- Without a key: "Data reload is unavailable in this build."
- `parity.md`: Settings iOS → `done`; Metro data sync iOS → `done` (its remaining Settings rules
  are now met).

---

## Task 4: Station Departures

**Objective:** The departures screen from `station-departures.md`.

**Work:**

- `Features/Departures/DepartureRoute.swift`: `Hashable` route (`stationID`, `line`,
  `terminusStationID`) for `navigationDestination(for:)`.
- `Features/Departures/StationDeparturesModel.swift` (`@Observable`): resolves the station and
  direction from `MetroDataStore.stations`. `content(at now:)` returns either rows (at most five,
  in effective-time order; destination = headsign) or one empty state: stale, unavailable, or no
  service. If the station or direction no longer exists, it shows no service. Ported from
  `StationDeparturesViewModel`.
- `Features/Departures/StationDeparturesView.swift`: as in *Decisions → UI*, wrapped in the
  15-second `TimelineView`. The time label stays a `Text` next to the destination, so VoiceOver
  reads both (`station-departures.md` 5).
- Register `navigationDestination(for: DepartureRoute.self)` in the Search tab's
  `NavigationStack` now, so Task 5 only appends routes.
- Tests mirroring `StationDeparturesViewModelTest`: direction filtering, five-row cutoff,
  ordering, stale vs unavailable vs no-service, a past departure dropped, and an unknown station.
  Add `#Preview`s for each state with fake boards.
- Docs: *iOS reference* in `station-departures.md`.

**Acceptance Criteria:**

- Previews render every state. Model tests pass.
- `parity.md`: Station departures iOS → `in progress`. It becomes `done` in Task 5, when the screen
  is reachable and checked on the simulator.

---

## Task 5: Station Search and Direction Picker

**Objective:** The Search tab from `station-search.md`.

**Work:**

- `Features/Search/SearchModel.swift` (`@Observable`): `query`, `selectedLine` (`toggleLine(_:)`
  sets it, or clears it when it is already selected; `All` clears), `filteredStations`
  (case-insensitive substring with `range(of:options: .caseInsensitive)`; an empty query matches
  everything; line filter), `selectedStation`, and `directionRows(at now:)` (direction, line,
  destination, next-departure display or stale/unavailable, `isFavorite`). Add
  `toggleFavorite(_ direction:)`, which acts only if the direction is currently displayed for the
  selected station (`station-search.md` 5).
- `Features/Search/SearchView.swift`: `.searchable` list, the chip row, station rows, the picker
  sheet, and a `NavigationPath`. Choosing a direction dismisses the sheet and appends a
  `DepartureRoute`.
- `Features/Search/DirectionPickerSheet.swift` and `Shared/DirectionRow.swift`: row as in
  *Decisions → UI*, with the star button's accessibility label and `.isSelected` trait reflecting
  favorite state.
- Tests mirroring `SearchViewModelTest` and `StationDirectionsTest`: query and filter combinations,
  the clear-on-reselect filter, direction rows at a terminus and an interchange, next-departure
  states, favorite flags, and the toggle guard.
- Docs: *iOS reference* in `station-search.md`.

**Acceptance Criteria:**

- On the simulator: searching "muz" finds Muzeum; the C chip narrows the list, and tapping C
  again clears it. Muzeum's picker shows four directions with labels. Starring one and reopening
  the picker shows it starred. Choosing a direction opens departures, and Back returns to Search.
- VoiceOver reads each direction row's line, destination, and time, and the star as "Add to
  favorites" or "Remove from favorites".
- `parity.md`: Station search and direction picker iOS → `done`; Station departures iOS → `done`.

---

## Task 6: Favorites on Home

**Objective:** The Home tab from `favorites-home.md`.

**Work:**

- `Features/Home/HomeModel.swift` (`@Observable`): resolves each favorite against current stations
  and directions, in stored order; favorites that no longer resolve are omitted. For each it
  provides station, line, destination (next departure's headsign, else terminus name), and next
  display (stale / unavailable / time). `remove(_:)` calls `FavoritesStore.remove`. Ported from
  `HomeViewModel`.
- `Features/Home/HomeView.swift`: the list and empty state from *Decisions → UI*, inside the
  15-second `TimelineView`. The empty state's **Search stations** button switches tabs through a
  closure from `RootTabView`.
- Tests mirroring `HomeViewModelTest`: order preserved, unresolvable favorites omitted, display
  states, and removal.
- Docs: *iOS reference* in `favorites-home.md`.

**Acceptance Criteria:**

- On the simulator: favorites starred in Search appear on Home in insertion order, with live
  labels that tick. Remove and swipe-delete work without confirmation. Favorites survive a
  relaunch. With no favorites, **Search stations** opens the Search tab.
- `parity.md`: Favorite station directions on Home iOS → `done`.

## Definition of Done

- All five feature rows in `docs/product/parity.md` show iOS `done`, each set by the PR that
  completed it.
- Every **Must match** rule in the five specs has an iOS test or a recorded simulator check, as
  listed in each task's acceptance criteria.
- The four shared fixtures are asserted by iOS tests exactly as `fixtures/README.md` documents.
- Persistence is compatible with Android's documented keys and formats (`favorites_v1`
  version-1 JSON, `theme_preference` values).
- No network request happens except from a user-started reload in Settings.
- iOS CI is green on every PR. `swift-format lint --strict` is clean. No warnings.
- Each spec's *iOS reference* section is filled in, and every iOS divergence is recorded in its
  *Platform-native* section and, where it is a product choice, in `decisions.md`.
- No key, team ID, or live response is committed.
