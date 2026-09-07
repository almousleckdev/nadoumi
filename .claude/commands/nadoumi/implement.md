---
description: Implement an approved Nadoumi plan or a well-scoped change, with tests and doc updates.
argument-hint: <plan reference or change description>
---

Implement: **$ARGUMENTS**

Use the `nadoumi-implementer` agent. Load `CLAUDE.md` and
`@.claude/rules/architecture.md`, `@.claude/rules/code-quality.md`,
`@.claude/rules/security.md`, `@.claude/rules/testing.md`,
`@.claude/rules/database.md`, `@.claude/rules/git.md`.

Work to the plan; if an architectural fork appears that the plan does not cover,
stop and surface it. Write the tests (including the security tests) in the same
change, run the affected test module, update the relevant `docs/*.md`, and do not
commit unless asked. Finish with files changed, test output, docs updated, open
items.
