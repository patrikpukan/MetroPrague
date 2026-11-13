# Repository Guidelines

## Repository Map

- `android/` contains the Kotlin, Jetpack Compose, and Gradle Android app.
- `ios/` contains the SwiftUI and Xcode iOS app.
- `docs/product/` contains shared feature behavior, platform status, and product decisions.
- `docs/api/` contains shared API research.
- `docs/ai/` contains agent roles, plans, reports, and workflow guidance.
- `fixtures/` contains platform-neutral test inputs.

## Platform Routing

- Before changing platform code, read that platform's `AGENTS.md`.
- Work only inside the assigned platform directory.
- Read the other platform only when a plan or spec points to a specific file for reference.
- Never copy idioms across platforms, including dependency injection, state containers, UI
  components, or naming.

## Shared Knowledge

- Feature behavior lives in `docs/product/features/`.
- Platform status lives in `docs/product/parity.md`.
- Product decisions live in `docs/product/decisions.md`.
- Golemio API facts live in `docs/api/golemio.md`.
- Shared test inputs live in `fixtures/`.
- When implementation finds a fact or decision that applies to both platforms, write it in the
  shared doc rather than in a platform code comment.

## No-Lockstep Policy

Feature specs in `docs/product/features/` describe target behavior. Each platform's status lives in
`docs/product/parity.md` and uses `not started`, `in progress`, `done`, `behind spec`, or `n/a`. A
platform PR never waits for the other platform. When a spec changes and one platform has not
implemented the change, mark that platform `behind spec` in `parity.md`.

Each feature spec has two required sections: **Must match** for data semantics, business rules,
formatting, request pacing, reload rules, persistence keys, and accessibility semantics; and
**Platform-native** for navigation, components, typography, and motion. Reviewers enforce only
**Must match** across platforms.

## Feature Workflow

1. Write or update the shared spec in `docs/product/features/` and record decisions in
   `docs/product/decisions.md`.
2. Create a platform plan at `docs/ai/plans/<feature>-<platform>-plan.md` that cites the spec's
   **Must match** rules.
3. Implement in one platform per PR.
4. Update that platform's row in `docs/product/parity.md` in the same PR.

## Conventions

- Use Conventional Commits with platform scopes: `feat(android):`, `fix(ios):`,
  `docs(shared):`, `ci:`, and `chore:`.
- Keep one platform per PR, except shared-doc-only PRs.
- Use release tags `android-vX.Y.Z` and `ios-vX.Y.Z`.

## Secrets

Never commit API keys or other secrets. Keep each platform's local configuration file gitignored.
