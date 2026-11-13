# iOS Quickstart

Read this after the monorepo migration ([guide](monorepo-migration-guide.md),
[plan](plans/monorepo-migration-plan.md)) is merged. It records the decisions made before the
migration about how the iOS app is built, and how to start it.

## Prerequisites

- The migration PR is merged. `android/` builds, and `docs/product/`, `docs/api/golemio.md`, and
  `fixtures/golemio/` exist.
- The local `MetroPrague-ios` folder is deleted. All iOS work happens in `MetroPrague/ios/`.
- Xcode with the iOS 26+ SDK and an iOS Simulator are installed.

## Decisions

| Topic | Decision |
| --- | --- |
| Goal | Learn SwiftUI, and ship and maintain both apps long term. |
| Approach | **Fully native SwiftUI.** No KMP for now. |
| Repository | Same monorepo, independent project in `ios/`, separate build and CI. |
| Minimum OS | **iOS 26+.** Use current APIs (Liquid Glass, `@Observable`) without back-deployment shims. |
| Parity | **Generally 1:1 with Android, but native first.** Follow each feature spec's *Must match* rules exactly. Adapt everything under *Platform-native* to iOS conventions. |
| Lockstep | None. iOS may lag. Track it in `docs/product/parity.md`. |
| Golemio key | Gitignored `.xcconfig` (for example `ios/Config/Secrets.xcconfig`), read through `Info.plist` build settings. As on Android, a key compiled into the client is recoverable. Shipping strategy (direct vs proxy) is still open. |
| Tests | Swift Testing. Data-layer tests read `fixtures/golemio/` and assert what `fixtures/README.md` documents. |

### Why not KMP (and when to revisit)

The shareable Android logic is about 1,400 lines, and it depends on JVM APIs (`HttpURLConnection`,
`java.time`, DataStore, Hilt). Sharing it would mean rewriting it for KMP anyway, and Swift
interop with `Flow` and `suspend` is the friction point. **Revisit** if the roadmap brings heavy,
subtle logic worth writing once, such as offline GTFS timetables, trams, or route planning. In that
case, add a KMP module for just that engine, consumed by both apps.

## Kotlin → Swift Mapping

Port behavior, not structure. These are starting points, not rules to translate line by line.

| Android | iOS |
| --- | --- |
| Compose + Material 3 Expressive | SwiftUI with system components and Liquid Glass |
| `ViewModel` + `StateFlow` | `@Observable` model classes, `@MainActor` where they drive UI |
| Coroutines, `Flow` | `async/await`, `AsyncSequence` / `AsyncStream` where a stream is needed |
| `GolemioHttpClient` (Mutex-paced) | `actor` wrapping `URLSession`, with the same pacing and 429 / Retry-After rules |
| kotlinx.serialization / `JsonObject` parsing | `Codable`, or `JSONSerialization` where the Android parser is lenient |
| DataStore (favorites, prefs, snapshot) | `UserDefaults` / `@AppStorage` for prefs; JSON file in Application Support for snapshot and favorites (or SwiftData if it earns its place) |
| Hilt | Plain initializer injection plus SwiftUI `Environment` |
| `strings.xml` | String Catalog (`Localizable.xcstrings`) |
| Navigation Compose, bottom bar | `TabView` + `NavigationStack`, `.searchable`, sheets for pickers |

## Steps

1. **Scaffold plan.** Have the planner write `docs/ai/plans/ios-scaffold-plan.md`. Scope:
   (Plan: [ios-scaffold-plan.md](plans/ios-scaffold-plan.md).)
   - an Xcode project in `ios/` (iOS 26+, SwiftUI app, Swift Testing target);
   - the `.xcconfig` for the key;
   - `ios/AGENTS.md`, `ios/CLAUDE.md`, `ios/README.md`, and `ios/.gitignore`;
   - `.github/workflows/ios.yml` on a macOS runner, using the same change-detection pattern as
     `android.yml` (`ios/`, `fixtures/`, its own workflow file);
   - an empty app shell with the tab structure;
   - a `parity.md` iOS status update.

   `ios/AGENTS.md` should cover:
   - build and test commands (`xcodebuild`);
   - SwiftUI conventions;
   - the instruction to use the Xcode / SwiftUI agent skills (`swiftui-specialist`,
     `swiftui-whats-new-27`);
   - the instruction to drive simulators through the device tools.
2. **Data layer first.** Steps 2–4 are planned in
   [ios-android-parity-plan.md](plans/ios-android-parity-plan.md). Port Golemio networking,
   parsing, catalogue loading, the snapshot store, favorites, and preferences, following `docs/product/features/metro-data-sync.md` and
   `docs/api/golemio.md`. Tests read the shared fixtures. Use the Android implementations as
   references for each rule, not as a template.
3. **Screens, one spec at a time,** in this order:
   1. settings (theme, metro data reload);
   2. station search and direction picker;
   3. station departures;
   4. favorites on Home.

   One feature per PR. Each PR updates the iOS row in `parity.md`.
4. **Native review.** For every screen, check whether the 1:1 layout feels right on iOS. Record
   intentional divergences in the spec's *Platform-native* section.

## Learning Goal

The owner is learning SwiftUI, so iOS plans and agent briefs should:

- explain non-obvious SwiftUI and Swift concurrency choices in the plan's *Decisions*, so the
  reasoning stays readable after the fact;
- prefer plain, idiomatic Apple frameworks over third-party libraries;
- keep tasks small enough to review and understand one at a time.

## Starter Prompt

> Read `AGENTS.md`, `docs/ai/quickstart-ios.md`, `docs/product/`, and `docs/api/golemio.md`.
> Then follow `docs/ai/agents/planner.md` to write `docs/ai/plans/ios-scaffold-plan.md` for step 1
> of the quickstart. Do not write iOS code yet.
