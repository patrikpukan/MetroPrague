# PR Review: feat: Implement Golemio API integration for metro data (#6)

Branch: `feat/golemio` → `main`  
Reviewed PR head: `6e6d523`, together with the existing uncommitted Claude fixes.  
Review date: 2026-10-14.

## Project profile

- Android-only Kotlin, Compose, Material 3, Hilt, coroutines/Flow, typed JSON DataStore.
- Four-space indentation, default `modifier` parameters, modifiers applied to root layouts, resource-backed user text, stable list keys, lifecycle-aware state collection.
- No repository ktlint/detekt/editorconfig configuration found. Conventions sampled from `SettingsScreen`, `SearchScreen`, `HomeScreen`, and `DirectionRow`.

## Issues

### Critical

None remaining from the two Copilot findings after the fixes and validation below.

### Addressed

- **F2 / Bug — fallback platform membership.** [Copilot finding](https://github.com/patrikpukan/MetroPrague/pull/6#discussion_r4273520166). A shared name and parent station could admit a surface stop into the 30-result departure request. `GolemioCatalogueLoader` now accepts IDs confirmed by metro trip stop times (including arrival-only platforms) or `MetroPlatformFallbacks`, with station and line consistency checks. Saved stop metadata is restricted to those accepted IDs too. The fallback snapshot contains 122 known metro platform IDs: 120 were checked against the current PID stop-level `metroA`/`metroB`/`metroC` classifications; the two known Flora metro IDs are retained explicitly while the current PID stop list omits them. Regression tests exclude a surface stop with the same station name and parent while preserving Flora and a temporarily unserved interchange.
- **F2 / Bug — past timestamp on an at-stop departure.** [Copilot finding](https://github.com/patrikpukan/MetroPrague/pull/6#discussion_r4273520235). `GolemioDepartureRepository` retains at-stop departures throughout the fresh snapshot window even when the timestamp is earlier than now. Ordinary past departures remain filtered. At 300 seconds the snapshot becomes stale and the at-stop exception ends. Tests cover the displayed `AtStation` state at 299 seconds and expiry at 300 seconds.

### Suggestions

The fallback list is a verified snapshot for unavailable trip patterns. Future changes to physical station/platform identities need updating that list along with the bundled topology. Active metro platforms continue to be learned from current trip patterns.

### Positive patterns

- Reload still validates downloads before atomically replacing the entire saved snapshot.
- Screens observe saved data with lifecycle-aware collection and make no automatic API requests.
- Existing station slug IDs remain compatible with favorites.

## Existing Claude fixes preserved

- Full station topology before the initial reload, so Search and existing favorites remain usable.
- A reload prompt before any catalogue is saved.
- Unreadable saved boards become unavailable instead of failing the screen flow.
- Application-scoped reload work survives cancellation of the Settings waiter; the active job owns the reload lock until completion.
- A long Retry-After cooldown reports HTTP 429 promptly rather than silently stalling a new reload.

## Validation

- `./gradlew assembleDebug testDebugUnitTest assembleDebugAndroidTest`: passed, **101 deterministic JVM tests**.
- `ANDROID_SERIAL=emulator-5556 ./gradlew connectedDebugAndroidTest`: passed, **25 tests on an API 36 Pixel 8 emulator**. The API level was checked directly before running; the other active emulator was left untouched.
- A temporary captured-data check reconstructed **58 stations and 122 platforms** from the real Golemio metadata and parsed **1,605 saved departures**, all with resolved directions. The full JVM run including this check passed **102 tests**. The temporary check was removed afterward; no raw API response or token was committed.
- `git diff --check`: passed.

## Summary

Both Copilot findings are addressed. API 36 validation is complete. Claude's existing changes are retained and pass the combined tests. Changes remain local for review.
