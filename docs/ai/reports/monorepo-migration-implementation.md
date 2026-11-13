# Monorepo Migration Implementation Log

Progress log for [`../plans/monorepo-migration-plan.md`](../plans/monorepo-migration-plan.md),
orchestrated per [`../monorepo-migration-guide.md`](../monorepo-migration-guide.md).

Branch: `chore/monorepo-migration` (main checkout, no worktrees). Builders: `builder.md` role on
`gpt-6-luna` (reasoning effort `max`), strictly sequential, one commit per accepted task.

## Status

| Task | Status | Commit |
| --- | --- | --- |
| 1. Preflight and worktree cleanup | done (with deviation) | none (local only) |
| 2. Move the Android project into `android/` | done | `chore: move Android project into android/` |
| 3. Root `.gitignore` | done | `chore: add root .gitignore` |
| 4. Android CI with change detection | done (PR run pending) | `ci: change-detected Android workflow` |
| 5. Layered agent context | done | `docs: layer agent context into root and android/` |
| 6. README split | done | `docs: split README into product and Android setup` |
| 7. Shared product docs and Golemio report move | todo | |
| 8. Shared fixtures | todo | |
| 9. Final verification | todo | |

## Task 1: Preflight and Worktree Cleanup

- Branch `chore/monorepo-migration` at `a0f8b40`, equal to `main` and `origin/main`.
- The working tree was not clean at the start:
  - The untracked plan, guide, and `docs/ai/quickstart-ios.md` were committed first as `53bda72`
    (`docs: add monorepo migration plan, guide, and iOS quickstart`).
  - Local IDE edits to `.idea/deploymentTargetSelector.xml` and `.idea/misc.xml` (JDK name
    `homebrew-21` → `temurin-21`, extra run-config selection states) were saved as a patch outside
    the repository and reverted so that `git mv` moves clean files. They can be reapplied with
    `git apply --directory=android <patch>` after the migration.
- Baseline from the repository root: `./gradlew assembleDebug test`, **BUILD SUCCESSFUL**.
  `testDebugUnitTest`: **105 tests, 0 failures, 0 skipped** across 20 test classes.
- **Deviation:** `git worktree remove --force` was denied by the agent's permission classifier, so
  the four linked worktrees are still present. They are untracked (`.claude/worktrees/` is in
  `.git/info/exclude`; the Antigravity one is outside the checkout) and do not affect the move.
  The owner removes them manually:

  ```sh
  git worktree remove --force .claude/worktrees/android-emulator-config-8f3054
  git worktree remove --force .claude/worktrees/plan-departure-seconds-planner-4759ab
  git worktree remove --force .claude/worktrees/pr7-review-bug-fix-9f8000
  git worktree remove --force ~/.gemini/antigravity/worktrees/MetroPrague/station_departures_search_screen
  git worktree prune
  ```

## Task 2: Move the Android Project into `android/`

- First builder run stopped on a wrong pointer in the orchestrator brief (it named a `BuildConfig`
  field; the key is the `golemio_api_key_base64` `resValue` string resource). Re-briefed; no
  changes had been made.
- Builder moved the Gradle project with `git mv`, removed the `.agents/skills/compose-skill`
  gitlink and `.agents/`, moved `local.properties` to `android/local.properties` (unstaged), and
  deleted root `build/`, `.gradle/`, `.kotlin/`, and the untracked root `.idea/` leftovers.
- Orchestrator verification:
  - `git status`: 139 renames (all 100% similarity) plus `D .agents/skills/compose-skill`.
  - `git ls-files | grep -v '^android/'`: only `.github/`, `AGENTS.md`, `CLAUDE.md`, `README.md`,
    `docs/`.
  - `cd android && ./gradlew assembleDebug test`: BUILD SUCCESSFUL, 105 tests, 0 failures.
  - Generated debug `golemio_api_key_base64` resource is non-empty, so `RepositoryModule` selects
    the live Golemio repositories in a debuggable build.
  - `git check-ignore android/local.properties` matches `android/.gitignore`.
  - `git log --follow android/app/build.gradle.kts` checked after commit.

## Task 3: Root `.gitignore`

- Builder (shared run with Task 4) created the root `.gitignore`: `.DS_Store`, `/.idea/`,
  `/.claude/worktrees/`, plus a comment pointing to the platform ignore files.
- Orchestrator verification: after `cd android && ./gradlew assembleDebug`, `git status` showed no
  build artifacts. `git check-ignore -v`: `android/local.properties` → `android/.gitignore:16`,
  `android/build` → `android/.gitignore:12`, `.claude/worktrees/x` → `.gitignore:3`.

## Task 4: Android CI with Change Detection

- `git mv .github/workflows/build.yml .github/workflows/android.yml`; added the `changes` job
  (native `git diff`, base from `pull_request.base.sha` or `before`, event values passed via
  `env:`) and gated `build` with `if:`, `working-directory: android`, and the setup-java
  `cache-dependency-path` entries. Action majors unchanged (`checkout@v4`, `setup-java@v4`).
- Orchestrator fix: the builder piped `git diff | grep -q` under `set -o pipefail`, which can
  report a match as no match when `grep` exits early and `git diff` gets SIGPIPE. Changed to
  capture the diff first, then `grep` a here-string.
- Orchestrator verification (detection step extracted from the YAML and run locally):
  - `a0f8b40..53bda72` (docs-only) → `android=false`
  - `53bda72..HEAD` (Android renames) → `android=true`
  - all-zero base, empty base, unreachable base → `android=true`
- `actionlint` is not installed; Ruby YAML parse passes. The GitHub PR run is checked in Phase 2.

## Task 5: Layered Agent Context

- Builder (shared run with Task 6): `git mv AGENTS.md android/AGENTS.md` and edited it (paths
  relative to `android/`, commands run from `android/`, kotlin-lsp root `android/`, Compose section
  → `compose-expert` skill, repo framing removed); new root `AGENTS.md` with the seven sections;
  root `CLAUDE.md` without `.agents/`; new `android/CLAUDE.md`; `docs/ai/README.md` with the
  historical-paths note; role doc fixes in `implementation-orchestrator.md` (Gradle from
  `android/`, `compose-expert` skill) and `planner.md` (`../../README.md`).
- Orchestrator fixes: moved the routing sentence in `android/AGENTS.md` below the H1; rewrapped
  an over-long line in `implementation-orchestrator.md`.
- Orchestrator verification: `grep -rn '\.agents/' AGENTS.md CLAUDE.md android/AGENTS.md
  docs/ai/agents` returns nothing; `cd android && ./gradlew assembleDebug` and `./gradlew test`
  pass from the root as written; root `AGENTS.md` has no build rules, `android/AGENTS.md` has no
  repository-wide policy.

## Task 6: README Split

- New `android/README.md` (Technology, Architecture, Project Structure rooted at `android/`,
  Getting Started, Command-Line Tasks from `android/`, Testing and Quality, Android data/dev
  setup). Root `README.md` keeps the product sections and adds Platforms, the layout tree, the
  `parity.md` link, and a data and privacy summary.
- Links to `docs/product/parity.md` and `docs/api/golemio.md` resolve after Task 7. The Android
  README still links the Golemio report at its old path; Task 7 updates it.
