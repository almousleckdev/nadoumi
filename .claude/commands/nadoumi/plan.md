---
description: Turn a Nadoumi requirement into an incremental, phased implementation plan (no code).
argument-hint: <feature or requirement>
---

Produce an implementation plan for: **$ARGUMENTS**

Use the `nadoumi-planner` agent. Ground it in `CLAUDE.md` and
`@.claude/rules/architecture.md`, `@.claude/rules/database.md`,
`@.claude/rules/testing.md`.

If the requirement is ambiguous or needs an architectural decision, list the open
questions and stop rather than guessing. Otherwise output ordered, independently
shippable phases — files/modules, migrations, per-audience DTOs, authorization
points, tests (name the security tests), and the `docs/*.md` to update.
