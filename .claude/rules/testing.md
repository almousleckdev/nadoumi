# Testing rules

Operationalizes `CLAUDE.md` §17, §22.

## What must have tests

Every meaningful business feature. A feature that only compiles is not done.
For each feature evaluate: unit, service, controller/API, **authorization**, and
integration/database tests.

## Required security tests

- Scholarship confidentiality: prove an unauthorized student request cannot
  retrieve the university association or any confidential field (`security.md`).
- Applicant access: prove a user without an access grant cannot read or mutate
  another applicant's data.
- Application ownership: prove staff-only transitions reject non-staff callers.

## Backend (JUnit 5)

- Service tests: mock the mapper, assert business rules and authorization
  branches.
- Controller tests: `@WebMvcTest` / MockMvc, assert status codes, DTO shape
  (no leaked internal fields), and auth failures return 403/401 not 500.
- Repository/mapper tests: run against a real MySQL schema (Testcontainers or the
  configured test DB), not H2, when SQL is non-trivial.
- Name tests `should<Behaviour>_when<Condition>`. One logical assertion group per
  test. No shared mutable state between tests.
- Existing backend tests live under `ruoyi-admin/src/test/...` — follow the
  patterns in `StaffUniversityTest.java`.

## Frontend

- `nadoumi-web`: Vitest unit tests for page/composable logic; keep the existing
  `tests/unit/pages/*.test.ts` structure. Test locale fallbacks and that
  public pages render without a session.

## Workflow

- Prefer test-first for business logic and bug fixes (superpowers
  `test-driven-development`). Write the failing test, then the code.
- Run the relevant test module before declaring work done; report real output.
  If tests fail or were skipped, say so.

## Reference

`.claude/rules/ecc/java/testing.md`, `.claude/rules/ecc/common/testing.md`,
`.claude/rules/ecc/vue/testing.md`, `.claude/rules/ecc/python/testing.md`.
ECC skills: `springboot-tdd`, `junit-5` (installed), `test-coverage`,
`vitest` (installed).
