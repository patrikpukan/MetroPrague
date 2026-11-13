# iOS App Guidelines

This file covers the iOS app in this directory. Repository-wide rules are in `../AGENTS.md`.

## Project Structure

```text
ios/
├── MetroPrague.xcodeproj/   # project.xcproj (JSON) + shared scheme
├── MetroPrague/             # app target (synchronized folder)
│   ├── App/                 # @main app, AppConfiguration
│   ├── Navigation/          # AppTab, RootTabView
│   ├── Domain/              # pure, nonisolated value types and rules (no I/O)
│   ├── Data/                # networking, parsing, storage, and app-level stores
│   ├── Features/<Feature>/  # one folder per feature spec
│   ├── Shared/              # views and helpers used by more than one feature
│   └── Resources/           # Assets.xcassets, Localizable.xcstrings
├── MetroPragueTests/        # Swift Testing target (synchronized folder)
│   └── Support/             # test helpers such as Fixtures
├── Config/                  # xcconfig files and the partial Info.plist (no target membership)
└── scripts/                 # build helpers
```

- The project uses Xcode 27's JSON project format, `MetroPrague.xcodeproj/project.xcproj`. It
  needs Xcode 27 or later.
- `MetroPrague/` and `MetroPragueTests/` are synchronized folders: files created there join their
  target automatically.
- Feature code goes in `MetroPrague/Features/<Feature>/`. Data code (Golemio, storage) goes in
  `MetroPrague/Data/`. Pure rules shared by data and screens go in `MetroPrague/Domain/`, and
  views used by several features go in `MetroPrague/Shared/`.

## Xcode Tools

- Use the Xcode plugin (`plugin:xcode-integration:xcode`) for project work: open the project with
  `XcodeOpenWorkspace`, create files and folders with `XcodeWrite` / `XcodeMakeDir`, move or remove
  them with `XcodeMV` / `XcodeRM`, change build settings with `UpdateTargetBuildSetting`, and use
  `BuildProject`, `RunAllTests`, `RunSomeTests`, `GetBuildLog`, and `RenderPreview`.
- Edit `project.xcproj` by hand only for what those tools cannot do (for example, a target's
  `specialized-configurations`). Keep its existing style, then confirm that Xcode still lists the
  targets and scheme and that the build passes.
- Use `DocumentationSearch` for Apple documentation.

## Build, Test, and Format

Run from `ios/`. Derived data goes to the gitignored `build/` folder.

```sh
DEST="$(scripts/simulator-destination.sh)"
xcodebuild build -project MetroPrague.xcodeproj -scheme MetroPrague -destination "$DEST" -derivedDataPath build
xcodebuild test  -project MetroPrague.xcodeproj -scheme MetroPrague -destination "$DEST" -derivedDataPath build
xcrun swift-format lint --strict --recursive MetroPrague MetroPragueTests
```

- Run one suite with `-only-testing:MetroPragueTests/<SuiteName>`.
- Format with `xcrun swift-format format --in-place --recursive MetroPrague MetroPragueTests`
  (settings in `.swift-format`).
- The build, test, and lint commands are the quality gates. CI adds
  `SWIFT_TREAT_WARNINGS_AS_ERRORS=YES`, so fix warnings too.
- If `xcrun` or `xcodebuild` fails with `couldn't create cache file ... Operation not permitted`,
  the agent command sandbox is the cause. Run the command outside it.

## Configuration and Secrets

- `Config/Debug.xcconfig` and `Config/Release.xcconfig` are the app target's base configurations.
  Both include `Config/Shared.xcconfig`.
- Debug also includes the gitignored `Config/Secrets.xcconfig` when it exists. Create it from
  `Config/Secrets.example.xcconfig`. Release builds never contain the Golemio key.
- `Config/Info.plist` maps `GOLEMIO_API_KEY` to the `GolemioAPIKey` key. Read it through
  `AppConfiguration.main.golemioAPIKey`, which is `nil` when no key was built in.
- Never print, log, commit, or paste the key.

## Swift and Concurrency

- Swift 6 language mode with default `MainActor` isolation and Approachable Concurrency.
- Views and UI models are main-actor by default; do not add `@MainActor` to them.
- Use an `actor` for shared mutable state that must live off the main actor (for example, the
  Golemio HTTP client).
- Mark pure value types, parsers, and helpers that must run off the main actor `nonisolated`.
- Prefer `async`/`await`. Use `AsyncStream` only where a stream is needed. No `DispatchQueue` and no
  Combine in new code.

## SwiftUI Conventions

- `@Observable` model classes for screen state, owned with `@State` by the view that creates them.
  Pass dependencies through initializers and the SwiftUI `Environment`. No singletons except
  `AppConfiguration.main`.
- One `NavigationStack` per tab (never `NavigationView`), a `Tab`-based `TabView`, `.searchable`
  for search, and sheets for pickers.
- System components, SF Symbols, and the system's Liquid Glass. Add custom glass effects only when a
  plan asks for them.
- User-visible text is a string literal or a `LocalizedStringResource`, collected in
  `Resources/Localizable.xcstrings`. Never build sentences with `+`.
- One primary view per file, named `<Thing>View`. UI models are named `<Screen>Model`.
- Every view has a `#Preview` with representative fake data.
- Use stable `id`s in `ForEach` and `List`.
- Give icon-only controls accessibility labels. Use text styles, not fixed font sizes, so Dynamic
  Type works.

## Deployment Target

iOS 26.0. iOS 27-only APIs need `if #available(iOS 27, *)` with an iOS 26 fallback, or must not be
used.

## Agent Skills

- Load `xcode-integration:swiftui-specialist` before writing or reviewing SwiftUI.
- Load `xcode-integration:swiftui-whats-new-27` when using or checking newer SwiftUI APIs.
- For screen PRs, also run `xcode-integration:accessibility-voiceover-specialist`,
  `xcode-integration:accessibility-dynamic-type-specialist`, and
  `xcode-integration:accessibility-sufficient-contrast-specialist`.

## Simulator and Devices

- Drive simulators only through the T3 device tools: `device_list`, `device_open` (and the
  `agent-device` command it returns), `device_screenshot`, and `device_close`.
- Use `xcrun simctl` only for builds, installs, and choosing a destination; never for launching,
  tapping, or screenshots.

## Testing

- Swift Testing only (`import Testing`, `@Suite`, `@Test`, `#expect`, `#require`, parameterized
  `arguments:`). No XCTest in `MetroPragueTests`.
- Test files mirror source paths and end in `Tests`.
- Data-layer tests read shared inputs through `Fixtures` (`MetroPragueTests/Support/Fixtures.swift`)
  and assert what `../fixtures/README.md` documents. Never copy fixtures into `ios/`.
- Cover one happy path and one edge case per new rule. No sleeps; inject clocks and HTTP transports.

## Dependencies

Apple frameworks only. Adding a Swift package requires an explicit plan decision.

## Learning Goal

The owner is learning SwiftUI. Plans explain non-obvious SwiftUI and concurrency choices in their
*Decisions*. Keep PRs small, and prefer plain, idiomatic Apple APIs over clever abstractions.
