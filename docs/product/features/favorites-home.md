# Favorite station directions on Home

## Summary

Users can save individual directions from the station picker and see those station-direction pairs on Home.

## Must match

1. A favorite is identified by station ID, line, and terminus station ID; display names are resolved from the current station catalogue. (`android/app/src/main/java/dev/pukan/metroprague/domain/model/FavoriteKey.kt`, `android/app/src/main/java/dev/pukan/metroprague/ui/screens/home/HomeViewModel.kt`)
2. Favorites retain insertion order. Adding an existing key is a no-op; adding a new key appends it; removing or toggling removes only the matching key. (`android/app/src/main/java/dev/pukan/metroprague/data/favorites/DataStoreFavoritesRepository.kt`, `android/app/src/test/java/dev/pukan/metroprague/data/favorites/DataStoreFavoritesRepositoryTest.kt`)
3. Persisted favorites use the `favorites_v1` preference key and a version-1 JSON record. Missing, blank, or malformed data resolves to an empty list, and entries with an unknown line are skipped. (`android/app/src/main/java/dev/pukan/metroprague/data/favorites/DataStoreFavoritesRepository.kt`, `android/app/src/test/java/dev/pukan/metroprague/data/favorites/DataStoreFavoritesRepositoryTest.kt`)
4. Home shows each resolvable favorite's station, line, direction, and next-departure display. A favorite that no longer resolves to a station and direction is omitted from display. (`android/app/src/main/java/dev/pukan/metroprague/ui/screens/home/HomeViewModel.kt`, `android/app/src/main/java/dev/pukan/metroprague/ui/screens/home/HomeScreen.kt`, `android/app/src/test/java/dev/pukan/metroprague/ui/screens/home/HomeViewModelTest.kt`)
5. Removing a favorite from Home removes it without a confirmation step. When there are no resolvable favorites, Home shows an empty state with an action to open Search. (`android/app/src/main/java/dev/pukan/metroprague/ui/screens/home/HomeScreen.kt`, `android/app/src/androidTest/java/dev/pukan/metroprague/ui/screens/home/HomeScreenTest.kt`)
6. The Home favorite action has an accessible remove label, and the picker action communicates whether it will add or remove the favorite. (`android/app/src/main/java/dev/pukan/metroprague/ui/screens/home/HomeScreen.kt`, `android/app/src/main/java/dev/pukan/metroprague/ui/components/DirectionRow.kt`, `android/app/src/main/res/values/strings.xml`)

## Platform-native

Each platform may adapt the Home layout, card/list components, iconography, and navigation while preserving favorite identity, ordering, displayed values, empty-state action, and accessible remove/add meaning.

## Android reference

- ViewModel: `android/app/src/main/java/dev/pukan/metroprague/ui/screens/home/HomeViewModel.kt`
- Repository: `android/app/src/main/java/dev/pukan/metroprague/data/favorites/DataStoreFavoritesRepository.kt`
- UI/model: `android/app/src/main/java/dev/pukan/metroprague/ui/screens/home/HomeScreen.kt`, `android/app/src/main/java/dev/pukan/metroprague/domain/model/FavoriteKey.kt`
- Tests: `android/app/src/test/java/dev/pukan/metroprague/ui/screens/home/HomeViewModelTest.kt`, `android/app/src/test/java/dev/pukan/metroprague/data/favorites/DataStoreFavoritesRepositoryTest.kt`, `android/app/src/androidTest/java/dev/pukan/metroprague/ui/screens/home/HomeScreenTest.kt`

## iOS reference

TBD

## Open questions

None identified in the current implementation review.
