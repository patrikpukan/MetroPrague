# Repository Guidelines

## Project Structure & Module Organization

- Single Android app module in `app/` using Gradle Kotlin DSL (`build.gradle.kts`,
  `settings.gradle.kts`, `gradle/libs.versions.toml` for version catalog).
- Source sets live under `app/src/main` with `AndroidManifest.xml` and `res/` for UI assets; Kotlin
  sources will live in `app/src/main/java/tech/pukan/metroidprg`.
- Tests: JVM unit tests in `app/src/test/java/tech/pukan/metroidprg`, instrumentation tests in
  `app/src/androidTest/java/tech/pukan/metroidprg`.
- Generated outputs land in `app/build/`; keep this directory untracked. Keep local SDK paths and
  secrets in `local.properties` (already gitignored).

## Build, Test, and Development Commands

- Sync/build in Android Studio, or from CLI:
    - `./gradlew assembleDebug` — compile the debug APK.
    - `./gradlew test` — run JVM unit tests.
    - `./gradlew connectedAndroidTest` — run instrumentation tests on an attached/emulated device (
      API 36 to match `minSdk`/`targetSdk`).
    - `./gradlew clean` — remove build outputs if you hit cache issues.
- Use the latest Android Studio/SDK tools; the project targets API level 36.

## Coding Style & Naming Conventions

- Language: Kotlin with planned Jetpack Compose UI with Material 3 Expressive; prefer idiomatic
  Kotlin (expression-based code, nullable-safety, scoped functions when clear).
- Formatting: 4-space indent, trailing commas where Kotlin allows for cleaner diffs, and organized
  imports. Align with Android/Kotlin style defaults.
- Names: `PascalCase` for classes/objects, `camelCase` for functions/vars, `UPPER_SNAKE_CASE` for
  const vals. Resource files stick to `snake_case`.
- Packages stay under `tech.pukan.metroidprg`; avoid creating parallel namespaces. Add dependencies
  through `gradle/libs.versions.toml` so versions remain centralized.
- UI approach: Compose-first. Do not introduce XML layouts, ViewBinding, Fragments, or
  `findViewById` unless explicitly requested.
- Composable naming: use `PascalCase` screen/component names (for example, `HomeScreen`,
  `TimetableCard`), with one top-level composable per file where practical.
- ViewModel naming: suffix with `ViewModel` and scope to a specific screen/feature.
- User-visible strings: keep in `res/values/strings.xml`; avoid hardcoded UI text in composables.

## Architecture & UI Patterns

- Use Navigation Compose with type-safe routes via `Kotlinx.serialization`. Define destinations as
  data object (for screens without arguments) or data class (for screens with arguments), entirely
  avoiding string-based routing.
- Use MVVM with unidirectional data flow: hoist state to ViewModel or higher-level composables, and
  pass events downward.
- For async/state: prefer Kotlin Coroutines + Flow, expose `StateFlow` from ViewModels, and collect
  with lifecycle-aware APIs in Compose.
- Use `Scaffold` + Material 3 Expressive components for screen structure when appropriate.
- Keep composables small and reusable; use `LazyColumn`/`LazyRow` with stable keys for dynamic
  lists.
- Use `Hilt` for Dependency Injection. Do not manually pass repository instances down the Composable
  tree; instead, inject them into ViewModels and scope them appropriately.

## Testing Guidelines

- Prefer small, deterministic tests. Place JVM tests alongside the code they cover in
  `app/src/test`, using the `*Test.kt` suffix.
- Instrumentation/UI tests go in `app/src/androidTest` using `AndroidJUnit4`; annotate slow or
  device-only cases clearly.
- When adding features, include at least one unit test and expand instrumentation coverage for
  user-facing flows.
- Compose UI tests should use `ui-test-junit4` semantics matchers and avoid sleep-based
  synchronization.
- Cover at least one happy path and one edge case when adding new logic.

## Configuration & Security Notes

- Do not commit secrets or API keys; keep them in `local.properties` or Gradle properties and
  reference via buildConfig or DI.
- Check compatibility with API level 36 devices/emulators before merging; align new libraries with
  the version catalog to avoid drift.
- Keep AGP/Kotlin/JDK/API targets as currently configured unless explicitly requested to change
  them.
- Do not add new repositories; keep dependency resolution through existing `google()` and
  `mavenCentral()`.

## Accessibility & UX

- Provide meaningful `contentDescription` values for non-decorative icons/images.
- Respect minimum touch target sizing in composables.
- Prefer previews with representative fake data for new reusable UI components.

## Quality Gates

- Ensure `./gradlew assembleDebug` succeeds for code changes.
- Keep lint warnings minimal and avoid introducing deprecated APIs.

## Jetpack Compose Expert

For all Jetpack Compose tasks, follow the workflow and checklists in
`.agents/skills/compose-expert/SKILL.md`.

Before answering any Compose question, consult the relevant reference:

- State management -> `.agents/skills/compose-expert/references/state-management.md`
- Performance -> `.agents/skills/compose-expert/references/performance.md`
- Navigation -> `.agents/skills/compose-expert/references/navigation.md`
- (see SKILL.md for the full topic -> file mapping)

For implementation details, check actual source code in
`.agents/skills/compose-expert/references/source-code/`.
