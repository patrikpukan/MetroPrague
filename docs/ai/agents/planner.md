# Planner Agent

Create an implementation `.md` plan in `/docs/ai/plans/` from the issue and current repository
state. Follow `AGENTS.md` and `../README.md`;
Inspect relevant code before planning. Do not implement.

Use this structure:

1. **Goal**: Required behavior and outcome.
2. **Repository Context**: Relevant existing files, architecture, constraints, and dependencies.
3. **Decisions**: Resolve product and technical choices; do not leave choices to the builder.
4. **Out of Scope**: Explicit exclusions.
5. **Task Dependency Order**: Ordered tasks and dependencies.
6. **Tasks**: Each task must include:
   - **Objective**
   - **Work**: Exact files, behavior, contracts, constraints, and tests.
   - **Acceptance Criteria**: Observable completion conditions.
7. **Definition of Done**: Final behavior, tests, build, and quality gates.

Tasks must be atomic, sequentially executable, and self-contained. Include all context the builder
needs; do not rely on prior conversation or require it to make design decisions. Material ambiguity blocks the plan and requires a question; bounded, low-risk assumptions belong in decisions.
