# Metro data sync

## Summary

Metro stations and departure boards are stored locally. The user starts a reload from Settings; screens read the saved snapshot and update time labels without polling the network.

## Must match

1. Reload is user initiated from Settings only. Opening Home or Search and restarting the app do not request a reload; leaving Settings does not cancel an already-started application-level reload. (`android/app/src/main/java/dev/pukan/metroprague/ui/screens/settings/SettingsViewModel.kt`, `android/app/src/main/java/dev/pukan/metroprague/data/repository/GolemioMetroDataRepository.kt`, `android/app/src/test/java/dev/pukan/metroprague/data/repository/GolemioMetroDataRepositoryTest.kt`)
2. A successful reload replaces one persisted snapshot containing the metro catalogue, saved board responses and per-board fetch times, reload time, and departure count. Cancellation or any failed partial reload leaves the previous complete snapshot intact. (`android/app/src/main/java/dev/pukan/metroprague/data/golemio/MetroDataSnapshot.kt`, `android/app/src/main/java/dev/pukan/metroprague/data/golemio/MetroSnapshotStore.kt`, `android/app/src/main/java/dev/pukan/metroprague/data/repository/GolemioMetroDataRepository.kt`, `android/app/src/test/java/dev/pukan/metroprague/data/repository/GolemioMetroDataRepositoryTest.kt`)
3. A board is fresh only when its whole-second age is 0 through 299 inclusive; all other ages are stale. Screens show an inline stale departure state (“Reload departures in Settings”), not a prompt or dialog, instead of treating expired departures as current; the five-minute threshold is communicated in Settings. (`android/app/src/main/java/dev/pukan/metroprague/data/repository/GolemioDepartureRepository.kt`, `android/app/src/main/res/values/strings.xml`, `android/app/src/test/java/dev/pukan/metroprague/data/repository/GolemioDepartureRepositoryTest.kt`)
4. A failed reload keeps the previous complete snapshot and does not rewrite favorites. HTTP 401/403 map to an authentication error, HTTP 429 maps to a rate-limit error, and other failures map to a network/data error; the Settings messages state that saved data is kept. (`android/app/src/main/java/dev/pukan/metroprague/data/repository/GolemioMetroDataRepository.kt`, `android/app/src/main/java/dev/pukan/metroprague/data/favorites/DataStoreFavoritesRepository.kt`, `android/app/src/main/res/values/strings.xml`, `android/app/src/test/java/dev/pukan/metroprague/data/repository/GolemioMetroDataRepositoryTest.kt`)
5. Requests through the shared HTTP client are serialized and spaced by at least 600 ms after each response. A numeric `Retry-After` on HTTP 429 is clamped to 1–3600 seconds; absent or nonnumeric values use 8 seconds. When a further call would wait more than the normal 600 ms pacing interval, it reports rate-limited promptly. (`android/app/src/main/java/dev/pukan/metroprague/data/golemio/GolemioBoardService.kt`, `android/app/src/test/java/dev/pukan/metroprague/data/golemio/GolemioBoardServiceTest.kt`)
6. A board request asks for the next 60 minutes, up to 30 departures, in departures mode ordered by real time. Only metro routes A, B, and C are loaded. (`android/app/src/main/java/dev/pukan/metroprague/data/golemio/GolemioBoardService.kt`, `android/app/src/main/java/dev/pukan/metroprague/data/golemio/GolemioCatalogueLoader.kt`, `android/app/src/test/java/dev/pukan/metroprague/data/golemio/GolemioBoardServiceTest.kt`)
7. Station search and direction resolution remain available before downloaded data exists. When current trip-pattern metadata omits a known metro platform, the platform fallback still limits departure matching to known metro platforms. (`android/app/src/main/java/dev/pukan/metroprague/data/golemio/MetroNetwork.kt`, `android/app/src/main/java/dev/pukan/metroprague/data/golemio/GolemioCatalogueLoader.kt`, `android/app/src/main/java/dev/pukan/metroprague/data/golemio/MetroPlatformFallbacks.kt`, `android/app/src/main/java/dev/pukan/metroprague/di/RepositoryModule.kt`, `android/app/src/test/java/dev/pukan/metroprague/data/repository/GolemioMetroDataRepositoryTest.kt`)

## Platform-native

Each platform may adapt Settings navigation, progress presentation, persistence technology, and network stack. Preserve user-initiated reload from Settings, complete-snapshot replacement, failure retention, staleness semantics, request pacing/rate-limit behavior, metro-only scope, and a usable station/direction fallback before downloaded data exists. Android currently stores JSON in `files/datastore/metro-data.json`; it uses the bundled `MetroNetwork` for initial station data and a curated `MetroPlatformFallbacks` list for platforms absent from current trip patterns. Android enables live repositories only for debug builds with a nonblank local key; other builds use mock repositories. (`android/app/src/main/java/dev/pukan/metroprague/di/RepositoryModule.kt`, `android/app/src/main/java/dev/pukan/metroprague/data/golemio/MetroNetwork.kt`, `android/app/src/main/java/dev/pukan/metroprague/data/golemio/MetroPlatformFallbacks.kt`)

iOS stores the snapshot as one JSON file, `Application Support/metro-data.json`, written atomically and excluded from backups. It keeps the catalogue's stations and platforms and the raw board responses, which are re-parsed on load; it does not store the raw GTFS routes, stops, or trip patterns, because nothing reads them. iOS goes live only in Debug builds with a Golemio key (only Debug builds can contain one) and otherwise uses generated mock departures. Its mock builds use the full bundled network (58 stations from `MetroNetwork`) rather than Android's 20-station mock list, which is test scaffolding, not spec behavior.

## Android reference

- ViewModel: `android/app/src/main/java/dev/pukan/metroprague/ui/screens/settings/SettingsViewModel.kt`
- Repositories: `android/app/src/main/java/dev/pukan/metroprague/data/repository/GolemioMetroDataRepository.kt`, `android/app/src/main/java/dev/pukan/metroprague/data/repository/GolemioDepartureRepository.kt`, `android/app/src/main/java/dev/pukan/metroprague/data/repository/GolemioStationRepository.kt`
- Storage/network/fallback: `android/app/src/main/java/dev/pukan/metroprague/data/golemio/MetroSnapshotStore.kt`, `MetroDataSnapshot.kt`, `GolemioBoardService.kt`, `GolemioCatalogueLoader.kt`, `MetroNetwork.kt`, `MetroPlatformFallbacks.kt`
- Tests: `android/app/src/test/java/dev/pukan/metroprague/data/repository/GolemioMetroDataRepositoryTest.kt`, `GolemioDepartureRepositoryTest.kt`, `android/app/src/test/java/dev/pukan/metroprague/data/golemio/GolemioBoardServiceTest.kt`, `MetroSnapshotStoreTest.kt`, `GolemioCatalogueLoaderTest.kt`
- API research: `docs/api/golemio.md`

## iOS reference

- Screen: `ios/MetroPrague/Features/Settings/SettingsView.swift` (Metro data section), `MetroDataStatus+Text.swift`
- Store: `ios/MetroPrague/Data/MetroDataStore.swift`
- Data sources: `ios/MetroPrague/Data/MetroDataSource.swift`, `GolemioMetroDataSource.swift`, `MockMetroDataSource.swift`
- Storage/network/fallback: `ios/MetroPrague/Data/Storage/SnapshotFileStore.swift`, `MetroSnapshot.swift`, `ios/MetroPrague/Data/Network/GolemioHTTPClient.swift`, `GolemioEndpoints.swift`, `CatalogueLoader.swift`, `MetroNetwork.swift`, `MetroPlatformFallbacks.swift`
- Staleness: `ios/MetroPrague/Domain/DepartureBoard.swift` (`evaluated(at:)`)
- Tests: `ios/MetroPragueTests/Data/MetroDataStoreTests.swift`, `ios/MetroPragueTests/Data/Network/GolemioHTTPClientTests.swift`, `CatalogueLoaderTests.swift`, `ios/MetroPragueTests/Data/Storage/SnapshotFileStoreTests.swift`, `ios/MetroPragueTests/Domain/DepartureBoardTests.swift`

## Open questions

None. Resolved 2026-11-13: stale departures use an inline state (“Reload departures in Settings”),
not a prompt or dialog; see `docs/product/decisions.md`.

## References

| Artifact | Location |
| --- | --- |
| Golemio API research | `docs/api/golemio.md` |
| Android data sync implementation and tests | `android/app/src/main/java/dev/pukan/metroprague/data/repository/GolemioMetroDataRepository.kt`, `android/app/src/test/java/dev/pukan/metroprague/data/repository/GolemioMetroDataRepositoryTest.kt` |
| Fixtures | [README](../../../fixtures/README.md), [departure-board samples](../../../fixtures/golemio/departureboards/) |
