# Nadoumi — Platform Architecture (authoritative direction)

**Date:** 2026-09-02
**Status:** APPROVED product + architecture direction. Every implementation
decision must move toward this. Supersedes narrower framing in older docs where
they conflict; those docs are reconciled section-by-section as each domain lands.

Nadoumi is an **enterprise international-education platform**, not a
scholarship-management app. Three experiences over one business platform:

| Experience | Surface | Audience / auth | May read |
| --- | --- | --- | --- |
| **Public website** | `nadoumi-web` (SSR) | anonymous | public catalog DTOs + `v_scholarship_student` **only** |
| **Student portal** | `nadoumi-web` `/dashboard`, `/onboarding` | `sys_user` `user_type` 10/20/30 | grant-scoped student DTOs (`/api/student/**`) |
| **Administration** | `nadoumi-admin` (:8082) | `sys_user` `user_type='00'` | `/api/staff/**` — RBAC + confidentiality DTOs |

`ruoyi-ui` (:1024) is a frozen RuoYi reference. No Nadoumi code there.

---

## 1. Core mental model

```
Student Account (sys_user, user_type 10/20/30)
  └─ manages ─> Managed Students        (nad_user_applicant_access grants)
        └─ Student Profile              (nad_applicant + children)
Universities ─< University Programs ─< (majors, intakes)
Scholarships  (configurable: category, funding, eligibility, fees, stipend,
               intakes, document requirements)
Applications  = a student's attempt at one opportunity
  ├─ profile snapshot        (captured at submit — never mutates)
  ├─ requirement snapshot    (the requirements that applied at submit)
  ├─ workflow / stages / decisions / tasks / submissions / notes / events
  ├─ documents
  ├─ admission
  └─ payments / fees
Communication   Notifications   Operational processing   Reporting
```

Administration provides the people, permissions, workflows, finance,
partnerships, content, communication, monitoring and operational capability to
run the business.

---

## 2. What is already correct (keep, build on)

- `sys_user` (account) ≠ `nad_applicant` (person). Better than the old Prisma
  `Student` which fused account + person + profile.
- `nad_user_applicant_access` — "apply for another student" / "My Students" is a
  real domain (grant, role, status, expiry, capability overrides, interim owner
  + invite), with a server-side predicate (`@na.canAccessApplicant`). Not a UI
  switch.
- Application as a business case: append-only `stage_history / event / decision /
  submission / note / task / link`, versioned workflow, engine-only
  `current_status`, `@Version` optimistic lock.
- Confidentiality split: `nad_scholarship` vs `nad_scholarship_internal`;
  `nad_partnership` as its own confidential context; `v_scholarship_student` as
  the only student-readable object; named CI leak tests.
- Document first-class: owner always set, immutable versions, events,
  requirements; bytes in object storage.
- Notification model: `nad_notification` + per-channel `nad_notification_delivery`
  (`UNIQUE(notification_id, channel)`, retry) + preference + per-locale template.
- Money/temporal discipline: `decimal(14,2)` + ISO-4217, `datetime`, named FKs,
  `ON DELETE RESTRICT`.
- Modular monolith on Spring Boot + RuoYi + MySQL + Flyway; one module per real
  bounded context; no empty scaffold modules.
- `nadoumi-admin` static nav manifest (only implemented modules render), `ui/*`
  component library, Orange-500 design system.
- Email-first identity, OTP ownership check, real password reset, session
  revocation, single frontend error-mapping.

## 3. What must change

| # | Problem | Fix |
| --- | --- | --- |
| C1 | Public navbar has `Programs` + `Destinations` as top-level product areas. | Navbar = **Home · Scholarships · Universities · About · Contact**. Programs are reached **through a university detail page**. Remove `/programs` and `/destinations` as discovery routes. |
| C2 | Onboarding modelled as `/dashboard/onboarding`. | Onboarding is a **separate profile-completion workflow** at `/onboarding`. Post-registration → `/onboarding`; only after `onboarding_state = COMPLETE` → `/dashboard`. Never labelled a dashboard. |
| C3 | `nad_application` stores only pointers → historical reads use *today's* profile + scholarship config. | At submit, capture `nad_application_profile_snapshot` (identity/contact/family/education as submitted) and `nad_application_requirement*` (copy of the document requirements that applied). Live config used only while `DRAFT`. |
| C4 | Scholarship = thin text CRUD (`benefits/eligibility/requirements` free text). | Small aggregate: `nad_scholarship` + `nad_scholarship_eligibility` (age min/max, nationalities, accepted countries, current-location / in-China, GPA, IELTS/TOEFL/Duolingo/HSK/CSCA, other) + `nad_scholarship_fee*` (typed line item: TUITION_BEFORE/AFTER, ACCOMMODATION_BEFORE/AFTER, REGISTRATION, APPLICATION, NADOUMI_APPLICATION, NADOUMI_SERVICE, INSURANCE, VISA, OTHER — each `decimal` + currency) + `nad_scholarship_stipend` (nullable: amount, currency, frequency, duration, conditions) + `nad_scholarship_intake*` + `nad_scholarship_document_requirement*` + `nad_scholarship_category` (extensible reference: CSC, GOVERNMENT, PROVINCIAL, UNIVERSITY, PRESIDENTIAL, LANGUAGE, TYPE_A/B/C/D, SELF_FUNDED, OTHER) + `funding_model` (FULLY / PARTIAL / SELF) + `teaching_language` (ENGLISH / CHINESE / BILINGUAL) + `available_slots`, publish/featured/hot flags. Prose (`benefits`, `policy`, `special_notes`) stays as text. |
| C5 | `nad_university` is a thin catalog row. | Additive V10: `name_cn`, `banner_document_id`, `province`, `type` (PUBLIC/PRIVATE), `founded_year`, `total_students`, `international_students`, `faculty_count`, `introduction`, `history`, `campus_info`, `accommodation_info`, `nearby_info`, `official_website`, `admissions_email`, `office_phone`, `is_recommended`, `is_featured`, `publish_status`. List-shaped: `nad_university_highlight*`, `nad_university_ranking*` (source, rank, year), `nad_university_gallery*` (`document_id`, caption). `is_partner` is **derived** from an ACTIVE `nad_partnership`, never a public column. |
| C6 | Program lacks type + majors; risk of becoming a top-level product. | `nad_program.program_type` (LANGUAGE / NON_DEGREE / DIPLOMA / BACHELOR / MASTER / PHD), `nad_program_major*`. Programs belong to a university and are exposed only via the university experience. |
| C7 | Notification trigger is in-process `ApplicationEventPublisher` — an event is lost if the app dies between commit and listener. | `nad_outbox_event` (aggregate_type, aggregate_id, type, payload_json, status PENDING/PROCESSING/DONE/FAILED, retry_count, created_at, processed_at, last_error). Domain services write the outbox row **in the same transaction** as the state change; a poller drains it → `NotificationService` + SSE fan-out. `nad_notification_delivery` still handles per-channel at-least-once after that. See `docs/DOMAIN_EVENTS.md`. |
| C8 | Docs lag the code (`DOMAIN_MODEL §1` says "no Nadoumi entity exists"; `DATABASE_DESIGN` sketches predate shipping). | Reconcile section-by-section as each domain lands; this doc is the umbrella. |

## 4. What is missing (not yet designed/built)

Applicant/Student profile depth (gender, residence, family / parent / sponsor,
academic summary, language quals, photo) · application + requirement snapshots ·
scholarship configurable sub-domain · university profile expansion · program
majors · `nad_outbox_event` · **Payment/Finance** (`nad_payment` fee kinds,
`nad_invoice`, `nad_refund`, period read models) · **Employee** ops profile +
application assignment/work queue · **CMS** (`nad_content_block` / feature slots
for the homepage) · public university-detail + scholarship-detail pages ·
public **Apply Now → self / other student → prefilled application** · Student
Dashboard as a real workspace · `ContactInquiry` / `PartnershipInquiry` capture ·
authoritative domain-event catalogue.

## 5. What to leave untouched

RuoYi internals (`sys_*`/`gen_*`, auth pipeline, RBAC, Quartz, Druid, code-gen) ·
`nadoumi-common` / `nadoumi-identity` (access model, capability matrix,
`MailSender`, OTP/ticket) · the shipped Applicant + University modules (extend,
don't replace) · the `nadoumi-admin` shell + `ui/*` + design tokens · `ruoyi-ui`
(frozen) · the shipped auth / OTP / password backend + `nadoumi-web` auth pages.

## 6. Architecture rules (apply to every step)

1. **Never an entity on the wire.** Per-audience `record` request/response DTOs.
   Explicit field mapping.
2. **Confidentiality is structural**, not field-hiding: student/public queries
   read student-safe tables/views only; internal linkage + commercial terms live
   in separate tables behind separate `nad:*:internal` / `nad:partnership:*`
   tokens.
3. **Historical accuracy**: a submitted application never changes because a
   profile or a scholarship config changed later. Snapshot at submit.
4. **Events survive a crash**: state change + `nad_outbox_event` row in one
   transaction; poller dispatches.
5. **Money** = `decimal(14,2)` + ISO-4217 currency column. **Never** float.
6. **Deletion**: business rows are archived (`status`), not `DELETE`d; `ON DELETE
   RESTRICT` on FKs. (`nad_university` DELETE stays only until programmes /
   partnerships FK it, then it becomes archive-only.)
7. **Concurrency**: `@Version` on aggregates that staff and the engine both
   mutate (Application). Don't sprinkle locking elsewhere without a reason.
8. **Audit**: RuoYi `@Log` on staff mutations + append-only `*_event` tables for
   business-significant changes.
9. **No fake data.** A metric with no real source shows "not available yet".
10. **Modules on demand.** A new `nadoumi-modules/<x>` appears only in the change
    that adds its first entity + migration + endpoint + test.

## 7. Module plan

```
nadoumi-common      shared SPIs, PageResponse, outbox (or its own module), events
nadoumi-identity    student auth, OTP, access grants, capability matrix, mail
nadoumi-applicant   nad_applicant + education/test_score/contact  (+ profile depth)
nadoumi-university   nad_university (+ profile depth), ranking/highlight/gallery
nadoumi-program     nad_program (+ type, majors), nad_program_intake
nadoumi-scholarship nad_scholarship aggregate + nad_scholarship_internal
nadoumi-application nad_application + snapshots + wf_* (+ workflow engine)
nadoumi-document    nad_document + version + event + requirement + storage
nadoumi-communication nad_conversation + participant + message + attachment
nadoumi-notification  nad_notification* + templates + SSE   (consumes outbox)
nadoumi-partnership  nad_partnership + contact + event       (CONFIDENTIAL)
nadoumi-payment     nad_payment + invoice + refund
nadoumi-finance     read models: revenue / expenses / earnings / outstanding
nadoumi-content     nad_content_block / feature slots (CMS)
nadoumi-reporting   rm_* read models for the operational dashboard
```

## 8. Implementation sequence

Each step = migration(s) + module + `/api/staff` + admin screen(s) + student/public
surface where relevant + tests. TDD; `mvn clean verify` + admin
lint/typecheck/test/build green per step; the owning doc section updated in the
same change. Report per step.

| Step | Scope | Migration |
| --- | --- | --- |
| **0 ✅** | **This doc** + fix outright-wrong stale lines in `DOMAIN_MODEL` / `DATABASE_DESIGN`; add `docs/DOMAIN_EVENTS.md` stub. | — |
| **1 ✅** | Public nav → `Home · Scholarships · Universities · About · Contact`; drop `/programs` + `/destinations` routes; onboarding → `/onboarding` + completion gate. Web only. | — |
| **2 ✅** | University profile depth (18 scalar cols) + `nad_university_ranking` / `nad_university_highlight`; admin sectioned drawer + detail; `PublicUniversityResponse` + `/api/public/universities` (published+active only); public `universities/[id]` page. | V10 |
| **2d ✅** | `nad_university_gallery` — up to 6 campus-life / dormitory / campus-view images with captions, edited whole as a third university child list; admin drawer Gallery section + detail grid; rendered on `universities/[id]` in place of the old placeholder. | V18 |
| **2b ✅** | Public website build-out: real Home / Scholarships / Universities / About / Contact on the Tailwind marketing shell. Contact slice — `nad_contact_inquiry` + anonymous rate-limited `POST /api/public/contact` (persist + notify support inbox), in `nadoumi-identity` until `nadoumi-content` exists. | V11 |
| **2c** | **Public website redesign** (`docs/PUBLIC_WEBSITE_REDESIGN.md`). R1: design-system layer + shell + Home (**PR-1 ✅**) then Universities list + detail + About + Contact (**PR-2 ✅**). Backend: university list filters (`UniversitySearch`); `nad_contact_inquiry` split-name + phone + category. | V14 |
| **4 (R3) ✅** | `nadoumi-scholarship`: configurable aggregate + `nad_scholarship_internal` split + `v_scholarship_student` view; staff CRUD (student-safe) + gated `/{id}/internal`; faceted anonymous discovery (`/api/public/scholarships` + `/facets` + `/{slugOrId}`); web **table** + detail + Home carousels; confidentiality CI tests passing. **Done before R2 at the user's request.** | V16 DDL + V17 seed |
| **3 (R2) ✅** | `nadoumi-program`: `nad_program` (belongs to one university, ON DELETE RESTRICT) + majors + intakes; university link validated / name resolved via `UniversityService` (no cross-module join). Staff `/api/staff/programs` CRUD + admin `/programs` list + `ProgramDrawer` + on-university management. Public `/api/public/programs` + `/programs/{id}` + `/universities/{id}/programs` (both programme AND university must be PUBLISHED + ACTIVE). Web: `ProgramCard`, real Programmes list on `universities/[id]`, Home "Hot programmes" carousel, `programs/[id]` detail. `mvn verify` + admin + web lint/typecheck/test/build green. | V19 DDL + V20 seed |
| **P1 ✅** | *(inserted ahead of Step 7, landed 2026-09-03/04.)* **Media / file storage layer** (`docs/superpowers/specs/2026-09-03-media-storage-and-application-engine-design.md` Part I): new foundational module `nadoumi-media` (peer of `nadoumi-identity`, DM8) + `MediaStorageService`/`MediaGateway` SPI in `nadoumi-common`, `CloudinaryMediaStorage` impl — **Cloudinary supersedes the original D5 MinIO/S3 provider choice (DM1)**, its SPI principle retained. `nad_media_asset` + `nad_media_access_log`; catalog imagery (university logo/banner/gallery, scholarship hero/cover, programme image) retrofitted onto it with a one-release `*_image_url` deprecation fallback; applicant profile photo (`APPLICANT_PHOTO`, PROTECTED) as the first protected-media use, proving the signed-URL path end to end. Reason for landing early: **Step 6's** JW202/admission attachments and **Step 7's** document versions both consume it. Docs updated: `ARCHITECTURE.md`, `DEPLOYMENT.md`, `DATABASE_DESIGN.md`, `DOCUMENT_MANAGEMENT.md`, `API_DESIGN.md`, `SECURITY.md`, `FRONTEND_ARCHITECTURE.md`, `ADMIN_ARCHITECTURE.md`. | V26–V29 |
| **5** | `nad_outbox_event` + poller + `nadoumi-notification` (`nad_notification*` + templates + SSE). First event: `ScholarshipPublished`. | next free (≥ V30) |
| **6** | `nadoumi-application`: `nad_application` + profile/requirement snapshots + append-only children + `nad_wf_*` (one default definition). Public **Apply Now** (self / other student, prefilled, review, submit→snapshot). Student portal: my applications. Admin: application workbench + status change → event → notify. **Consumes `nadoumi-media`** (P1) for JW202 / admission-document attachments via `MediaGateway`. | next free (≥ V30) |
| **7** | `nadoumi-document`: version/event/requirement; **consumes `nadoumi-media`** — `nad_document_version.media_asset_id` FK to `nad_media_asset` (P1), replacing the originally-planned raw `storage_key`; access-class + SENSITIVE-proxy doc-type overrides (`PASSPORT`/`VISA`/`FINANCIAL_PROOF`/`TRANSCRIPT`/`POLICE_CLEARANCE`) per `docs/DOCUMENT_MANAGEMENT.md` §3.2a; per-application checklist from the requirement snapshot; wires into onboarding + applications. | next free (≥ V30) |
| **8** | `nadoumi-communication`: conversations + messages + attachments on an application; SSE. | next free (≥ V30) |
| **9** | `nadoumi-payment` + `nadoumi-finance`: fee kinds + invoice + refund; application-fee gate on the workflow; real dashboard finance aggregates. | next free (≥ V30) |
| **10** | Employee ops + assignment surface → CMS (incl. `nad_contact_inquiry` triage, move to `nadoumi-content`) → Reporting depth. | next free (≥ V30) |

Student Dashboard (workspace: profile completion, applications + pending actions,
documents, notifications, communication, relevant opportunities, "My Students"
switcher) is filled in progressively across Steps 6–9.

**Migration numbering (updated after P1):** `V26`–`V29` are applied (media
layer, §P1 row above). Migration numbers are assigned at creation time, not
reserved in advance — the **next new migration is `V30`**, whichever of Steps
5–10 lands next. `docs/DATABASE_DESIGN.md` §6 carries the authoritative ledger.
