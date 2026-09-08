# Code quality rules

Operationalizes `CLAUDE.md` §18.

## Comments and language

- All comments, identifiers, log messages, and docs are **English only**.
- Short, purposeful comments explaining *why*, not *what*. No C#-style ceremony
  Javadoc on every getter.

## Java style

- Modern, clean Java. K&R braces. Records for immutable data carriers / DTOs.
- Small cohesive classes, explicit single responsibility. No god services.
- Constructor injection, `final` fields. No field injection.
- Transactional boundaries at the service method, deliberately scoped. Writes use
  `@Transactional(rollbackFor = Exception.class)`; reads use `readOnly = true`.
- Structured errors — typed exceptions mapped to consistent API responses; no
  swallowed exceptions, no bare `catch (Exception e)` that hides failures.
- No magic constants — name them.

## TypeScript / Vue / Nuxt

- Strict TypeScript. No `any` without a written reason.
- Components small and focused; composables for shared logic.
- Follow the conventions already present in `nadoumi-web/app/` and
  `ruoyi-admin/`.

## General

- Match the surrounding code's naming, structure, and comment density.
- No unnecessary abstraction, no premature generic framework, no duplicated
  business logic, no hidden side effects.
- Validate inputs at boundaries. Log at meaningful points, not everywhere.
- Delete dead code rather than commenting it out.

## Before declaring done

- Build passes. Lint/format clean. Types check.
- Relevant docs in `docs/` updated in the same change.
- No stray `console.log` / `System.out.println` / debug prints.

## Reference

`.claude/rules/ecc/java/coding-style.md`,
`.claude/rules/ecc/typescript/coding-style.md`,
`.claude/rules/ecc/vue/coding-style.md`, `.claude/rules/ecc/common/coding-style.md`.
ECC skills: `java-coding-standards`, `coding-standards`, `plankton-code-quality`.
