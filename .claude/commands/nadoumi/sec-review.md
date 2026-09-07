---
description: Security-review a Nadoumi diff, endpoint, or module (authorization, confidentiality, DTO leakage, input, secrets).
argument-hint: [diff | endpoint | module path]
---

Run a security review.

Target: ${ARGUMENTS:-the current working tree changes (`git diff` against master)}

Use the `nadoumi-security-reviewer` agent against `@.claude/rules/security.md`
and `CLAUDE.md` §8/§13/§16.

Focus: server-side authorization on every changed endpoint; scholarship
confidentiality and its tests; DTO leakage; parameterized SQL; secrets; error
leakage; auditability; uploads. Report findings by severity with file:line and
the exploit/leak scenario. If a Critical is found, say so first. End with
go / no-go.
