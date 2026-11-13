# Product decisions

Dates below come from the cited source text or, where noted, the Git creation date of that source document. Platform-specific implementation choices do not require the other platform to ship in lockstep.

| Date | Decision | Source | Platforms affected |
| --- | --- | --- | --- |
| 2026-09-26 | First release scope is Prague metro lines A, B, and C only; do not include trams in this phase. | `docs/api/golemio.md` (owner decision dated 2026-09-26) | Android, iOS |
| 2026-09-26 | Prefer stable Golemio stop IDs over exact-name matching for departure requests; a station may map to multiple platform stops. | `docs/api/golemio.md` (owner/API research dated 2026-09-26) | Android, iOS |
| 2026-09-26 | Direct Golemio access was accepted for a first working local integration, with backend use and public distribution left for a later operations/product decision. | `docs/api/golemio.md` (owner decision dated 2026-09-26) | Android |
| 2026-09-25 | A saved favorite represents one station and one direction, with the destination derived from the line's termini. | `docs/ai/plans/favorite-station-directions-plan.md` (Git creation date) | Android, iOS |
| 2026-09-25 | Persist favorites by stable station, line, and terminus IDs, preserving user insertion order. | `docs/ai/plans/favorite-station-directions-plan.md` (Git creation date) | Android, iOS |
| 2026-06-15 | Theme options are System, Light, and Dark; System follows device appearance, while Light and Dark force their modes. | `docs/ai/plans/dark-mode-dynamic-color-segmented-settings-plan.md` (Git creation date) | Android, iOS |
| 2026-10-14 | The live metro catalogue covers 58 unique stations across A/B/C and uses downloaded trip patterns plus a curated fallback for known platforms omitted from current patterns. | `docs/api/golemio.md` (implementation research dated 2026-10-14); `android/app/src/main/java/dev/pukan/metroprague/data/golemio/MetroPlatformFallbacks.kt` | Android, iOS |
| 2026-10-14 | Live reload is manual from Settings; saved data is local, replacement is atomic, and old complete data is retained on failure. | `docs/api/golemio.md` (implementation research dated 2026-10-14); `android/app/src/main/java/dev/pukan/metroprague/data/repository/GolemioMetroDataRepository.kt` | Android, iOS |
| 2026-10-14 | Departure snapshots become stale after five minutes, and the API client must pace requests and handle HTTP 429. | `docs/api/golemio.md` (implementation research dated 2026-10-14); `android/app/src/main/java/dev/pukan/metroprague/data/golemio/GolemioBoardService.kt` | Android, iOS |
| 2026-11-13 | On iOS, the Search tab uses the system search tab role and sits last in the tab bar (Home, Settings, Search); the system controls its placement and styling. | `docs/ai/plans/ios-scaffold-plan.md` | iOS |
| 2026-11-13 | Stale departure snapshots (five minutes or older) show an inline “Reload departures in Settings” state; no prompt or dialog. | Owner decision during the monorepo migration; `docs/product/features/metro-data-sync.md` | Android, iOS |
