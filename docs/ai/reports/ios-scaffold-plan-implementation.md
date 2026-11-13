# iOS Scaffold Implementation

Progress log for [`ios-scaffold-plan.md`](../plans/ios-scaffold-plan.md). Implemented on
2026-11-13 in one session, with Xcode 27.2 and the Xcode plugin (`plugin:xcode-integration:xcode`).

## Tasks

| Task | Status | Notes |
| --- | --- | --- |
| 1. `ios/.gitignore` | done | `git check-ignore` confirms secrets, `build/`, and `xcuserdata/` are ignored and the shared scheme is not. |
| 2. Xcode project | done | Created with `XcodeNewProject`. Build settings were set with `UpdateTargetBuildSetting`, and files were moved and removed with `XcodeMV` / `XcodeRM`. See deviations 1–4. |
| 3. Configuration and `AppConfiguration` | done | Built `Info.plist`: Debug with a temporary `Secrets.xcconfig` → `test-value`; Release → empty; Debug without secrets → empty. The temporary file was deleted. |
| 4. Tab shell | done | Simulator (iPhone 18 Pro, iOS 27.2, via T3 device tools): Home shows "Favorites" and its empty state; Settings and Search show their placeholders; tab labels are Home, Settings, Search. `Localizable.xcstrings` holds all nine strings. |
| 5. Fixtures | done | Fixtures are read from `fixtures/` via `#filePath`; nothing is copied into `ios/`. |
| 6. Script and formatter | done | `scripts/simulator-destination.sh` prints a UDID, and `SIMULATOR_UDID=abc` prints `id=abc`. `swift-format lint --strict` is clean. |
| 7. iOS docs | done | `ios/AGENTS.md`, `ios/CLAUDE.md`, `ios/README.md`. |
| 8. CI | done locally | `.github/workflows/ios.yml`. The change filter was checked against sample paths. Not yet run on GitHub. |
| 9. Shared docs | done | `parity.md` note (feature rows unchanged), root `README.md`, `AGENTS.md`, `.gitignore`, `quickstart-ios.md`, `decisions.md`. |
| 10. Verification | done | See below. |

## Verification

All run from `ios/`, with no `Secrets.xcconfig` and an empty `build/`:

- `xcrun swift-format lint --strict --recursive MetroPrague MetroPragueTests`: clean.
- `xcodebuild test -project MetroPrague.xcodeproj -scheme MetroPrague -destination "$(scripts/simulator-destination.sh)" -derivedDataPath build CODE_SIGNING_ALLOWED=NO SWIFT_TREAT_WARNINGS_AS_ERRORS=YES`:
  `** TEST SUCCEEDED **`. 13 tests: 7 in `AppConfigurationTests`, 6 in `FixturesTests`.
- Xcode plugin `BuildProject` reported no warnings, and `RunAllTests` passed 13 of 13.
- `git status` shows no `xcuserdata`, `build/`, or `Secrets.xcconfig`.

## Deviations (approved by the owner on 2026-11-13)

1. **Project format.** Xcode 27.2 creates projects in the JSON `project.xcproj` format, which Xcode
   26 cannot open. The owner chose to keep it and run CI on the `xcode-27` preview runner image
   with Xcode 27.2 selected. The plan's *Decisions* were updated.
2. **Template.** Xcode 27 has only the multiplatform App template. Both targets were restricted to
   iOS (`SUPPORTED_PLATFORMS`, `SDKROOT`), and the macOS and visionOS settings were removed.
3. **Hand edits to `project.xcproj`.** The plugin cannot delete a target, remove some settings, or
   set a target's xcconfig, so these were edited by hand in the readable JSON: removing the UI test
   target; removing `REGISTER_APP_GROUPS` and the macOS `LD_RUNPATH_SEARCH_PATHS`; adding the
   `Config` folder and `specialized-configurations` (key name from Apple's open-source
   `xcode-project-format`). Xcode kept these edits when it next saved the file.
4. **Shared scheme.** Xcode auto-created a scheme for the JSON project, but it disappeared after a
   project reload, so `xcshareddata/xcschemes/MetroPrague.xcscheme` was written explicitly.
5. **Search tab placement.** On the iOS 27.2 simulator, the search-role tab renders inside the
   tab bar as its last item, not as a separate button. The wording in the plan and in
   `decisions.md` was corrected.

## Open Items

- iOS CI has not run on GitHub yet. The `xcode-27` image is a preview, so queueing or flakiness is
  possible.
- Xcode's `XcodeMV` staged the files it moved. They were unstaged; nothing is committed.
