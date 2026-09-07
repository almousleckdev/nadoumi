---
description: Refactor Nadoumi code without changing behaviour, keeping tests green.
argument-hint: <target class/module and the smell to fix>
---

Refactor: **$ARGUMENTS**

Use the `nadoumi-refactorer` agent against `@.claude/rules/code-quality.md` and
`@.claude/rules/architecture.md`.

Confirm characterization tests exist first (add them, or hand to
`nadoumi-test-engineer`, if thin). One refactor type per pass; run the affected
test module after each step and revert any step that goes red. No behaviour
change, no API/schema change, no unrelated edits, no commit unless asked. Finish
with before/after shape and test output.
