# Favorite Station Directions and Mocked Departures Plan

## Goal

Let a user pick a direction of travel at any searched station, see the next departure for that
direction immediately, and save the station-plus-direction pair to the Home screen as a favorite.

Required behavior:

1. **Search**: tapping a station row opens a modal bottom sheet for that station.
2. **Direction picker**: the sheet lists every direction of travel available at that station. A
   direction is "toward terminus X on line L". A terminus station offers one direction; a mid-line
   station offers two; an interchange such as Muzeum (A + C) offers four.
3. **Departure preview**: each direction row shows the next departure for that direction, sourced
   from mocked data and refreshed while the screen is visible.
4. **Favoriting**: each direction row has a toggle that adds or removes that station-and-direction
   pair from favorites. The toggle reflects current state and persists across process death.
5. **Home**: the Home screen lists saved favorites as cards showing station name, line, direction,
   and next departure, refreshing while visible. It shows an explicit empty state when nothing is
   saved and an explicit no-service state when a board has no upcoming departures.

All departure data is mocked in this plan. The mock's shape deliberately mirrors the Golemio PID
departure board API so that replacing it later is a repository-implementation change only.

## Repository Context

### Relevant existing files

- `domain/model/Station.kt` — `data class Station(id, name, line: Line)`. Single line per station,
  which cannot represent interchange stations.
- `domain/model/Line.kt` — `enum class Line(colorHex: Long)` with `A`, `B`, `C`.
- `domain/repository/StationRepository.kt` — one method, `getStations(): Flow<List<Station>>`.
- `data/repository/MockStationRepository.kt` — nine hardcoded stations, three per line, ids
  `a1`–`c3`. No station ordering, no termini.
- `ui/screens/search/SearchScreen.kt` — query field, line `FilterChip` row, `LazyColumn` of
  `StationItem`. Rows are not clickable. Screen has `testTag("SearchScreen")`.
- `ui/screens/search/SearchViewModel.kt` — `combine` of stations, query, and selected line into
  `filteredStations: StateFlow<List<Station>>` via `stateIn(WhileSubscribed(5000))`.
- `ui/screens/home/HomeScreen.kt` — placeholder `Text("Home Screen - Favorites")`.
- `ui/screens/MainScreen.kt` — `Scaffold` + `NavHost` with `HomeRoute`, `SearchRoute`,
  `SettingsRoute`; `composable<HomeRoute> { HomeScreen() }`.
- `ui/navigation/Screen.kt` — type-safe `@Serializable data object` routes.
- `di/PreferencesModule.kt` — provides a `@Singleton DataStore<Preferences>` named
  `user_preferences`; binds `UserPreferencesRepository`.
- `di/RepositoryModule.kt` — binds `StationRepository` to `MockStationRepository`.
- `data/preferences/DataStoreUserPreferencesRepository.kt` — the persistence pattern to follow:
  typed key, `catch { IOException -> emit(emptyPreferences()) }`, `map` to a domain type.
- `ui/screens/settings/SettingsScreen.kt` — the screen pattern to follow: a stateless
  `SettingsScreen(uiState, callbacks)` plus a `SettingsScreenRoute()` wrapper that owns
  `hiltViewModel()`.

### Existing tests that this work changes

- `app/src/androidTest/.../ui/screens/MainScreenTest.kt` asserts the literal string
  `"Home Screen - Favorites"` in three tests. Replacing the Home screen breaks all three; they must
  be updated as part of this work.
- `app/src/test/.../data/preferences/DataStoreUserPreferencesRepositoryTest.kt` shows the JVM
  DataStore test pattern (`TemporaryFolder`, `PreferenceDataStoreFactory.create`, `TestScope`).

### Pre-existing repository condition

`app/src/androidTest/java/dev/pukan/metroprague/HiltComponentActivity.kt` is staged in the index but
missing from the working tree. `MainScreenTest` imports it, so the `androidTest` source set does not
currently compile. Task 0 restores it before any other work.

### Constraints

- `AGENTS.md`: Compose-only (no XML layouts, Fragments, or ViewBinding), MVVM with unidirectional
  data flow, Hilt for DI, `StateFlow` from ViewModels, type-safe navigation, user-visible strings in
  `res/values/strings.xml`, 4-space indent, trailing commas, `LazyColumn` with stable keys, one
  top-level composable per file where practical.
- Dependencies must come from `gradle/libs.versions.toml`. **This plan adds no new dependencies.**
  Everything required is already declared: `datastore-preferences`, `kotlinx-serialization-json`,
  `androidx-lifecycle-runtime-compose`, `androidx-material3` (`1.5.0-alpha21`, which contains
  `ModalBottomSheet`), `hilt-android`, `kotlinx-coroutines-test`, `hilt-android-testing`,
  `androidx-ui-test-junit4`.
- `minSdk = 36`, so `java.time` is available without desugaring.
- Tests: at least one happy path and one edge case per new piece of logic; instrumentation tests use
  semantics matchers and must not use sleeps.

### Reference: real Golemio PID departure board schema

The mock's domain model mirrors this response shape (`GET
https://api.golemio.cz/v2/pid/departureboards/`, header `x-access-token`):

```
{ departures[], stops[], infotexts[] }

departures[].departure_timestamp: { scheduled, predicted, minutes }
departures[].arrival_timestamp:   { scheduled, predicted }
departures[].delay:               { is_available, minutes, seconds }
departures[].route:               { short_name, type, is_night, is_regional }
departures[].trip:                { id, headsign, is_at_stop, is_canceled, ... }
departures[].last_stop:           { id, name }
departures[].stop:                { id, platform_code }
```

Two facts from that schema drive decisions below: the API has **no direction parameter** (a board is
per stop, and direction is read from `trip.headsign`, which for the metro is the terminus name), and
it exposes **both a scheduled and a predicted timestamp plus a delay**.

## Decisions

### Domain model

- **D1.** `Station` becomes `data class Station(id: String, name: String, lines: List<LinePosition>)`
  where `data class LinePosition(line: Line, order: Int)`. This is what allows one `Station` to serve
  two lines. `Station.line` is removed.
- **D2.** `order` is the station's **true index along the full real line**, not its index in the mock
  subset. Later expansion to the full network adds rows without renumbering existing ones.
- **D3.** Termini are **derived**, never stored: the terminus of line `L` in a given direction is the
  station with the minimum (respectively maximum) `order` among stations serving `L`. No terminus
  names are duplicated anywhere in the data.
- **D4.** A direction is `data class Direction(line: Line, terminusStationId: String,
  terminusName: String)`. Available directions at station `S` = for each `LinePosition(L, o)` of `S`,
  each terminus `T` of `L` where `T.order != o`.
- **D5.** Station ids become stable slugs (`"muzeum"`, `"depo-hostivar"`), not line-prefixed, because
  an interchange is one station on two lines. Ids are ASCII, lowercase, hyphen-separated, diacritics
  stripped. Nothing persisted depends on the old `a1`–`c3` ids, so no migration is needed.

### Mock station data

- **D6.** The mock set is a 20-station subset covering all three lines. It includes **both real
  termini of every line** (so derivation in D3 produces correct names) and all three interchanges
  (Můstek A/B, Muzeum A/C, Florenc B/C). The full 61-station import is explicitly out of scope.

### Departures

- **D7.** `DepartureRepository.getDepartureBoard(stationId: String): Flow<DepartureBoard>` is
  **station-scoped, not direction-scoped**, matching the real API. Direction filtering happens in the
  domain layer.
- **D8.** `Departure` carries `scheduled`, `predicted`, `delaySeconds`, `isAtStop`, `isCanceled`, and
  `headsign` — mirroring the real payload — even though the mock produces simple values for some of
  them. Designing the UI against a single time field would have to be redone when real data lands.
- **D9.** Minutes-to-departure is **computed locally** from `predicted ?: scheduled` against the
  injected clock. The real API's `minutes` field is a display-only `String` (it can read `"<1"`) and
  is not modelled.
- **D10.** Time comes from an injected `java.time.Clock`, provided by a new Hilt `ClockModule` as
  `Clock.systemDefaultZone()`, so departure generation is deterministic under test.
- **D11.** The mock emits a new board every 15 seconds while collected. There are no timers,
  `LaunchedEffect` loops, or manual refresh calls in composables; liveness comes from the Flow plus
  `collectAsStateWithLifecycle` plus `stateIn(WhileSubscribed(5_000))`, which stops the work when the
  screen is backgrounded.
- **D12.** Mock headway by local time of day: `07:00–08:59` and `15:00–18:59` → 2 minutes;
  `09:00–14:59` and `19:00–19:59` → 5 minutes; `04:30–06:59` and `20:00–00:29` → 8 minutes;
  `00:30–04:29` → **no service, empty departures list**. The no-service state is a required UI state,
  not an afterthought.
- **D13.** Each direction gets a deterministic phase offset so opposing directions do not depart in
  lockstep: `offsetSeconds = (stationId + terminusStationId).hashCode().absoluteValue % headwaySeconds`.
- **D14.** Departures are generated per direction, three per direction, merged into one board and
  sorted ascending by effective departure time.
- **D15.** Mock delay: every fifth trip slot per direction, numbered from the scheduled local time
  and current headway rather than the index in the three-departure board, gets
  `delaySeconds = 60`; all others get `0`. `predicted = scheduled + delaySeconds`. `isCanceled` is
  always `false` in the mock. `isAtStop` is `true` when the effective time is within 30 seconds of
  now.
- **D16.** In the mock, `headsign` is set to the direction's terminus name, so direction filtering is
  `it.line == direction.line && it.headsign == direction.terminusName`. This filter lives in one
  function, `DepartureBoard.forDirection`, because real data has short-turn trips whose headsign is
  not a line terminus, and that is the single place the fallback will later be added. Add a code
  comment saying exactly that.

### Favorites persistence

- **D17.** Favorites are stored in the **existing** `DataStore<Preferences>` singleton, under
  `stringPreferencesKey("favorites_v1")`, as a `kotlinx.serialization` JSON string. Room is **not**
  introduced: no new dependency, ordering is preserved (a `Set` would lose it), and the real
  justification for a local database is caching the station dataset and timetable, which is out of
  scope here.
- **D18.** The persisted DTO is versioned and lives in the data layer, separate from domain types:
  `@Serializable data class StoredFavoritesV1(version: Int = 1, favorites: List<StoredFavorite>)`.
  This makes a later migration to Room a one-time read-and-import.
- **D19.** A favorite is identified by `FavoriteKey(stationId, line, terminusStationId)` — ids only,
  never denormalized names. Resolution to display data happens by joining against
  `StationRepository`.
- **D20.** Read is total: a missing key, blank value, malformed JSON, or an unknown `Line` name
  yields an empty list or drops only the offending entry. Never throw, never crash.
- **D21.** Adding an existing favorite is a no-op that preserves position. New favorites append to
  the end. Reordering is out of scope.
- **D22.** A favorite whose `stationId`, line, or terminus no longer resolves against the current
  station data is **silently dropped from display** and left untouched on disk.

### UI

- **D23.** Screens follow the existing `SettingsScreenRoute` pattern: a stateless
  `XScreen(uiState, callbacks)` plus an `XScreenRoute()` wrapper owning `hiltViewModel()`. This keeps
  instrumentation tests free of Hilt fakes.
- **D24.** The direction picker is a `ModalBottomSheet`. Its rows are a separate reusable
  `DirectionRow` composable so a future station-detail route can host the same list.
- **D25.** ViewModels emit a sealed `DepartureDisplay` rather than preformatted text; composables map
  it to `stringResource`. This keeps formatting testable on the JVM and strings in `strings.xml`.
  Variants: `NoService`, `Cancelled`, `AtStation`, `Now`, `InMinutes(minutes: Int)`,
  `AtTime(hour: Int, minute: Int)`. `InMinutes` is used below 60 minutes, `AtTime` at or above it.
- **D26.** The Home empty state offers a control that navigates to Search. `MainScreen` passes the
  navigation lambda; the Home screen does not touch `NavController`.
- **D27.** Removing a favorite from Home is a trailing filled-star `IconButton` on the card. There is
  no confirmation dialog and no undo.
- **D28.** Line colour is rendered as a filled circle using `Color(line.colorHex)`, matching the
  existing `StationItem` treatment, and is always accompanied by the line letter in text so colour is
  never the only carrier of meaning.

## Out of Scope

- Any real network call, API client, API key handling, or Golemio integration.
- Importing the full 61-station Prague metro dataset; trams, buses, or any non-metro mode.
- Room, or any local database.
- Full timetables, journey planning, route search, or a station-detail screen.
- Service alerts and info texts rendered in the UI (the field is modelled, not displayed).
- Reordering favorites, favorite groups, widgets, or notifications.
- Localization of new strings into Czech (English `strings.xml` entries only).
- Changing navigation structure, the bottom bar, or the Settings screen.
- Wheelchair/air-conditioning flags, platform codes, and zones from the real schema.

## Task Dependency Order

```
Task 0  Restore HiltComponentActivity                      (no dependencies)
Task 1  Domain model: LinePosition, Direction, mock data    (depends on 0)
Task 2  Departure domain model + mock repository + clock DI (depends on 1)
Task 3  Search: clickable rows + direction picker sheet     (depends on 2)
Task 4  Favorites repository + sheet toggle                 (depends on 3)
Task 5  Home screen favorites list + MainScreenTest update  (depends on 4)
```

Each task must compile, pass `./gradlew test`, and leave the app runnable before the next begins.

---

## Tasks

### Task 0 — Restore the missing instrumentation test activity

**Objective.** Make the `androidTest` source set compile again before any other change.

**Work.**

- Restore `app/src/androidTest/java/dev/pukan/metroprague/HiltComponentActivity.kt` with exactly:

  ```kotlin
  package dev.pukan.metroprague

  import androidx.activity.ComponentActivity
  import dagger.hilt.android.AndroidEntryPoint

  @AndroidEntryPoint
  class HiltComponentActivity : ComponentActivity()
  ```

- Do not modify any other file.

**Acceptance Criteria.**

- The file exists in the working tree.
- `./gradlew assembleDebugAndroidTest` compiles.

---

### Task 1 — Multi-line stations, derived directions, and expanded mock data

**Objective.** Represent a station's position on every line it serves, derive directions and termini
from that data, and expand the mock dataset so termini and interchanges are exercisable.

**Work.**

1. Replace `domain/model/Station.kt`:

   ```kotlin
   data class LinePosition(val line: Line, val order: Int)

   data class Station(
       val id: String,
       val name: String,
       val lines: List<LinePosition>,
   )
   ```

   `order` is the station's index along the full real line (Decision D2).

2. Add `domain/model/Direction.kt`:

   ```kotlin
   data class Direction(
       val line: Line,
       val terminusStationId: String,
       val terminusName: String,
   )
   ```

3. Add `domain/model/StationDirections.kt` with pure functions over a station list:

   - `fun List<Station>.terminiOf(line: Line): List<Station>` — the stations with the minimum and
     maximum `order` among stations serving `line`. Returns an empty list if no station serves the
     line, and a single-element list if only one does.
   - `fun List<Station>.directionsAt(station: Station): List<Direction>` — for each
     `LinePosition(line, order)` of `station`, one `Direction` per terminus of that line whose
     `order` differs from `station`'s `order` on that line. Ordered by `Line` declaration order, then
     by terminus `order` ascending. Deterministic.

   These are top-level extension functions on `List<Station>`, not members of `Station`, because
   direction derivation needs the whole line.

4. Replace the station list in `data/repository/MockStationRepository.kt` with exactly the 20
   stations below. Ids are slugs (D5); `order` values are real full-line indices (D2).

   | id | name | lines (line@order) |
   | --- | --- | --- |
   | `nemocnice-motol` | Nemocnice Motol | A@0 |
   | `dejvicka` | Dejvická | A@4 |
   | `hradcanska` | Hradčanská | A@5 |
   | `malostranska` | Malostranská | A@6 |
   | `staromestska` | Staroměstská | A@7 |
   | `mustek` | Můstek | A@8, B@12 |
   | `muzeum` | Muzeum | A@9, C@9 |
   | `depo-hostivar` | Depo Hostivař | A@16 |
   | `zlicin` | Zličín | B@0 |
   | `andel` | Anděl | B@9 |
   | `karlovo-namesti` | Karlovo náměstí | B@10 |
   | `narodni-trida` | Národní třída | B@11 |
   | `namesti-republiky` | Náměstí Republiky | B@13 |
   | `florenc` | Florenc | B@14, C@7 |
   | `cerny-most` | Černý Most | B@23 |
   | `letnany` | Letňany | C@0 |
   | `hlavni-nadrazi` | Hlavní nádraží | C@8 |
   | `ip-pavlova` | I. P. Pavlova | C@10 |
   | `vysehrad` | Vyšehrad | C@11 |
   | `haje` | Háje | C@19 |

   Keep the list sorted by line then `order` for readability. `getStations()` still returns
   `flowOf(stations)`.

5. Update `ui/screens/search/SearchViewModel.kt`: the line filter becomes
   `station.lines.any { it.line == line }`.

6. Update `StationItem` in `ui/screens/search/SearchScreen.kt`: render one colour dot per entry in
   `station.lines` (ordered by `Line` declaration order), each with the line letter as text beside or
   within the row (D28). Give each row `testTag("StationItem_${station.id}")`.

**Tests.** New `app/src/test/java/dev/pukan/metroprague/domain/model/StationDirectionsTest.kt`
against the real mock list obtained from `MockStationRepository`:

- `terminiOf` returns Nemocnice Motol and Depo Hostivař for line A, Zličín and Černý Most for B,
  Letňany and Háje for C.
- `directionsAt` for a mid-line single-line station (Anděl) returns exactly 2 directions, toward
  Zličín and Černý Most.
- Edge case: `directionsAt` for a terminus (Háje) returns exactly 1 direction, toward Letňany.
- Edge case: `directionsAt` for the interchange Muzeum returns exactly 4 directions — A toward
  Nemocnice Motol, A toward Depo Hostivař, C toward Letňany, C toward Háje.
- `directionsAt` output order is stable across repeated calls.

**Acceptance Criteria.**

- `Station` has no `line` property; nothing in the codebase references it.
- Muzeum, Můstek, and Florenc each have two `LinePosition` entries.
- Search still filters by query and by line chip, and interchange stations appear under both of their
  lines' chips.
- Search rows render one colour dot per line served.
- `./gradlew test` passes.

---

### Task 2 — Departure domain model, injected clock, and mock departure repository

**Objective.** Provide mocked, time-aware departure boards shaped like the real Golemio payload,
refreshing on a timer, with a no-service window.

**Work.**

1. Add `domain/model/Departure.kt`:

   ```kotlin
   data class Departure(
       val tripId: String,
       val line: Line,
       val headsign: String,
       val scheduled: Instant,
       val predicted: Instant?,
       val delaySeconds: Int?,
       val isAtStop: Boolean,
       val isCanceled: Boolean,
   ) {
       val effectiveTime: Instant get() = predicted ?: scheduled
   }

   data class DepartureBoard(
       val stationId: String,
       val departures: List<Departure>,
       val infoTexts: List<String> = emptyList(),
   )
   ```

   Use `java.time.Instant`. Add a KDoc line on `Departure` naming the Golemio fields each property
   mirrors (`departure_timestamp.scheduled`, `departure_timestamp.predicted`, `delay.seconds`,
   `trip.headsign`, `trip.is_at_stop`, `trip.is_canceled`, `trip.id`, `route.short_name`).

2. Add `DepartureBoard.forDirection(direction: Direction): List<Departure>` as an extension in the
   same file, filtering `it.line == direction.line && it.headsign == direction.terminusName`
   (D16). Include a comment stating that real data contains short-turn trips whose headsign is not a
   line terminus, and that this function is where the position-based fallback belongs.

3. Add `domain/repository/DepartureRepository.kt`:

   ```kotlin
   interface DepartureRepository {
       fun getDepartureBoard(stationId: String): Flow<DepartureBoard>
   }
   ```

4. Add `di/ClockModule.kt` — a Hilt `@Module @InstallIn(SingletonComponent::class)` providing
   `@Singleton fun provideClock(): Clock = Clock.systemDefaultZone()`.

5. Add `data/repository/MockDepartureRepository.kt`, injecting `StationRepository` and `Clock`.

   `getDepartureBoard(stationId)` returns a `Flow` that emits a freshly generated board immediately
   and then every 15 seconds (D11), for as long as it is collected. Implement with a
   `flow { while (currentCoroutineContext().isActive) { emit(build()); delay(15_000) } }`; do not
   use `GlobalScope` or a background service.

   Board generation, given `now = Instant.now(clock)` and the station list:

   - Resolve the station by id. If unknown, emit `DepartureBoard(stationId, emptyList())`.
   - Determine local time via `clock.zone`. Apply the headway table in D12. Inside the no-service
     window, emit an empty departures list.
   - For each direction from `directionsAt(station)`, compute
     `offsetSeconds` per D13, then take the first 3 instants strictly after `now` whose
     seconds-since-local-midnight satisfy `t % headwaySeconds == offsetSeconds % headwaySeconds`.
     Departures generated past the start of the no-service window are dropped.
   - Build each `Departure` with `tripId = "$stationId-${direction.terminusStationId}-$index"`,
     `line = direction.line`, `headsign = direction.terminusName`, delay per D15, `isAtStop` per D15,
     `isCanceled = false`.
   - Merge all directions and sort ascending by `effectiveTime` (D14).

   The generator must be a pure function of `(now, station, allStations)` — extract it as an internal
   function so tests can call it without collecting the flow.

6. Bind it in `di/RepositoryModule.kt`:
   `@Binds @Singleton abstract fun bindDepartureRepository(impl: MockDepartureRepository): DepartureRepository`.

**Tests.** New `app/src/test/java/dev/pukan/metroprague/data/repository/MockDepartureRepositoryTest.kt`
using `Clock.fixed(...)` with the Europe/Prague zone:

- At 08:00 local, a mid-line station's board contains departures for both directions, 3 each, all
  strictly after `now`, sorted ascending by `effectiveTime`.
- At 08:00, consecutive departures within one direction are exactly 120 seconds apart (peak headway).
- At 12:00, the same station's within-direction gap is 300 seconds (off-peak headway).
- Edge case: at 02:00 local, the board's departures list is empty (no-service window).
- Edge case: at 08:00, a terminus station (Háje) produces departures for exactly one direction, and
  the interchange Muzeum produces departures for exactly four.
- Opposing directions at the same station do not share identical departure instants (phase offset).
- `forDirection` returns only the departures whose line and headsign match.
- An unknown station id produces an empty board rather than throwing.

**Acceptance Criteria.**

- No new Gradle dependency was added.
- No composable, ViewModel, or repository calls `Instant.now()` or `System.currentTimeMillis()`
  directly; all time reads go through the injected `Clock`.
- `./gradlew test` passes.

---

### Task 3 — Clickable search results and the direction picker sheet

**Objective.** Tapping a search result opens a bottom sheet listing that station's directions, each
showing its next departure, refreshed live.

**Work.**

1. Add `ui/model/DepartureDisplay.kt`:

   ```kotlin
   sealed interface DepartureDisplay {
       data object NoService : DepartureDisplay
       data object Cancelled : DepartureDisplay
       data object AtStation : DepartureDisplay
       data object Now : DepartureDisplay
       data class InMinutes(val minutes: Int) : DepartureDisplay
       data class AtTime(val hour: Int, val minute: Int) : DepartureDisplay
   }
   ```

   Add `fun Departure?.toDisplay(now: Instant, zone: ZoneId): DepartureDisplay` in the same file:
   `null` → `NoService`; `isCanceled` → `Cancelled`; `isAtStop` → `AtStation`; under 60 seconds away
   → `Now`; under 60 minutes → `InMinutes` (rounded down); otherwise `AtTime` from the effective time
   in `zone`.

2. Add these strings to `res/values/strings.xml`:

   ```xml
   <!-- Departures -->
   <string name="departure_no_service">No service</string>
   <string name="departure_cancelled">Cancelled</string>
   <string name="departure_at_station">At station</string>
   <string name="departure_now">Now</string>
   <string name="departure_in_minutes">%1$d min</string>
   <string name="departure_at_time">%1$02d:%2$02d</string>
   <string name="direction_toward">To %1$s</string>
   <string name="line_label">Line %1$s</string>
   <!-- Direction picker -->
   <string name="direction_picker_title">Choose a direction</string>
   <string name="direction_picker_close">Close</string>
   <string name="favorite_add">Add to favorites</string>
   <string name="favorite_remove">Remove from favorites</string>
   ```

3. Add `ui/components/DirectionRow.kt` — a stateless composable taking `directionLabel: String`,
   `lineLetter: String`, `lineColor: Color`, `departure: DepartureDisplay`,
   `isFavorite: Boolean`, `onToggleFavorite: () -> Unit`. Renders the line dot plus letter, the
   "To <terminus>" label, the formatted departure, and a star `IconButton` whose
   `contentDescription` is `favorite_add` or `favorite_remove` accordingly. In this task the
   `isFavorite`/`onToggleFavorite` parameters exist but are wired to `false` and a no-op; Task 4
   connects them.

4. Extend `SearchViewModel`:

   - `private val _selectedStationId = MutableStateFlow<String?>(null)`.
   - `fun onStationSelected(station: Station)` and `fun onSheetDismissed()`.
   - `val sheetState: StateFlow<DirectionSheetUiState?>` built by `flatMapLatest` on the selected id:
     `null` when nothing is selected; otherwise combine the station list with
     `departureRepository.getDepartureBoard(id)` into

     ```kotlin
     data class DirectionSheetUiState(
         val stationName: String,
         val directions: List<DirectionRowUiState>,
     )

     data class DirectionRowUiState(
         val direction: Direction,
         val lineLetter: String,
         val lineColorHex: Long,
         val nextDeparture: DepartureDisplay,
         val isFavorite: Boolean,
     )
     ```

     `nextDeparture` is `board.forDirection(direction).firstOrNull().toDisplay(Instant.now(clock), clock.zone)`.
     Inject `Clock` into the ViewModel. `isFavorite` is `false` for now.
   - Use `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)`.

5. Split `SearchScreen` per D23 into a stateless `SearchScreen(...)` taking `searchQuery`,
   `selectedLine`, `stations`, `sheetState`, and the callbacks, plus a `SearchScreenRoute()` wrapper
   that owns `hiltViewModel()` and collects with `collectAsStateWithLifecycle`. Update
   `MainScreen.kt` to call `SearchScreenRoute()`. Keep `testTag("SearchScreen")` on the stateless
   screen so `MainScreenTest` continues to pass.

6. Make `StationItem` clickable via the `Surface(onClick = ...)` overload, calling
   `onStationSelected`.

7. Add `ui/screens/search/DirectionPickerSheet.kt` — a `ModalBottomSheet`
   (`@OptIn(ExperimentalMaterial3Api::class)`) shown when `sheetState != null`, with
   `onDismissRequest = onSheetDismissed`, a title, and a `Column` of `DirectionRow`s keyed by
   `direction.line` + `direction.terminusStationId`. Give the sheet `testTag("DirectionPickerSheet")`
   and each row `testTag("DirectionRow_${direction.line}_${direction.terminusStationId}")`.

**Tests.**

- New `app/src/test/.../ui/model/DepartureDisplayTest.kt`: `null` → `NoService`; a canceled
  departure → `Cancelled`; `isAtStop` → `AtStation`; 30 seconds out → `Now`; 5 minutes out →
  `InMinutes(5)`; 90 minutes out → `AtTime` with the correct local hour and minute. Edge case:
  exactly 60 minutes out → `AtTime`, not `InMinutes(60)`.
- New `app/src/test/.../ui/screens/search/SearchViewModelTest.kt` with a fake `StationRepository`, a
  fake `DepartureRepository`, and a fixed `Clock`: `sheetState` is `null` initially; after
  `onStationSelected(muzeum)` it exposes 4 direction rows with the expected terminus names; after
  `onSheetDismissed()` it is `null` again. Edge case: selecting a station whose board is empty yields
  rows whose `nextDeparture` is `NoService`.
- New `app/src/androidTest/.../ui/screens/search/SearchScreenTest.kt` driving the **stateless**
  `SearchScreen` with hand-built state: rendering a sheet state shows `DirectionPickerSheet` and one
  `DirectionRow` node per direction; clicking a `StationItem_*` node invokes the
  `onStationSelected` callback. No sleeps; use semantics matchers only.

**Acceptance Criteria.**

- Tapping any search result opens the sheet for that station.
- Muzeum shows 4 direction rows; Háje shows 1; Anděl shows 2.
- Each row shows a departure value that changes over time without any manual refresh.
- Dismissing the sheet returns to the list with the query and filter intact.
- `./gradlew test` passes and `./gradlew assembleDebugAndroidTest` compiles.

---

### Task 4 — Favorites repository and the sheet toggle

**Objective.** Persist station-and-direction favorites and make the sheet's star reflect and change
that state.

**Work.**

1. Add `domain/model/FavoriteKey.kt`:

   ```kotlin
   data class FavoriteKey(
       val stationId: String,
       val line: Line,
       val terminusStationId: String,
   )
   ```

2. Add `domain/repository/FavoritesRepository.kt`:

   ```kotlin
   interface FavoritesRepository {
       val favorites: Flow<List<FavoriteKey>>
       suspend fun add(key: FavoriteKey)
       suspend fun remove(key: FavoriteKey)
   }
   ```

3. Add `data/favorites/DataStoreFavoritesRepository.kt`, injecting the existing
   `DataStore<Preferences>`. Follow the `DataStoreUserPreferencesRepository` pattern exactly:
   `stringPreferencesKey("favorites_v1")`, `.catch { if (it is IOException) emit(emptyPreferences()) else throw it }`,
   `.map { decode(it[key]) }`.

   Private serialization types in the same file (D18):

   ```kotlin
   @Serializable
   private data class StoredFavoritesV1(
       val version: Int = 1,
       val favorites: List<StoredFavorite> = emptyList(),
   )

   @Serializable
   private data class StoredFavorite(
       val stationId: String,
       val line: String,
       val terminusStationId: String,
   )
   ```

   Decoding is total (D20): missing key, blank string, or `SerializationException` → `emptyList()`;
   an entry whose `line` does not match a `Line` enum name is dropped, others are kept. Use a
   configured `Json { ignoreUnknownKeys = true }` instance.

   `add` appends only if no equal `FavoriteKey` is present, preserving existing order (D21).
   `remove` filters by equality. Both use `dataStore.edit`.

4. Bind it in a new `di/FavoritesModule.kt` (or extend `PreferencesModule`) as
   `@Binds @Singleton`. Do not create a second `DataStore` instance — reuse the provided singleton.

5. Wire the toggle in `SearchViewModel`: include `favoritesRepository.favorites` in the sheet-state
   combine so `DirectionRowUiState.isFavorite` is real, and add
   `fun onToggleFavorite(direction: Direction)` which resolves the current selected station id, builds
   the `FavoriteKey`, and calls `add` or `remove` in `viewModelScope` depending on current state.

6. Pass the real `isFavorite` and `onToggleFavorite` through `DirectionPickerSheet` into
   `DirectionRow`.

**Tests.**

- New `app/src/test/.../data/favorites/DataStoreFavoritesRepositoryTest.kt`, mirroring the existing
  DataStore test setup (`TemporaryFolder`, `PreferenceDataStoreFactory.create`, `TestScope`):
  empty store → empty list; add then read round-trips; adding the same key twice keeps one entry and
  the original position; remove deletes only the matching key; insertion order is preserved across
  three adds; a second repository instance over the same store reads the saved values.
  Edge cases: a stored value of `"not json"` yields an empty list without throwing; a stored entry
  with `"line": "Z"` is dropped while a valid sibling entry survives.
- Extend `SearchViewModelTest`: toggling a direction adds a `FavoriteKey` with the right station,
  line, and terminus; toggling again removes it; `isFavorite` in the emitted row state flips both
  times.

**Acceptance Criteria.**

- Starring a direction, killing the app, and reopening it shows that direction still starred.
- Starring the same direction twice leaves exactly one stored favorite.
- Corrupt stored data degrades to "no favorites" without a crash.
- `./gradlew test` passes.

---

### Task 5 — Home screen favorites list

**Objective.** Replace the Home placeholder with a live list of favorite station-and-direction cards,
including empty and no-service states.

**Work.**

1. Add these strings to `res/values/strings.xml`:

   ```xml
   <!-- Home -->
   <string name="home_title">Favorites</string>
   <string name="home_empty_title">No favorites yet</string>
   <string name="home_empty_body">Search for a station and pick a direction to add it here.</string>
   <string name="home_empty_action">Search stations</string>
   ```

2. Add `ui/screens/home/HomeUiState.kt`:

   ```kotlin
   data class HomeUiState(
       val isLoading: Boolean = true,
       val favorites: List<FavoriteCardUiState> = emptyList(),
   )

   data class FavoriteCardUiState(
       val key: FavoriteKey,
       val stationName: String,
       val lineLetter: String,
       val lineColorHex: Long,
       val terminusName: String,
       val nextDeparture: DepartureDisplay,
   )
   ```

3. Add `ui/screens/home/HomeViewModel.kt` injecting `FavoritesRepository`, `StationRepository`,
   `DepartureRepository`, and `Clock`.

   - Combine favorites with the station list and resolve each `FavoriteKey` to its station and
     terminus name. Drop unresolvable favorites silently (D22).
   - `flatMapLatest` to the departure boards: **if the resolved list is empty, emit
     `HomeUiState(isLoading = false, favorites = emptyList())` directly** — do not call `combine` on
     an empty list of flows, which never emits and would leave the screen stuck loading.
   - Otherwise `combine` one `getDepartureBoard(stationId)` flow per distinct station id and map each
     favorite to a `FavoriteCardUiState` whose `nextDeparture` is
     `board.forDirection(direction).firstOrNull().toDisplay(Instant.now(clock), clock.zone)`.
   - Expose `uiState: StateFlow<HomeUiState>` via `stateIn(viewModelScope,
     SharingStarted.WhileSubscribed(5_000), HomeUiState())`.
   - `fun onRemoveFavorite(key: FavoriteKey)` delegates to the repository.

4. Rewrite `ui/screens/home/HomeScreen.kt` per D23:

   - `HomeScreen(uiState, onRemoveFavorite, onNavigateToSearch)` — stateless. `Scaffold` with a title,
     `testTag("HomeScreen")`. Empty state: title, body, and a button labelled `home_empty_action`
     calling `onNavigateToSearch`. Otherwise a `LazyColumn` with
     `key = { "${it.key.stationId}-${it.key.line}-${it.key.terminusStationId}" }` rendering a
     `FavoriteCard` per item, each with `testTag("FavoriteCard_${...same key...}")`.
   - `FavoriteCard` shows station name, line dot plus letter, "To <terminus>", the formatted
     departure, and a trailing filled-star `IconButton` with `contentDescription` from
     `favorite_remove` that calls `onRemoveFavorite` (D27).
   - `HomeScreenRoute(onNavigateToSearch: () -> Unit)` owns `hiltViewModel()` and collects with
     `collectAsStateWithLifecycle`.

5. Update `MainScreen.kt`:
   `composable<HomeRoute> { HomeScreenRoute(onNavigateToSearch = { navController.navigate(SearchRoute) { launchSingleTop = true } }) }`.

6. Update `MainScreenTest`: replace the three `"Home Screen - Favorites"` assertions with
   `onNodeWithTag("HomeScreen")` assertions (`assertExists` / `assertDoesNotExist`), preserving each
   test's original intent.

7. Add `@Preview` composables for the Home screen in both the empty and populated states, following
   the light/dark preview pattern already used in `SettingsScreen.kt`.

**Tests.**

- New `app/src/test/.../ui/screens/home/HomeViewModelTest.kt` with fakes and a fixed `Clock`:
  no favorites → `isLoading = false` and an empty list (this specifically guards the empty-`combine`
  trap); two favorites at different stations → two cards with correct station and terminus names
  ordered as stored; `onRemoveFavorite` removes the card.
  Edge cases: a favorite referencing an unknown `stationId` is dropped while its valid sibling
  survives; a favorite whose board has no departures yields `nextDeparture = NoService`.
- New `app/src/androidTest/.../ui/screens/home/HomeScreenTest.kt` driving the stateless `HomeScreen`
  with hand-built state: the empty state renders `home_empty_title` and its action invokes the
  navigation callback; a populated state renders one `FavoriteCard_*` node per favorite and the star
  button invokes `onRemoveFavorite` with the right key.

**Acceptance Criteria.**

- A direction starred in Search appears on Home immediately on returning to it.
- Each card's departure updates without user interaction while Home is visible.
- Removing the last favorite reveals the empty state, whose action navigates to Search.
- With the clock inside the no-service window, cards read "No service" rather than blank.
- `MainScreenTest` passes unmodified in intent.
- `./gradlew test` passes.

---

## Definition of Done

**Behavior.**

- Searching, tapping a station, choosing a direction, seeing its next departure, starring it, and
  finding it on Home all work end to end against mock data.
- Interchange stations offer four directions; termini offer one; mid-line stations offer two.
- Favorites survive process death.
- Departures refresh while a screen is visible and stop refreshing when it is not.
- The no-service window and the empty-favorites state both render explicit, readable text.

**Tests.**

- `./gradlew test` and `./gradlew connectedAndroidTest` both pass.
- New JVM tests exist for direction derivation, mock departure generation, departure formatting,
  favorites persistence, `SearchViewModel`, and `HomeViewModel`, each covering at least one happy
  path and one edge case.
- New instrumentation tests exist for the direction picker sheet and the Home screen.

**Build and quality.**

- `./gradlew assembleDebug` succeeds with no new warnings introduced by this work.
- `gradle/libs.versions.toml` and `app/build.gradle.kts` are unchanged — no new dependencies.
- No XML layouts, Fragments, ViewBinding, or `findViewById`.
- All new user-visible strings are in `res/values/strings.xml`; no hardcoded UI text.
- No composable reads the clock directly or runs its own refresh timer.
- Repository interfaces live in `domain/repository`, implementations in `data/`, and every binding
  goes through a Hilt module.

**Follow-up work this plan deliberately leaves open** (not part of Done): swapping
`MockDepartureRepository` for a Golemio client, importing the full station dataset with Room, adding
the short-turn headsign fallback in `DepartureBoard.forDirection`, and Czech localization.
