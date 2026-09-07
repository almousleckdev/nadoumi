---
description: General code review of a Nadoumi diff or file set (correctness, quality, tests, DB, docs).
argument-hint: [diff | file paths]
---

Run a code review.

Target: ${ARGUMENTS:-the current working tree changes (`git diff` against master)}

Use the `nadoumi-code-reviewer` agent against `@.claude/rules/code-quality.md`,
`@.claude/rules/testing.md`, `@.claude/rules/database.md`,
`@.claude/rules/git.md`.

Report Critical / Major / Minor with file:line, the problem, the failure it
causes, and a fix. End with approve / approve-with-changes / rework and the top 3
fixes. For a dedicated security or architecture pass, use `/nadoumi:sec-review`
or `/nadoumi:arch-review`.
