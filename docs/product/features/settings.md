# Settings

## Summary

Settings exposes the persisted appearance choice and informational About rows.

## Must match

1. Theme choices are System, Light, and Dark. System follows the current system appearance; Light and Dark force their respective appearance. (`android/app/src/main/java/dev/pukan/metroprague/domain/model/ThemePreference.kt`, `android/app/src/main/java/dev/pukan/metroprague/ui/theme/Theme.kt`, `android/app/src/test/java/dev/pukan/metroprague/ui/theme/ThemeResolutionTest.kt`)
2. The theme choice persists under `theme_preference`; a missing or unrecognized stored value resolves to System. (`android/app/src/main/java/dev/pukan/metroprague/data/preferences/DataStoreUserPreferencesRepository.kt`, `android/app/src/main/java/dev/pukan/metroprague/domain/model/ThemePreference.kt`, `android/app/src/test/java/dev/pukan/metroprague/data/preferences/DataStoreUserPreferencesRepositoryTest.kt`)
3. Choosing a theme closes the selection dialog and updates the value observed by the app. (`android/app/src/main/java/dev/pukan/metroprague/ui/screens/settings/SettingsViewModel.kt`, `android/app/src/main/java/dev/pukan/metroprague/ui/theme/AppThemeViewModel.kt`, `android/app/src/test/java/dev/pukan/metroprague/ui/screens/settings/SettingsViewModelTest.kt`)
4. About contains a Changelog row that opens the embedded release notes and an informational App version row that is not clickable. (`android/app/src/main/java/dev/pukan/metroprague/ui/screens/settings/SettingsScreen.kt`, `android/app/src/main/java/dev/pukan/metroprague/ui/screens/settings/SettingsUiState.kt`, `android/app/src/androidTest/java/dev/pukan/metroprague/ui/screens/settings/SettingsScreenTest.kt`)
5. Interactive About controls expose their click actions, while the version row exposes no click action. (`android/app/src/main/java/dev/pukan/metroprague/ui/screens/settings/SettingsComponents.kt`, `android/app/src/androidTest/java/dev/pukan/metroprague/ui/screens/settings/SettingsScreenTest.kt`)

## Platform-native

Each platform may adapt the settings navigation, selection control, dialog or sheet, About row presentation, typography, and motion. The theme choices, persistence behavior, release-note access, and noninteractive version semantics stay aligned.

iOS uses a `Form`: the theme is a navigation-link `Picker` that returns to Settings on a choice, Changelog is a pushed list, and App version is a non-interactive `LabeledContent`. iOS has no Language row: Android's row is an unpersisted placeholder that the spec does not require, and iOS offers per-app language in the system Settings app. The iOS 1.0 changelog therefore lists "Theme preferences." instead of "Theme and language preferences."

## Android reference

- ViewModel: `android/app/src/main/java/dev/pukan/metroprague/ui/screens/settings/SettingsViewModel.kt`, `android/app/src/main/java/dev/pukan/metroprague/ui/theme/AppThemeViewModel.kt`
- Repository: `android/app/src/main/java/dev/pukan/metroprague/data/preferences/DataStoreUserPreferencesRepository.kt`
- UI/model: `android/app/src/main/java/dev/pukan/metroprague/ui/screens/settings/SettingsScreen.kt`, `SettingsComponents.kt`, `SettingsUiState.kt`, `android/app/src/main/java/dev/pukan/metroprague/domain/model/ThemePreference.kt`
- Tests: `android/app/src/test/java/dev/pukan/metroprague/ui/screens/settings/SettingsViewModelTest.kt`, `android/app/src/test/java/dev/pukan/metroprague/data/preferences/DataStoreUserPreferencesRepositoryTest.kt`, `android/app/src/test/java/dev/pukan/metroprague/ui/theme/ThemeResolutionTest.kt`, `android/app/src/androidTest/java/dev/pukan/metroprague/ui/screens/settings/SettingsScreenTest.kt`

## iOS reference

- Screen: `ios/MetroPrague/Features/Settings/SettingsView.swift`, `ChangelogView.swift`, `AppVersion.swift`, `MetroDataStatus+Text.swift`
- Store: `ios/MetroPrague/Data/AppSettings.swift`, applied with `.preferredColorScheme` in `ios/MetroPrague/App/MetroPragueApp.swift`
- Model: `ios/MetroPrague/Domain/ThemePreference.swift`
- Tests: `ios/MetroPragueTests/Data/AppSettingsTests.swift`, `ios/MetroPragueTests/Domain/ThemePreferenceTests.swift`, `ios/MetroPragueTests/Features/Settings/`

## Open questions

None identified in the current implementation review.
