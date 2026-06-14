# Persistent Theme and M3E Settings Plan

## Goal

Implement a real application-wide theme preference with three modes:

- **System**: follow the current Android system light/dark setting;
- **Light**: always use the light color scheme;
- **Dark**: always use the dark color scheme.

The selected mode must apply immediately, survive process death and app restarts, and use the
device's Material You dynamic colors. The Settings screen must also adopt Material 3 Expressive
segmented list items instead of its current flat `ListItem` rows.

## Current Repository State

- `MetroPragueTheme` already calls `dynamicLightColorScheme` and `dynamicDarkColorScheme`, but its
  `darkTheme` value is still derived from the system because the Settings preference is not wired
  to the app root.
- `ThemePreference` currently lives in
  `ui/screens/settings/SettingsUiState.kt` and is only held in the in-memory
  `SettingsViewModel`.
- Recreating `SettingsViewModel` resets the selected theme.
- `MainActivity` owns `MetroPragueTheme`, so persistent theme state must be collected above
  `MainScreen`.
- The project currently resolves Material 3 `1.3.2` through Compose BOM `2025.05.01`.
- Material 3 `1.4.0` does not contain `SegmentedListItem`. The M3E API is available in the current
  `1.5.0-alpha21` release and remains an expressive/experimental API.
- M3E exposes `SegmentedListItem`, `ListItemDefaults.segmentedShapes`, and
  `ListItemDefaults.SegmentedGap`; it does not expose a `SegmentedList` container composable.
- The app currently uses a manual `AppContainer`, although repository rules require Hilt for new
  dependency injection work.
- `minSdk` is 36, so every supported device has Android dynamic-color support.

## Product Decisions

- Preserve `System` as the default preference.
- Persist only the explicit preference enum, not the resolved dark/light Boolean.
- Store a stable lowercase string value: `system`, `light`, or `dark`.
- Invalid or missing persisted values fall back to `System`.
- Apply theme changes immediately without manually recreating the activity.
- Always use Material You dynamic colors in production.
- Keep static light/dark schemes only for deterministic previews and tests.
- Keep the existing theme-selection dialog and current Settings information architecture.
- Group Settings rows into three segmented groups:
  1. Appearance: Theme.
  2. Language: Language.
  3. About: Changelog and App version.
- The outer screen remains vertically scrollable. Each section uses a `Column` to lay out its
  segmented items because there is no M3E `SegmentedList` container.

## Out of Scope

- Persisting or applying the language preference.
- Changing app locale APIs.
- Adding a user-facing switch to disable Material You colors.
- Supporting Android versions below the current `minSdk` 36.
- Redesigning navigation, home, or search UI.
- Replacing dynamic colors with a custom branded palette.
- Broad design-system or spacing-token refactors.

## Target Architecture

```text
Preferences DataStore
        |
        v
UserPreferencesRepository
        |
        +--------------------+
        |                    |
        v                    v
AppThemeViewModel      SettingsViewModel
        |                    |
        v                    v
MainActivity           SettingsScreenRoute
        |
        v
MetroPragueTheme
        |
        v
MainScreen / NavHost
```

`ThemePreference` belongs in a shared preferences/domain package rather than the Settings UI
package because both the application root and Settings feature consume it.

## Task Dependency Order

1. Task 1 upgrades dependencies and establishes Hilt.
2. Task 2 adds the persisted theme preference repository.
3. Task 3 applies repository state at the application theme root.
4. Task 4 connects `SettingsViewModel` to the repository.
5. Task 5 migrates Settings rows to M3E segmented list items.
6. Task 6 updates unit and Compose UI tests.
7. Task 7 performs final verification and cleanup.

Tasks should be assigned sequentially. Tasks 3 and 5 touch mostly independent UI files, but both
depend on the dependency and preference contracts established by Tasks 1 and 2. Do not ask
separate models to edit the same worktree concurrently.

---

## Task 1: Upgrade Material 3 and Establish Hilt

### Sonnet Assignment

> Upgrade this Android app's dependencies so it can use Material 3 Expressive
> `SegmentedListItem`, Preferences DataStore, and Hilt. Follow
> `.agents/plans/dark-mode-dynamic-color-segmented-settings-plan.md`, Task 1 only. Inspect the
> existing Gradle configuration and current manual DI before editing. Add all versions and aliases
> through `gradle/libs.versions.toml`. Pin Material 3 to `1.5.0-alpha21` rather than assuming the
> existing BOM supplies the API. Configure Hilt for the application and migrate the existing
> `StationRepository` binding and `SearchViewModel` construction away from `AppContainer`. Keep
> behavior unchanged. Run a debug compile or `assembleDebug` and report exactly what changed.

### Objective

Provide the dependency and injection foundation required by all later tasks without changing
theme behavior yet.

### Files

- Update `gradle/libs.versions.toml`.
- Update the root `build.gradle.kts` if required for the Hilt plugin declaration.
- Update `app/build.gradle.kts`.
- Update `app/src/main/java/dev/pukan/metroprague/MetroPragueApplication.kt`.
- Update `app/src/main/java/dev/pukan/metroprague/di/AppContainer.kt`, or replace it with focused
  Hilt modules.
- Update `app/src/main/java/dev/pukan/metroprague/ui/screens/search/SearchViewModel.kt`.
- Update call sites affected by the Search ViewModel factory removal.

### Work

- Add Preferences DataStore.
- Add Hilt Android, Hilt compiler/KSP, and Hilt Navigation Compose dependencies as required by the
  project's Kotlin and AGP versions.
- Configure the Hilt Gradle plugin and annotation processor through the version catalog.
- Pin `androidx.compose.material3:material3` to `1.5.0-alpha21`.
  - Do not upgrade the entire Compose BOM merely to obtain this one alpha artifact.
  - Confirm dependency resolution selects the pinned Material 3 version.
- Annotate `MetroPragueApplication` with `@HiltAndroidApp`.
- Annotate `MainActivity` with `@AndroidEntryPoint`.
- Bind `StationRepository` to `MockStationRepository` in a singleton Hilt module.
- Convert `SearchViewModel` to `@HiltViewModel` constructor injection.
- Replace its custom `ViewModelProvider.Factory` usage with `hiltViewModel()` at the route boundary.
- Remove `AppContainer` and its application property only after all usages are migrated.
- Do not add theme persistence or change Settings behavior in this task.

### Acceptance Criteria

- Material 3 resolves to `1.5.0-alpha21`.
- `SegmentedListItem` is available to compile in later tasks.
- DataStore and Hilt dependencies resolve.
- The application starts through Hilt.
- Search still receives the same `MockStationRepository`.
- No manual `AppContainer` or custom Search ViewModel factory remains.
- `./gradlew assembleDebug` succeeds.

---

## Task 2: Add the Persistent Theme Preference Repository

### Sonnet Assignment

> Implement Task 2 from
> `.agents/plans/dark-mode-dynamic-color-segmented-settings-plan.md` only. Create a shared
> `ThemePreference` model and a Hilt-injected Preferences DataStore repository that persists
> `system`, `light`, or `dark`. Move existing Settings references to the shared model without yet
> applying the preference to `MetroPragueTheme` or changing segmented-list UI. Add deterministic
> repository tests, including invalid persisted-value fallback. Run the relevant unit tests.

### Objective

Create one persistent source of truth for the user's theme preference.

### Suggested Files

- Create
  `app/src/main/java/dev/pukan/metroprague/domain/model/ThemePreference.kt`.
- Create
  `app/src/main/java/dev/pukan/metroprague/data/preferences/UserPreferencesRepository.kt`.
- Create
  `app/src/main/java/dev/pukan/metroprague/data/preferences/DataStoreUserPreferencesRepository.kt`.
- Create or update a Hilt module under
  `app/src/main/java/dev/pukan/metroprague/di/`.
- Update
  `app/src/main/java/dev/pukan/metroprague/ui/screens/settings/SettingsUiState.kt`.
- Add tests under `app/src/test/java/dev/pukan/metroprague/data/preferences/`.

Exact filenames may follow an existing pattern discovered during implementation, but preserve the
layer boundaries.

### Work

- Move `ThemePreference` out of the Settings UI package.
- Keep enum values `System`, `Light`, and `Dark`.
- Define a repository interface:

```kotlin
interface UserPreferencesRepository {
    val themePreference: Flow<ThemePreference>
    suspend fun setThemePreference(preference: ThemePreference)
}
```

- Define one application-scoped Preferences DataStore instance.
- Persist a stable string rather than `enum.name`.
- Map `system`, `light`, and `dark` to the enum.
- Map unknown, blank, or absent values to `ThemePreference.System`.
- Bind the implementation through Hilt as a singleton.
- Update Settings imports to use the shared model while preserving current in-memory behavior.
- Do not expose DataStore types outside the data layer.
- Do not swallow `IOException` silently; emit the default preference for recoverable DataStore read
  failures and rethrow unrelated exceptions.

### Tests

- Missing key emits `System`.
- Stored `system`, `light`, and `dark` values map correctly.
- `setThemePreference(Dark)` persists `dark`.
- An invalid stored value emits `System`.
- A new repository instance reading the same test store restores the saved value.

### Acceptance Criteria

- The repository exposes `Flow<ThemePreference>`.
- Persistence survives repository and ViewModel recreation.
- The UI/domain layers do not import Preferences DataStore.
- Existing Settings code compiles with the relocated enum.
- Repository unit tests pass.

---

## Task 3: Apply Theme Preference at the Application Root

### Sonnet Assignment

> Implement Task 3 from
> `.agents/plans/dark-mode-dynamic-color-segmented-settings-plan.md` only. Add an application-level
> Hilt ViewModel that observes `UserPreferencesRepository.themePreference`, collect it above
> `MetroPragueTheme` in `MainActivity`, resolve System/Light/Dark correctly, and keep production on
> Material You dynamic colors. Do not modify Settings ViewModel persistence or segmented-list UI
> in this task. Add focused unit tests for preference-to-theme resolution and run tests plus
> `assembleDebug`.

### Objective

Make the persistent preference control the entire app theme immediately and consistently.

### Suggested Files

- Create
  `app/src/main/java/dev/pukan/metroprague/ui/theme/AppThemeViewModel.kt`.
- Update `app/src/main/java/dev/pukan/metroprague/MainActivity.kt`.
- Update `app/src/main/java/dev/pukan/metroprague/ui/theme/Theme.kt`.
- Update `app/src/main/java/dev/pukan/metroprague/ui/theme/Color.kt` only if fallback colors are
  renamed or removed.
- Add tests under `app/src/test/java/dev/pukan/metroprague/ui/theme/`.

### Work

- Add an `@HiltViewModel` that exposes the persisted `ThemePreference` as `StateFlow`.
- Give its state an explicit initial value of `System`.
- Collect the ViewModel state lifecycle-aware in `MainActivity`.
- Resolve the effective dark-mode Boolean in Compose:
  - `System` -> `isSystemInDarkTheme()`;
  - `Light` -> `false`;
  - `Dark` -> `true`.
- Pass the resolved value to `MetroPragueTheme`.
- Keep dynamic Material You colors enabled in production.
- Because `minSdk` is 36, production can call dynamic light/dark schemes directly.
- Retain a `dynamicColor: Boolean = true` preview/test escape hatch if existing previews depend on
  deterministic fallback schemes.
- Update system-bar icon appearance to match the effective theme.
- Prefer edge-to-edge/system-bar APIs compatible with the current SDK; do not introduce deprecated
  APIs during cleanup.
- Avoid a visible flash caused by defaulting to Light before DataStore emits. The initial `System`
  value should resolve against the current system theme.

### Tests

- System resolves to the supplied system-dark value.
- Light resolves to false regardless of system state.
- Dark resolves to true regardless of system state.
- Repository emissions are reflected by the app theme state.

### Acceptance Criteria

- Selecting a new repository value can recompose the whole app theme without activity recreation.
- System mode follows runtime configuration changes.
- Forced Light and Dark ignore the system mode.
- Dynamic light and dark color schemes are selected from the effective mode.
- `./gradlew test` and `./gradlew assembleDebug` pass.

---

## Task 4: Persist Theme Changes from Settings

### Sonnet Assignment

> Implement Task 4 from
> `.agents/plans/dark-mode-dynamic-color-segmented-settings-plan.md` only. Convert
> `SettingsViewModel` to Hilt constructor injection, derive its theme field from
> `UserPreferencesRepository.themePreference`, and persist selections. Preserve current language
> and dialog behavior. Update unit tests with a fake repository and coroutine test support. Do not
> change Settings row visuals to segmented items yet.

### Objective

Connect the existing Settings interaction to the same repository observed by the application root.

### Files

- Update
  `app/src/main/java/dev/pukan/metroprague/ui/screens/settings/SettingsViewModel.kt`.
- Update
  `app/src/main/java/dev/pukan/metroprague/ui/screens/settings/SettingsScreen.kt`.
- Update
  `app/src/test/java/dev/pukan/metroprague/ui/screens/settings/SettingsViewModelTest.kt`.
- Add a reusable test fake only if it is used by more than one test class.

### Work

- Annotate `SettingsViewModel` with `@HiltViewModel`.
- Inject `UserPreferencesRepository`.
- Keep transient dialog and mock language state local to the ViewModel.
- Combine the repository theme flow with transient Settings state to produce one immutable
  `SettingsUiState`.
- Expose it as `StateFlow` using `stateIn` and `SharingStarted.WhileSubscribed`.
- Persist selection from `selectTheme()` in `viewModelScope`.
- Close the theme dialog immediately when a valid option is selected.
- Do not optimistically maintain a second long-lived theme value that can diverge from DataStore.
- Obtain the ViewModel with `hiltViewModel()` in `SettingsScreenRoute`.
- Keep `SettingsScreen` stateless and previewable.
- Leave language behavior in memory and clearly separate it from persisted theme behavior.

### Tests

- Initial state reflects the fake repository's initial preference.
- Selecting Dark writes Dark to the repository and closes the dialog.
- A repository emission updates `SettingsUiState.theme`.
- Recreating the ViewModel with the same fake repository restores Dark.
- Dialog dismissal does not write a preference.
- Existing language and changelog behavior remains unchanged.

### Acceptance Criteria

- Settings and the application root observe one source of truth.
- A selection persists and applies immediately.
- A new ViewModel restores the saved selection.
- No DataStore implementation details enter the ViewModel.
- Updated unit tests pass without sleeps.

---

## Task 5: Migrate Settings Rows to M3E Segmented List Items

### Sonnet Assignment

> Implement Task 5 from
> `.agents/plans/dark-mode-dynamic-color-segmented-settings-plan.md` only. Replace the current
> flat Settings `ListItem` rows with Material 3 Expressive `SegmentedListItem` groups. Verify the
> actual `1.5.0-alpha21` API from downloaded sources before coding. Use
> `ListItemDefaults.segmentedShapes(index, count)` and `ListItemDefaults.SegmentedGap`; do not
> invent a nonexistent `SegmentedList` composable. Keep components stateless, accessible, and
> previewable. Do not alter persistence architecture.

### Objective

Adopt Material 3 Expressive segmented list styling while preserving existing behavior and
accessibility.

### Files

- Update
  `app/src/main/java/dev/pukan/metroprague/ui/screens/settings/SettingsComponents.kt`.
- Update
  `app/src/main/java/dev/pukan/metroprague/ui/screens/settings/SettingsScreen.kt`.
- Update `app/src/main/res/values/strings.xml` only if accessibility labels are needed.

### Work

- Verify the exact Material 3 `1.5.0-alpha21` signatures before implementation.
- Add a reusable stateless settings group component that:
  - accepts a `modifier`;
  - arranges items vertically;
  - uses `ListItemDefaults.SegmentedGap`;
  - supplies `ListItemDefaults.segmentedShapes(index, count)` to each item.
- Represent item data with a small immutable UI model or a scoped item-building API. Do not pass a
  ViewModel into the group.
- Replace clickable rows with the general click overload of `SegmentedListItem`.
- Replace the app-version row with the non-interactive overload.
- Preserve leading icons, headline text, optional supporting text, and trailing navigation
  affordance.
- Use `Modifier.fillMaxWidth()` and preserve minimum touch target behavior.
- Apply `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` at the narrowest practical component or
  file scope required by the resolved API.
- Build these groups:
  - Appearance group with one Theme item.
  - Language group with one Language item.
  - About group with Changelog and App version items.
- Keep section headings outside the segmented containers.
- Keep the screen's outer `verticalScroll`; replacing every `Column` is neither possible nor
  desirable because segmented items still require a layout parent.
- Use `MaterialTheme` tokens and M3E defaults; do not hardcode container colors or shapes.
- Update previews for:
  - a one-item segmented group;
  - a two-item segmented group;
  - full Settings in light mode;
  - full Settings in dark mode.

### Accessibility Requirements

- Decorative icons use `contentDescription = null`.
- Each clickable item exposes one click action for the full item.
- The version item is not announced as clickable.
- Text remains readable at increased font scale.
- Touch targets remain at least 48 dp.

### Acceptance Criteria

- No Settings row uses the old flat `ListItem` implementation.
- One-item groups receive the standalone shape.
- The About group's first and last items receive the correct segmented shapes.
- Group gaps come from `ListItemDefaults.SegmentedGap`.
- Theme, language, and changelog interactions are unchanged.
- Previews and `assembleDebug` compile with the expressive API.

---

## Task 6: Complete Unit and Compose UI Coverage

### Sonnet Assignment

> Implement Task 6 from
> `.agents/plans/dark-mode-dynamic-color-segmented-settings-plan.md` only. Audit existing tests and
> add focused coverage for persisted theme behavior, app-level theme resolution, and M3E Settings
> interactions/semantics. Prefer JVM tests for repository and ViewModel logic. Use Compose tests
> only for visible rows, clicks, selection summaries, and non-clickable version semantics. Do not
> redesign production code beyond small testability fixes. Run all available tests and report any
> device-dependent suite that could not run.

### Objective

Protect persistence, theme resolution, and the segmented Settings UI against regression.

### Files

- Update or add JVM tests under `app/src/test/java/dev/pukan/metroprague/`.
- Add or update Compose tests under
  `app/src/androidTest/java/dev/pukan/metroprague/ui/screens/settings/`.
- Update navigation UI tests if their Settings assertions depend on old row structure.

### Required Coverage

- Repository defaults to System.
- Repository persists and restores all three preferences.
- Invalid persisted values fall back to System.
- System/Light/Dark resolve to the correct effective dark mode.
- `SettingsViewModel` observes repository changes.
- Selecting a theme writes the repository and dismisses the dialog.
- A reconstructed ViewModel restores the persisted value.
- Settings displays Appearance, Language, and About groups.
- Theme row opens all three options.
- Selecting Dark updates the visible summary.
- Changelog remains clickable.
- Version remains visible and non-clickable.
- Segmented items expose expected click semantics without duplicate child click targets.

### Testing Rules

- Use coroutine test dispatchers or `runTest` for asynchronous ViewModel/repository tests.
- Use a fake repository rather than mocking Flow internals.
- Do not use sleeps.
- Do not require wallpaper colors to have specific RGB values.
- Do not assert private shape implementation details through pixels; validate structure and
  semantics, leaving exact M3 rendering to the library.

### Acceptance Criteria

- `./gradlew test` passes.
- `./gradlew connectedAndroidTest` passes when an API 36 device/emulator is available.
- Lack of a connected device is reported explicitly.
- Tests cover at least one happy path and one invalid/edge path for persistence.

---

## Task 7: Final Verification and Scope Audit

### Sonnet Assignment

> Perform Task 7 from
> `.agents/plans/dark-mode-dynamic-color-segmented-settings-plan.md`. Review the complete feature
> against every acceptance criterion. Run dependency insight for Material 3, `test`,
> `assembleDebug`, and instrumentation tests if a device is present. Fix only defects required by
> this plan. Inspect the final diff for stale manual DI, duplicate theme state, hardcoded colors or
> strings, deprecated APIs, and accidental language persistence. Summarize verification results
> and any residual experimental-API risk.

### Objective

Confirm that the integrated feature is buildable, persistent, visually coherent, and limited to
the requested scope.

### Verification Commands

```bash
./gradlew dependencyInsight \
  --dependency androidx.compose.material3:material3 \
  --configuration debugRuntimeClasspath
./gradlew test
./gradlew assembleDebug
./gradlew connectedAndroidTest
```

Run `connectedAndroidTest` only when an API 36 device or emulator is available.

### Manual Verification Matrix

1. Start with no stored preference:
   - app follows the current system theme;
   - Settings summary shows System default.
2. Select Dark:
   - dialog closes;
   - the entire app changes immediately;
   - Settings summary shows Dark;
   - the choice survives process termination and relaunch.
3. Select Light:
   - the entire app changes immediately;
   - the choice survives process termination and relaunch.
4. Select System:
   - app follows system dark/light changes;
   - the choice survives relaunch.
5. Change wallpaper/system palette:
   - app uses the corresponding Material You colors.
6. Inspect Settings:
   - section groups have M3E segmented shapes and gaps;
   - About has correctly grouped Changelog and version items;
   - version is not clickable;
   - dialogs and navigation remain functional.

### Scope Audit

- No persisted language behavior was added.
- No XML layouts, Fragments, or ViewBinding were introduced.
- No hardcoded user-visible strings were added.
- No static color overrides prevent dynamic Material You colors.
- No second theme preference source exists in Settings.
- No manual `AppContainer` or ViewModel factory remains after Hilt migration.
- No unrelated package, navigation, or feature refactor was included.
- Existing unrelated worktree changes were not reverted.

### Acceptance Criteria

- Material 3 resolves to the intended M3E-capable version.
- `./gradlew test` and `./gradlew assembleDebug` pass.
- Instrumentation status is recorded.
- All three theme modes work and persist.
- Dynamic color works in forced Light, forced Dark, and System modes.
- Settings uses M3E segmented list items without accessibility regressions.

## Definition of Done

- Theme preference is persisted through Preferences DataStore.
- System, Light, and Dark affect the complete application immediately.
- System mode tracks Android configuration changes.
- Production colors come from Material You dynamic light/dark schemes.
- Settings and the app root observe the same repository state.
- Settings rows use M3E `SegmentedListItem` with correct grouped shapes and gaps.
- Hilt owns repositories and ViewModels touched by the feature.
- Unit tests cover persistence, restoration, invalid values, and theme resolution.
- Compose tests cover the user-visible Settings flow and semantics.
- Required build and test quality gates pass.

## Implementation Risks

- `SegmentedListItem` is supplied by an alpha Material 3 release and its API can change in future
  upgrades. Keep its usage isolated in `SettingsComponents.kt`.
- Pinning Material 3 above the BOM-managed version can expose compatibility issues. Dependency
  insight and a full debug build are mandatory.
- Hilt setup is broader than the visual feature because the repository rules prohibit adding a
  parallel manual injection path. Complete the existing Search migration before removing
  `AppContainer`.
- DataStore emits asynchronously. Use System as the initial state so startup follows the current
  system theme instead of flashing a forced light or dark scheme.
