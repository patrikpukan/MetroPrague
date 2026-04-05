## Goal
Implement the main Jetpack Compose UI with three tabs (Home, Search, Settings) accessible via a bottom navigation bar, and their corresponding initial screens.

## Approach
The application will be built using Jetpack Compose and Material 3 guidelines. We will implement a `Scaffold` with a `NavigationBar` at the bottom and a `NavHost` to handle routing between the Home, Search, and Settings screens. Since the project currently lacks the necessary Gradle configuration for Kotlin and Compose, we will first update `libs.versions.toml`, the root `build.gradle.kts`, and the app-level `build.gradle.kts` to enable Jetpack Compose, including the modern Kotlin Compose compiler plugin. We will establish a basic Material 3 theme and scaffold the three primary screens with placeholder content matching the requested features.

## File Changes
- **Modify** `gradle/libs.versions.toml`: Add Kotlin plugin, Compose compiler plugin, Compose BOM, Navigation Compose, and other Compose UI libraries.
- **Modify** `build.gradle.kts`: Add Kotlin and Compose compiler plugins to the root project.
- **Modify** `app/build.gradle.kts`: Apply Kotlin and Compose compiler plugins, enable `buildFeatures { compose = true }`, and add all necessary Compose dependencies.
- **Create** `app/src/main/java/dev/pukan/metroprague/ui/theme/Color.kt`: Define basic Material 3 color placeholders.
- **Create** `app/src/main/java/dev/pukan/metroprague/ui/theme/Theme.kt`: Define `MetroPragueTheme` based on Material 3.
- **Create** `app/src/main/java/dev/pukan/metroprague/ui/navigation/Screen.kt`: Define sealed class `Screen` with routes for Home, Search, and Settings, including their labels and icons.
- **Create** `app/src/main/java/dev/pukan/metroprague/ui/navigation/BottomNavigationBar.kt`: Create the `NavigationBar` composable that iterates over the `Screen` items.
- **Create** `app/src/main/java/dev/pukan/metroprague/ui/screens/home/HomeScreen.kt`: Create Home screen composable with placeholder list for favorited stations and next departures.
- **Create** `app/src/main/java/dev/pukan/metroprague/ui/screens/search/SearchScreen.kt`: Create Search screen composable with placeholder search bar, filters, and station list.
- **Create** `app/src/main/java/dev/pukan/metroprague/ui/screens/settings/SettingsScreen.kt`: Create Settings screen composable with placeholder settings items.
- **Create** `app/src/main/java/dev/pukan/metroprague/ui/screens/MainScreen.kt`: Create `MainScreen` holding `Scaffold` and `NavHost` to connect the bottom bar to the screens.
- **Modify** `app/src/main/java/dev/pukan/metroprague/MainActivity.kt`: Update `setContent` to use `MetroPragueTheme` and call `MainScreen`.

## Implementation Steps
1. **Task 1: Gradle Setup**
   Update `gradle/libs.versions.toml`, `build.gradle.kts`, and `app/build.gradle.kts` to fully support Kotlin (e.g., version 2.0.0) and Jetpack Compose (using Compose BOM and Navigation Compose).
2. **Task 2: Theming**
   Create `Color.kt` and `Theme.kt` in `dev.pukan.metroprague.ui.theme` to establish a basic Material 3 theme for the application.
3. **Task 3: Navigation Definition**
   Create `Screen.kt` in `dev.pukan.metroprague.ui.navigation` to define navigation routes (Home, Search, Settings) and associated icons (using `androidx.compose.material.icons`).
4. **Task 4: Screen Placeholders**
   Create `HomeScreen.kt`, `SearchScreen.kt`, and `SettingsScreen.kt` in `dev.pukan.metroprague.ui.screens` with basic layouts (e.g., `Column`, `Text`) representing the described features.
5. **Task 5: Main Scaffold and Navigation Host**
   Create `BottomNavigationBar.kt` and `MainScreen.kt` in `dev.pukan.metroprague.ui.screens` to assemble the `Scaffold`, `NavigationBar`, and `NavHost`.
6. **Task 6: Main Activity Update**
   Modify `MainActivity.kt` to wrap `MainScreen` inside `MetroPragueTheme` instead of the generic `MaterialTheme`.

## Acceptance Criteria
- `app/build.gradle.kts` correctly syncs with Kotlin and Compose dependencies.
- The app compiles successfully via `./gradlew build`.
- `MainActivity` launches `MainScreen` containing a bottom navigation bar.
- The bottom navigation bar has three items: Home, Search, Settings with distinct icons.
- Tapping each tab successfully navigates to the respective screen without crashing.
- Home screen displays a placeholder text "Home Screen - Favorites".
- Search screen displays a placeholder text "Search Screen - Filters".
- Settings screen displays a placeholder text "Settings".

## Verification Steps
- Run `./gradlew assembleDebug` to ensure it compiles without errors.
- Run the app on an emulator.
- Manually click on the Home, Search, and Settings tabs in the bottom navigation bar to verify the screen content updates accordingly.

## Risks & Mitigations
- **Risk:** Gradle sync failure due to incorrect Compose or Kotlin version compatibility, given the existing `libs.versions.toml` lacks Kotlin definitions.
  **Mitigation:** Explicitly use Kotlin 2.0.0 and the `org.jetbrains.kotlin.plugin.compose` compiler plugin, which is standard for AGP 9.1.0. Ensure the Compose BOM is used to manage other Compose library versions.