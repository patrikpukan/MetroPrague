# Settings Screen Implementation Plan

## Goal

Replace the current placeholder settings screen with a polished, accessible Jetpack Compose
settings experience that demonstrates:

- app version information;
- theme selection: System, Light, and Dark;
- language selection;
- a changelog;
- basic app/about information.

This iteration is mock-only. Selections should update the visible UI during the current process,
but they must not be persisted and must not call platform locale APIs, remote APIs, databases, or
DataStore.

## Repository Context

- Active package/namespace: `dev.pukan.metroprague`.
- Existing destination: `SettingsRoute` in
  `app/src/main/java/dev/pukan/metroprague/ui/navigation/Screen.kt`.
- Existing placeholder:
  `app/src/main/java/dev/pukan/metroprague/ui/screens/settings/SettingsScreen.kt`.
- `MainScreen` already hosts the settings destination inside its existing `Scaffold`; do not add a
  second screen-level `Scaffold`.
- The app theme currently accepts a `darkTheme: Boolean`, but this feature should not change the
  app-wide theme yet. The selected theme is mock state displayed by the settings UI only.
- The project does not currently use Hilt. Do not introduce or migrate dependency injection as part
  of this feature.
- Put every new user-visible string in `app/src/main/res/values/strings.xml`.

## Product Decisions

- Screen title: **Settings**.
- Sections:
  1. **Appearance**: Theme row showing the current selection.
  2. **Language**: Language row showing the current selection.
  3. **About**: Changelog and App version rows.
- Theme choices: **System default**, **Light**, **Dark**.
- Language choices: **System default**, **English**, **Czech**.
- Theme and language rows open single-choice dialogs.
- Choosing an option closes its dialog and immediately updates the row summary.
- Changelog opens a dialog containing representative mock release notes for version `1.0`.
- App version is a non-clickable informational row using mock value `1.0 (1)`.
- Back/dismiss actions close an open dialog without changing the current selection.
- No save button is needed because mock selections apply immediately to the screen state.

## Out of Scope

- Persisting preferences with DataStore or any other storage.
- Applying the selected theme to `MetroPragueTheme`.
- Changing the Android application locale or restarting/recreating the activity.
- Loading changelog content from a file, service, repository, or API.
- Hilt setup or changes to the existing application container.
- New navigation destinations or nested settings screens.
- Changes to Gradle dependencies unless compilation proves an existing required Compose lifecycle
  API is unavailable.

## Task Dependency Order

1. Task 1 defines the state and event contract.
2. Task 2 implements mock state behavior.
3. Task 3 builds reusable settings UI components.
4. Task 4 assembles the complete screen and dialogs.
5. Task 5 integrates the stateful route with existing navigation.
6. Task 6 adds unit tests for state behavior.
7. Task 7 adds Compose UI and navigation tests.
8. Task 8 runs final quality checks and resolves only feature-related failures.

Tasks 3 and 6 may run in parallel after Tasks 1 and 2 are complete. All other tasks should follow
the order above.

---

## Task 1: Define the Settings State Contract

**Objective:** Create stable, explicit models for rendering and interacting with the mock settings
screen.

**Files:**

- Create `app/src/main/java/dev/pukan/metroprague/ui/screens/settings/SettingsUiState.kt`.

**Work:**

- Define a `ThemePreference` enum with `System`, `Light`, and `Dark`.
- Define an `AppLanguage` enum with `System`, `English`, and `Czech`.
- Define a dialog state that can represent no dialog, theme selection, language selection, and
  changelog.
- Define an immutable `SettingsUiState` containing:
  - selected theme;
  - selected language;
  - active dialog;
  - mock version name and version code;
  - mock changelog entries.
- Use stable identifiers or enum values in state; do not store Android resource IDs in the
  ViewModel contract.
- Keep display-label mapping in the UI/resource layer.
- Use defaults matching the product decisions above.

**Acceptance criteria:**

- The state can fully render the screen without reading platform services.
- The state has no mutable collections exposed to the UI.
- The contract contains no navigation controller, `Context`, Compose state, or persistence types.
- Version defaults to `1.0` and `1`; theme and language default to System.

---

## Task 2: Implement the Mock Settings ViewModel

**Objective:** Provide unidirectional, in-memory settings behavior through `StateFlow`.

**Files:**

- Create `app/src/main/java/dev/pukan/metroprague/ui/screens/settings/SettingsViewModel.kt`.

**Work:**

- Add a screen-scoped `SettingsViewModel`.
- Back it with a private `MutableStateFlow<SettingsUiState>`.
- Expose a read-only `StateFlow<SettingsUiState>`.
- Add explicit event methods to:
  - open theme selection;
  - select a theme;
  - open language selection;
  - select a language;
  - open the changelog;
  - dismiss the active dialog.
- Selecting a theme or language must update state and dismiss the dialog atomically.
- Do not add repositories, delays, coroutines, persistence, Android locale APIs, or injected
  dependencies.

**Acceptance criteria:**

- State changes are deterministic and synchronous.
- The ViewModel contains no Compose runtime state.
- Dismissing a dialog preserves selected values.
- Recreating the ViewModel resets all choices to their mock defaults, documenting the intended
  non-persistent behavior.

---

## Task 3: Build Reusable Settings Components

**Objective:** Create stateless Material 3 components that give the screen consistent structure.

**Files:**

- Create `app/src/main/java/dev/pukan/metroprague/ui/screens/settings/SettingsComponents.kt`.
- Update `app/src/main/res/values/strings.xml`.

**Work:**

- Add string resources for the screen title, section titles, row labels, option labels, dialog
  titles/buttons, version formatting, changelog content, and accessibility descriptions.
- Create stateless components for:
  - section heading;
  - clickable settings row with leading icon, title, optional summary, and trailing affordance;
  - non-clickable information row;
  - reusable single-choice dialog.
- Give every public reusable composable a `modifier: Modifier = Modifier` parameter.
- Use Material 3 theme typography, colors, and shapes; do not hardcode light/dark colors.
- Make the entire interactive row clickable, with a minimum 48 dp touch target.
- Treat icons beside visible labels as decorative with `contentDescription = null`.
- Ensure each dialog exposes a clear title, selected state, dismiss action, and radio-button
  semantics.
- Add focused previews using representative mock values, including at least one dark-theme preview.

**Acceptance criteria:**

- Components are stateless and driven only by parameters/callbacks.
- No user-visible text is hardcoded in composables.
- Click behavior is attached once per row, avoiding duplicate child click targets.
- Components remain readable with long translated labels and font scaling.
- Previews compile without requiring a ViewModel or navigation controller.

---

## Task 4: Assemble the Stateless Settings Content and Dialogs

**Objective:** Replace the placeholder with the complete settings layout driven by `SettingsUiState`.

**Files:**

- Replace the contents of
  `app/src/main/java/dev/pukan/metroprague/ui/screens/settings/SettingsScreen.kt`.

**Work:**

- Split the screen into:
  - a stateful route wrapper added in Task 5;
  - a stateless `SettingsScreen` content composable accepting state and event callbacks.
- Use a single vertically scrolling container that respects the padding supplied by `MainScreen`.
  A `Column` with `verticalScroll` is sufficient for this fixed, small set of rows; do not use a
  nested `Scaffold`.
- Render the Appearance, Language, and About sections in the agreed order.
- Show the current theme and language labels as row summaries.
- Render the app version as `1.0 (1)` from state.
- Display exactly one dialog based on the active dialog state:
  - theme single-choice dialog;
  - language single-choice dialog;
  - changelog dialog with mock version `1.0` release notes.
- Use existing Material icons where appropriate without adding image assets.
- Add a screen test tag only if needed for robust Compose tests; prefer semantic text/roles first.
- Add a full-screen preview with representative `SettingsUiState`.

**Acceptance criteria:**

- The placeholder text is completely removed.
- All required sections and rows are visible and scrollable on a compact device.
- Selecting a radio option is possible by tapping its full option row.
- Dialog dismissal does not mutate the selected setting.
- No dialog, row, or screen performs platform or persistence work.
- The UI works in both light and dark Material color schemes.

---

## Task 5: Connect the Stateful Route to Existing Navigation

**Objective:** Collect ViewModel state at the navigation boundary without changing the navigation
graph shape.

**Files:**

- Update `app/src/main/java/dev/pukan/metroprague/ui/screens/settings/SettingsScreen.kt`.
- Update `app/src/main/java/dev/pukan/metroprague/ui/screens/MainScreen.kt` only if the route wrapper
  name changes the existing call site.

**Work:**

- Add a `SettingsRoute` composable wrapper in the settings package, or use an equivalently clear
  route-level name that does not conflict with the serializable navigation destination.
- Obtain the `SettingsViewModel` at the settings navigation entry.
- Collect its `StateFlow` with `collectAsStateWithLifecycle`.
- Pass immutable state and method references into the stateless screen.
- Keep the existing type-safe `composable<SettingsRoute>` destination.
- If the composable wrapper is also named `SettingsRoute`, alias one import to keep the call site
  unambiguous; otherwise prefer a distinct name such as `SettingsScreenRoute`.

**Acceptance criteria:**

- Navigating to the existing Settings bottom-navigation item renders the new UI.
- The ViewModel is scoped to the settings back-stack entry.
- The stateless content can still be previewed and tested without a ViewModel.
- No string-based route is introduced.

---

## Task 6: Add ViewModel Unit Tests

**Objective:** Cover the mock settings state transitions independently of Compose.

**Files:**

- Create
  `app/src/test/java/dev/pukan/metroprague/ui/screens/settings/SettingsViewModelTest.kt`.

**Test cases:**

- Initial state uses System theme, System language, no dialog, and version `1.0 (1)`.
- Opening theme selection sets the correct dialog.
- Selecting Dark updates the theme and closes the dialog.
- Opening language selection sets the correct dialog.
- Selecting Czech updates the language and closes the dialog.
- Opening then dismissing changelog closes it without changing theme or language.
- A fresh ViewModel returns to defaults, proving settings are intentionally not persisted.

**Acceptance criteria:**

- Tests are deterministic and require no Android runtime, dispatcher rule, sleeps, or mocks.
- At least one happy path and one dismissal/edge path are covered.
- `./gradlew test` passes.

---

## Task 7: Add Compose UI and Navigation Tests

**Objective:** Verify the user-visible settings flow and update the existing navigation assertion.

**Files:**

- Create
  `app/src/androidTest/java/dev/pukan/metroprague/ui/screens/settings/SettingsScreenTest.kt`.
- Update
  `app/src/androidTest/java/dev/pukan/metroprague/ui/screens/MainScreenTest.kt`.

**Test cases:**

- Settings content shows Appearance, Language, About, and version `1.0 (1)`.
- Theme row opens the dialog with all three choices.
- Selecting Dark closes the dialog and updates the row summary.
- Language row opens the dialog; selecting Czech updates its summary.
- Changelog row opens mock version `1.0` release notes and dismisses correctly.
- Interactive rows and dialog choices expose expected click/selection semantics.
- Existing bottom-navigation test checks for the new Settings title/content instead of the removed
  placeholder text.

**Testing rules:**

- Render the stateless screen with controlled state for layout assertions.
- Use a small stateful test host, or the real route only where interaction state is required.
- Prefer text, role, selected-state, and click-action matchers over implementation-specific tags.
- Do not use sleeps; rely on Compose test synchronization.

**Acceptance criteria:**

- Tests fail if a required row disappears or a selection no longer updates the visible summary.
- The old `"Settings Screen - Preferences"` assertion is removed.
- Tests do not depend on system language, system theme, network, or persisted state.

---

## Task 8: Final Verification and Scope Audit

**Objective:** Confirm the feature is complete, buildable, accessible, and still mock-only.

**Work:**

1. Run `./gradlew test`.
2. Run `./gradlew assembleDebug`.
3. If an API 36 emulator/device is available, run
   `./gradlew connectedAndroidTest`.
4. Inspect the final diff for:
   - no hardcoded user-visible strings;
   - no XML layouts, Fragments, or ViewBinding;
   - no DataStore, locale APIs, remote APIs, or new repositories;
   - no app-wide theme behavior accidentally wired in;
   - no unrelated package, Gradle, or navigation refactors;
   - no changes to the user-modified `AGENTS.md` or `README.md`.
5. Manually verify light and dark previews, long labels, scrolling, dialog dismissal, and 48 dp
   touch targets.

**Acceptance criteria:**

- `./gradlew test` and `./gradlew assembleDebug` pass.
- Instrumentation results are recorded if a device is available; lack of a device is reported
  rather than hidden.
- The final implementation remains entirely local and mock-driven.
- Any dependency addition is justified by a concrete compile error and made through
  `gradle/libs.versions.toml`.

## Definition of Done

- Settings is a complete Material 3 Compose screen reachable from the current bottom navigation.
- Theme and language selections work as in-memory UI demonstrations.
- Changelog and version information are visible with mock content.
- State follows ViewModel + `StateFlow` unidirectional data flow.
- The content composable is stateless, reusable, previewable, and testable.
- Accessibility semantics and touch targets are covered.
- Unit and Compose UI tests cover happy paths and dismissal/default-state edge cases.
- No real persistence, locale application, theme application, or API integration has been added.
