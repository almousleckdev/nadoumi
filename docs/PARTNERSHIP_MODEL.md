# Nadoumi — Partnership Model

Status: **BASELINE** · **EXISTING** · **PLANNED** · **OPEN**.

> Reconciled with Rev 3. Draft DDL: `docs/ddl/nad_core.draft.sql` §4 (dependency stub;
> the Partnership slice is finalized here and lands as a later Flyway migration).

---

## 1. What exists today (EXISTING)

Nothing. No university / partnership / program / scholarship entity. `sys_dept` is
Nadoumi's internal org tree only and is **not** used to scope any of these.

## 2. Core rule (CLAUDE.md §8)

`University != Partnership`.
- A **University** is public catalog; it may have **zero** partnerships.
- A **Partnership** is the confidential commercial relationship: contract, commission,
  contacts, status, internal notes.
- Scholarships may internally belong to a partner university; students never receive
  that linkage, and **which universities are partners is itself confidential**.
  Enforced server-side (`DOMAIN_MODEL.md` §6, `SECURITY.md` §5).

## 3. Entities (BASELINE)

```
nad_university            id, name, country, city?, website?, ranking_tier?,
                          logo_document_id?, status(ACTIVE|INACTIVE)      -- PUBLIC, no commercial field
nad_program               id, university_id, name, degree_level, field?, language?,
                          tuition_amount?, tuition_currency?, duration_months?,
                          workflow_definition_code?, status               -- PUBLIC
nad_program_intake        id, program_id, term, application_open?, application_close?, status

nad_partnership           id, university_id, status(DRAFT|ACTIVE|SUSPENDED|TERMINATED),
                          tier?, commission_model_json?, contract_start?, contract_end?,
                          agreement_document_id?, internal_notes?          -- CONFIDENTIAL
                          + generated active_guard  (INV4)
nad_partnership_contact   id, partnership_id, name, role?, email?, phone?  -- CONFIDENTIAL
nad_partnership_event     id, partnership_id, event_type, detail_json?, actor_user_id?, at  -- append-only

nad_scholarship           STUDENT-SAFE columns only (title, country, degree_level, field,
                          benefits, eligibility, requirements, deadline, binding, status)
nad_scholarship_internal  scholarship_id (PK=FK), university_id?, partnership_id?,
                          internal_status?, operational_notes?, confidential_terms?  -- CONFIDENTIAL
nad_scholarship_program   scholarship_id, program_id   (allowed programmes when binding='PROGRAM_BOUND')
v_scholarship_student     DB VIEW — safe columns of PUBLISHED scholarships only; the ONLY
                          object student/public mappers read
```

### Relationships & invariants

```
nad_university 1───0..*  nad_program 1───0..* nad_program_intake
nad_university 1───0..1  nad_partnership   (ACTIVE)   -- INV4: partial-unique via generated
                                                         active_guard; unlimited TERMINATED history
nad_partnership 1───0..* nad_partnership_contact
nad_scholarship 1───1    nad_scholarship_internal ──?→ nad_partnership , ──?→ nad_university
nad_scholarship 1───0..* nad_scholarship_program ──→ nad_program   (only if binding='PROGRAM_BOUND')
```

- **INV4 (fix G-P1):** at most **one `ACTIVE`** partnership per university. Historical
  `TERMINATED` rows are retained forever; partnerships are **never hard-deleted**.
- **`nad_partnership_country` — dropped for v1 (fix G-P2/decision).** A partnership's
  country is the university's country. Reintroduce only if a partnership genuinely
  spans multiple campuses/countries.
- **`nad_partnership_program` — OPEN, deferred.** Commission stays at partnership level
  in v1. Add this table (with `commission_override_json`) only when per-programme
  terms are a real requirement.
- **Commission ledger** (money earned per enrolment) is Reporting/Finance scope —
  forward reference `nad_commission_ledger`, not built now.

## 4. Confidentiality boundary (BASELINE — enforced + tested)

| Data | Public / Student | Staff (default) | Staff w/ `nad:scholarship:internal:view` | Staff w/ `nad:partnership:view` | Staff w/ `nad:partnership:terms:view` |
| --- | :---: | :---: | :---: | :---: | :---: |
| University name, country, programmes, public rankings | ✅ | ✅ | ✅ | ✅ | ✅ |
| Scholarship title / benefits / eligibility / deadline | ✅ | ✅ | ✅ | ✅ | ✅ |
| Scholarship → university / partnership linkage, internal/operational status | ❌ | ❌ | ✅ | ✅ | ✅ |
| Partnership existence, university, status, tier, contract dates, contacts | ❌ | ❌ | ❌ | ✅ | ✅ |
| Commission model, confidential terms, internal notes | ❌ | ❌ | ❌ | ❌ | ✅ |

Enforcement (all layers — see `DOMAIN_MODEL.md` §6.2):
1. Storage separation (`nad_scholarship` vs `nad_scholarship_internal` vs
   `nad_partnership`).
2. `v_scholarship_student` view — the only object student/public mappers touch.
3. Single choke-point `ScholarshipQueryService` (`findStudentView` / `findInternalView`,
   the latter `@PreAuthorize`-gated and in a staff-only module).
4. Type-safe `record` DTOs — `ScholarshipStudentView` has no field able to carry a
   confidential value; explicit field-by-field mapping.
5. Response-body denylist net on `/api/public/**` + `/api/student/**`.
6. Query-param guard (confidential sort/filter → 400).
7. Discovery facets / search index / exports built from the view only — **no facet
   groups scholarships or programmes by university partner status**.
8. **Partner-indistinguishability (fix G-P4):** a programme under a partner university
   is byte-for-byte identical on public APIs to one under a non-partner university.
   No `isPartner` flag, no partner badge, no partner-only scholarship list on a
   university page. "Partner universities" is a staff-only report
   (`nad:partnership:list`).
9. No `/api/public` or `/api/student` route returns any partnership field or any
   scholarship→university association — there is no such route at all.

CI-blocking: `ScholarshipConfidentialityTest`, `PartnershipExposureTest`,
`UniversityPartnerLeakTest` (`SECURITY.md` §9).

## 5. Lifecycle (BASELINE)

`Partnership`: `DRAFT → ACTIVE → (SUSPENDED ↔ ACTIVE) → TERMINATED`. Every transition
appends `nad_partnership_event`. `TERMINATED` does **not** delete the university or its
public programmes; it flips `nad_scholarship_internal.internal_status` for linked
scholarships. Whether a formerly partner-funded scholarship stays discoverable is a
business rule set on the scholarship's own `status` (`PUBLISHED` vs `CLOSED`), not on
the partnership — keeps the confidentiality boundary clean.

Permissions: `nad:partnership:create/edit/suspend` (partnerships_manager, ops_manager),
`nad:partnership:terminate` + `nad:partnership:terms:*` (partnerships_manager, and
`terms:view` for finance). See `docs/PERMISSION_CATALOGUE.md` §2.9.

## 6. Reporting (PLANNED)

Partnership performance (applications, admits, enrolments, commission accrued per
partnership / country / term) lives in Reporting read models, visible only to
`partnerships_manager` / `finance` / `read_only_analyst`
(`nad:report:partnership:view`).

## 7. Decision status

| Item | Status |
| --- | --- |
| Keep history of terminated partnerships (`TERMINATED`, never hard-delete) | **APPROVED** |
| At most one `ACTIVE` partnership per university (INV4) | **APPROVED** |
| `nad_partnership_country` | **DROPPED** for v1 (country = university's country) |
| Commission representation | **APPROVED** — `commission_model_json` at partnership level for v1 |
| `nad_partnership_program` (per-programme commission) | **OPEN** — deferred until needed |
| Are formerly partner-funded scholarships still publicly discoverable | **APPROVED** — governed by `nad_scholarship.status`, decided per scholarship, not by the partnership |
