# Station departures

## Summary

Show the upcoming departures for a selected station and travel direction.

## Must match

1. A direction is selected by station, line, and terminus station ID. Only departures assigned to that line and direction are shown, in ascending effective departure time; predicted time is used when present, otherwise scheduled time. (`android/app/src/main/java/dev/pukan/metroprague/ui/screens/departures/StationDeparturesViewModel.kt`, `android/app/src/main/java/dev/pukan/metroprague/domain/model/Departure.kt`, `android/app/src/main/java/dev/pukan/metroprague/data/golemio/GolemioDepartureBoardParser.kt`, `android/app/src/test/java/dev/pukan/metroprague/ui/screens/departures/StationDeparturesViewModelTest.kt`)
2. The screen shows at most five matching departures. (`android/app/src/main/java/dev/pukan/metroprague/ui/screens/departures/StationDeparturesViewModel.kt`, `android/app/src/test/java/dev/pukan/metroprague/ui/screens/departures/StationDeparturesViewModelTest.kt`)
3. A canceled departure displays “Cancelled”; one marked at-stop displays “At station”; otherwise an effective time less than 60 seconds away displays “Now”, less than 60 minutes away displays the whole-minute value rounded down, and 60 minutes or more displays local hour and minute. (`android/app/src/main/java/dev/pukan/metroprague/ui/model/DepartureDisplay.kt`, `android/app/src/test/java/dev/pukan/metroprague/ui/model/DepartureDisplayTest.kt`)
4. Past departures are omitted, except departures marked at-stop while their snapshot is fresh. An expired board displays the stale state; an unavailable board and an empty matching list use their respective unavailable and no-service states. (`android/app/src/main/java/dev/pukan/metroprague/data/repository/GolemioDepartureRepository.kt`, `android/app/src/main/java/dev/pukan/metroprague/ui/screens/departures/StationDeparturesViewModel.kt`, `android/app/src/test/java/dev/pukan/metroprague/data/repository/GolemioDepartureRepositoryTest.kt`)
5. The back action returns to the prior screen, and departure-time text remains available as text alongside the destination. (`android/app/src/main/java/dev/pukan/metroprague/ui/screens/departures/StationDeparturesScreen.kt`, `android/app/src/androidTest/java/dev/pukan/metroprague/ui/screens/departures/StationDeparturesScreenTest.kt`)

## Platform-native

Each platform may adapt the screen structure, navigation affordance, row/card presentation, typography, and motion. Preserve the selected direction, departure ordering and cutoff rules, time labels, and distinct empty/stale/unavailable states.

iOS shows the departures in a `List` pushed onto the Search tab's `NavigationStack`, with the station name as the inline title, the line badge and "To <terminus>" as the section header, and the system back button. The stale, unavailable, and no-service states use `ContentUnavailableView` with distinct symbols; the stale state adds that snapshots expire after five minutes. Time labels refresh every 15 seconds through `TimelineView`.

## Android reference

- ViewModel: `android/app/src/main/java/dev/pukan/metroprague/ui/screens/departures/StationDeparturesViewModel.kt`
- Repository: `android/app/src/main/java/dev/pukan/metroprague/data/repository/GolemioDepartureRepository.kt`
- Formatting/model: `android/app/src/main/java/dev/pukan/metroprague/ui/model/DepartureDisplay.kt`, `android/app/src/main/java/dev/pukan/metroprague/domain/model/Departure.kt`
- Tests: `android/app/src/test/java/dev/pukan/metroprague/ui/screens/departures/StationDeparturesViewModelTest.kt`, `android/app/src/test/java/dev/pukan/metroprague/data/repository/GolemioDepartureRepositoryTest.kt`, `android/app/src/test/java/dev/pukan/metroprague/ui/model/DepartureDisplayTest.kt`, `android/app/src/androidTest/java/dev/pukan/metroprague/ui/screens/departures/StationDeparturesScreenTest.kt`

## iOS reference

- Model: `ios/MetroPrague/Features/Departures/StationDeparturesModel.swift`
- UI/route: `ios/MetroPrague/Features/Departures/StationDeparturesView.swift`, `DepartureRoute.swift`
- Formatting/model: `ios/MetroPrague/Domain/DepartureDisplay.swift`, `ios/MetroPrague/Shared/DepartureDisplay+Label.swift`, `ios/MetroPrague/Domain/DepartureBoard.swift`
- Tests: `ios/MetroPragueTests/Features/Departures/StationDeparturesModelTests.swift`, `ios/MetroPragueTests/Domain/DepartureDisplayTests.swift`, `ios/MetroPragueTests/Domain/DepartureBoardTests.swift`

## Open questions

None identified in the current implementation review.
