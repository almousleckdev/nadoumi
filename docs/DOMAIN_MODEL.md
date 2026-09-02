# Nadoumi — Domain Model

Status legend: **BASELINE** = approved architecture, implement as written ·
**EXISTING** = present in this repository today · **PLANNED** = approved, not yet
built · **OPEN** = still needs an explicit decision.

> **Revision 3 — Phase 1 (Documentation Reconciliation & Final Design). APPROVED
> BASELINE.** Decisions D1, D2, D4, D5, D6, D7, D8, D9, D10, D11, D12, D13, D14 are
> approved and folded in here and across `docs/`. This document, `ARCHITECTURE.md`
> and `DATABASE_DESIGN.md` are the frozen baseline for Phase 2. Still **no tables
> created, no DDL executed, no business code** — Phase 1 is documentation only.
> Companion artifacts produced this phase: `docs/PERMISSION_CATALOGUE.md`,
> `docs/WORKFLOW_PROGRAM_WITH_SCHOLARSHIP.md`, `docs/ddl/nad_core.draft.sql` (draft,
> non-executable).
>
> **§3 (Identity) and §4 (User ↔ Applicant access) are IMPLEMENTED as of Phase 3** —
> see `docs/PHASE_3_IDENTITY_APPLICANT.md`. Migrations `V2`–`V4`, modules
> `nadoumi-identity` / `nadoumi-applicant`, the `@na` bean, and the `/api/student/*`
> + `/api/staff/applicants/*` surface are live and covered by CI-blocking tests.

---

## 1. What exists today (EXISTING)

Unchanged from Revision 1. The only implemented domain is RuoYi **system
administration**:

| Entity | Table | Purpose |
| --- | --- | --- |
| `SysUser` | `sys_user` | Authenticated platform identity. `password` BCrypt. `user_type varchar(2) default '00'` — **a dormant column: not read anywhere in the Java code today**. |
| `SysRole` | `sys_role` | Role + `data_scope` (1 all · 2 custom depts · 3 own dept · 4 dept+children · 5 self). |
| `SysMenu` | `sys_menu` | Tree of `M` directory / `C` route / `F` button. `perms` token e.g. `system:user:edit`. |
| `SysDept` | `sys_dept` | Nadoumi's **own** org tree (`ancestors` materialized path). Basis of staff data scope. |
| `SysPost`, `SysDictType/Data`, `SysConfig`, `SysNotice/NoticeRead`, `SysOperLog`, `SysLogininfor`, `SysJob/JobLog`, `GenTable/Column` | `sys_*`, `gen_*` | Job titles, enumerations, runtime config, announcements + read receipts, audit trails, Quartz schedules, code-gen metadata. |

Join tables: `sys_user_role`, `sys_role_menu`, `sys_role_dept`, `sys_user_post`.

Runtime fact confirming a design constraint below: on login, RuoYi caches a `LoginUser`
(`SysUser user` + `Set<String> permissions`) in Redis under `login_tokens:<uuid>` for
the token lifetime. **Authority data is snapshotted into that cache.** Anything that
must be revocable *immediately* (applicant access grants) therefore cannot live in
`LoginUser.permissions`.

**No Nadoumi entity, table, endpoint, or screen exists.**

---

## 2. Nadoumi bounded contexts (PLANNED)

Logical module boundaries — **not** microservices. Initial modular monolith
(`nadoumi-modules/*`, see `docs/DEVELOPMENT_GUIDELINES.md` §3).

### 2.1 Core delivery contexts

```
Identity & Access   sys_user (all humans) + nad_user_applicant_access (external authz)
Applicant           applicant profile, education history, test scores, contacts
                    → full onboarding (identity extras, residence, interests,
                      languages, work, certifications, photo/passport) is PROPOSED
                      in docs/APPLICANT_ONBOARDING.md — a later phase, not built now
University           public catalog entity (may exist with no partnership)
Program              degree programmes + intakes offered by a university
Scholarship          funding opportunity; STUDENT view vs INTERNAL view
Application          the business case (applicant + opportunity + lifecycle + history)
Document             business document (type, version, verification) over stored files
Workflow             workflow definition / instance / stage / transition; tasks
Partnership          Nadoumi ↔ university commercial relationship (confidential)
Communication        conversations, participants, messages, attachments, read state
Notification         multi-channel delivery (in-app, email, SMS, push, WhatsApp)
```

### 2.2 Business-platform contexts (PLANNED — full scope, sequenced by phase)

Each is a **separate** bounded context. Finance, Payroll, and Payments are distinct
domains that integrate through read models and domain events, never shared tables.
High-level entities, relationships, and per-context authorization boundaries are in
**`docs/ADMIN_ARCHITECTURE.md` §7**; sensitive-data rules in `docs/SECURITY.md` §10.

```
Employee            nad_employee (0..1 sys_user), assignments, employee documents,
                    emergency contacts — NOT salary
Finance             chart of accounts, double-entry ledger, invoices + lines,
                    expenses + categories, revenue, commission ledger
Payroll             effective-dated salary, payroll runs, payslips, adjustments
                    (most restricted context; self-service = own payslips only)
Payments            application-fee/deposit capture, provider transactions, refunds;
                    emits PaymentSettled / RefundIssued for Finance to post
Marketing / CMS     public pages, articles, FAQ, destination guides, media,
                    campaigns, captured leads
Reporting           rm_* read models / aggregates for the admin dashboard (§6 of
                    ADMIN_ARCHITECTURE) and audited exports — read-only, derived
```

**Not built in the current `nadoumi-web` build.** RuoYi-Vue3 (`nadoumi-admin/`) is
the Admin UI baseline for all of these (D1). These contexts do not reopen any
approved decision — they extend the planned module map.

---

## 3. Identity model — who is a "user" (BASELINE · D10 APPROVED)

### 3.1 One `sys_user` table for every authenticated human

**Recommendation:** all authenticated humans — Nadoumi staff, self-applying students,
authorized agents, guardians — are rows in `sys_user`. They are distinguished by
`sys_user.user_type` (today dormant), given concrete values:

| `user_type` | Meaning | Gets RuoYi roles/menus? | Authorization source |
| --- | --- | --- | --- |
| `00` | Staff / internal operator (existing default) | **Yes** — `sys_role`, `sys_menu`, data scope | RuoYi RBAC |
| `10` | Student / applicant-side self-service user | **No** | `nad_user_applicant_access` grants + resource rules only |
| `20` | External agent (submits on behalf of applicants) | **No** | `nad_user_applicant_access` grants only |
| `30` | Guardian / sponsor | **No** | `nad_user_applicant_access` grants only |

Rationale: reuses RuoYi authentication (BCrypt, captcha, lockout, JWT, Redis session)
with zero new auth code; keeps the admin RBAC surface (`sys_role`/`sys_menu`)
**staff-only**, so an external user can never be granted an admin permission by mistake.

### 3.2 Separate login endpoints (PLANNED)

- `/login` (existing) — **staff only**; rejects `user_type != 00`.
- `/api/student/login` (new) — external users only (`user_type in (10,20,30)`); rejects
  staff. Same `SysLoginService` pipeline; returns a student-shaped `getInfo`
  (no menus/roles; instead: accessible applicants + capabilities).

**Revision 2 — external identity is email-first.** For `user_type` 10/20/30,
`sys_user.user_name` is a **server-generated internal handle** (e.g. `stu_<slug>`),
never shown; the **verified `email`** (`sys_user.email_verified = 1`, set via an OTP
flow) is the login key and must be unique among externals (`idx_sys_user_email` +
a service-level uniqueness check). Staff identity (`user_type='00'`) is unchanged —
still `user_name` + RuoYi RBAC. See spec §15 / `docs/API_DESIGN.md` §4.1.

Belt-and-braces on top of RBAC: a student token presented to `/system/**` fails on
permissions anyway, but endpoint separation makes the boundary explicit and auditable.

### 3.3 D10 — APPROVED

Approved: (a) reuse `sys_user` for external users with the `user_type` values above,
(b) externals receive **no** `sys_role`/`sys_menu` rows, (c) a distinct
`/api/student/login` that rejects `user_type='00'` while `/login` rejects
`user_type != '00'`. The rejected alternative was a separate `nad_account` table +
parallel auth stack.

Implementation notes carried to Phase 2:
- `sys_user.user_type` is populated on every create path; a seed dict
  `nad_user_type` (`00/10/20/30`) backs the admin UI.
- Admin user-management screens (`/system/user/**`) filter `user_type='00'` by
  default so staff lists are not polluted by students/agents/guardians
  (risk R8). A separate `/api/staff/external-users/**` view lists externals.
- `getInfo` for an external user returns **no** `roles`/`permissions`; instead
  `{ accessibleApplicants: [{applicantId, accessRole, capabilities[]}], ... }`.

---

## 4. `User ↔ Applicant` access model (BASELINE · D7 APPROVED)

Core rule (CLAUDE.md §8): `User != Applicant`. A user is a login identity; an applicant
is the person an application is *about*. One user may act for **many** applicants; one
applicant may be reachable by **many** users.

### 4.1 The grant entity

`nad_user_applicant_access` — one row per (user, applicant) relationship:

| Column | Notes |
| --- | --- |
| `id` | PK |
| `user_id` → `sys_user` | the acting user |
| `applicant_id` → `nad_applicant` | the subject |
| `application_id` → `nad_application` (nullable) | if set, the grant is scoped to **one** application only (e.g. an agent hired for a single case) |
| `access_role` | `OWNER` \| `AGENT` \| `GUARDIAN` \| `VIEWER` |
| `status` | `PENDING` \| `ACTIVE` \| `REVOKED` \| `EXPIRED` |
| `invited_email` | for `PENDING` invites where the user row does not exist yet |
| `granted_by_user_id`, `granted_at` | audit |
| `revoked_by_user_id`, `revoked_at`, `revoke_reason` | audit (soft revoke — row retained) |
| `expires_at` (nullable) | time-bounded grants; a nightly job flips to `EXPIRED` |
| `capability_overrides_json` (nullable) | per-grant additions/removals to the role's default capability set (§4.2). Adding a privileged capability (e.g. `SUBMIT_APPLICATION` for a `GUARDIAN`) requires a staff approval carrying `nad:applicant:access:manage`. |
| `is_interim` (boolean, default false) | `true` on the transitional staff-held `OWNER` grant for a staff-created applicant (§4.3). |

Invariants:
- **Exactly one `ACTIVE` `OWNER` per applicant at all times** (partial-unique on
  `applicant_id` where `access_role='OWNER' AND status='ACTIVE'`).
- Partial-unique on `(user_id, applicant_id, application_id)` where `status='ACTIVE'`
  (a user holds at most one active grant per scope).
- A user cannot hold `OWNER` and another role on the same applicant simultaneously.

### 4.2 Roles → capabilities (default matrix)

Capabilities: `VIEW_PROFILE`, `EDIT_PROFILE`, `VIEW_APPLICATION`, `CREATE_APPLICATION`,
`SUBMIT_APPLICATION`, `UPLOAD_DOCUMENT`, `VIEW_DOCUMENT`, `MESSAGE_STAFF`,
`MANAGE_ACCESS`.

| Capability | OWNER | AGENT | GUARDIAN | VIEWER |
| --- | :---: | :---: | :---: | :---: |
| VIEW_PROFILE / VIEW_APPLICATION / VIEW_DOCUMENT | ✅ | ✅ | ✅ | ✅ |
| EDIT_PROFILE | ✅ | ✅ | ✅¹ | ❌ |
| CREATE_APPLICATION | ✅ | ✅ | ❌¹ | ❌ |
| SUBMIT_APPLICATION | ✅ | ✅ | ❌¹ | ❌ |
| UPLOAD_DOCUMENT | ✅ | ✅ | ✅ | ❌ |
| MESSAGE_STAFF | ✅ | ✅ | ✅ | ❌ |
| MANAGE_ACCESS (grant/revoke others) | ✅ | ❌ | ❌ | ❌ |

¹ **D7 sub-decision — FINALIZED.** Guardian defaults: `VIEW_*`, `EDIT_PROFILE`,
`UPLOAD_DOCUMENT`, `MESSAGE_STAFF`. **No** `CREATE_APPLICATION` / `SUBMIT_APPLICATION`
/ `MANAGE_ACCESS`. For a guardian of a minor who must submit, a staff member with
`nad:applicant:access:manage` adds `SUBMIT_APPLICATION` via `capability_overrides_json`
on that grant. Country-specific age-of-majority rules that would change this default
remain **OPEN** pending legal review — the safe default (no submit) ships first.

Staff never use grants: a `user_type='00'` user is authorized by RBAC permission
(`nad:applicant:*`) plus data scope / case assignment, evaluated separately.

### 4.3 Lifecycle

- **Creation of an applicant → OWNER:**
  - Self-registration (`user_type='10'` registers, then creates an applicant "about
    me"): the registering user becomes `OWNER`.
  - **Staff-created applicant (D7 sub-decision — FINALIZED):** in the same operation
    the service creates **two** rows: (a) a transitional `OWNER` grant held by the
    **creating staff user** — `status='ACTIVE'`, `is_interim=true`,
    `expires_at = now + 30 days`; and (b) a `PENDING` `OWNER` **invite**
    (`invited_email` required, no `user_id`). When the invitee registers/authenticates
    and accepts, the engine atomically promotes the invite to `ACTIVE OWNER` and sets
    the interim grant to `REVOKED` (`revoke_reason='ownership_transferred'`). A Quartz
    job escalates unaccepted invites at 7 / 14 / 30 days. At `expires_at` with no
    acceptance the applicant is flagged `UNLINKED` (`nad_applicant.status='UNLINKED'`):
    it stays staff-owned and readable, but student-side submission is blocked until an
    owner is linked. **No synthetic/service principal is introduced** — the interim
    owner is always a real, audited staff user.
- **Delegation:** only an `ACTIVE OWNER` (or staff with `nad:applicant:access:manage`)
  creates `AGENT` / `GUARDIAN` / `VIEWER` grants, via direct user-id or email invite.
  Optional `expires_at`. Configurable cap on number of active delegates.
- **Ownership transfer:** an `OWNER` may transfer ownership to a user who currently
  holds any `ACTIVE` grant on the applicant. Atomic + audited: new user → `OWNER`,
  previous owner → chosen role or `REVOKED`. Cannot leave the applicant ownerless.
- **Revocation:** `OWNER` (or staff) sets a non-owner grant to `REVOKED`
  (`revoked_by/at/reason`). The `OWNER` grant is revoked **only** as part of a
  transfer. Revoked rows are never deleted (audit).
- **Immediate effect:** the authorization predicate reads **current** grant state from
  `nad_user_applicant_access` on every request. Grants are **not** copied into the
  JWT or `LoginUser.permissions` (which are Redis-cached for the token lifetime).
  A revoke takes effect on the revoked user's very next request.

### 4.4 Authorization predicate (enforced server-side, defense in depth)

`@na.canAccessApplicant(applicantId, capability)` and
`@na.canAccessApplication(applicationId, capability)`:

1. Load the caller. If `user_type='00'` (staff): allow iff they hold the mapped RBAC
   permission **and** the resource is within their data scope / case assignment.
2. Else (external): find an `ACTIVE`, non-expired grant for `(caller, applicantId)`
   (or the application's applicant), optionally narrowed by `application_id`. Allow iff
   `capability` is in that `access_role`'s capability set (§4.2), applying any
   per-grant overrides.
3. Deny → `403` (never `200` with filtered data; never leak existence beyond `404`
   where appropriate).

Enforcement layers, all three mandatory:
- `@PreAuthorize("@na.canAccessApplication(#id,'VIEW_APPLICATION')")` on the controller;
- an explicit re-check inside the service method;
- **every** applicant-/application-scoped MyBatis query takes an explicit `authScope`
  parameter (accessible applicant IDs / "is staff + scope clause"); no unscoped finder
  is exposed to a controller.

### 4.5 D7 — APPROVED

Approved as written: the grant entity shape (incl. `capability_overrides_json`,
`is_interim`), the 4 roles + capability matrix, the "exactly one ACTIVE OWNER"
invariant, the finalized guardian defaults (§4.2 note ¹), the finalized interim-owner
flow for staff-created applicants (§4.3), the per-application grant scoping, and live
(non-cached) revocation. Only country-specific guardian-of-minor legal nuance stays
**OPEN** (safe default ships first).

---

## 5. What an Application targets (BASELINE · D9 APPROVED)

CLAUDE.md §9 calls the target an "Opportunity". Nadoumi must support three real
shapes, so the opportunity is **not** a single polymorphic FK:

| `application_type` | `program_id` | `scholarship_id` | `intake_id` | Meaning |
| --- | :---: | :---: | :---: | --- |
| `PROGRAM_ONLY` | **required** | null | required | Admission to a specific programme, self-funded. |
| `PROGRAM_WITH_SCHOLARSHIP` | **required** | **required** | required | Admission **and** a funding request, decided together. |
| `SCHOLARSHIP_LED` | optional | **required** | optional | The scholarship drives placement (e.g. a government scholarship that assigns the university/programme later). Programme + intake may be filled in during the workflow. |

- **`nad_program`** belongs to a **`nad_university`** and has **`nad_program_intake`**
  rows (term, open/close dates). An application always resolves a *target university*
  either directly (`program.university_id`) or, for `SCHOLARSHIP_LED` with no
  programme yet, via `nad_application.target_university_id` (nullable, may be set
  during the workflow) or "to be determined".
- A **`nad_scholarship`** is either `program_bound` (tied to specific
  universities/programmes — enforced: the chosen `program_id` must be in the
  scholarship's allowed set) or `open` (student picks any eligible programme).
- Invariants enforced in the DB and service:
  `PROGRAM_ONLY` ⇒ `program_id NOT NULL AND scholarship_id IS NULL`;
  `PROGRAM_WITH_SCHOLARSHIP` ⇒ both NOT NULL;
  `SCHOLARSHIP_LED` ⇒ `scholarship_id NOT NULL`.
- **One application = one type + one primary opportunity.** A student pursuing the
  same programme with two different scholarships files **two** applications; a
  `nad_application_link` (`related_application_id`, `relation`) records that they are
  siblings so staff see them together.

**D9 — APPROVED.** The three-type discriminator + nullable
`program_id`/`scholarship_id`/`intake_id` + invariants INV6–INV8 (DATABASE_DESIGN
§5.5) is the baseline. The alternatives (single polymorphic opportunity FK;
programme-always-required) are rejected. The first concrete process for the
`PROGRAM_WITH_SCHOLARSHIP` type is specified in
`docs/WORKFLOW_PROGRAM_WITH_SCHOLARSHIP.md`.

---

## 6. Scholarship / University confidentiality — enforcement model (BASELINE)

Requirement (CLAUDE.md §8): a student must never learn which university/partnership a
scholarship is tied to, nor which universities are Nadoumi partners. Table separation
alone is **not** sufficient; the following layers are all required and all tested.

### 6.1 What is confidential

| Data | Public / Student | Staff (default) | Staff w/ `nad:scholarship:internal:view` or `nad:partnership:view` |
| --- | :---: | :---: | :---: |
| University name, country, city, programmes, public rankings | ✅ | ✅ | ✅ |
| Scholarship title, country, degree, field, benefits, eligibility, requirements, deadline | ✅ | ✅ | ✅ |
| Scholarship → university / programme linkage | ❌ | ❌ | ✅ |
| Scholarship → partnership, internal status, commission, operational notes | ❌ | ❌ | ✅ |
| Partnership existence / terms / contacts / which universities are partners | ❌ | ❌ | ✅ |
| "Partner" badges, partner-only scholarship lists on a university page | ❌ | ❌ | ✅ |

### 6.2 Enforcement layers

1. **Storage separation.** `nad_scholarship` holds student-safe columns only.
   `nad_scholarship_internal` (PK = FK `scholarship_id`) holds `university_id`,
   `partnership_id`, internal status, operational notes, confidential terms.
   `nad_partnership` is entirely confidential.
2. **A read-only DB view** `v_scholarship_student` projecting **only** the safe
   columns of `nad_scholarship`. The student/public MyBatis mapper reads the **view**,
   never the base tables — a coding error cannot select an internal column that isn't
   in the view.
3. **Single choke-point service.** `ScholarshipQueryService` exposes
   `findStudentView(...)` (returns `ScholarshipStudentView`) and `findInternalView(...)`
   (returns `ScholarshipInternalView`, guarded by
   `@PreAuthorize("@na.canViewScholarshipInternal()")`). Student-facing controllers
   are in a module that **cannot** reference the internal method (compile-time
   boundary).
4. **Type-safe DTOs.** `ScholarshipStudentView` is a Java `record` whose components
   physically cannot carry `universityId` / `partnershipId` / internal notes.
   Mapping is explicit field-by-field; **no** `BeanUtils.copyProperties` from an
   entity, no reflection copy. A unit test asserts the record's component set against
   an allow-list.
5. **Response denylist net.** A response advice on `/api/public/**` and
   `/api/student/**` inspects serialized scholarship/university bodies for forbidden
   keys (`universityId`, `university`, `partnershipId`, `partnership`, `internalNotes`,
   `commission`, `operationalNotes`, …); a hit aborts the response with `500` and logs
   a security event. Catches accidental leakage through nested objects.
6. **Query-parameter guard.** Student endpoints reject `sort` / `filter` /
   `groupBy` params that name a confidential field (`400`, not `200` with a leak).
7. **Derived data.** Search index documents, discovery facets ("browse by country /
   field / degree"), CSV/PDF exports, and any caching layer are all built from the
   student view — never from the base tables. No facet groups scholarships by
   university.
8. **University pages.** The public university representation has no `isPartner` flag
   and no partner-only scholarship listing; "partner universities" is a staff-only
   report.
9. **Logging.** Student-side controllers never log full scholarship entities.

### 6.3 Tests (CI-blocking) — see `docs/SECURITY.md` §9

`ScholarshipConfidentialityTest`, `PartnershipExposureTest`, `UniversityPartnerLeakTest`:
enumerate every public/student scholarship & university route (list, detail, search,
facets, export) with an external token and assert no forbidden key in body **or**
headers; assert direct internal routes → `403`; assert confidential sort/filter → `400`.

---

## 7. Aggregate map (BASELINE, indicative — realized DDL in `docs/ddl/nad_core.draft.sql`)

Contradictions from Revision 1 are resolved here (FK directions, single task table,
no array columns, derived-vs-stored status).

```
sys_user (all humans; user_type discriminates staff vs external)
   └─< nad_user_applicant_access >── nad_applicant   (grant: role, status, expiry,
                                        │             capability_overrides, is_interim)
nad_applicant (root)   status: DRAFT | ACTIVE | UNLINKED | ARCHIVED
 ├─ nad_applicant_education*      (institution, level, gpa, dates)
 ├─ nad_applicant_test_score*     (test_type, score, taken_on)
 ├─ nad_applicant_contact*        (guardian/emergency; not a login)
 └─ PII: dob, nationality, passport_no            (see SECURITY §6)

nad_university (root, PUBLIC)
 ├─ nad_program*                  (degree_level, field, language, tuition, status)
 │    └─ nad_program_intake*      (term, application_open, application_close)
 └─ (0..1 ACTIVE) nad_partnership  ── CONFIDENTIAL, separate authz boundary
        ├─ nad_partnership_contact*
        └─ nad_partnership_event*                 (append-only)

nad_scholarship (root)                            STUDENT-SAFE columns only
 └─ (1..1) nad_scholarship_internal               CONFIDENTIAL
        ├─ university_id? → nad_university
        └─ partnership_id? → nad_partnership
 └─ nad_scholarship_program*  (allowed programmes, only when program_bound)

nad_application (root)
 ├─ applicant_id → nad_applicant
 ├─ application_type  (PROGRAM_ONLY | PROGRAM_WITH_SCHOLARSHIP | SCHOLARSHIP_LED)
 ├─ program_id?  scholarship_id?  intake_id?  target_university_id?
 ├─ workflow_instance_id → nad_wf_instance
 ├─ current_stage_id → nad_wf_stage   (pointer)
 ├─ current_status   (DENORMALIZED coarse label; written ONLY by the workflow engine)
 ├─ assignee_user_id? → sys_user (staff)
 ├─ version   (optimistic lock)
 ├─ nad_application_stage_history*   (append-only)
 ├─ nad_application_event*           (append-only timeline)
 ├─ nad_application_task*            (SINGLE table: engine-materialized + ad-hoc;
 │                                    optional wf_stage_task_template_id)
 ├─ nad_application_note*            (visibility: INTERNAL | SHARED)
 ├─ nad_application_decision*        (append-only; decision_type + outcome; may
 │                                    reference the transition it justifies)
 ├─ nad_application_submission*      (channel, external_reference, submitted_at)
 ├─ nad_application_link*            (related_application_id, relation)
 └─ nad_application_document*        (M:N to nad_document; requirement_id?, added_by)

nad_document (root)
 ├─ applicant_id → nad_applicant    (always set = owner)
 ├─ application_id?                  (set only for application-specific documents)
 ├─ doc_type (dict), status (derived from current version), reviewer_user_id?,
 │  rejection_reason?, expires_on?
 ├─ current_version_id → nad_document_version   (authoritative version pointer)
 ├─ nad_document_version*           (immutable rows; verification_status, scan_status)
 └─ nad_document_event*             (append-only)
nad_document_requirement*            (scope: PROGRAM | SCHOLARSHIP | WF_STAGE; ref_id;
                                     doc_type; mandatory) → drives per-application checklist

nad_conversation (root)
 ├─ application_id?  → nad_application    (FK lives HERE; an application has many
 │                                         conversations; Application does NOT store a
 │                                         conversation_id)
 ├─ nad_conversation_participant*  (user_id, role, last_read_message_id, muted)
 └─ nad_message*  ─< nad_message_attachment*   (lightweight blob unless "promoted"
                                                to a nad_document by staff)

nad_notification (root)             recipient_user_id, type, read_at,
 │                                  application_id?  conversation_id?  message_id?
 └─ nad_notification_delivery*      channel, provider, status, sent_at
nad_notification_preference*        user_id, type, channel, enabled
nad_notification_template*          type, channel, locale, subject_tpl, body_tpl
```

---

## 8. Cross-model review — resolved contradictions & filled gaps (BASELINE)

Findings from reviewing Application / Document / Workflow / Communication / Partnership
against each other. **In Phase 1 every `→ doc` item below has been propagated** into
the named document, so all 13 `docs/` files are now mutually consistent with this
baseline. The list is retained as the change record.

### Application
- **G-A1 (contradiction, fixed):** Rev 1 had `Application.conversationId?` *and*
  `nad_conversation.application_id`. **Resolved:** the FK lives on
  `nad_conversation.application_id` (1 application → N conversations). Application
  stores no conversation id. → COMMUNICATION_AND_NOTIFICATIONS.md
- **G-A2 (contradiction, fixed):** `current_status` was described as both "derived
  coarse label" and a stored column. **Resolved:** it **is** a stored, denormalized
  column for query/reporting, but is written **only** by the workflow engine from
  stage metadata — never by a controller/service directly.
  → APPLICATION_WORKFLOW.md
- **G-A3 (gap):** no `intake_id` — applications had no term/deadline anchor. Added
  (`PROGRAM_*` types require it).
- **G-A4 (gap):** no record of "submitted to university". Added
  `nad_application_submission` (channel: UNIVERSITY_PORTAL / EMAIL / AGENT / OTHER,
  `external_reference`, `submitted_at`, `submitted_by`).
- **G-A5 (gap):** decisions were under-modelled. `nad_application_decision` gains
  `decision_type` (`UNIVERSITY_OFFER` / `SCHOLARSHIP_AWARD` / `APPLICANT_RESPONSE` /
  `NADOUMI_INTERNAL`) and `outcome` (`OFFER` / `CONDITIONAL_OFFER` / `REJECT` /
  `WAITLIST` / `AWARDED` / `DECLINED_BY_APPLICANT` / `ACCEPTED_BY_APPLICANT`).
- **G-A6 (gap):** no applicant-initiated `WITHDRAWN` / `ON_HOLD` path. Added to the
  status vocabulary; withdrawal is a workflow transition to a terminal stage.
- **G-A7 (gap):** no link to fees. `nad_application` ↔ `nad_payment` (0..*).
  → PAYMENT (future doc).
- **G-A8 (gap):** sibling applications (same programme, different scholarship) had no
  representation. Added `nad_application_link`.

### Document
- **G-D1 (contradiction, fixed):** Rev 1 showed `Application.documentIds[]` (array)
  while `nad_document` had `application_id?`. **Resolved:** no array column.
  `nad_document.applicant_id` is always set (owner). `nad_document.application_id` is
  set only for application-specific docs. Reusable applicant-level docs (passport) are
  attached to applications via **`nad_application_document`** (M:N) with
  `requirement_id?`, `added_by`, `added_at`. → DOCUMENT_MANAGEMENT.md
- **G-D2 (gap):** no concept of *required* documents. Added `nad_document_requirement`
  (scope PROGRAM / SCHOLARSHIP / WF_STAGE) → a computed per-application checklist that
  workflow transitions can gate on. → DOCUMENT_MANAGEMENT.md, APPLICATION_WORKFLOW.md
- **G-D3 (ambiguity, fixed):** which version drives `nad_document.status`. **Resolved:**
  `nad_document.current_version_id` names the authoritative version; document status is
  derived from that version's `verification_status` + `expires_on`.
- **G-D4 (gap):** visibility of `rejection_reason` vs internal review notes was
  implicit. **Resolved:** `rejection_reason` is `SHARED` (external users with
  `VIEW_DOCUMENT` see it); internal review notes are `nad_application_note`
  `visibility='INTERNAL'` or a `nad_document_event` not exposed externally.

### Workflow
- **G-W1 (contradiction, fixed):** two task tables (`ApplicationTask` in DOMAIN_MODEL,
  `nad_wf_instance_task` in APPLICATION_WORKFLOW). **Resolved:** **one** table
  `nad_application_task`, with a nullable `wf_stage_task_template_id` distinguishing
  engine-materialized tasks from ad-hoc staff tasks. → APPLICATION_WORKFLOW.md
- **G-W2 (gap):** `nad_program` / `nad_scholarship` had no pointer to a workflow
  definition. Added `workflow_definition_code` on both (nullable → falls back to a
  default definition). → APPLICATION_WORKFLOW.md, DATABASE_DESIGN.md
- **G-W3 (risk, decided):** `guard_expr` free-form expression language on transitions
  is a security + complexity risk. **Recommendation:** v1 has **no** expression
  language; guards are structured predicates only — `ALL_MANDATORY_TASKS_DONE`,
  `ALL_REQUIRED_DOCUMENTS_VERIFIED`, `DECISION_RECORDED(type)`,
  `PAYMENT_SETTLED(kind)`. Add SpEL later only if a real need appears.
  → APPLICATION_WORKFLOW.md
- **G-W4 (gap):** `stage_type='DECISION'` had no mechanism connecting the decision to
  the chosen outgoing transition. **Resolved:** a `nad_application_decision` row may
  carry `drives_transition_code`; the engine validates the decision exists before
  allowing that transition.
- **G-W5 (gap):** definition versioning mid-flight. **Resolved:** an instance pins
  `definition_version`; **no** auto-migration; a manual migration tool is a later
  enhancement. → APPLICATION_WORKFLOW.md
- **G-W6 (dependency):** SLA/timer sweeps depend on Quartz **JDBC** job store; the app
  currently logs `RAMJobStore`. Must be resolved before workflow timers are trusted.
  → DEPLOYMENT.md, ARCHITECTURE.md risk A7.

### Communication
- **G-C1 (contradiction, fixed):** see G-A1 (FK direction). `nad_conversation` owns
  `application_id`.
- **G-C2 (gap):** system/timeline events inside a thread. **Resolved:** authored
  `nad_message` rows and `nad_application_event` rows stay **separate**; the UI merges
  them chronologically. No SYSTEM message rows. → COMMUNICATION_AND_NOTIFICATIONS.md
- **G-C3 (gap):** a new message must raise a notification, but
  `nad_notification` had no conversation/message reference. Added optional
  `conversation_id` / `message_id`. → COMMUNICATION_AND_NOTIFICATIONS.md
- **G-C4 (ambiguity, fixed):** chat attachments vs governed documents. **Resolved:**
  chat attachments are lightweight blobs (own storage key) unless staff explicitly
  "promote to document", which creates a `nad_document`.
  → DOCUMENT_MANAGEMENT.md, COMMUNICATION_AND_NOTIFICATIONS.md
- **G-C5 (rule):** confidential content must not be posted as chat messages (all
  participants see them). Internal notes use `nad_application_note` `INTERNAL`.

### Partnership
- **G-P1 (ambiguity, fixed):** "0..1 partnership per university" vs "keep history".
  **Resolved:** at most **one `ACTIVE`** partnership per university (partial-unique
  index); unlimited historical `TERMINATED` rows; partnerships are never hard-deleted.
- **G-P2 (gap):** partnership ↔ programme/scholarship coverage.
  `nad_scholarship_internal.partnership_id` covers scholarships. For programmes:
  **recommendation** — keep commission at partnership level for v1; add
  `nad_partnership_program` (with `commission_override`) only if per-programme terms
  are actually required. **OPEN** sub-item (deferred). → PARTNERSHIP_MODEL.md
- **G-P3 (scope):** commission **accrual/ledger** (money earned per enrolment) is
  Reporting/Finance scope — forward reference `nad_commission_ledger`, not built now.
- **G-P4 (rule):** a programme under a partner university must be byte-for-byte
  indistinguishable from a non-partner programme on public APIs (no partner flag).
  → PARTNERSHIP_MODEL.md, API_DESIGN.md

### Identity & Access
- **G-I1 (constraint):** applicant grants must **not** be baked into
  `LoginUser.permissions` (Redis-cached for the token lifetime) — revocation would lag.
  The predicate reads `nad_user_applicant_access` live per request. (§1, §4.3)
- **G-I2 (gap):** `sys_dept` is Nadoumi's internal org tree only; it is **not** used
  for applicants, universities, or partnerships. Stated to prevent misuse of RuoYi
  data-scope for external resources.

---

## 9. Naming & ID conventions (PLANNED)

- Tables: **`nad_<context>_<entity>`** (`nad_applicant`, `nad_application_event`,
  `nad_document_version`, `nad_scholarship_internal`). Keeps Nadoumi tables sorted
  away from `sys_*` / `gen_*` / `QRTZ_*`.
- Permissions (staff): **`nad:<context>:<action>`** (`nad:application:view`,
  `nad:scholarship:internal:view`, `nad:partnership:view`,
  `nad:applicant:access:manage`).
- PKs: `bigint` surrogate `id`. **Recommendation (D13):** DB auto-increment for v1
  (RuoYi-consistent, simplest); revisit application-generated snowflake IDs only if
  offline creation or sharding becomes a real requirement.
- Audit columns: reuse `BaseEntity` (`create_by/create_time/update_by/update_time/remark`).
- Soft delete: business entities use explicit status lifecycles + retained history,
  **not** `del_flag`, except where a true reversible delete is needed.
- Append-only tables (`*_event`, `*_history`, `*_decision`,
  `nad_document_version`): rows are never `UPDATE`d or `DELETE`d.

---

## 10. Decision status after Phase 1

| ID | Decision | Status |
| --- | --- | --- |
| **D7** | `User ↔ Applicant` access model (§4) | **APPROVED** — guardian defaults + interim-owner flow finalized (§4.2 ¹, §4.3). |
| **D9** | Application target model (§5) — 3-type discriminator + invariants | **APPROVED** |
| **D10** | External users in `sys_user` + `user_type` + `/api/student/login` (§3) | **APPROVED** |
| **D11** | Document attachment: applicant-owned docs + `nad_application_document` M:N + `nad_document_requirement` | **APPROVED** |
| **D12** | Single `nad_application_task` table | **APPROVED** |
| **D13** | PK strategy — `bigint` auto-increment for v1 | **APPROVED** |
| **D14** | Migrations — Flyway; `V1` = current schema; Nadoumi migrations from `V3` | **APPROVED** |
| **D4** | Workflow engine — data-driven tables + structured guard predicates, no expression language | **APPROVED** |
| **D5** | Object storage — S3-compatible via `DocumentStorage` SPI | **APPROVED** |
| **D1 / D2 / D3 / D6 / D8** | Admin UI (→ RuoYi-Vue3), public/student (→ Nuxt 3 + BFF cookie), maintained fork, realtime (→ SSE + email-only + Redis fan-out), DB creds/naming | **APPROVED** (see `ARCHITECTURE.md` §7) |
| — | Guardian-of-minor legal nuance; API response envelope + versioning confirmation; PII encryption scope + data residency; concrete email/SMS vendors; runtime target | **OPEN** — do not block Phase 2 schema/identity work. |
