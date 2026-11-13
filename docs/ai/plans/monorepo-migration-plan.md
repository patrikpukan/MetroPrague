# Monorepo Migration Plan

## Goal

Turn the `MetroPrague` repository into a monorepo that holds two independent native apps, Android
(existing) and iOS (later), with shared product and agent context:

- The Android project moves unchanged into `android/` and still builds and tests exactly as before.
- Android CI builds only when Android-relevant paths change, and a skipped build never blocks a
  merge.
- Agent instructions are split into a shared root layer and a platform layer, so an agent working on
  one platform loads only what it needs.
- Platform-neutral product knowledge (feature behavior, decisions, Golemio API research, API
  fixtures) lives at the root where both platforms can use it.
- The platforms are **not** kept in lockstep. Shared specs describe the target behavior, and each
  platform reports its own status.

No app behavior changes. No iOS code or Xcode project is created by this plan.

Read `docs/ai/monorepo-migration-guide.md` for how to orchestrate this plan, the local-machine steps,
and the post-merge checklist.

## Repository Context

- Single Gradle project at the repository root: `settings.gradle.kts` (`rootProject.name =
  "MetroPrague"`, `include(":app")`), `build.gradle.kts`, `gradle.properties`, `gradle/`
  (`libs.versions.toml`, `gradle-daemon-jvm.properties` pinning daemon JDK 21, wrapper),
  `gradlew`, `gradlew.bat`, and the `app/` module.
- `app/build.gradle.kts` reads `GOLEMIO_API_KEY` from `rootProject.file("local.properties")`. After
  the move, the Gradle root is `android/`, so the file must live at `android/local.properties`. No
  build script change is needed.
- Tracked `.idea/` files (code styles, inspection profile, run configurations, Gradle and misc
  settings) live at the repository root and belong to the Android Studio project.
- Root `.gitignore` contains only Android/Gradle entries. Several are root-anchored (`/build`,
  `/local.properties`, `/.kotlin/`, `/captures`, `/.idea/...`).
- `.agents/skills/compose-skill` is tracked as a **gitlink (submodule pointer) with no
  `.gitmodules`**. Fresh clones cannot populate it, and the directory is empty locally. `AGENTS.md`
  points at `.agents/skills/compose-expert/skills/compose-expert/SKILL.md`, which does not exist.
  The `compose-expert` skill is installed as an agent plugin instead. Fixing how the project
  provides this skill is a tracked follow-up (see the guide's *Follow-ups* section), not part of
  this plan.
- CI: `.github/workflows/build.yml` ("Android CI") runs `./gradlew build` on JDK 17 for pushes and
  PRs to `main`.
- Agent context: root `CLAUDE.md` ("read `AGENTS.md`"), root `AGENTS.md` (Android-only guidance),
  role docs in `docs/ai/agents/`, historical plans in `docs/ai/plans/`, reports in
  `docs/ai/reports/`.
- `README.md` mixes the product overview with Android setup and commands.
- Golemio test inputs are inline JSON strings in
  `app/src/test/java/dev/pukan/metroprague/data/golemio/GolemioDepartureBoardParserTest.kt` and
  JSON builders in `MetroTestData.kt`. There are no JSON fixture files.
- Untracked local state at the root: `local.properties` (SDK path and Golemio key), `build/`,
  `.gradle/`, `.kotlin/`, untracked `.idea/` files, and `.claude/worktrees/` (excluded through
  `.git/info/exclude`).

## Decisions

- **Layout:** `android/` holds the full Gradle project. The root holds shared context only: `README.md`,
  `AGENTS.md`, `CLAUDE.md`, `docs/`, `fixtures/`, `.github/`. `ios/` is created later by the iOS
  scaffold plan.
- **Use `git mv` for every tracked Android file** so history and blame follow the rename. Do not
  copy and delete.
- **One branch, one commit per task:** all work happens on `chore/monorepo-migration` in the main
  checkout. No worktrees and no parallel builders, because every task touches overlapping paths.
- **Gradle root name stays `MetroPrague`.** Application ID, namespace, and versions are unchanged.
- **The orphan gitlink is removed** (`git rm --cached .agents/skills/compose-skill`). It cannot
  work without `.gitmodules`. Android Compose guidance points at the installed `compose-expert`
  skill by name.
- **CI uses a native change-detection job** (a `git diff` in a shell step, no third-party
  path-filter action). The build job is skipped with `if:` when nothing relevant changed. GitHub
  counts a skipped job as passing a required check, so this avoids the "required check never runs"
  trap of workflow-level `paths:` filters.
- **The Android CI trigger paths are** `android/**`, `fixtures/**`, and
  `.github/workflows/android.yml`. Docs-only changes do not build Android.
- **Agent context is layered:**
  - Root `AGENTS.md`: repository map, cross-platform rules, the no-lockstep policy, where shared
    knowledge lives, git/PR conventions, and routing to the platform docs.
  - `android/AGENTS.md`: the current Android guidance, with paths relative to `android/`.
  - `CLAUDE.md` at root and in `android/` each say "read `AGENTS.md` in this directory". Claude Code
    loads a nested `CLAUDE.md` when it works in that subtree; Codex-style tools load the nearest
    `AGENTS.md`.
- **No lockstep:** feature specs in `docs/product/features/` describe target behavior. Each platform
  status lives in `docs/product/parity.md` (`not started`, `in progress`, `done`, `behind spec`,
  `n/a`). A platform PR never waits for the other platform. A spec change that one platform does not
  implement yet marks that platform `behind spec`.
- **Each feature spec has two required sections:** **Must match** (data semantics, business rules,
  formatting, request pacing, reload rules, persistence keys, accessibility semantics) and
  **Platform-native** (navigation, components, typography, motion). Reviewers enforce only Must
  match across platforms.
- **Conventions:** Conventional Commits with a platform scope: `feat(android):`, `fix(ios):`,
  `docs(shared):`, `ci:`, `chore:`. One platform per PR, except shared-doc-only PRs. Release tags
  are `android-vX.Y.Z` and `ios-vX.Y.Z`.
- **Historical plans and reports are not rewritten.** They describe the pre-migration layout. A
  short note in `docs/ai/README.md` says that `app/...` paths in documents written before this
  migration mean `android/app/...`.
- **Agent role docs (`docs/ai/agents/`) get minimal path fixes only.** Gradle commands are run from
  `android/`. Making the roles platform-neutral is deferred.
- **The Golemio report moves** from `docs/ai/reports/golemio-api-integration.md` to
  `docs/api/golemio.md` (`git mv`), and every link to it is updated.
- **Shared fixtures** start with the departure-board JSON samples copied verbatim from
  `GolemioDepartureBoardParserTest.kt`. The Android tests keep their inline JSON in this plan.

## Out of Scope

- Any Android source, resource, dependency, Gradle plugin, SDK level, or behavior change.
- Creating `ios/`, an Xcode project, iOS CI, or iOS agent docs. These are the next plan, `ios-scaffold-plan.md`.
- KMP or any shared code module.
- Making the agent role docs platform-neutral, beyond the path fixes above.
- Rewriting historical plans and reports.
- Restoring a working, repo-pinned Compose skill (vendored copy or a proper submodule). This is a
  follow-up.
- Switching the Android tests to read `fixtures/`.
- Branch protection and GitHub settings changes. The guide lists them for the owner.
- Deleting local or remote branches.

## Task Dependency Order

1. Task 1: preflight and worktree cleanup (local only, no commit).
2. Task 2: move the Android project into `android/`.
3. Task 3: root `.gitignore`.
4. Task 4: CI workflow.
5. Task 5: layered agent context.
6. Task 6: README split.
7. Task 7: shared product docs and the Golemio report move.
8. Task 8: shared fixtures.
9. Task 9: final verification.

All tasks are strictly sequential. Task 2 must pass its build gate before anything else starts.

---

## Task 1: Preflight and Worktree Cleanup

**Objective:** Start from a clean, known baseline with no linked worktrees.

**Work:**

- Confirm the main checkout is on `chore/monorepo-migration`, branched from an up-to-date `main`,
  with a clean `git status`.
- Run `./gradlew assembleDebug test` from the repository root and record the result. A red baseline
  must be reported. Do not fix it as part of this plan.
- Remove all linked worktrees with `git worktree remove --force <path>`. The owner confirmed that
  all their work is finished, including the uncommitted changes in `pr7-review-bug-fix-9f8000` and
  the unmerged plan file in `plan-departure-seconds-planner-4759ab`. Do not archive or restore
  anything from them:
  - `.claude/worktrees/android-emulator-config-8f3054`
  - `.claude/worktrees/plan-departure-seconds-planner-4759ab`
  - `.claude/worktrees/pr7-review-bug-fix-9f8000`
  - `~/.gemini/antigravity/worktrees/MetroPrague/station_departures_search_screen` (PR #7 is
    merged)
- Run `git worktree prune`. Keep the branches.

**Acceptance criteria:**

- `git worktree list` shows only the main checkout.
- The baseline build and test result is recorded in the progress log.

---

## Task 2: Move the Android Project into `android/`

**Objective:** Relocate the Gradle project without changing how it builds.

**Work:**

- `git mv` into `android/`: `app`, `gradle`, `gradlew`, `gradlew.bat`, `build.gradle.kts`,
  `settings.gradle.kts`, `gradle.properties`, `.idea`, and `.gitignore` (it becomes
  `android/.gitignore`; its root-anchored entries now anchor to `android/`).
- `git rm --cached .agents/skills/compose-skill`, then remove the empty `.agents/` directory.
- Local, untracked steps:
  - Move `local.properties` to `android/local.properties` with plain `mv`. Never stage it.
  - Delete the stale root `build/`, `.gradle/`, and `.kotlin/`.
  - Delete the untracked leftovers in the root `.idea/` (after the `git mv`, the root `.idea/`
    holds only untracked files).
- Do not edit any file under `android/` in this task.

**Acceptance criteria:**

- `cd android && ./gradlew assembleDebug test` passes, and the test count matches the Task 1
  baseline.
- A debug build with a key still selects the Golemio repositories, proving that
  `android/local.properties` is read.
- `git status` shows only renames plus the gitlink removal. `git log --follow
  android/app/build.gradle.kts` shows pre-migration history.
- `git ls-files | grep -v '^android/'` lists only `.github/`, `AGENTS.md`, `CLAUDE.md`,
  `README.md`, and `docs/`.

---

## Task 3: Root `.gitignore`

**Objective:** Ignore OS and editor noise at the root without touching platform ignores.

**Work:**

- Create the root `.gitignore` with: `.DS_Store`, `/.idea/` (in case someone opens the root in a
  JetBrains IDE), and `/.claude/worktrees/` (so the ignore no longer relies only on the local
  `.git/info/exclude`).
- Add a comment that platform ignore rules live in `android/.gitignore` and, later,
  `ios/.gitignore`.

**Acceptance criteria:**

- `git status` is clean after a local build in `android/`.
- `git check-ignore android/local.properties android/build` matches `android/.gitignore` rules.

---

## Task 4: Android CI with Change Detection

**Objective:** Build Android only when relevant paths change, without blocking unrelated PRs.

**Work:**

- `git mv .github/workflows/build.yml .github/workflows/android.yml`. Keep the name `Android CI`.
- Triggers: unchanged (`push` to `main`, `pull_request` to `main`), with no workflow-level
  `paths:` filter.
- Job `changes` (ubuntu-latest):
  - Check out with `fetch-depth: 0`.
  - Compute the base: `github.event.pull_request.base.sha` for PRs, `github.event.before` for
    pushes.
  - Set output `android=true` when `git diff --name-only <base> HEAD` matches
    `^(android/|fixtures/|\.github/workflows/android\.yml$)`.
  - Treat a missing base or an all-zero SHA as `android=true`.
- Job `build`: `needs: changes`, `if: needs.changes.outputs.android == 'true'`,
  `defaults.run.working-directory: android`. Keep the existing steps (checkout, Temurin JDK 17 with
  Gradle cache, `chmod +x gradlew`, `./gradlew build`). Point the setup-java cache at
  `cache-dependency-path: android/**/*.gradle*` and
  `android/gradle/wrapper/gradle-wrapper.properties`.
- Keep the action major versions that are already in use.

**Acceptance criteria:**

- On this branch's PR, `build` runs (the diff touches `android/`) and passes.
- The workflow logic shows that a docs-only diff produces `android=false` and skips `build`. Verify
  by reading the expression, or run the detection script locally against a docs-only commit range.
- `actionlint` reports no errors, if it is available.

---

## Task 5: Layered Agent Context

**Objective:** Shared rules at the root, Android rules in `android/`, minimal role-doc fixes.

**Work:**

- **`android/AGENTS.md`**: move the current root `AGENTS.md` here with `git mv`, then edit it:
  - Paths become relative to `android/`, for example `app/src/main/...`.
  - All commands say they run from `android/`.
  - Remove the "Single Android app module" repo framing that the root doc now covers.
  - The Compose section becomes: "For Compose work, use the `compose-expert` skill."
  - All other rules stay as they are.
- **`android/CLAUDE.md`**: "Always read and follow `./AGENTS.md` and the root `../AGENTS.md`."
- **New root `AGENTS.md`**, with these sections:
  1. *Repository map*: `android/` (Kotlin, Compose, Gradle), `ios/` (SwiftUI, Xcode; planned),
     `docs/product/`, `docs/api/`, `docs/ai/`, `fixtures/`.
  2. *Platform routing*: before changing platform code, read that platform's `AGENTS.md`. Work only
     inside the assigned platform directory. Read the other platform only when a plan or spec
     points to a specific file for reference. Never copy idioms across platforms (DI, state
     containers, UI components, naming).
  3. *Shared knowledge*: behavior lives in `docs/product/features/`, status in
     `docs/product/parity.md`, decisions in `docs/product/decisions.md`, API facts in
     `docs/api/golemio.md`, test inputs in `fixtures/`. If an implementation finds a fact or decision
     that applies to both platforms, write it in the shared doc, not in platform code comments.
  4. *No-lockstep policy*: the decision text from this plan, including how to mark `behind spec`.
  5. *Feature workflow*: spec (shared) → plan per platform (`docs/ai/plans/<feature>-<platform>-plan.md`)
     → implementation → `parity.md` update in the same PR.
  6. *Conventions*: commit scopes, one platform per PR, release tags.
  7. *Secrets*: never commit API keys; each platform's local config file is gitignored.
- **Root `CLAUDE.md`**: keep the "read `AGENTS.md`" instruction and keep pointing to `docs/ai/`.
  Remove the `.agents/` reference.
- **`docs/ai/agents/*.md`**: run Gradle commands from `android/` (for example `cd android &&
  ./gradlew assembleDebug`). Point `.agents/skills/compose-expert/references/` at the
  `compose-expert` skill. Fix the planner's `../README.md` reference so it points to the root
  `README.md`. No other edits.
- **New `docs/ai/README.md`**: a short index of `agents/`, `plans/`, and `reports/`, plus the
  historical-paths note from Decisions.

**Acceptance criteria:**

- `grep -rn '\.agents/' AGENTS.md CLAUDE.md android/AGENTS.md docs/ai/agents` returns nothing.
- Every command in `android/AGENTS.md` and `docs/ai/agents/` works when run as written.
- The root `AGENTS.md` contains no Android-specific build rules, and `android/AGENTS.md` contains no
  repository-wide policy.

---

## Task 6: README Split

**Objective:** The root README covers the product, and each platform README covers its own setup.

**Work:**

- **`android/README.md`**: move the Technology, Architecture, Project Structure (paths updated),
  Getting Started (open `android/` in Android Studio, `local.properties` in `android/`), Command-Line
  Tasks (run from `android/`), Testing and Quality, and the dev-setup part of Data and Privacy here
  from the root README.
- **Root `README.md`**: keep the project description, Planned Features, Navigation and UX, Offline
  and Personalization, Status, and License. Add:
  - a "Platforms" table: Android (Kotlin, Jetpack Compose, Material 3 Expressive) → `android/`;
    iOS (Swift, SwiftUI) → `ios/`, planned;
  - the monorepo layout tree;
  - a link to `docs/product/parity.md`;
  - a data and privacy summary that links `docs/api/golemio.md`.
- Update all relative links.

**Acceptance criteria:**

- Every relative link in both READMEs resolves to an existing file.
- No setup or command text is duplicated between the two READMEs.

---

## Task 7: Shared Product Docs and the Golemio Report Move

**Objective:** Capture current behavior as platform-neutral specs that the iOS port can follow.

**Work:**

- `git mv docs/ai/reports/golemio-api-integration.md docs/api/golemio.md`. Update the links in the
  READMEs, `AGENTS.md` files, and agent role docs. Platform-specific paragraphs, such as the Android
  debug/release key handling, stay but are labelled "Android".
- Create `docs/product/features/` with one file per shipped feature:
  - `station-search.md`: search and the direction picker sheet.
  - `favorites-home.md`: favorite station directions on Home.
  - `station-departures.md`: the departures screen for a selected direction, including departure
    time display rules.
  - `settings.md`: theme (System/Light/Dark) and the about rows.
  - `metro-data-sync.md`: manual reload from Settings only, snapshot persistence, the five-minute
    staleness prompt, failure keeping the previous dataset and favorites, request pacing and 429
    handling, and platform fallbacks.
- Each spec uses this template: *Summary*; *Must match* (numbered, testable rules); *Platform-native*
  (what each platform may adapt); *Android reference* (paths to the implementing ViewModel and
  repository, plus tests); *iOS reference* (`TBD`); *Open questions*.
- Derive every rule from the current Android code, tests, and existing plans and reports. Do not
  invent behavior. Where code and docs disagree, the code wins, and the mismatch goes under *Open
  questions*.
- Create `docs/product/parity.md`: a table of features × {Android, iOS} using the status values from
  Decisions. Android is `done` where the feature is shipped, and iOS is `not started`. Add a
  "Last reviewed" date column.
- Create `docs/product/decisions.md`: dated product decisions extracted from the plans and reports,
  for example "First release scope is metro A, B, C only (2026-09-26)". Each entry gives the date,
  the decision, the source doc, and the platforms affected.

**Acceptance criteria:**

- Every *Must match* rule cites, or is traceable to, an Android source file, test, or doc.
- `grep -rn 'golemio-api-integration' .` returns nothing outside historical plans and reports.
- `parity.md` lists every feature in `docs/product/features/`.

---

## Task 8: Shared Fixtures

**Objective:** Give both platforms' test suites one source of truth for API inputs.

**Work:**

- Create `fixtures/golemio/departureboards/` with the four JSON samples from
  `GolemioDepartureBoardParserTest.kt`, copied verbatim and pretty-printed:
  `metro-predicted-delay.json`, `mixed-transport-scheduled-only.json`, `empty-board.json`,
  `short-turn.json`.
- Create `fixtures/README.md` covering:
  - the purpose (platform-neutral API inputs);
  - the naming rule (`<endpoint>/<scenario>.json`);
  - the rule that fixtures must never contain tokens or personal data;
  - that each fixture lists, in a sibling `<scenario>.md` or in the README table, the assertions
    every platform's test must make;
  - that the Android tests still use inline copies (follow-up item).
- Add a "Fixtures" row to the table at the end of `metro-data-sync.md` that links these files.

**Acceptance criteria:**

- Each fixture is valid JSON (`python3 -m json.tool` passes) and semantically equal to its inline
  source.
- `fixtures/README.md` documents the expected parse result of every fixture.

---

## Task 9: Final Verification

**Objective:** Prove that the migration changed layout and docs only.

**Work:**

- From `android/`: `./gradlew clean assembleDebug test`. Run `connectedAndroidTest` on an existing
  API 37.2 emulator if one is available. Otherwise, record it as not run.
- Search the repository for stale paths: `grep -rnE '(^|[^/])\./gradlew|app/src' --include=*.md .`.
  Outside historical plans and reports, every hit must either be inside `android/` docs or say to
  run from `android/`.
- Check every relative Markdown link in the changed docs.
- Run `git diff main --stat -M`. Every Android source file shows as a rename with 100% similarity.

**Acceptance criteria:**

- Build and unit tests pass, with the same test count as the baseline.
- No Android source or resource file has content changes.
- The progress log in `docs/ai/reports/monorepo-migration-implementation.md` is complete.

## Definition of Done

- The Android project lives in `android/` and builds, tests, and runs identically.
- `.github/workflows/android.yml` builds on Android-relevant changes and skips everything else
  without blocking merges.
- Root and `android/` agent docs are layered as described, with no dead references.
- The root README covers the product, and `android/README.md` covers Android setup.
- `docs/product/` (features, parity, decisions), `docs/api/golemio.md`, and `fixtures/golemio/`
  exist and follow the rules above.
- All work is committed on `chore/monorepo-migration`, one commit per task, ready for a single PR.
