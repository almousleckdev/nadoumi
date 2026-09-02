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
| **0** | **This doc** + fix outright-wrong stale lines in `DOMAIN_MODEL` / `DATABASE_DESIGN`; add `docs/DOMAIN_EVENTS.md` stub. | — |
| **1** | Public nav → `Home · Scholarships · Universities · About · Contact`; drop `/programs` + `/destinations` routes; onboarding → `/onboarding` + completion gate. Web only. | — |
| **2** | University profile depth + ranking/highlight/gallery; admin detail sections; public university-detail DTO. | V10 |
| **3** | `nadoumi-program`: program type + majors + intake; Programs shown under the university (admin + public). | V11 |
| **4** | `nadoumi-scholarship`: configurable aggregate + internal split + `v_scholarship_student`; staff CRUD (confidentiality DTOs); public list + detail. Confidentiality CI tests. | V12 |
| **5** | `nad_outbox_event` + poller + `nadoumi-notification` (`nad_notification*` + templates + SSE). First event: `ScholarshipPublished`. | V13 |
| **6** | `nadoumi-application`: `nad_application` + profile/requirement snapshots + append-only children + `nad_wf_*` (one default definition). Public **Apply Now** (self / other student, prefilled, review, submit→snapshot). Student portal: my applications. Admin: application workbench + status change → event → notify. | V14–V15 |
| **7** | `nadoumi-document`: version/event/requirement + object storage; per-application checklist from the requirement snapshot; wires into onboarding + applications. | V16 |
| **8** | `nadoumi-communication`: conversations + messages + attachments on an application; SSE. | V17 |
| **9** | `nadoumi-payment` + `nadoumi-finance`: fee kinds + invoice + refund; application-fee gate on the workflow; real dashboard finance aggregates. | V18–V19 |
| **10** | Employee ops + assignment surface → CMS → Reporting depth. | V20+ |

Student Dashboard (workspace: profile completion, applications + pending actions,
documents, notifications, communication, relevant opportunities, "My Students"
switcher) is filled in progressively across Steps 6–9.
