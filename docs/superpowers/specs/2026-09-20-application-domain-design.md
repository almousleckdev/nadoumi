# Nadoumi — Application Domain — Design Status & Implementation Addendum

Status: **ADDENDUM to an existing approved baseline**. Author: Claude Sonnet 5.
Date: 2026-09-20.

> This is **not** a new design. A complete, detailed design for the Application
> domain already exists and is still architecturally valid — see §1. This document
> verifies it against the current codebase, updates the parts that have gone stale
> since it was written, and resolves/refreshes its open questions so it is ready to
> hand to `writing-plans` without re-litigating decisions already made.

---

## 1. The authoritative design (read these first)

| Document | Status | Covers |
| --- | --- | --- |
| `docs/superpowers/specs/2026-09-03-media-storage-and-application-engine-design.md` **Part II** (§II.1–II.12) | BASELINE, approved, **Part I already shipped** (P1 ✅) | Module boundary, full DDL (`nad_wf_*`, `nad_application*`), `WorkflowService` API, guard evaluator, `activate` validity checks, staff/student/public API surface, snapshot strategy, authorization model, test plan, doc updates |
| `docs/APPLICATION_WORKFLOW.md` | BASELINE, approved | Why a data-driven config engine (not BPMN/Flowable/Camunda — D4), the definition/instance model, requirements traced to `CLAUDE.md` §9–§10 |
| `docs/WORKFLOW_PROGRAM_WITH_SCHOLARSHIP.md` | BASELINE, Phase 1 approved | The concrete first workflow definition: 13 stages, 15 transitions, 19 task templates, document-requirement list |
| `CLAUDE.md` §9, §10 | Source requirement | Application = business case (Applicant/Opportunity/Stage/Status/Assignment/Tasks/Documents/Notes/Events/Decisions/Messages/History, auditable); workflow must not be a hard-coded global enum |

**Nothing in this addendum overrides those.** Where this addendum is silent, the
2026-09-03 spec's Part II governs.

## 2. Verified still accurate (2026-09-20 recheck)

- No `nad_application`, `nad_wf_*`, or `workflow_definition_code` table/column exists
  anywhere in `ruoyi-admin/src/main/resources/db/migration/` — Part II is exactly as
  unbuilt as the spec assumed. Nothing else has landed in this space to conflict with it.
- `nadoumi-modules/` today: `applicant, common, finance, hr, identity, media,
  notification, program, scholarship, university` — `nadoumi-application` slots in
  the same way `nadoumi-scholarship` does (mirrors its `domain/`, `domain/enums/`,
  `mapper/`, `service/` (public + admin split), `web/` + `web/request` + `web/response`
  layout — confirmed by reading `ScholarshipAdminService`/`ScholarshipMapper`/
  `StaffScholarshipController` directly).
- **Part I (media) is fully built and ready to consume**, ahead of what Part II
  assumed:
  - `MediaCategoryPolicy` already has rules for `APPLICATION_DOCUMENT`,
    `ADMISSION_DOCUMENT`, `JW202`, `OTHER_ATTACHMENT`, `APPLICANT_DOCUMENT`, and
    `APPLICANT_PASSPORT` (a category the spec didn't even anticipate — passport
    already ships through the media layer, PROTECTED, 10 MB, `raw`). The Application
    module can start using these categories immediately for admission/JW202
    attachments; no Part I follow-up work is needed.
  - Open item #5 from the spec's §6 ("applicant photo access class") is **RESOLVED**:
    `APPLICANT_PHOTO` is PROTECTED in production code today, as recommended.
- **The authorization hook this module must implement already exists and is
  load-bearing** — this is the one integration point the original spec names only
  generically ("checks `NadoumiAccessService`") without spelling out the exact
  contract. Confirmed by reading the actual implementation:
  - `com.nadoumi.common.access.ApplicationApplicantResolver` — a one-method SPI
    (`Long applicantIdOf(Long applicationId)`) already defined in `nadoumi-common`,
    with its Javadoc stating plainly: *"The Application slice provides the
    implementation; until then external callers are denied application-scoped
    access."*
  - `NadoumiAccessServiceImpl` (`nadoumi-identity`) already injects
    `Optional<ApplicationApplicantResolver>` and already calls
    `canAccessApplication(applicationId, capability)` — today it always resolves to
    `false` for external callers because the `Optional` is empty (no bean registered).
  - `UserApplicantAccessMapper.findActiveApplicationGrant(userId, applicantId,
    applicationId)` already supports **per-application grants**, not just
    applicant-wide ones (`nad_user_applicant_access.application_id`, nullable, set by
    V3). This is more granular than CLAUDE.md §8 states explicitly and already
    matches DA5 ("on-behalf-of… Apply Now").
  - **Action for the implementer:** register a
    `@Component class ApplicationApplicantResolverImpl implements
    ApplicationApplicantResolver` in the new `nadoumi-application` module (backed by
    `ApplicationMapper.findApplicantId(applicationId)`), and add it as a Spring bean —
    `NadoumiAccessServiceImpl`'s constructor already accepts it via `Optional`, so
    application-scoped authorization activates automatically the moment this module
    exists. No change needed in `nadoumi-identity`.
- `NotificationType.APPLICATION_SUBMITTED` / `APPLICATION_STATUS_CHANGED` + their `en`
  templates are already seeded (migration V55, per `docs/DOMAIN_EVENTS.md`), waiting
  for exactly the producer this module builds. Payload contract already fixed:
  `recipientUserIds` + `applicationRef, opportunityTitle, firstName` (submitted) /
  adds `status` (status changed) — the spec's `ApplicationTransitioned` Spring event
  → outbox write must emit this exact shape.

## 3. What's stale and needs updating

- **Migration numbering.** The spec reserved V29–V33; the real next-unused number is
  now **V63** (last applied: V62, `nad_applicant_welcomed`). Renumber straight across,
  same content and same order:
  - `V63__nad_wf_definition.sql` (was V29)
  - `V64__nad_application.sql` (was V30)
  - `V65__nad_application_menu_seed.sql` (was V31)
  - `V66__nad_wf_program_scholarship_v1_seed.sql` (was V32) — also adds
    `nad_program.workflow_definition_code` / `nad_scholarship.workflow_definition_code`
    per the original spec.
  - `V67__nad_wf_program_only_v1_seed.sql` (was V33)
  - **New, not in the original spec:** `V68__nad_uaa_application_fk.sql` — add
    `constraint fk_uaa_application foreign key (application_id) references
    nad_application(id) on delete restrict` to `nad_user_applicant_access`. The
    column has existed since V3 with no FK (nothing to point at yet);
    `database.md`'s table-design checklist requires the explicit `ON DELETE`
    behaviour now that the target table will exist. `RESTRICT` matches the FK style
    already used for `fk_uaa_user`/`fk_uaa_grantor` on the same table.
- `WORKFLOW_PROGRAM_WITH_SCHOLARSHIP.md`'s own header still says seed rows land "as a
  Phase 3 Flyway migration (`V7`/`V8` region)" — update that note to V66/V67 in the
  same PR that implements this (doc-sync, not a design change).
- `PLATFORM_ARCHITECTURE.md` §8 still lists Applications under "Still PLANNED" for the
  admin console row and "no producer yet" for the notification rows — flip both once
  P2 (backend) ships.

## 4. Workflow mechanism — reaffirming the recommendation

Independently re-evaluated per this session's directive (propose 2–3 approaches):

1. **Single linear stage list per opportunity type, hard-coded in Java** — rejected:
   violates CLAUDE.md §10 directly ("do not hard-code the entire process into one
   global enum") the moment a second programme needs a different flow.
2. **Data-driven config engine — `nad_wf_definition/stage/transition/
   stage_task_template` rows + a small `WorkflowService` with a fixed-dispatch guard
   evaluator (no expression language)** — **this is DA1/what the existing spec
   already chose**, and remains the right call: it satisfies §10's
   Workflow/Definition/Instance/Stage/Transition/Task/Assignment shape exactly,
   stays inside YAGNI (no interpreter, no plugin system), and the `activate()`
   validity checks (§II.4) give a fail-closed guarantee that a bad definition can
   never go live — including "reject if a guard predicate has no registered
   provider," which is precisely how DA3 lets the Document/Payment domains stay
   deferred without leaving a silently-broken guard in production.
3. **A general BPMN engine (Flowable/Camunda/Spring Statemachine)** — rejected, same
   as the spec's D4: no such dependency exists on the classpath, it's a large
   operational surface (a new runtime, its own persistence, its own failure modes)
   for two workflow definitions, and CLAUDE.md §18 explicitly warns against
   "premature generic frameworks."

**Recommendation: keep DA1 as designed.** No change from the existing spec.

## 5. Scope / size (what P2 actually is)

Per the existing spec's phasing (§3), unchanged:

- **P2 — Application engine (backend only, no UI):** one new module
  (`nadoumi-application`), 5 migrations (V63–V67, plus the new V68 FK above), ~11
  tables/entities (`WfDefinition`, `WfStage`, `WfTransition`, `WfStageTaskTemplate`,
  `Application`, `WfInstance`, `ApplicationStageHistory`, `ApplicationEvent`,
  `ApplicationDecision`, `ApplicationTask`, `ApplicationSnapshot`), one
  `WorkflowService`, staff API (9 endpoints), 9 backend test classes already named in
  §II.11. This is a real but bounded, single-module slice — not a rewrite of anything
  existing.
- **P3 — Public Apply Now + student portal** and **P4 — Admin workbench** are
  frontend-only follow-ups against P2's API, each its own PR per the spec's phasing
  table.

## 6. Decisions required (carried forward + refreshed)

From the original spec's §6, re-checked:

| # | Item | Status |
| --- | --- | --- |
| 1 | `nadoumi-media` module name | **RESOLVED** — shipped as-is. |
| 2 | `/api/media/{id}` indirection for PUBLIC images, DTOs carry direct `secure_url` | Still open, but does not block Application work — Application's media use is PROTECTED/SENSITIVE categories only (documents), which never go through this endpoint. Defer to whoever revisits Part I. |
| 3 | Deprecated `*_image_url` columns — drop after one release? | Same as above — irrelevant to Application. Not this module's decision to make. |
| 4 | **`PROGRAM_ONLY_V1` intake requirement** — is `intake_id` mandatory? | **Still open — needs your call.** Spec recommends yes (same as the combined type), for a simple reason: every seeded `FIELD_SET` guard and the admin filter UI assume `intake_id` is always present once submitted. Making it optional for one application type only would need a second `submit` guard variant. Recommend confirming "yes, mandatory" to keep one guard rule. |
| 5 | Applicant photo access class | **RESOLVED** — PROTECTED, confirmed live in `MediaCategoryPolicy`. |
| 6 | *(new)* Migration renumbering V63–V68 above | Not really a decision, just needs a sign-off that the renumbering is correct before `writing-plans` locks it in. |
| 7 | *(new)* `fk_uaa_application` retroactive FK (V68) | Confirm — recommend yes, closes an existing gap flagged by `database.md`'s own checklist, low risk (column has been `NULL` for everyone until now, nothing to migrate/backfill). |

## 7. Next step

This addendum plus the existing 2026-09-03 spec Part II is implementation-ready.
Recommended: `writing-plans` for **P2 only** first (backend, no UI, as the spec's own
phasing already prescribes) — it is the dependency root for the Document and
Messaging domains being designed in parallel this session, so it should land before
either of those two are implemented (not necessarily before they're *designed*).

---

*End of addendum. Authoritative content lives in the 2026-09-03 spec; this file adds
only what changed or was newly verified on 2026-09-20.*
