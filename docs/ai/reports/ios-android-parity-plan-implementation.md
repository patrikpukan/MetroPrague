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
