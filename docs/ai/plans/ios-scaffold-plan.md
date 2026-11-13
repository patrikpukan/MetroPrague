# iOS Scaffold Plan

## Goal

Create the iOS app's foundation in `ios/` so that later feature PRs only add code. This covers
step 1 of [the iOS quickstart](../quickstart-ios.md). When the plan is complete:

- `ios/MetroPrague.xcodeproj` builds a SwiftUI app for iOS 26+ and runs a Swift Testing test
  target, both from the command line with `xcodebuild`.
- The app launches to an empty three-tab shell (Home, Search, Settings) using native SwiftUI tabs.
- A gitignored `ios/Config/Secrets.xcconfig` supplies the Golemio key to Debug builds through
  `Info.plist`. Release builds and builds without the file get an empty key.
- Test code can load the shared `fixtures/` files, which proves the path the data-layer tests will
  use in step 2.
- `ios/AGENTS.md`, `ios/CLAUDE.md`, `ios/README.md`, and `ios/.gitignore` exist.
- `.github/workflows/ios.yml` builds and tests on a macOS runner only when iOS-relevant paths
  change, using the same change-detection pattern as `android.yml`.
- Shared docs say that iOS is scaffolded. No feature row in `docs/product/parity.md` changes status.

No feature behavior (data layer, search, departures, favorites, settings content) is implemented.

## Repository Context

- Monorepo layout and rules: root `AGENTS.md`. Work for this plan stays inside `ios/`, except for
  the shared files listed in Tasks 8 and 9 (`.github/workflows/ios.yml`, root `README.md`, root
  `AGENTS.md`, root `.gitignore`, `docs/product/parity.md`, `docs/ai/quickstart-ios.md`).
- `ios/` does not exist yet. The old local `MetroPrague-ios` folder is gone.
- Decisions already made in `docs/ai/quickstart-ios.md`: fully native SwiftUI, no KMP, iOS 26+
  minimum, Swift Testing, key in a gitignored `.xcconfig` read through `Info.plist`, separate build
  and CI.
- Android reference points (for identity and tab names only; do not copy structure or idioms):
  - Bundle/application ID `dev.pukan.metroprague`, version name `1.0`, version code `1`
    (`android/app/build.gradle.kts`).
  - App name `MetroPrague` (`android/app/src/main/res/values/strings.xml`).
  - Tabs in order Home, Search, Settings (`android/app/src/main/java/dev/pukan/metroprague/ui/navigation/Screen.kt`).
  - Android has only English strings (`values/`); there is no Czech localization.
  - On Android, only debug builds with a nonblank local key use Golemio; release builds get an empty
    key (`docs/product/features/metro-data-sync.md`, *Platform-native*).
- CI reference: `.github/workflows/android.yml`. Its `changes` job compares the PR base (or push
  `before`) with `HEAD` and runs the build only when `^(android/|fixtures/|\.github/workflows/android\.yml$)`
  matches. An invalid, missing, or all-zero base falls back to building.
- Shared fixtures: `fixtures/golemio/departureboards/{metro-predicted-delay,mixed-transport-scheduled-only,empty-board,short-turn}.json`,
  documented in `fixtures/README.md`.
- The repository is public on GitHub (`patrikpukan/MetroPrague`), so macOS runner minutes are free.
- Local toolchain on the owner's machine: Xcode 27.2. `swift-format` ships in the Xcode toolchain.
- Agent tooling available to the builder:
  - the Xcode MCP server (`plugin:xcode-integration:xcode`), which can create projects from Xcode
    templates (`XcodeListTemplates`, `XcodeNewProject`) and build/run them;
  - the T3 device tools (`device_list`, `device_open`, `device_screenshot`, `device_close`) for
    driving the iOS Simulator;
  - the `xcode-integration:swiftui-specialist` and `xcode-integration:swiftui-whats-new-27` skills.
- Command sandbox note: `xcrun`/`xcodebuild` may fail inside an agent's command sandbox with
  `couldn't create cache file ... Operation not permitted`. That is the sandbox, not the project.
  Run those commands outside the sandbox.

## Decisions

### Project creation

- **Create the project with Xcode's own App template, never by hand-writing the project file.**
  The project file is a large, ID-cross-referenced format; a hand-written one is easy to corrupt and
  hard to review. Use the Xcode MCP `XcodeNewProject` tool. If it is unavailable or the owner does
  not approve it, stop and ask the owner to create the project in Xcode (File → New → Project → iOS
  → App) with the settings in Task 2.
- **Folder-synchronized groups (Xcode 16+ default).** Files added to `ios/MetroPrague/` or
  `ios/MetroPragueTests/` on disk join their target automatically, without editing the project
  file. This keeps future agent PRs small and lets you add files from any editor.
- **Xcode 27 JSON project format** (`MetroPrague.xcodeproj/project.xcproj`). *Revised during
  implementation (owner-approved, 2026-11-13):* Xcode 27.2 creates new projects in Apple's
  open-source JSON format instead of `project.pbxproj`. It is readable, merges cleanly, and is
  meant to be editable by agents, but only Xcode 27+ can open it. This replaces the earlier
  "Xcode 16.0 format for Xcode 26 CI" decision; CI uses Xcode 27 instead (see *CI*).
- **Use the Xcode plugin** (`plugin:xcode-integration:xcode`) for project operations: creating
  files and folders, moving and removing them, build settings, builds, and tests. Edit
  `project.xcproj` by hand only for what the plugin cannot do.
- **Names:** project `MetroPrague`, app target `MetroPrague`, unit test target `MetroPragueTests`,
  one shared scheme `MetroPrague` that builds the app and runs `MetroPragueTests`. **The scheme must
  be committed** (`MetroPrague.xcodeproj/xcshareddata/xcschemes/MetroPrague.xcscheme`). Xcode 27
  only auto-creates a scheme for the JSON format, and that scheme disappeared after a project
  reload, so CI must not depend on it.
- **No UI test target** (XCUITest) yet. It uses XCTest rather than Swift Testing and has nothing to
  test in an empty shell. Add it with the first screen that needs it.

### Identity and platform

- Bundle ID `dev.pukan.metroprague` (same as Android). Test bundle `dev.pukan.metroprague.tests`.
- Display name `MetroPrague`, marketing version `1.0`, build `1` (same as Android).
- Deployment target **iOS 26.0**. iPhone only (`TARGETED_DEVICE_FAMILY = 1`); iPad is a later
  decision. Keep the template's default orientations.
- No `DEVELOPMENT_TEAM` is committed. Simulator builds need no signing. The owner may set
  `DEVELOPMENT_TEAM` in the gitignored `Secrets.xcconfig` for device builds.
- English only. The String Catalog (`Localizable.xcstrings`) has English as source language, and
  SwiftUI string literals are extracted into it at build time.

### Swift language and concurrency settings

- **Swift 6 language mode** (`SWIFT_VERSION = 6.0`) for both targets. It enforces complete data-race
  checking at compile time, so concurrency mistakes show up as build errors instead of rare runtime
  bugs.
- **Default actor isolation `MainActor`** (`SWIFT_DEFAULT_ACTOR_ISOLATION = MainActor`) and
  **Approachable Concurrency** (`SWIFT_APPROACHABLE_CONCURRENCY = YES`) for both targets. These are
  the defaults of Xcode 26+ app templates. In this mode, unannotated code runs on the main actor,
  which is right for views and `@Observable` UI models and removes most `@MainActor` boilerplate.
  Background work is opted into explicitly: the step-2 Golemio client will be an `actor`, and pure
  value types or parsers that must run off the main actor are marked `nonisolated`. Write this rule
  into `ios/AGENTS.md`.
- Warnings are errors in CI only (passed on the `xcodebuild` command line), so local
  experimentation is not blocked.

### Source layout

```text
ios/
├── MetroPrague.xcodeproj/
├── MetroPrague/                      # app target (synchronized folder)
│   ├── App/
│   │   ├── MetroPragueApp.swift      # @main App
│   │   └── AppConfiguration.swift    # reads Info.plist values such as the Golemio key
│   ├── Navigation/
│   │   ├── AppTab.swift              # tab identity enum
│   │   └── RootTabView.swift         # TabView shell
│   ├── Features/
│   │   ├── Home/HomeView.swift
│   │   ├── Search/SearchView.swift
│   │   └── Settings/SettingsView.swift
│   └── Resources/
│       ├── Assets.xcassets
│       └── Localizable.xcstrings
├── MetroPragueTests/                 # Swift Testing target (synchronized folder)
│   ├── AppConfigurationTests.swift
│   ├── FixturesTests.swift
│   └── Support/Fixtures.swift
├── Config/
│   ├── Shared.xcconfig
│   ├── Debug.xcconfig
│   ├── Release.xcconfig
│   ├── Secrets.example.xcconfig      # tracked template
│   ├── Secrets.xcconfig              # gitignored, owner-created
│   └── Info.plist                    # only custom keys; merged with the generated plist
├── scripts/
│   └── simulator-destination.sh
├── .swift-format
├── .gitignore
├── AGENTS.md
├── CLAUDE.md
└── README.md
```

Feature folders are grouped by feature, not by layer, so each spec in step 3 maps to one folder.
Step 2 adds `MetroPrague/Data/`; it is not created now.

### Golemio key configuration

- `Config/Shared.xcconfig` declares `GOLEMIO_API_KEY =` (empty).
- `Config/Debug.xcconfig` includes `Shared.xcconfig`, then optionally includes `Secrets.xcconfig`
  with `#include? "Secrets.xcconfig"`. The `?` makes a missing file not an error, so clean clones
  and CI build without it.
- `Config/Release.xcconfig` includes only `Shared.xcconfig`. A Release build never contains the key,
  matching Android, where release builds get an empty key.
- These files are the **target-level base configurations** of the `MetroPrague` app target (Debug →
  `Debug.xcconfig`, Release → `Release.xcconfig`). The test target has no base configuration.
- `Config/Info.plist` contains one key, `GolemioAPIKey`, with value `$(GOLEMIO_API_KEY)`. The app
  target keeps `GENERATE_INFOPLIST_FILE = YES` and sets `INFOPLIST_FILE = Config/Info.plist`; Xcode
  merges the two. The plist lives in `Config/`, outside the synchronized `MetroPrague/` folder, so
  Xcode does not also copy it into the bundle as a resource.
- `AppConfiguration` reads the key through `Bundle.main.object(forInfoDictionaryKey:)` and exposes
  `golemioAPIKey: String?`, which is `nil` when the value is missing, blank, or still the literal
  unexpanded `$(GOLEMIO_API_KEY)`. Nothing uses it until step 2.
- Like Android, a key compiled into a Debug build can be recovered from the app. Debug builds must
  not be distributed. The direct-vs-proxy decision for shipping stays open (`docs/api/golemio.md`).

### Tab shell

- Use the iOS 18+ `Tab` API inside `TabView(selection:)`, driven by an `AppTab` enum
  (`home`, `search`, `settings`) held in `@State` in `RootTabView`.
- The Search tab uses `Tab(value: .search, role: .search)`, so the system places and styles it as
  the search tab at the trailing end of the tab bar; the visual order is Home, Settings, then Search.
  (Verified on an iOS 27.2 simulator: with no `.searchable` yet it renders inside the bar as the
  last item.) This is an intentional *Platform-native* divergence from Android's
  Home/Search/Settings bar. The `.searchable` field itself is added in the station-search PR.
- Each tab's root view owns its own `NavigationStack` with a navigation title, so each tab keeps
  its own push history (the departures screen will push inside it later).
- Labels and SF Symbols: Home `house`, Search `magnifyingglass`, Settings `gear`.
- Placeholder bodies use `ContentUnavailableView`, with the copy given in Task 4.
- No custom tab bar, colors, or glass effects. The system tab bar already uses Liquid Glass on
  iOS 26.

### Fixtures in tests

- Tests read fixtures straight from the repository instead of copying them into the test bundle.
  `Fixtures` finds the repository root by walking up from `#filePath` until it finds a directory
  containing `fixtures/README.md`. This works locally and in CI because the simulator reads host
  file paths, and it means there is no second copy of the fixtures to keep in sync.
- The scaffold only checks that every fixture listed in `fixtures/README.md` loads as a JSON object
  with a `departures` array. Parsing and the documented assertions belong to step 2.

### CI

- New workflow `.github/workflows/ios.yml`, named `iOS CI`, triggered like `android.yml` (push and
  PR to `main`).
- The `changes` job copies `android.yml`'s logic verbatim except for the output name (`ios`) and
  the pattern `^(ios/|fixtures/|\.github/workflows/ios\.yml$)`.
- The `build` job runs on `xcode-27` (GitHub's preview image with Xcode 27; the stable `macos-26`
  image only has Xcode 26.x, which cannot open the JSON project). It selects
  `/Applications/Xcode_27.2.app` to match local development, then, in working directory `ios`,
  prints `xcodebuild -version`,
  lints with `swift-format`, picks a simulator with `scripts/simulator-destination.sh`, and runs
  `xcodebuild test` with `CODE_SIGNING_ALLOWED=NO` and `SWIFT_TREAT_WARNINGS_AS_ERRORS=YES`.
- The image is a preview, so queueing or flakiness is possible. Move to a stable image with
  Xcode 27 when GitHub releases one.

### Simulator selection

- `ios/scripts/simulator-destination.sh` prints an `xcodebuild -destination` value. It uses
  `SIMULATOR_UDID` when that is set; otherwise it picks the first available iPhone simulator on the
  newest installed iOS runtime that is 26.0 or later. Local commands and CI both use it, so nobody
  hard-codes a device name that differs between machines.
- `xcrun simctl` is used here only to choose a build destination. Agents drive a running simulator
  (launch, tap, screenshot) through the T3 device tools, not `simctl`.

### Build commands

All commands run from `ios/`. Derived data goes to `ios/build/` (gitignored) so that builds stay
inside the repository and work the same in CI and in sandboxed agent shells.

```sh
DEST="$(scripts/simulator-destination.sh)"
xcodebuild build -project MetroPrague.xcodeproj -scheme MetroPrague -destination "$DEST" -derivedDataPath build
xcodebuild test  -project MetroPrague.xcodeproj -scheme MetroPrague -destination "$DEST" -derivedDataPath build
xcrun swift-format lint --strict --recursive MetroPrague MetroPragueTests
```

### Formatting

- `swift-format` from the Xcode toolchain, with no third-party linter. `ios/.swift-format` sets
  4-space indentation and a 100-column line length, matching the Android and docs line width, and
  keeps every other rule at its default.

## Out of Scope

- Data layer: Golemio client, parsing, catalogue, snapshot store, favorites, preferences (step 2).
- Any feature screen content: search field, direction picker, departures, favorites, theme picker,
  metro-data reload, About rows (step 3).
- Changes to `docs/product/features/*.md`. Their *iOS reference* sections stay `TBD`.
- Changing any feature row in `docs/product/parity.md` away from `not started`.
- UI test target, iPad layout, app icon artwork, launch screen customization, Czech localization.
- App Store, TestFlight, signing, release tags, and the direct-vs-proxy key decision.
- Third-party dependencies, Swift packages, XcodeGen, Tuist, SwiftLint.
- Any change under `android/` or to `.github/workflows/android.yml`.

## Task Dependency Order

1. Task 1: `ios/.gitignore` (before any Xcode output exists).
2. Task 2: create the Xcode project. Depends on 1.
3. Task 3: configuration files and `AppConfiguration`. Depends on 2.
4. Task 4: tab shell. Depends on 2.
5. Task 5: fixture loader and smoke tests. Depends on 2.
6. Task 6: simulator destination script and `.swift-format`. Depends on 2.
7. Task 7: `ios/AGENTS.md`, `ios/CLAUDE.md`, `ios/README.md`. Depends on 3 and 6.
8. Task 8: `.github/workflows/ios.yml`. Depends on 6.
9. Task 9: shared docs updates. Depends on 2.
10. Task 10: final verification. Depends on all.

Tasks 3, 4, 5, and 6 are independent of each other after Task 2. Run them sequentially anyway: they
share one project, and one-at-a-time changes are easier to review and understand.

---

## Task 1: Add `ios/.gitignore`

**Objective:** Keep Xcode user state, build output, and secrets out of Git from the first commit.

**Work:**

- Create `ios/.gitignore` with exactly these entries (paths are relative to `ios/`):

  ```gitignore
  # Xcode per-user state
  xcuserdata/
  *.xcuserstate
  *.xcscmblueprint

  # Build output
  /build/
  DerivedData/
  *.xcresult

  # Local secrets (see Config/Secrets.example.xcconfig)
  /Config/Secrets.xcconfig

  .DS_Store
  ```

- Do not ignore `*.xcodeproj/project.xcworkspace/` or `xcshareddata/`; the shared scheme lives there.

**Acceptance Criteria:**

- `git check-ignore ios/Config/Secrets.xcconfig ios/build/x ios/MetroPrague.xcodeproj/xcuserdata/a.xcuserdatad`
  reports all three paths.
- `git check-ignore ios/MetroPrague.xcodeproj/xcshareddata/xcschemes/MetroPrague.xcscheme` reports
  nothing (the exit code is 1).

---

## Task 2: Create the Xcode Project

**Objective:** A buildable, testable Xcode project in `ios/` with the identity and settings from
*Decisions*.

**Work:**

1. Use `XcodeListTemplates` (`kind: "project"`, `platformFilter: ["ios"]`) to find the iOS **App**
   template and its option keys.
2. Run `XcodeNewProject` with:
   - `productName`: `MetroPrague`;
   - `organizationIdentifier`: `dev.pukan` (the template may derive `dev.pukan.MetroPrague`; fix it
     in step 4);
   - interface SwiftUI, language Swift, testing system **Swift Testing**, storage **None**, and no
     UI tests if the template offers that choice;
   - no `teamIdentifier`;
   - `destinationPath`: a scratch directory outside the repository.
3. Move the generated `MetroPrague.xcodeproj/`, `MetroPrague/`, and `MetroPragueTests/` into
   `ios/`. Do not move a nested `.git` directory if the template created one. If the template still
   created a UI test target, delete it in Xcode (target and folder).
4. Set these build settings at **target level** (Xcode project editor, or `UpdateTargetBuildSetting`
   in the Xcode MCP). Edit `project.xcproj` by hand only for what the plugin cannot do.

   | Setting | `MetroPrague` | `MetroPragueTests` |
   | --- | --- | --- |
   | `PRODUCT_BUNDLE_IDENTIFIER` | `dev.pukan.metroprague` | `dev.pukan.metroprague.tests` |
   | `IPHONEOS_DEPLOYMENT_TARGET` | `26.0` | `26.0` |
   | `TARGETED_DEVICE_FAMILY` | `1` | `1` |
   | `SWIFT_VERSION` | `6.0` | `6.0` |
   | `SWIFT_DEFAULT_ACTOR_ISOLATION` | `MainActor` | `MainActor` |
   | `SWIFT_APPROACHABLE_CONCURRENCY` | `YES` | `YES` |
   | `MARKETING_VERSION` | `1.0` | — |
   | `CURRENT_PROJECT_VERSION` | `1` | — |
   | `INFOPLIST_KEY_CFBundleDisplayName` | `MetroPrague` | — |
   | `DEVELOPMENT_TEAM` | unset | unset |

   Remove any project-level `IPHONEOS_DEPLOYMENT_TARGET` that differs from `26.0`.
5. Keep Xcode 27's JSON project format (`project.xcproj`), and restrict both targets to iOS
   (`SUPPORTED_PLATFORMS = iphoneos iphonesimulator`, `SDKROOT = iphoneos`), removing the
   multiplatform template's macOS and visionOS settings.
6. Commit a shared `MetroPrague` scheme whose Test action includes `MetroPragueTests`.
7. Delete the template's `ContentView.swift` and the sample test in `MetroPragueTests/`; Tasks 4
   and 5 replace them. Move `Assets.xcassets` into `MetroPrague/Resources/`. Keep the template's
   `MetroPragueApp.swift` and move it to `MetroPrague/App/` (Task 4 rewrites its body). Because the
   folders are synchronized, moving files on disk is enough.

**Acceptance Criteria:**

- `ios/MetroPrague.xcodeproj/project.xcproj` declares `MetroPrague` and `MetroPragueTests` as
  synchronized folders, contains `dev.pukan.metroprague`, and has no `DEVELOPMENT_TEAM`.
- `ios/MetroPrague.xcodeproj/xcshareddata/xcschemes/MetroPrague.xcscheme` exists.
- `xcodebuild -list -project MetroPrague.xcodeproj` (from `ios/`) lists targets `MetroPrague` and
  `MetroPragueTests` and scheme `MetroPrague`, and nothing else.
- `git status` shows no `xcuserdata` and no nested repository.

---

## Task 3: Configuration Files and `AppConfiguration`

**Objective:** Get the Golemio key from a gitignored file into Debug builds, and expose it safely to
code.

**Work:**

- Create `ios/Config/Shared.xcconfig`:

  ```xcconfig
  // Settings shared by every configuration of the MetroPrague app target.
  // Empty unless Debug.xcconfig includes Secrets.xcconfig.
  GOLEMIO_API_KEY =
  ```

- Create `ios/Config/Debug.xcconfig`:

  ```xcconfig
  #include "Shared.xcconfig"
  // Optional: local, gitignored. Copy Secrets.example.xcconfig to create it.
  #include? "Secrets.xcconfig"
  ```

- Create `ios/Config/Release.xcconfig`:

  ```xcconfig
  // Release builds never contain the Golemio key.
  #include "Shared.xcconfig"
  ```

- Create `ios/Config/Secrets.example.xcconfig`:

  ```xcconfig
  // Copy to Secrets.xcconfig (gitignored) and fill in. Never commit the real file.
  // Used by Debug builds only.
  GOLEMIO_API_KEY =
  // Optional, for running on a physical device:
  // DEVELOPMENT_TEAM =
  ```

- Create `ios/Config/Info.plist` with one entry: key `GolemioAPIKey`, string value
  `$(GOLEMIO_API_KEY)`.
- In Xcode, set the `MetroPrague` target's base configuration to `Config/Debug.xcconfig` for Debug
  and `Config/Release.xcconfig` for Release. Set target build settings `INFOPLIST_FILE =
  Config/Info.plist` and keep `GENERATE_INFOPLIST_FILE = YES`. The `Config/` files must not be
  members of any target.
- Create `ios/MetroPrague/App/AppConfiguration.swift`:
  - `struct AppConfiguration: Sendable` with a stored `golemioAPIKey: String?`;
  - `init(infoDictionary: [String: Any])`. It reads `GolemioAPIKey` as `String` and trims
    whitespace. It stores `nil` if the value is missing, not a string, empty after trimming, or
    starts with `$(`;
  - `static let main = AppConfiguration(infoDictionary: Bundle.main.infoDictionary ?? [:])`;
  - mark the type `nonisolated` so it can be used off the main actor later. Add a one-line comment
    explaining why;
  - never log or print the key.
- Create `ios/MetroPragueTests/AppConfigurationTests.swift` (Swift Testing, `@Suite`, `@Test`,
  `#expect`) with cases: a valid key is returned trimmed; missing key → `nil`; empty and
  whitespace-only → `nil`; unexpanded `$(GOLEMIO_API_KEY)` → `nil`; non-string value → `nil`.
  Use one parameterized `@Test(arguments:)` for the `nil` cases.

**Acceptance Criteria:**

- A Debug build with no `Secrets.xcconfig` succeeds. The built app's `Info.plist` has an empty
  `GolemioAPIKey`. Check with `plutil -extract GolemioAPIKey raw <app>/Info.plist`.
- With a temporary `Secrets.xcconfig` containing `GOLEMIO_API_KEY = test-value`, the Debug app's
  `GolemioAPIKey` is `test-value`, and a Release build's is empty. Delete the temporary file
  afterwards. Never use the owner's real key for this check.
- `AppConfigurationTests` pass.
- `git status` never shows `ios/Config/Secrets.xcconfig`.

---

## Task 4: Tab Shell

**Objective:** The app launches into an empty, native three-tab shell.

**Work:**

- `ios/MetroPrague/Navigation/AppTab.swift`: `enum AppTab: Hashable { case home, search, settings }`.
- `ios/MetroPrague/Navigation/RootTabView.swift`:
  - `@State private var selectedTab: AppTab = .home`;
  - `TabView(selection: $selectedTab)` with
    `Tab("Home", systemImage: "house", value: .home) { HomeView() }`,
    `Tab("Settings", systemImage: "gear", value: .settings) { SettingsView() }`, and
    `Tab("Search", systemImage: "magnifyingglass", value: .search, role: .search) { SearchView() }`;
  - a `#Preview`.
- `ios/MetroPrague/Features/Home/HomeView.swift`: `NavigationStack` with title "Favorites" and a
  `ContentUnavailableView("No favorites yet", systemImage: "star", description: Text("Search for a station and pick a direction to add it here."))`.
  The copy matches Android's `home_title` and empty-state strings, so step 3.4 can keep it. Add a
  `#Preview`.
- `ios/MetroPrague/Features/Search/SearchView.swift`: `NavigationStack` with title "Search" and
  `ContentUnavailableView("Station search", systemImage: "magnifyingglass", description: Text("Coming soon."))`.
  Add a `#Preview`.
- `ios/MetroPrague/Features/Settings/SettingsView.swift`: `NavigationStack` with title "Settings"
  and `ContentUnavailableView("Settings", systemImage: "gear", description: Text("Coming soon."))`.
  Add a `#Preview`.
- `ios/MetroPrague/App/MetroPragueApp.swift`: `@main struct MetroPragueApp: App` whose
  `WindowGroup` shows `RootTabView()`.
- Create `ios/MetroPrague/Resources/Localizable.xcstrings` (English source) in Xcode (File → New →
  File → String Catalog). After a build, the string literals above appear in it. Commit the catalog
  with those extracted entries.
- Use string literals in `Text`, `Tab`, and `ContentUnavailableView` so they are localizable
  `LocalizedStringKey`s. Do not wrap them in `String(...)`.
- Before writing views, load the `xcode-integration:swiftui-specialist` skill and follow it. Use
  `xcode-integration:swiftui-whats-new-27` only to check that nothing used is iOS 27-only, because
  the deployment target is 26.0.

**Acceptance Criteria:**

- The app builds with no warnings.
- On an iOS 26+ simulator (opened through the T3 device tools), the app launches to Home with the
  title "Favorites" and the empty state. Tapping Settings and the separate Search button shows each
  placeholder, and each tab keeps its own navigation title. Take a screenshot with
  `device_screenshot` for the PR description.
- VoiceOver labels of the tab items are "Home", "Search", and "Settings" (they come from the `Tab`
  titles; no extra modifiers are needed).
- `Localizable.xcstrings` contains every user-visible string above.

---

## Task 5: Fixture Loader and Smoke Tests

**Objective:** Prove that tests can read shared `fixtures/` files from the repository.

**Work:**

- `ios/MetroPragueTests/Support/Fixtures.swift`:
  - `enum Fixtures` with `static func url(_ relativePath: String, filePath: StaticString = #filePath) throws -> URL`
    and `static func data(_ relativePath: String, filePath: StaticString = #filePath) throws -> Data`;
  - `relativePath` is relative to the repository's `fixtures/` directory, for example
    `"golemio/departureboards/empty-board.json"`;
  - find the repository root by walking up parent directories from `filePath` until one contains
    `fixtures/README.md`. Throw a descriptive error naming the path if the root or the file is not
    found;
  - add a short doc comment stating that fixtures are read from the repository, not bundled, and
    why.
- `ios/MetroPragueTests/FixturesTests.swift`:
  - one parameterized `@Test(arguments:)` over the four departure-board fixture paths listed in
    `fixtures/README.md`. Each must load and decode with `JSONSerialization` to a
    `[String: Any]` containing a `departures` key whose value is an array;
  - one `@Test` asserting that `empty-board.json`'s `departures` array is empty;
  - one `@Test` asserting that a missing fixture throws.

**Acceptance Criteria:**

- `FixturesTests` pass locally and in CI.
- No fixture file is copied into `ios/`, and the test target has no fixture resources.

---

## Task 6: Simulator Destination Script and Formatter Config

**Objective:** Same test destination locally and in CI, and one formatter configuration.

**Work:**

- `ios/scripts/simulator-destination.sh` (`#!/usr/bin/env bash`, `set -euo pipefail`, executable):
  - if `SIMULATOR_UDID` is set and nonempty, print `id=$SIMULATOR_UDID` and exit 0;
  - otherwise parse `xcrun simctl list devices available` (text output; only Xcode tools and POSIX
    utilities, no `jq` or Python). Pick the newest `-- iOS X.Y --` section with X ≥ 26 that contains
    an iPhone, take its first line beginning with `iPhone`, and print `id=<UDID>`;
  - if none is found, print `No available iPhone simulator with iOS 26 or later.` to stderr and exit
    1;
  - add a header comment with usage.
- `ios/.swift-format`: JSON with `"version": 1`, `"indentation": { "spaces": 4 }`,
  `"lineLength": 100`, and all other options at defaults.
- Run `xcrun swift-format format --in-place --recursive MetroPrague MetroPragueTests` once, from
  `ios/`, so the existing sources conform.

**Acceptance Criteria:**

- `scripts/simulator-destination.sh` prints `id=` followed by a UUID on the owner's machine.
  `SIMULATOR_UDID=abc scripts/simulator-destination.sh` prints `id=abc`.
- `xcrun swift-format lint --strict --recursive MetroPrague MetroPragueTests` exits 0.

---

## Task 7: iOS Agent and Developer Docs

**Objective:** An agent or developer working in `ios/` has everything iOS-specific in one place.

**Work:**

- `ios/CLAUDE.md`: exactly `Always read and follow \`./AGENTS.md\` and the root \`../AGENTS.md\`.`
  (same as `android/CLAUDE.md`).
- `ios/AGENTS.md`, titled `# iOS App Guidelines`, opening with "This file covers the iOS app in this
  directory. Repository-wide rules are in `../AGENTS.md`." Sections:
  1. **Project Structure:** the layout tree from *Decisions*. Synchronized folders: create files on
     disk; edit `project.xcproj` by hand only when the plugin cannot. Change build settings through Xcode or the Xcode
     MCP. Feature code goes in `MetroPrague/Features/<Feature>/`, data code (from step 2) in
     `MetroPrague/Data/`.
  2. **Build, Test, and Format:** the commands from *Build commands*, run from `ios/`, plus how to
     run one suite (`-only-testing:MetroPragueTests/<SuiteName>`). `xcodebuild build`, `test`, and
     `swift-format lint` are the quality gates. If `xcrun` fails with `couldn't create cache file`,
     the agent sandbox is the cause; run outside it.
  3. **Configuration and Secrets:** `Config/` files, `Secrets.example.xcconfig` → `Secrets.xcconfig`,
     Debug-only key, `AppConfiguration.main.golemioAPIKey`. Never print, log, commit, or paste the
     key.
  4. **Swift and Concurrency:** Swift 6 mode. Default `MainActor` isolation: views and UI models
     need no `@MainActor`. Use an `actor` for shared mutable state off the main actor (the Golemio
     client). Mark pure value types, parsers, and helpers `nonisolated` when they must run off the
     main actor. Prefer `async`/`await`; use `AsyncStream` only where a stream is needed. No
     `DispatchQueue` and no Combine in new code.
  5. **SwiftUI Conventions:**
     - `@Observable` model classes for screen state, owned with `@State` by the view that creates
       them; pass dependencies through initializers and SwiftUI `Environment`; no singletons except
       `AppConfiguration.main`;
     - `NavigationStack` per tab (never `NavigationView`), `Tab`-based `TabView`, `.searchable` for
       search, sheets for pickers;
     - system components and SF Symbols, and system Liquid Glass. Add custom glass effects only when
       a plan asks for them;
     - user-visible text as string literals or `LocalizedStringResource`, collected in
       `Localizable.xcstrings`;
     - one primary view per file, named `<Thing>View`. UI models are named `<Screen>Model`;
     - a `#Preview` with representative fake data for every view;
     - stable `id`s in `ForEach`/`List`;
     - accessibility labels on icon-only controls, and Dynamic Type-friendly text styles (no fixed
       font sizes).
  6. **Deployment Target:** iOS 26.0. iOS 27-only APIs require `if #available(iOS 27, *)` with an
     iOS 26 fallback, or must not be used.
  7. **Agent Skills:** load `xcode-integration:swiftui-specialist` before writing or reviewing
     SwiftUI. Load `xcode-integration:swiftui-whats-new-27` when using or checking newer SwiftUI
     APIs. For screen PRs, also run the accessibility skills
     (`xcode-integration:accessibility-voiceover-specialist`,
     `xcode-integration:accessibility-dynamic-type-specialist`,
     `xcode-integration:accessibility-sufficient-contrast-specialist`).
  8. **Simulator and Devices:** drive simulators only through the T3 device tools (`device_list`,
     `device_open` and the `agent-device` command it returns, `device_screenshot`,
     `device_close`). Use `xcrun simctl` only for builds, installs, and choosing a destination,
     never for launching, tapping, or screenshots.
  9. **Testing:** Swift Testing only (`import Testing`, `@Suite`, `@Test`, `#expect`,
     `#require`, parameterized `arguments:`); no XCTest in `MetroPragueTests`. Tests mirror source
     paths and use the suffix `Tests`. Data-layer tests read shared inputs through `Fixtures` and
     assert what `../fixtures/README.md` documents. Cover one happy path and one edge case per new
     rule. No sleeps; inject clocks and HTTP transports.
  10. **Dependencies:** Apple frameworks only. Adding a Swift package requires an explicit plan
      decision.
  11. **Learning Goal:** the owner is learning SwiftUI. Plans explain non-obvious SwiftUI and
      concurrency choices in *Decisions*. Keep PRs small. Prefer plain, idiomatic Apple APIs over
      clever abstractions.
- `ios/README.md`: requirements (Xcode with the iOS 26+ SDK and an iOS 26+ simulator), first-time
  setup (copy `Config/Secrets.example.xcconfig` to `Config/Secrets.xcconfig`, add the key, optional
  `DEVELOPMENT_TEAM`), open `MetroPrague.xcodeproj` and run the `MetroPrague` scheme, the CLI build
  and test commands, and a security note (Debug builds contain the key and must not be distributed;
  Release builds contain none). Link `../docs/api/golemio.md` for getting a key and
  `../docs/product/parity.md` for status.

**Acceptance Criteria:**

- All three files exist, and every command in them works as written from `ios/`.
- `ios/AGENTS.md` covers every item the quickstart requires: `xcodebuild` commands, SwiftUI
  conventions, the SwiftUI skills, and the device tools.
- No file contains a key, a team ID, or a machine-specific path.

---

## Task 8: iOS CI Workflow

**Objective:** CI builds, lints, and tests iOS only when iOS-relevant files change.

**Work:**

- Create `.github/workflows/ios.yml`:
  - `name: iOS CI`; `on` identical to `android.yml`;
  - `changes` job: copy `android.yml`'s `changes` job exactly, renaming the output and variable to
    `ios`, the step name to `Detect iOS changes`, and the pattern to
    `^(ios/|fixtures/|\.github/workflows/ios\.yml$)`;
  - `build` job: `needs: changes`, `if: needs.changes.outputs.ios == 'true'`,
    `runs-on: xcode-27`, `defaults.run.working-directory: ios`, steps (after `sudo xcode-select -s /Applications/Xcode_27.2.app`):
    1. `actions/checkout@v4`;
    2. `xcodebuild -version`;
    3. `xcrun swift-format lint --strict --recursive MetroPrague MetroPragueTests`;
    4. `echo "DEST=$(scripts/simulator-destination.sh)" >> "$GITHUB_ENV"`;
    5. `xcodebuild test -project MetroPrague.xcodeproj -scheme MetroPrague -destination "$DEST" -derivedDataPath build CODE_SIGNING_ALLOWED=NO SWIFT_TREAT_WARNINGS_AS_ERRORS=YES`.
- Confirm against GitHub's `actions/runner-images` docs that the `xcode-27` image includes
  `/Applications/Xcode_27.2.app` and an iOS 26+ iPhone simulator.
- Do not change `android.yml`.

**Acceptance Criteria:**

- On the PR, `iOS CI` runs `build`, and the job passes.
- The workflow's change filter would skip `build` for a docs-only change. Check this by running
  the filter's `grep -Eq` line locally against a sample path list such as
  `docs/ai/README.md` (no match) and `fixtures/README.md` (match).
- A skipped `build` job does not block merging (same behavior as Android CI).

---

## Task 9: Shared Docs Updates

**Objective:** Shared docs reflect that the iOS project exists, without claiming any feature.

**Work:**

- `docs/product/parity.md`: below the status sentence, add
  `iOS project scaffold (Xcode project, tab shell, CI) landed on <PR merge date>; no iOS feature is implemented yet.`
  Set `Last reviewed` to that date. Leave every feature row's iOS status `not started`.
- Root `README.md`: in the Platforms table, set iOS status to `Scaffolded`. In the layout tree,
  change `# iOS app (planned)` to `# iOS app (SwiftUI, Xcode project)`.
- Root `AGENTS.md`: change "`ios/` will contain the SwiftUI and Xcode iOS app; it is planned." to
  "`ios/` contains the SwiftUI and Xcode iOS app."
- Root `.gitignore`: change the trailing comment to
  `# Platform-specific ignore rules live in android/.gitignore and ios/.gitignore.`
- `docs/ai/quickstart-ios.md`: under step 1, add the line `Plan: [ios-scaffold-plan.md](plans/ios-scaffold-plan.md).`
- In `docs/product/decisions.md`, add one row dated the merge date: "On iOS, the Search tab uses the
  system search tab role, so it appears as a separate trailing tab-bar button; tab identity and
  order of Home and Settings are otherwise unchanged." Platforms affected: iOS. Source: this plan.

**Acceptance Criteria:**

- The diff touches only these files' stated lines.
- No feature status changed in `parity.md`.

---

## Task 10: Final Verification

**Objective:** Everything works from a clean clone.

**Work:**

- From a fresh clone of the branch (or after `git clean -xdf ios/` in a worktree, which removes
  untracked and ignored files under `ios/` only), with no `Secrets.xcconfig`:
  - run the *Build commands* (build, test, lint) from `ios/`;
  - launch the app through the T3 device tools and confirm the shell from Task 4; close the device
    session afterwards.
- Confirm `git ls-files ios` contains no `xcuserdata`, `build/`, or `Secrets.xcconfig`, and that
  `grep -rI "GOLEMIO_API_KEY = ." ios` finds nothing but the empty declarations and example.
- Commit with Conventional Commits, for example `feat(ios): scaffold Xcode project and tab shell`
  and `ci: add iOS workflow`. Open one PR containing all tasks. It is the iOS platform PR, plus the
  shared-doc lines from Task 9 that describe it.

**Acceptance Criteria:**

- All gates in the Definition of Done pass.

## Definition of Done

- `ios/MetroPrague.xcodeproj` exists, created by Xcode, in the Xcode 27 JSON format (`project.xcproj`), synchronized folders,
  a shared `MetroPrague` scheme, and the build settings from Task 2.
- From `ios/` on a clean checkout without secrets: `xcodebuild build` and `xcodebuild test` succeed
  on an iOS 26+ iPhone simulator, and `swift-format lint --strict` reports nothing.
- `AppConfigurationTests` and `FixturesTests` pass. Fixtures are read from `fixtures/`, not copied.
- The app launches to a native Home/Settings/Search tab shell with placeholder content and
  correct VoiceOver tab labels.
- Debug builds embed the key from gitignored `Config/Secrets.xcconfig` when present. Release builds
  never do. No key or team ID is committed.
- `ios/AGENTS.md`, `ios/CLAUDE.md`, `ios/README.md`, `ios/.gitignore`, `ios/.swift-format`, and
  `ios/scripts/simulator-destination.sh` exist as specified.
- `iOS CI` passes on the PR and is skipped for changes outside `ios/`, `fixtures/`, and its own
  workflow file.
- Shared docs are updated as in Task 9. `android/` and `android.yml` are unchanged.
