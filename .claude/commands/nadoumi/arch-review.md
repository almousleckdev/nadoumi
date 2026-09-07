---
description: Review a design, diff, or module against Nadoumi architecture rules.
argument-hint: [diff | module path | design summary]
---

Run an architecture review.

Target: ${ARGUMENTS:-the current working tree changes (`git diff` against master)}

Use the `nadoumi-architecture-reviewer` agent against `CLAUDE.md` §3–§16 and
`@.claude/rules/architecture.md`, `@.claude/rules/database.md`.

Report Critical / Major / Minor findings with file:line, the rule broken, the
risk, and a direction. End with approve / approve-with-changes / redesign.
