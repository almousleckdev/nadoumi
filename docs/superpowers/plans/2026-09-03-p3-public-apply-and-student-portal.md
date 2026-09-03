# P3 — Public Apply Now + Student Portal — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: `superpowers:subagent-driven-development` or `superpowers:executing-plans`. Steps `- [ ]`.
> **Depends on P2** (`nadoumi-application` engine + staff API merged; migrations through V34).
> Adds **no migrations**. Task granularity = task + interfaces + test list; executor writes the RED/GREEN/commit micro-steps following P1's worked pattern.

**Goal:** Public "Apply Now" (self and on-behalf-of), the `submit` transition with profile/requirement snapshot, and the student "My applications" portal — API + `nadoumi-web` UI.

**Architecture:** New public/student controllers in `nadoumi-application` delegating to the existing `WorkflowService`. Authorization via `NadoumiAccessService` capabilities (`CREATE_APPLICATION`, `SUBMIT_APPLICATION`, `VIEW_APPLICATION`). Student-facing DTOs expose only safe fields — no internal notes, decision rationales, assignee identities, SLA data. `nadoumi-web` gets a multi-step apply flow and a portal under `/dashboard/applications`.

**Tech Stack:** Spring Boot 4.1 (backend), Nuxt 3 + Vue 3 + `@nuxtjs/i18n@9` (web), Vitest + `@nuxt/test-utils`.

**Spec:** `docs/superpowers/specs/2026-09-03-media-storage-and-application-engine-design.md` §II.7–II.8.

## Global Constraints

P1's "Global Constraints", plus:
- Student endpoints are `@Anonymous`-free — they require a student JWT (`/api/student/login`). Public draft-creation still requires auth (an applicant profile must exist).
- Every applicant-scoped call checks the acting identity's `ApplicantCapability` via `NadoumiAccessService` — `CREATE_APPLICATION` (start/edit draft), `SUBMIT_APPLICATION` (submit + student transitions), `VIEW_APPLICATION` (read). Cross-applicant access → `403`.
- Student timeline DTO: `stageCode`, human `statusLabel`, safe `event_type`s only. Never `nad_application_decision.rationale`, internal notes, `assignee_user_id`, `sla_hours`.
- `nadoumi-web`: SSR-safe; `@nuxtjs/i18n` key parity across `en`/`fr`/`ar`/`zh` enforced by `i18n-keys.test.ts` — add every new key to all four.
- Reuse the existing dashboard shell (`app/components/dashboard/DashboardShell.vue`, `ApplicantSwitcher.vue`) and `useApi()` / `useApplicant()` data layer.

## File Structure

**Modified — `nadoumi-application`**
- `web/PublicApplicationController.java` — `POST /api/public/applications`.
- `web/StudentApplicationController.java` — `GET`/`PUT`/`submit`/`transitions` under `/api/student/applications`.
- `web/response/StudentApplicationListItem.java`, `StudentApplicationDetail.java`, `StudentTimelineEntry.java` — safe DTOs.
- `service/WorkflowService.java` — add `studentTransition(...)` convenience wrapping `execute` with the capability lookup; `listForActor(Actor)`; `studentView(long appId, Actor)`.
- `service/StudentTimelineProjector.java` — maps events/history → safe entries; a whitelist of surfaced `event_type`s.

**Modified — `ruoyi-admin` tests**
- New: `ruoyi-admin/src/test/java/com/ruoyi/nadoumi/PublicApplyFlowTest.java`, `StudentPortalTest.java`, `OnBehalfOfApplyTest.java`.

**Modified — `nadoumi-web`**
- `app/pages/apply.vue` — the multi-step flow (`/apply?program=&scholarship=&intake=`).
- `app/components/apply/ApplyStepOpportunity.vue`, `ApplyStepProfileReview.vue`, `ApplyStepConfirm.vue`, `ApplyProgress.vue`.
- `app/pages/dashboard/applications/index.vue`, `app/pages/dashboard/applications/[id].vue`.
- `app/components/dashboard/ApplicationStatusChip.vue`, `ApplicationTimeline.vue`.
- `app/composables/useApplications.ts` — `list()`, `get(id)`, `startDraft(payload)`, `submit(id)`, `transition(id, code, reason?)`.
- `app/types/application.ts` — `ApplicationListItem`, `ApplicationDetail`, `TimelineEntry`, `ApplyDraftInput`.
- `app/components/marketing/*` + `scholarships/[slug].vue` + `programs/[slug].vue` — wire the "Apply Now" CTA.
- `server/api/public/[...path].ts` / a new `server/api/student/[...path].ts` BFF passthrough for `/api/student/*` (mirror the existing public BFF; forward the auth cookie/header).
- `i18n/locales/{en,fr,ar,zh}.json` — `apply.*`, `dashboard.applications.*`.
- `tests/unit/pages/apply.test.ts`, `tests/unit/pages/dashboard-applications.test.ts`, `tests/unit/composables/useApplications.test.ts`.

**Modified — docs**
- `docs/API_DESIGN.md` (public/student application endpoints), `docs/FRONTEND_ARCHITECTURE.md` (apply flow + portal), `docs/APPLICATION_WORKFLOW.md` (student-actor transitions surfaced), `docs/PLATFORM_ARCHITECTURE.md §8`.

---

## Task 1: `POST /api/public/applications` — start a draft (self + on-behalf-of)

**Files:** `web/PublicApplicationController.java`, `web/request/StartApplicationRequest.java` (`Long applicantId, ApplicationType applicationType, Long programId, Long scholarshipId, Long intakeId`), `service/WorkflowService` (reuse `startDraft`).
**Interfaces:**
- Auth: a student JWT. `actor = Actor.student(currentUserId, req.applicantId())`. `startDraft` internally calls `accessService.requireCapability(applicantId, CREATE_APPLICATION)` — which returns true for the user's own applicant profile **and** any applicant the user holds a live grant carrying `CREATE_APPLICATION` for.
- Response `201` `StudentApplicationDetail` (draft).
- Validation: `PROGRAM_ONLY` ⇒ `scholarshipId` null; both types ⇒ `programId` + `intakeId` set; `PROGRAM_WITH_SCHOLARSHIP` ⇒ `scholarshipId` set.
**Tests (IT):** `PublicApplyFlowTest` — own applicant → `201`, draft at START stage; `OnBehalfOfApplyTest` — grant holder → `201`; no grant → `403`; `PROGRAM_ONLY` + scholarship → `400`.
**Commit:** `feat(application): POST /api/public/applications — start draft (self + on-behalf-of)`.

---

## Task 2: Student read — list + detail + safe timeline

**Files:** `web/StudentApplicationController.java` (`GET /api/student/applications`, `GET /{id}`), `web/response/StudentApplicationListItem.java`, `StudentApplicationDetail.java`, `StudentTimelineEntry.java`, `service/StudentTimelineProjector.java`, `service/WorkflowService` (`listForActor`, `studentView`).
**Interfaces:**
- `GET /api/student/applications` — every application whose `applicant_id` the caller holds `VIEW_APPLICATION` for (across all their applicants). Fields: `id`, `applicationType`, `programName`, `universityName`, `scholarshipTitle?`, `intakeLabel`, `statusLabel`, `stageCode`, `nextActionHint`, `submittedAt?`, `updatedAt`.
- `GET /api/student/applications/{id}` — `VIEW_APPLICATION` for that applicant else `403`. `StudentApplicationDetail` = list fields + `timeline: StudentTimelineEntry[]` + `openActions: string[]` (student-executable transition codes available now) + `referenceCode?`.
- `StudentTimelineProjector` — surfaces only whitelisted `event_type`s (`APPLICATION_CREATED`, `APPLICATION_SUBMITTED`, `CASE_ASSIGNED`→"In review", `DOCUMENTS_REQUESTED`, `SUBMITTED_TO_UNIVERSITY`, `UNIVERSITY_OFFER`, `SCHOLARSHIP_AWARDED`, `OFFER_ACCEPTED`, `ENROLLED`, `APPLICATION_WITHDRAWN`, terminal-unsuccessful). Never rationale/notes/assignee.
**Tests (IT):** `StudentPortalTest` — list returns only the caller's applicants' apps; detail of another applicant's app → `403`; timeline of a mid-workflow app contains no rationale string / assignee id / "internal" text.
**Commit:** `feat(application): student applications list + detail with safe timeline`.

---

## Task 3: `submit` + snapshot; student transitions

**Files:** `web/StudentApplicationController.java` (`PUT /{id}`, `POST /{id}/submit`, `POST /{id}/transitions/{code}`), `service/WorkflowService.studentTransition`.
**Interfaces:**
- `PUT /api/student/applications/{id}` `{programId?, scholarshipId?, intakeId?}` — `CREATE_APPLICATION`; only while `current_status='DRAFT'`; re-validates the type/opportunity rules.
- `POST /api/student/applications/{id}/submit` — `SUBMIT_APPLICATION`; runs the `submit` transition (guard: opportunity `FIELD_SET`s) → `SnapshotAssembler` writes `PROFILE` + `REQUIREMENTS`; `submitted_at` stamped. Returns the updated detail.
- `POST /api/student/applications/{id}/transitions/{accept_offer|decline_offer|withdraw}` `{reason?}` — `studentTransition` looks up the capability the definition assigns to that transition (`SUBMIT_APPLICATION`), then `execute`. `withdraw` requires `reason` (→ `stage_history.reason`).
**Tests (IT):** `PublicApplyFlowTest` (extend) — edit draft → submit → both snapshots present, `current_status='IN_REVIEW'`; submit twice → second is a `400` (not in DRAFT); `withdraw` without reason → `400`; `accept_offer` from a non-`OFFER_RESPONSE` stage → `400`.
**Commit:** `feat(application): student submit (+snapshot) and accept/decline/withdraw transitions`.

---

## Task 4: `nadoumi-web` — student BFF + `useApplications` composable

**Files:** `server/api/student/[...path].ts`, `app/composables/useApplications.ts`, `app/types/application.ts`, `tests/unit/composables/useApplications.test.ts`.
**Interfaces:**
- `server/api/student/[...path].ts` — mirrors `server/api/public/[...path].ts` but forwards the auth header/cookie to `${backendBaseUrl}/api/student/...`.
- `useApplications()` → `{ list, get, startDraft, submit, transition }` using `useApi()`'s `studentGet` / `studentPost` (add these to `useApi` alongside `publicGet` if not present).
**Tests:** mocked `$fetch` — each method hits the right path/verb; errors propagate.
**Commit:** `feat(web): student applications BFF + useApplications composable`.

---

## Task 5: `nadoumi-web` — Apply Now flow

**Files:** `app/pages/apply.vue`, `app/components/apply/*`, `scholarships/[slug].vue` + `programs/[slug].vue` (CTA), `i18n/locales/*` (`apply.*`), `tests/unit/pages/apply.test.ts`.
**Interfaces / behaviour (spec §II.8):**
- CTA on scholarship detail → `/apply?scholarship=<slug|id>&program=&intake=`; on programme detail → `/apply?program=<slug|id>` with an optional "add a scholarship" toggle.
- Unauthenticated → redirect `/login?next=<encoded apply url>`.
- Step 1 `ApplyStepOpportunity` — applicant selector (from `useApplicant()` grants; hidden if only self), programme picker (constrained to the scholarship's university when arriving from a scholarship), scholarship picker / "no scholarship" (→ `PROGRAM_ONLY`), intake picker. On "continue" → `startDraft(...)` → holds the draft id.
- Step 2 `ApplyStepProfileReview` — read-only summary of the applicant profile pulled live (`useApplicant`), with "edit profile" deep-links to `/dashboard/profile`. A checklist of what will be snapshotted.
- Step 3 `ApplyStepConfirm` — final review + submit (`submit(draftId)`); on success → confirmation panel with the reference and a link to `/dashboard/applications/<id>`.
- `ApplyProgress` — 3-step indicator; back/next; guards incomplete steps.
**Tests:** step gating; arriving from a scholarship pre-selects + locks the university; `PROGRAM_ONLY` path when no scholarship; submit calls `submit` with the draft id; locale parity.
**Commit:** `feat(web): Apply Now multi-step flow (self + on-behalf-of)`.

---

## Task 6: `nadoumi-web` — student portal

**Files:** `app/pages/dashboard/applications/index.vue`, `app/pages/dashboard/applications/[id].vue`, `app/components/dashboard/{ApplicationStatusChip,ApplicationTimeline}.vue`, `i18n/locales/*` (`dashboard.applications.*`), dashboard nav entry, `tests/unit/pages/dashboard-applications.test.ts`.
**Interfaces / behaviour:**
- `/dashboard/applications` — `useApplications().list()`; card per application (status chip, opportunity line, `nextActionHint`, submitted date); empty state; grouped by applicant when the user has several.
- `/dashboard/applications/[id]` — `get(id)`; header (opportunity + status chip + reference), `ApplicationTimeline` (from `detail.timeline`), an "Actions" area rendering buttons for `detail.openActions` (`accept_offer` / `decline_offer` / `withdraw` — `withdraw` opens a reason field), calling `transition(id, code, reason?)` then refetching.
- Reuses `DashboardShell`; add "Applications" to the dashboard sidebar.
**Tests:** list renders without leaking staff fields; detail action buttons reflect `openActions`; `withdraw` requires a reason; locale parity.
**Commit:** `feat(web): student applications portal (list + detail + actions)`.

---

## Task 7: Docs + gate

**Files:** `docs/API_DESIGN.md`, `docs/FRONTEND_ARCHITECTURE.md`, `docs/APPLICATION_WORKFLOW.md`, `docs/PLATFORM_ARCHITECTURE.md §8`, `phase-status.md` memory.
**Steps:**
- [ ] `mvn -o -pl ruoyi-admin verify` (background) — `PublicApplyFlowTest`, `StudentPortalTest`, `OnBehalfOfApplyTest` green, no regressions.
- [ ] `cd nadoumi-web && pnpm run typecheck && pnpm run test` — apply + portal + composable + i18n-keys green.
- [ ] Docs updated; `PLATFORM_ARCHITECTURE.md §8` Step 6 → "public Apply Now + student portal ✅; admin workbench pending P4".
- [ ] Commit `docs: public Apply Now + student portal` and `chore(application): P3 gate green`.

---

## Self-review (authoring time)

- **Spec coverage:** `POST /api/public/applications` self + on-behalf (T1); student list/detail/safe-timeline (T2); `submit` + snapshot + student transitions (T3); web BFF + composable (T4); Apply Now flow (T5); portal (T6); docs + gate (T7). Matches spec §II.7–II.8.
- **Placeholders:** none — endpoints, DTO field lists, capability checks, and test cases are concrete. Micro-steps delegated per the header note.
- **Type consistency:** `StartApplicationRequest` shape shared T1↔T3 (edit reuses the opportunity fields). `Actor.student(userId, applicantId)` from P2 T9. `StudentApplicationDetail.openActions` (string codes) consistent T2↔T6. `useApplications` method set fixed T4, consumed T5/T6.
- **Dependency note:** `useApi()` may lack `studentGet`/`studentPost` — T4 adds them; if the login/session plumbing for the student JWT on the web side isn't in place from the earlier auth work, T4 must also wire the auth header forwarding (check `server/api/public/[...path].ts` + the existing login flow first).
