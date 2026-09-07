---
description: Design and write tests for a Nadoumi feature/module and close coverage gaps.
argument-hint: [feature | module path]
---

Add / strengthen tests for: ${ARGUMENTS:-the current working tree changes}

Use the `nadoumi-test-engineer` agent against `@.claude/rules/testing.md` and
`@.claude/rules/security.md`.

Cover service, controller/API, mapper, and frontend as applicable. Always include
the required security tests (scholarship confidentiality, applicant-access,
staff-only transitions). Run the affected test module and report real pass/fail
output. Finish with tests added, gaps closed, gaps remaining.
