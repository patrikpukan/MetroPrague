# Monorepo Migration Guide

This guide explains how to orchestrate the move from a single Android repository to a monorepo
with two native apps. The implementation details are in
[`plans/monorepo-migration-plan.md`](plans/monorepo-migration-plan.md). This guide covers the order
of work, who does each step, the gates between steps, and what happens after merge.

## End State

```text
MetroPrague/
├── AGENTS.md / CLAUDE.md   # shared rules, repo map, platform routing, no-lockstep policy
├── README.md               # product overview, platforms, layout
├── docs/
│   ├── product/            # features/*.md (Must match / Platform-native), parity.md, decisions.md
│   ├── api/golemio.md      # platform-neutral API research
│   └── ai/                 # agents/, plans/, reports/, this guide
├── fixtures/golemio/       # API samples shared by both test suites
├── android/                # complete Gradle project + AGENTS.md, CLAUDE.md, README.md
├── ios/                    # added by the next plan (ios-scaffold-plan.md)
└── .github/workflows/
    └── android.yml         # change-detected; ios.yml arrives with the iOS scaffold
```

## Principles

1. **Two projects, one repository.** There is no shared code and no shared build. Each platform
   builds and ships on its own.
2. **Shared context, platform code.** Behavior, decisions, API facts, and fixtures are written once
   at the root. Code and idioms stay native to each platform.
3. **No lockstep.** Specs describe the target behavior, and `parity.md` records where each platform
   stands. One platform being behind never blocks a merge on the other.
4. **Agents stay scoped.** An agent loads the root `AGENTS.md` plus the `AGENTS.md` of its
   platform, and works inside one platform directory.

## Phases

| Phase | Who | Where | Output |
| --- | --- | --- | --- |
| 0. Preflight | Owner + orchestrator | Main checkout | Clean baseline, worktrees removed (plan Task 1) |
| 1. Execute | Orchestrator → builders | `chore/monorepo-migration` | One commit per plan Task 2–9 |
| 2. Review and PR | Reviewer, then owner | Same branch | One PR to `main` |
| 3. Post-merge local | Owner | Local machine | IDEs re-rooted, `MetroPrague-ios` deleted |
| 4. GitHub settings | Owner | GitHub | Required checks, labels |
| 5. Next | Planner | — | `ios-scaffold-plan.md` |

### Phase 0: Preflight

- Work happens in the **main checkout** on `chore/monorepo-migration`, with **no worktrees**. Every
  task moves or edits overlapping paths, so isolation would only add merge pain.
- Plan Task 1 removes all four linked worktrees: the three in `.claude/worktrees/` and the
  Antigravity one in `~/.gemini/antigravity/worktrees/MetroPrague/`. Their work is finished,
  including the uncommitted changes in `pr7-review-bug-fix` and the unmerged
  `departure-seconds-m3e-typography-plan.md`. Nothing is archived.
- Close Android Studio on the old root before starting. Gradle daemons and IDE indexing fight a
  `git mv` of the project.

### Phase 1: Execute

Run the `implementation-orchestrator` role on the plan. These overrides apply to this run:

- **Strictly sequential.** One builder at a time, no parallel tasks.
- **Commit after each accepted task.** This overrides the role's default "do not commit". Use a
  Conventional Commit per task, for example `chore: move Android project into android/`,
  `ci: change-detected Android workflow`, or `docs(shared): add product feature specs`.
- **Task 2 is the hard gate.** Nothing else starts until `cd android && ./gradlew assembleDebug
  test` passes with the baseline test count.
- **Gradle commands run from `android/`** from Task 2 onward. The role docs say otherwise until
  Task 5 fixes them, so state this in every builder brief.
- The progress log goes to `docs/ai/reports/monorepo-migration-implementation.md`.

Starter brief for the orchestrator:

> Implement `docs/ai/plans/monorepo-migration-plan.md` following
> `docs/ai/agents/implementation-orchestrator.md` and `docs/ai/monorepo-migration-guide.md`
> (Phase 1 overrides). Work on branch `chore/monorepo-migration` in the main checkout. Do not
> create worktrees. Run tasks strictly in order and commit after each accepted task. Stop and
> escalate if Task 2's build gate fails, if any Android source file shows content changes, or if
> a task needs a product decision the plan does not make.

**Stop conditions:** escalate to the owner, and do not work around:

- a red baseline in Task 1;
- a Task 2 build or test failure, or a test-count change;
- any non-rename diff under `android/app/src`;
- a spec rule in Task 7 that cannot be traced to code, tests, or docs;
- CI behaving differently from the plan's expectation.

### Phase 2: Review and PR

1. A `reviewer` pass over `git diff main -M` against the plan. Focus areas:
   - renames are 100% similarity;
   - no dead links or `.agents/` references;
   - the CI change-detection logic;
   - every *Must match* rule is traceable.
2. Push the branch and open **one PR**, `chore: migrate to monorepo (android/ + shared context)`.
   The PR body links the plan and this guide and lists the stop conditions that were checked.
3. Confirm on the PR that `Android CI / build` ran and passed. The diff touches `android/`, so it
   must run.
4. Merge with a **merge commit or rebase, not squash**. Either keeps the per-task commits and the
   rename history intact for `git log --follow`.

### Phase 3: Post-merge Local Steps (Owner)

1. `git checkout main && git pull`.
2. Make sure `android/local.properties` exists with `sdk.dir` and `GOLEMIO_API_KEY`. Task 2 moved
   it, but check on every machine and clone.
3. Android Studio: open **`android/`** as the project, not the repo root. Re-select the run
   configuration if needed.
4. kotlin-lsp: set the workspace or project root to `android/`, and let the Gradle import finish.
5. Agent sessions for Android work: either start in `android/`, or start at the root and point the
   agent at `android/AGENTS.md`. The root `AGENTS.md` routes it there either way.
6. Delete the now-unused local folder `~/Developer/pers/MetroPrague-ios`. It is empty. Any future
   iOS work happens in `MetroPrague/ios/`.
7. Optional: delete merged local branches (`claude/*`, `docs/*`, `feat/golemio`,
   `station_departures_search_screen`) once you no longer need them.

### Phase 4: GitHub Settings (Owner)

- Branch protection on `main`: make `Android CI / build` a required check. A skipped run counts as
  passing, so docs-only PRs are not blocked. Add `iOS CI / build` once it exists.
- Labels: `android`, `ios`, `shared`, `ci`.
- Update the repository description to mention both platforms.

### Phase 5: Next Plan

Continue with [`quickstart-ios.md`](quickstart-ios.md), which holds the iOS decisions and steps. Its
first step is for the planner to write `docs/ai/plans/ios-scaffold-plan.md`, covering:

- `ios/` with an Xcode project for iOS 26+, SwiftUI, `@Observable`, and Swift Testing;
- a gitignored `.xcconfig` for the Golemio key;
- `ios/AGENTS.md`, `ios/CLAUDE.md`, and `ios/README.md`;
- `ios/.gitignore`;
- `.github/workflows/ios.yml` with the same change-detection pattern (`ios/`, `fixtures/`, its own
  workflow file) on a macOS runner;
- iOS tests consuming `fixtures/golemio/`.

After that, iOS features are ported one spec at a time from `docs/product/features/`.

## Working in the Monorepo Afterwards

**Feature flow.** Each step can happen on its own timeline per platform.

1. Spec: create or update `docs/product/features/<feature>.md` and log any decision in
   `decisions.md`. This is a shared-doc PR.
2. Plan per platform: `docs/ai/plans/<feature>-android-plan.md` and/or `<feature>-ios-plan.md`.
   Each plan cites the spec's *Must match* rules it implements.
3. Implement: one platform per PR. The same PR updates that platform's row in `parity.md`.
4. Review: the reviewer checks the *Must match* rules and the fixtures. *Platform-native* choices
   are judged only against that platform's conventions.

**When a spec changes and only one platform follows:** the PR that changes the spec marks the other
platform `behind spec` in `parity.md` with a one-line note. Nothing else is required.

**Context hygiene for agents:**

- Give each agent one platform directory as its working scope.
- Point it to specific files in the other platform only when they are the reference for a rule. For
  example, the Android `GolemioDepartureBoardParser` is the reference when porting the parser.
- Shared facts discovered during implementation go into `docs/`, not into code comments in one
  platform.

## Follow-ups

Known issues that this migration deliberately leaves unfixed. Remove an item once it is resolved.

- [ ] **Compose skill is broken in the repo.** Before the migration, `.agents/skills/compose-skill` was
  tracked as a gitlink with no `.gitmodules`, so no clone could populate it, and `AGENTS.md`
  pointed at `.agents/skills/compose-expert/skills/compose-expert/SKILL.md`, which never existed.
  The migration removes the gitlink and points `android/AGENTS.md` and the agent role docs at the
  globally installed `compose-expert` plugin skill. That only works on machines and agents with the
  plugin installed. Decide whether to vendor the skill into `android/.agents/skills/`, add it back
  as a proper submodule, or document the plugin as a setup requirement in `android/README.md`.
- [ ] **Agent role docs are Android-flavoured.** `docs/ai/agents/*.md` only got path fixes. Make
  them platform-neutral before the first iOS feature plan.
- [ ] **Android tests use inline JSON.** Switch them to read `fixtures/golemio/` so both platforms
  test against the same files.

## Rollback

- Before merge: abandon or reset the branch. `main` is untouched.
- After merge: `git revert -m 1 <merge-commit>` restores the old layout in one commit. Then move
  `android/local.properties` back to the root and reopen the root in Android Studio.

## Known Risks

| Risk | Mitigation |
| --- | --- |
| Agents mixing platform idioms or loading both codebases | Root routing rules, platform-scoped briefs, shared facts in `docs/` |
| Required check never reporting on docs-only PRs | Job-level `if:` skip instead of workflow `paths:` filters |
| Pressure to keep platforms in lockstep | `parity.md` statuses, one platform per PR, `behind spec` instead of blocking |
| Stale `app/...` paths in old docs | Historical-paths note in `docs/ai/README.md` |
| Lost local config (`local.properties`) on other machines | Phase 3, step 2, and the `android/README.md` setup section |
| Squash merge flattening rename history | Merge commit or rebase only for the migration PR |
