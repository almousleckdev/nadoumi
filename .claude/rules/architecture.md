# Architecture rules

Operationalizes `CLAUDE.md` §3–§16. Read this before any structural change.

## Foundation vs domain

- RuoYi-Vue + Spring Boot + Spring Security + MyBatis + MySQL + Maven is the
  **foundation**. Do not swap core technology without an approved decision record
  in `docs/`.
- Reuse RuoYi for auth, RBAC, users/roles/permissions/menus, logging, code
  generation, scheduled jobs, system admin.
- RuoYi's generic CRUD conventions do **not** define the Nadoumi domain. Model
  business capabilities deliberately.

## Module boundaries

- Backend is a **modular monolith** under `nadoumi-modules/` — one module per
  domain (`applicant`, `university`, `program`, `scholarship`, `application`,
  `document`, `workflow`, `partnership`, `communication`, `notification`,
  `content`, `payment`, `reporting`). Do not introduce microservices, a second
  datastore, or a second frontend framework without an approved `docs/` decision.
- A module owns its entities, mappers, services, DTOs, and controllers. Cross-
  module calls go through the other module's **service interface**, never its
  mapper or entities.
- Controllers are thin (validation + DTO mapping + delegation). Business logic
  lives in services. Persistence details stay in mappers.

## API surface

- Separate controllers/DTOs for **public**, **student**, and **staff/internal**
  audiences. Never return a persistence entity directly.
- A student-facing DTO must never carry an internal field just because the DB
  relation exists (see `security.md` — scholarship confidentiality).
- New endpoints follow the conventions already in `docs/API_DESIGN.md`; update
  that doc in the same change.

## Change management

1. State the problem. 2. State the proposed solution. 3. List affected modules.
4. List migration risks. 5. Update the relevant `docs/*.md`. 6. Implement
incrementally. 7. Run tests.

The live phase tracker is `docs/PLATFORM_ARCHITECTURE.md §8`. Keep it current.

## Frontend

- `ruoyi-admin` (Vue) — internal staff/operations UI.
- `nadoumi-web` (Nuxt 4) — public + student experience. SSR/SEO matters here;
  keep public pages server-rendered and locale-aware (default `en`, then `zh`).
- Document any new frontend architecture decision in
  `docs/FRONTEND_ARCHITECTURE.md`.

## Reference

Java structure and patterns: `.claude/rules/ecc/java/patterns.md`.
Backend patterns generally: ECC skills `backend-patterns`, `hexagonal-architecture`,
`jpa-patterns` (adapt to MyBatis), `springboot-patterns`.
