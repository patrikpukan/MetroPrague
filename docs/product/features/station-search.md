# Station search and direction picker

## Summary

Search the metro station catalogue by name, optionally narrow the results to one line, then choose a travel direction at the selected station.

## Must match

1. Search matches station names by case-insensitive substring; an empty query does not exclude any station. Applying a line filter retains stations that serve that line, and selecting the already-selected filter clears it. (`android/app/src/main/java/dev/pukan/metroprague/ui/screens/search/SearchViewModel.kt`, `android/app/src/test/java/dev/pukan/metroprague/ui/screens/search/SearchViewModelTest.kt`)
2. Selecting a station opens its direction picker. Available directions are derived from the station's line memberships and the first and last stations on each line; the selected station itself is not a destination. (`android/app/src/main/java/dev/pukan/metroprague/domain/model/StationDirections.kt`, `android/app/src/main/java/dev/pukan/metroprague/ui/screens/search/SearchViewModel.kt`, `android/app/src/test/java/dev/pukan/metroprague/domain/model/StationDirectionsTest.kt`)
3. Each direction row identifies the line and destination, previews the next matching departure when one is available, and reflects whether that station-and-direction is favorited. Missing departures, stale boards, and unavailable boards have distinct display states. (`android/app/src/main/java/dev/pukan/metroprague/ui/screens/search/SearchViewModel.kt`, `android/app/src/main/java/dev/pukan/metroprague/ui/components/DirectionRow.kt`, `android/app/src/test/java/dev/pukan/metroprague/ui/screens/search/SearchViewModelTest.kt`)
4. Choosing a direction opens departures for that station and direction; dismissing the picker or activating its close control closes it. (`android/app/src/main/java/dev/pukan/metroprague/ui/screens/search/SearchScreen.kt`, `android/app/src/main/java/dev/pukan/metroprague/ui/screens/search/DirectionPickerSheet.kt`, `android/app/src/androidTest/java/dev/pukan/metroprague/ui/screens/search/SearchScreenTest.kt`)
5. The favorite control exposes add or remove content according to current favorite state and toggles only a direction displayed for the selected station. (`android/app/src/main/java/dev/pukan/metroprague/ui/components/DirectionRow.kt`, `android/app/src/main/java/dev/pukan/metroprague/ui/screens/search/SearchViewModel.kt`, `android/app/src/test/java/dev/pukan/metroprague/ui/screens/search/SearchViewModelTest.kt`)

## Platform-native

Each platform may choose its own search field, line-filter controls, station-list presentation, picker surface, navigation, typography, and motion. Keep the filtering, direction derivation, departure preview states, favorite identity, and accessible add/remove meaning aligned.

## Android reference

- ViewModel: `android/app/src/main/java/dev/pukan/metroprague/ui/screens/search/SearchViewModel.kt`
- Repository: `android/app/src/main/java/dev/pukan/metroprague/data/repository/GolemioStationRepository.kt`
- UI: `android/app/src/main/java/dev/pukan/metroprague/ui/screens/search/SearchScreen.kt`, `DirectionPickerSheet.kt`, `android/app/src/main/java/dev/pukan/metroprague/ui/components/DirectionRow.kt`
- Tests: `android/app/src/test/java/dev/pukan/metroprague/ui/screens/search/SearchViewModelTest.kt`, `android/app/src/test/java/dev/pukan/metroprague/domain/model/StationDirectionsTest.kt`, `android/app/src/androidTest/java/dev/pukan/metroprague/ui/screens/search/SearchScreenTest.kt`

## iOS reference

TBD

## Open questions

None identified in the current implementation review.
