# Nadoumi — Staff Permission Catalogue & Role Matrix

> Read `docs/PLATFORM_ARCHITECTURE.md` first — the approved enterprise-platform direction this doc rolls up to (2026-09-02).

Status: **BASELINE** (Phase 1, approved). Companion to `docs/SECURITY.md`,
`docs/ADMIN_ARCHITECTURE.md`, `docs/DOMAIN_MODEL.md` §3–§4.

This document defines **staff (`sys_user.user_type='00'`) authorization only**.
External users (`10/20/30`) are **never** governed by these permissions — they use
`nad_user_applicant_access` capabilities (DOMAIN_MODEL §4.2). The two systems do not
overlap.

---

## 1. Mechanism (EXISTING, reused)

- Permission tokens live in `sys_menu.perms`, are attached to roles via
  `sys_role_menu`, and are checked with `@PreAuthorize("@ss.hasPermi('nad:application:view')")`
  (also `@ss.hasAnyPermi(...)`, `@ss.lacksPermi(...)`), plus a service-layer re-check.
- **RuoYi constraint:** `@ss.hasPermi` does an **exact string match** against the
  user's permission set, with one special case — the wildcard `*:*:*` (held only by
  the RuoYi `admin` account / a role granted `*:*:*`). Prefixed wildcards like
  `nad:*:*` or `nad:application:*` are **not** expanded. Therefore every role below is
  granted an **explicit enumerated list** of tokens; the `*` in "`nad:applicant:*`" in
  prose is shorthand for "all `nad:applicant:…` tokens listed in §2".
- **Data scope** (`@DataScope` + `sys_role.data_scope`) narrows *which rows* a
  permitted action can touch: `ALL` / `DEPT` / `DEPT_AND_CHILD` / `SELF`. Nadoumi adds
  an **assignment scope** for case work (see §4).
- Seeding: all `sys_menu` rows and `sys_role` / `sys_role_menu` assignments are
  delivered as a Flyway migration (`V2` / role-seed migration), never console edits.

---

## 2. Permission catalogue

Token = `nad:<context>:<action>`. `» sensitive` marks tokens that gate confidential
or high-impact operations (see §5).

### 2.1 `applicant`
| Token | Grants |
| --- | --- |
| `nad:applicant:view` | Read an applicant profile (non-PII fields). |
| `nad:applicant:list` | List / search applicants. |
| `nad:applicant:create` | Create an applicant (triggers the interim-owner + invite flow, DOMAIN_MODEL §4.3). |
| `nad:applicant:edit` | Edit profile, education, test scores, contacts. |
| `nad:applicant:archive` | Set `status='ARCHIVED'` (no hard delete). |
| `nad:applicant:export` | Bulk export applicant data. » sensitive |
| `nad:applicant:pii:view` | See unmasked `dob`, `passport_no`, `national_id`, full contact details. Audited on every read. » sensitive |
| `nad:applicant:merge` | Merge duplicate applicant records. » sensitive |
| `nad:applicant:access:view` | View the `nad_user_applicant_access` grants on an applicant. |
| `nad:applicant:access:manage` | Grant / revoke / transfer access; approve a `capability_overrides_json` (e.g. guardian submit). » sensitive |

### 2.2 `application`
| Token | Grants |
| --- | --- |
| `nad:application:view` | Read an application (business case) and its timeline. |
| `nad:application:list` | List / search applications, pipeline board. |
| `nad:application:create` | Staff-initiated application creation. |
| `nad:application:edit` | Edit application fields (opportunity, intake, target university). |
| `nad:application:assign` | Assign / reassign `assignee_user_id`. |
| `nad:application:claim` | Self-assign an unassigned application. |
| `nad:application:transition` | Execute a workflow transition (subject to the transition's own role gate + guards). |
| `nad:application:withdraw` | Move to `WITHDRAWN` on behalf of the applicant. |
| `nad:application:decide` | Record a `nad_application_decision` (university offer, scholarship award, internal). |
| `nad:application:note:view` | Read `SHARED` notes. |
| `nad:application:note:internal:view` | Read `INTERNAL` notes. » sensitive |
| `nad:application:note:add` | Add notes (visibility per payload; `INTERNAL` requires `…note:internal:view`). |
| `nad:application:submission:record` | Record `nad_application_submission` (external portal reference). |
| `nad:application:export` | Export application data. » sensitive |

### 2.3 `document`
| Token | Grants |
| --- | --- |
| `nad:document:view` | Read document + version metadata and status. |
| `nad:document:download` | Stream / pre-signed-URL the actual file bytes. » sensitive |
| `nad:document:request` | Raise a document request / requirement against an application. |
| `nad:document:verify` | Set a version `verification_status='VERIFIED'`. » sensitive |
| `nad:document:reject` | Set `verification_status='REJECTED'` (reason mandatory). |
| `nad:document:requirement:view` | View requirement templates / per-application checklist. |
| `nad:document:requirement:manage` | Create / edit `nad_document_requirement` rows. |

### 2.4 `workflow`
| Token | Grants |
| --- | --- |
| `nad:workflow:definition:view` | View workflow definitions, stages, transitions. |
| `nad:workflow:definition:list` | List definitions. |
| `nad:workflow:definition:create` | Create a new `DRAFT` definition. |
| `nad:workflow:definition:edit` | Edit a `DRAFT` definition. |
| `nad:workflow:definition:activate` | Publish a definition version (`ACTIVE`). » sensitive (change control) |
| `nad:workflow:definition:retire` | Retire a definition version. » sensitive |

### 2.5 `university` — **IMPLEMENTED** (seed: Flyway `V9`)
`nad:university:view` · `nad:university:list` · `nad:university:create` ·
`nad:university:edit` · `nad:university:remove` · `nad:university:export`
Granted by `V9`: full set → `nadoumi_super_admin`, `ops_manager`,
`partnerships_manager`; `view` + `list` only → `case_officer`, `content_editor`,
`read_only_analyst`, `support_agent`. (`export` is defined but no endpoint yet.)
`V10` (profile depth + ranking/highlight tables + `/api/public/universities`) adds
**no new permission** — the profile fields, child lists and the anonymous public
read are all covered by the existing set (public GET is `@Anonymous`).

### 2.6 `program`
`nad:program:view` · `nad:program:list` · `nad:program:create` · `nad:program:edit` ·
`nad:program:remove` · `nad:program:intake:manage`

### 2.7 `scholarship` (student-safe data) — **IMPLEMENTED** (seed: Flyway `V17`)
`nad:scholarship:view` · `nad:scholarship:list` · `nad:scholarship:create` ·
`nad:scholarship:edit` · `nad:scholarship:remove` · `nad:scholarship:publish` ·
`nad:scholarship:export`
Granted by `V17`: full set → `nadoumi_super_admin`, `ops_manager`,
`partnerships_manager`; `view` + `list` → `case_officer`, `read_only_analyst`,
`content_editor`. (`publish` / `export` are defined; `publish` is applied via the
PUT body's `publishStatus`, `export` has no endpoint yet.) Anonymous discovery
needs no permission — `/api/public/scholarships*` is `@Anonymous` and reads only
`v_scholarship_student`.

### 2.8 `scholarship:internal` (operational linkage — NOT commercial terms) — **IMPLEMENTED** (seed: `V17`)
| Token | Grants |
| --- | --- |
| `nad:scholarship:internal:view` | See `nad_scholarship_internal`: which `university_id` / `partnership_id` a scholarship maps to, `internal_status`, `operational_notes`, `confidential_terms`. `GET /api/staff/scholarships/{id}/internal`. Needed by case work. » sensitive |
| `nad:scholarship:internal:edit` | Edit those fields. `PUT …/internal`. » sensitive |
| `nad:scholarship:program:manage` *(deferred)* | Manage `nad_scholarship_program` (allowed programmes for a `PROGRAM_BOUND` scholarship). Lands with `nadoumi-program`. » sensitive |

`V17` grants `internal:view` + `internal:edit` to `nadoumi_super_admin`,
`ops_manager`, `partnerships_manager`; `internal:view` only to `case_officer` and
`read_only_analyst`. `StaffScholarshipTest` proves a `case_officer` gets `200` on
GET `…/internal` and `403` on PUT.

### 2.9 `partnership` (confidential commercial relationship)
| Token | Grants |
| --- | --- |
| `nad:partnership:view` | Existence, university, status, tier, contract dates, contacts. » sensitive |
| `nad:partnership:list` | List partnerships. » sensitive |
| `nad:partnership:create` | Create a `DRAFT` partnership. » sensitive |
| `nad:partnership:edit` | Edit non-financial fields. » sensitive |
| `nad:partnership:terms:view` | See `commission_model_json`, `confidential_terms`, `internal_notes`. » sensitive |
| `nad:partnership:terms:edit` | Edit commercial terms. » sensitive |
| `nad:partnership:suspend` | `ACTIVE → SUSPENDED` / back. » sensitive |
| `nad:partnership:terminate` | `→ TERMINATED`. » sensitive |
| `nad:partnership:contact:manage` | Manage `nad_partnership_contact`. » sensitive |
| `nad:partnership:export` | Export partnership data. » sensitive |

### 2.10 `conversation`
`nad:conversation:view` · `nad:conversation:list` · `nad:conversation:participate`
(post / read as a participant) · `nad:conversation:participant:manage` (add/remove
participants) · `nad:conversation:close` · `nad:conversation:message:moderate`
(soft-delete / edit others' messages) » sensitive

### 2.11 `notification`
`nad:notification:template:view` · `nad:notification:template:manage` »sensitive ·
`nad:notification:delivery:view` (delivery log) · `nad:notification:broadcast`
(create a `sys_notice`-style announcement to a cohort) » sensitive

### 2.12 `content`
`nad:content:view` · `nad:content:list` · `nad:content:create` · `nad:content:edit` ·
`nad:content:publish` · `nad:content:remove`

### 2.13 `payment`
`nad:payment:view` · `nad:payment:list` · `nad:payment:invoice:create` ·
`nad:payment:refund` » sensitive · `nad:payment:export` » sensitive

### 2.14 `report`
`nad:report:application:view` · `nad:report:partnership:view` » sensitive ·
`nad:report:finance:view` » sensitive · `nad:report:export` » sensitive

### 2.15 Reused RuoYi tokens (admin console infrastructure)
`system:user:*`, `system:role:*`, `system:menu:*`, `system:dept:*`, `system:post:*`,
`system:dict:*`, `system:config:*`, `system:notice:*`, `monitor:*`, `tool:gen:*`.
Kept staff-only and unchanged. Admin user-management screens filter
`user_type='00'` by default (DOMAIN_MODEL §3.3).

---

## 3. Roles → permissions

Ten roles. `assignment` scope = the role sees only applications/applicants where the
user is `assignee_user_id` **or** in the same `sys_dept` sub-tree (configurable per
role via `sys_role.data_scope`). `ALL` = organisation-wide.

| Role (`sys_role.role_key`) | Data scope | Permission set (summary — see §2 for exact tokens) |
| --- | --- | --- |
| **`nadoumi_super_admin`** | ALL | `*:*:*` (RuoYi `admin` account). Break-glass; named individuals only; every action audited. |
| **`ops_manager`** | ALL | All `applicant:*` (incl. `pii:view`, `access:manage`, `merge`), all `application:*` (incl. `note:internal:*`, `decide`), all `document:*`, `university:*`, `program:*`, `scholarship:*`, `scholarship:internal:*`, `conversation:*`, `notification:*`, `content:view`, `payment:view`, `report:application:view` + `report:partnership:view`, `workflow:definition:view`, `assignment` (`assign`). **Excludes** `partnership:terms:*`, `partnership:terminate`, `payment:refund`, `workflow:definition:activate/retire`, `report:finance:view`. |
| **`workflow_admin`** | ALL | `workflow:definition:*` (create/edit/activate/retire), `document:requirement:*`, `application:view`, `application:list`, `notification:template:view`. Read-only on everything else. |
| **`case_officer`** | assignment | `applicant:view/list/edit/pii:view`, `application:view/list/edit/claim/transition/withdraw/decide/note:view/note:internal:view/note:add/submission:record`, `document:view/download/request/reject`, `scholarship:view/list`, `scholarship:internal:view`, `program:view`, `university:view`, `conversation:view/participate`, `report:application:view` (own). **No** `document:verify`, **no** `partnership:*`, **no** `applicant:export`, **no** `applicant:access:manage`. |
| **`document_reviewer`** | assignment | `document:view/download/verify/reject`, `document:requirement:view`, `application:view` (context), `applicant:view/pii:view` (to verify identity docs), `conversation:view/participate`. **No** application edit/transition, **no** partnership, **no** export. |
| **`partnerships_manager`** | ALL | `partnership:*` (incl. `terms:view/edit`, `suspend`, `terminate`, `contact:manage`, `export`), `scholarship:*` + `scholarship:internal:*` + `scholarship:program:manage`, `university:*`, `program:*`, `report:partnership:view`, `application:view/list` (pipeline visibility). **No** `applicant:pii:view`, **no** `application:edit/transition`, **no** `payment:*`. |
| **`content_editor`** | ALL (content) | `content:*`, `university:view`, `program:view`, `scholarship:view` (student-safe only). Nothing else. |
| **`finance`** | ALL | `payment:*` (incl. `invoice:create`, `refund`, `export`), `partnership:terms:view` (commission only), `report:finance:view` + `report:partnership:view` + `report:export`, `application:view/list` (read). **No** `applicant:pii:view`, **no** `partnership:edit`, **no** `document:download`. |
| **`support_agent`** | ALL (read) + own conversations | `conversation:*` (participate, participant:manage, close), `application:view/list` (read), `applicant:view/list` (no PII), `document:view` (metadata only — **no** `download`), `notification:delivery:view`. Routes queries to case officers; cannot change case state. |
| **`read_only_analyst`** | ALL (read) | `report:application:view` + `report:partnership:view` + `report:export`, `application:view/list`, `applicant:view/list` (no PII), `scholarship:view/list`, `scholarship:internal:view` (linkage, not terms), `partnership:view/list` (no `terms:view`), `university:view`, `program:view`. No write anywhere, no PII, no commission, no downloads. |

Users may hold **multiple** roles; permissions are the union, data scope the widest
of the roles (RuoYi behaviour) — assign narrowly.

---

## 4. Data-scope / assignment rules (BASELINE)

- **`case_officer` / `document_reviewer`** default to `data_scope = DEPT_AND_CHILD`
  plus a service-layer `authScope` that further restricts application/applicant reads
  to rows where `assignee_user_id = self` **or** the row is unassigned and in the
  user's dept queue. Unassigned + in-queue is visible so they can `claim`.
- **`ops_manager` / `partnerships_manager` / `finance` / analysts** = `data_scope =
  ALL`.
- `sys_dept` models **only** Nadoumi's internal org (teams / regional desks). It is
  never used to scope universities, scholarships, partnerships, or applicants by
  "ownership" — those are unscoped catalog / governed by the tokens above.
- Every applicant-/application-scoped MyBatis query takes an explicit `authScope`
  parameter; there is no unscoped finder on a controller path.

---

## 5. Sensitive-permission matrix (the ones that matter for audit & review)

| Sensitive capability | Token(s) | Roles that hold it by default |
| --- | --- | --- |
| Unmasked applicant PII | `nad:applicant:pii:view` | `ops_manager`, `case_officer`, `document_reviewer`, `nadoumi_super_admin` |
| Bulk PII export | `nad:applicant:export` | `ops_manager`, `nadoumi_super_admin` |
| Manage who can act for an applicant | `nad:applicant:access:manage` | `ops_manager`, `nadoumi_super_admin` |
| Scholarship → university/partnership linkage | `nad:scholarship:internal:view` | `ops_manager`, `case_officer`, `partnerships_manager`, `read_only_analyst`, `nadoumi_super_admin` |
| Partnership commercial terms / commission | `nad:partnership:terms:view` | `partnerships_manager`, `finance`, `nadoumi_super_admin` |
| Terminate a partnership | `nad:partnership:terminate` | `partnerships_manager`, `nadoumi_super_admin` |
| Verify a document | `nad:document:verify` | `document_reviewer`, `nadoumi_super_admin` |
| Download document bytes | `nad:document:download` | `ops_manager`, `case_officer`, `document_reviewer`, `nadoumi_super_admin` |
| Activate a workflow definition | `nad:workflow:definition:activate` | `workflow_admin`, `nadoumi_super_admin` |
| Issue a refund | `nad:payment:refund` | `finance`, `nadoumi_super_admin` |
| Broadcast to a cohort | `nad:notification:broadcast` | `ops_manager`, `nadoumi_super_admin` |
| Read internal application notes | `nad:application:note:internal:view` | `ops_manager`, `case_officer`, `nadoumi_super_admin` |

**Absolute rule (never role-gated away):** *no* staff role, and *no* combination of
staff roles, causes any confidential scholarship/partnership field to appear on a
`/api/public/**` or `/api/student/**` response. Staff visibility is about the
`/api/staff/**` surface only; the student boundary is enforced structurally
(DOMAIN_MODEL §6) and independently of roles.

---

## 6. Tests (CI-blocking) — see `docs/SECURITY.md` §9

- `RolePermissionMatrixTest` — data-driven from §3: for each (role, token) assert
  `hasPermi` matches the table; for each sensitive token assert **only** the listed
  roles pass.
- `AssignmentScopeTest` — a `case_officer` cannot read an application assigned to
  another dept/officer.
- `PiiAccessAuditTest` — every `applicant:pii:view` read writes a `sys_oper_log` row.
- `ExternalUserHasNoStaffPermissionTest` — a `user_type in (10,20,30)` token fails
  every `nad:*` and `system:*` permission check.
