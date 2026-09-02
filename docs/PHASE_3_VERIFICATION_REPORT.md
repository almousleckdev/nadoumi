# Phase 3 — Verification Report

Read-only verification of the Identity & Access + Applicant slice. **No code was
modified.** Companion to `docs/PHASE_3_IDENTITY_APPLICANT.md` (design/scope).

## Verdict

**Phase 3 is complete.** `mvn clean verify` → **BUILD SUCCESS**, **24 tests, 0
failures / 0 errors / 0 skipped** (Docker up locally; Testcontainers ITs executed).

| Suite | Tests |
| --- | --- |
| `AccessCapabilityMatrixTest` (unit) | 9 |
| `CapabilityOverridesTest` (unit) | 4 |
| `FlywayMigrationsIT` (Testcontainers MySQL) | 3 |
| `LoginBoundaryTest` (full context + MySQL/Redis) | 2 |
| `ApplicantIsolationTest` — CI-blocking | 2 |
| `RevocationTakesEffectNextRequestTest` — CI-blocking | 1 |
| `ExternalUserHasNoStaffPermissionTest` — CI-blocking | 1 |
| `InterimOwnerFlowTest` | 1 |

`AbstractNadIntegrationTest` is an abstract base (no runnable tests).

---

## Item-by-item verification

### 1. Student registration / login — ✅
- `POST /api/student/register` (`@Anonymous`, `@RateLimiter(count=5,time=60)`) — gated
  by `sys_config` key `nad.student.register.enabled`; captcha honoured when
  `sys.account.captchaEnabled`; `checkUserNameUnique`; creates a `sys_user` with
  `user_type='10'`, **no** roles/menus.
- `POST /api/student/login` (`@Anonymous`, `@RateLimiter(count=10,time=60)`) — rejects
  `user_type='00'` with `NadForbiddenException` → **403 problem+json**; otherwise
  reuses `SysLoginService` (captcha, 5-try/10-min lockout, JWT, Redis session);
  on success runs `acceptInvitesFor(userId, email)`.
- `GET /api/student/me` — `{userId, username, nickName, accessibleApplicants:[{applicantId,
  accessRole, capabilities[]}]}`, **no** roles/permissions; rejects staff (403).
- `POST /api/student/logout` — deletes the Redis session (204).
- `/login` (staff) — `StaffLoginGuard.assertStaffLogin` rejects `user_type != '00'`.
- Tests: `LoginBoundaryTest` (staff→`/api/student/login`=403; student→`/login`
  rejected; happy paths both ways). Register+login exercised end-to-end in
  `InterimOwnerFlowTest`.

### 2. Applicant profile — ✅
- Tables `nad_applicant` + `nad_applicant_education` / `_test_score` / `_contact`
  (`V4`). Entity + `ApplicantMapper` (explicit `<resultMap>`; `mapUnderscoreToCamelCase`
  is off in this repo).
- `ApplicantService`: `createAboutMe` (student, status `ACTIVE`), `createByStaff`
  (staff, status `DRAFT`), `get`, `update`, `archive` (→ `ARCHIVED`), `listForStaff`
  (PageHelper), `listMine`, and add/list/update/delete for education / test-scores /
  contacts. Every applicant-scoped method re-checks `@na.canAccessApplicant(...)`.

### 3. User ↔ Applicant access — ✅
- `nad_user_applicant_access` (`V3`): `user_id`, `applicant_id`, `application_id?`,
  `access_role`, `status`, `invited_email?`, `capability_overrides_json?`,
  `is_interim`, `granted_by_user_id` / `granted_at`, `revoked_by_user_id` /
  `revoked_at` / `revoke_reason`, `expires_at?`, audit columns.
- `UserApplicantAccessService` lifecycle: `grantOwnerOnSelfRegistration`,
  `createStaffApplicantAccess` (interim `ACTIVE OWNER` `is_interim=1`
  `expires_at≈now+30d` **+** `PENDING OWNER` email invite), `acceptInvitesFor`
  (atomically promotes an owner invite and revokes the interim grant,
  `revoke_reason='ownership_transferred'`), `delegate`, `revoke`, `transferOwnership`,
  `listForApplicant`.
- **INV1** (one `ACTIVE OWNER`/applicant) and **INV2** (one `ACTIVE` grant per
  user+applicant+application) — enforced by STORED generated guard columns
  (`owner_guard`, `active_guard`) + plain `UNIQUE`, **and** a service-layer
  `SELECT … FOR UPDATE` (`lockActiveOwner`) on the ownership paths.
- `FlywayMigrationsIT.ownerGuard_rejectsASecondActiveOwnerForTheSameApplicant`
  proves INV1 at the DB.
- `fk_uaa_applicant` added in `V4`; `fk_uaa_application` is **intentionally deferred**
  to the Application slice (column present, currently unconstrained).

### 4. OWNER / AGENT / GUARDIAN / VIEWER authorization — ✅
- `AccessCapabilityMatrix` is the executable copy of `docs/DOMAIN_MODEL.md` §4.2:
  OWNER = all 9; AGENT = all except `MANAGE_ACCESS`; GUARDIAN = `VIEW_*` +
  `EDIT_PROFILE` + `UPLOAD_DOCUMENT` + `MESSAGE_STAFF`; VIEWER = `VIEW_*` only.
- `capability_overrides_json` (`{"add":[…],"remove":[…]}`) parsed by
  `CapabilityOverrides`; unknown names dropped; `remove` wins over `add`.
- `AccessCapabilityMatrixTest` (9) + `CapabilityOverridesTest` (4) cover the matrix,
  the override merge, and immutability.
- Controller guards: student routes use `@PreAuthorize("@na.canAccessApplicant(#id,
  '<CAP>')")` (`VIEW_PROFILE` for reads, `EDIT_PROFILE` for writes, `MANAGE_ACCESS`
  for the owner access endpoints); staff routes use `@ss.hasPermi('nad:applicant:*')`.

### 5. Live authorization / revocation — ✅
- `NadoumiAccessServiceImpl` (bean `@na`) resolves `user_type` **live** from
  `sys_user` (`CurrentCaller`) and reads `nad_user_applicant_access` **on every
  request** (`findActiveApplicantGrant`, filtered `status='ACTIVE'` and
  `expires_at is null or expires_at > now()`). Grants are never written into the JWT
  or the Redis-cached `LoginUser.permissions`.
- `RevocationTakesEffectNextRequestTest`: owner grants `VIEWER`, viewer GETs 200,
  owner revokes, viewer's **next** GET with the **same token** → **403** (no
  re-login).

### 6. Staff / admin applicant management — ✅
- `/api/staff/applicants/**` guarded by `@ss.hasPermi('nad:applicant:list|view|create|
  edit|archive')` + service re-check; `/api/staff/applicants/{id}/access/**` by
  `nad:applicant:access:view|manage`.
- Permission tokens + the `ops_manager` / `case_officer` roles + `sys_role_menu`
  grants seeded in `V2` (`nadoumi_super_admin` added later in the cleanup pass, `V5`).
- `ExternalUserHasNoStaffPermissionTest`: a student token → `/api/staff/applicants`
  (GET/POST) = **403 problem+json**, and `/system/user/list` = RuoYi `AjaxResult`
  (HTTP 200, body `code:403`).

### 7. Flyway migration(s) — ✅
`ruoyi-admin/src/main/resources/db/migration/`:
- `V1__ruoyi_baseline.sql` — RuoYi + Quartz baseline (recorded, not executed, on a
  populated DB).
- **`V2__nad_reference_and_menu_seed.sql`** — `nad.student.register.enabled` config;
  dicts `nad_user_type` / `nad_access_role` / `nad_degree_level` / `nad_test_type`;
  `Nadoumi` menu group + `Applicants` screen + 10 `nad:applicant:*` buttons; roles
  `ops_manager` / `case_officer` + `sys_role_menu`.
- **`V3__nad_identity_access.sql`** — `nad_user_applicant_access` + `owner_guard` /
  `active_guard` STORED generated columns.
- **`V4__nad_applicant.sql`** — `nad_applicant` + 3 children + `fk_uaa_applicant`.
- `V5__nadoumi_baseline_seed.sql` — **cleanup phase**, not Phase 3 (RuoYi demo-data
  removal + `almousleck` super-admin).

`FlywayMigrationsIT` verifies the full chain on a fresh DB **and** the
baseline-then-apply path on a pre-existing RuoYi DB.

### 8. API endpoints — ✅ (complete list)
| Method | Path | Guard |
| --- | --- | --- |
| POST | `/api/student/register` | `@Anonymous` |
| POST | `/api/student/login` | `@Anonymous` (rejects staff) |
| GET | `/api/student/me` | authenticated + external |
| POST | `/api/student/logout` | authenticated |
| GET | `/api/student/applicants` | authenticated + external (service) |
| POST | `/api/student/applicants` | authenticated + external (service) |
| GET/PUT | `/api/student/applicants/{id}` | `@na` VIEW/EDIT_PROFILE |
| GET/POST/PUT/DELETE | `/api/student/applicants/{id}/education[/{eid}]` | `@na` VIEW/EDIT_PROFILE |
| GET/POST/DELETE | `/api/student/applicants/{id}/test-scores[/{sid}]` | `@na` VIEW/EDIT_PROFILE |
| GET/POST/DELETE | `/api/student/applicants/{id}/contacts[/{cid}]` | `@na` VIEW/EDIT_PROFILE |
| GET/POST | `/api/student/applicants/{id}/access` | `@na` VIEW_PROFILE / MANAGE_ACCESS |
| DELETE | `/api/student/applicants/{id}/access/{grantId}` | `@na` MANAGE_ACCESS |
| POST | `/api/student/applicants/{id}/access/transfer-ownership` | `@na` MANAGE_ACCESS |
| GET | `/api/staff/applicants` | `@ss` `nad:applicant:list` |
| GET | `/api/staff/applicants/{id}` | `@ss` `nad:applicant:view` |
| POST | `/api/staff/applicants` | `@ss` `nad:applicant:create` + `@Log` |
| PUT | `/api/staff/applicants/{id}` | `@ss` `nad:applicant:edit` + `@Log` |
| DELETE | `/api/staff/applicants/{id}` (archive) | `@ss` `nad:applicant:archive` + `@Log` |
| GET/POST/DELETE | `/api/staff/applicants/{id}/{education,test-scores,contacts}[/…]` | `@ss` `nad:applicant:view`/`edit` |
| GET/POST | `/api/staff/applicants/{id}/access` | `@ss` `nad:applicant:access:view`/`manage` (+ `@Log` on POST) |
| DELETE | `/api/staff/applicants/{id}/access/{grantId}` | `@ss` `nad:applicant:access:manage` + `@Log` |
| POST | `/api/staff/applicants/{id}/access/transfer-ownership` | `@ss` `nad:applicant:access:manage` + `@Log` |
| POST | `/login` (RuoYi) | now rejects `user_type != '00'` |

Errors: RFC 9457 `application/problem+json` for `/api/**` (`NadApiExceptionHandler`,
`@Order(HIGHEST_PRECEDENCE)`, `basePackages="com.nadoumi"`); RuoYi console keeps
`AjaxResult`.

### 9. DTOs / validation — ✅
- 19 Java `record` DTOs, split `web/request/*` (with `jakarta.validation` —
  `@NotBlank`, `@NotNull`, `@Email`, `@Size`) and `web/response/*` (plain).
  Controllers use `@Valid @RequestBody`.
- No MyBatis entity is returned on the wire; mapping is explicit
  (`ApplicantResponse.of(...)`, `AccessGrantResponse.of(...)`, …).

### 10. Audit / history — ⚠️ partial (see debt #4)
- **Present:** RuoYi `@Log(title, businessType)` on staff mutating endpoints (applicant
  create/update/archive; access grant/revoke/transfer) → `sys_oper_log`.
  `nad_user_applicant_access` carries full row-level audit (`granted_by`/`at`,
  `revoked_by`/`at`/`reason`, `is_interim`, `create_by`/`update_by`/times).
  `nad_applicant*` rows carry `create_by`/`update_by`/`create_time`/`update_time`.
- **Not present:** no `nad_applicant_event` / `_history` table (profile changes are
  not append-only audited); `@Log` is not applied to any `/api/student/**` write, nor
  to the staff child-collection mutations (education / test-score / contact
  add/delete). `nad_user_applicant_access` rows are never hard-deleted (soft revoke).

### 11. Security tests — ✅
`LoginBoundaryTest`, `ExternalUserHasNoStaffPermissionTest`,
`RevocationTakesEffectNextRequestTest`, `InterimOwnerFlowTest` (full context, MySQL +
Redis via Testcontainers, real JWT + Redis session, MockMvc through the security
filter chain). Unit: `AccessCapabilityMatrixTest`, `CapabilityOverridesTest`.

### 12. Applicant isolation tests — ✅ (CI-blocking)
`ApplicantIsolationTest`: (a) student B GET student A's applicant → **403**;
(b) `GET /api/student/applicants` for B returns **only B's** applicant.

### 13. Full `mvn clean verify` — ✅
**BUILD SUCCESS**, 24 tests, 0 failures / 0 errors / 0 skipped, ~33 s.

---

## Out-of-scope check — no accidental business implementation

Verified **absent** from the backend:

| Domain | Evidence |
| --- | --- |
| University / Program / Intake | no `nad_university` / `nad_program` / `nad_program_intake` table in `V2`–`V5`; no `*University*` / `*Program*` class/mapper/controller in `nadoumi-modules` |
| Scholarship | no `nad_scholarship*` table, no `v_scholarship_student` view, no scholarship class |
| Partnership | no `nad_partnership*` table or class |
| Chat / Communication | no `nad_conversation` / `nad_message` table or class |
| Payment | no `nad_payment*` table or class |
| Advanced Workflow | no `nad_wf_*` table; no `WorkflowService`; no workflow engine code |
| Application (business case) | no `nad_application*` table; no `ApplicationService` |

Skeleton / seam artifacts that are **not** business implementation (interfaces / enums
only, zero logic):
- `com.nadoumi.common.workflow.GuardPredicate` (Phase 2 SPI enum).
- `com.nadoumi.common.notification.NotificationChannel` + `…Kind` / `…SendStatus` /
  `…SendResult` (Phase 2 SPI; split into one-type-per-file during the cleanup pass).
- `com.nadoumi.common.storage.DocumentStorage` (Phase 2 SPI interface).
- `com.nadoumi.common.access.ApplicationApplicantResolver` — **added in Phase 3** as a
  seam so `NadoumiAccessService.canAccessApplication` can be wired by the future
  Application slice. Interface only, **no implementation**; with no bean present the
  external application-scoped path returns `false`.

Frontend (`nadoumi-web`) contains scaffold pages named `scholarships/`, `universities/`,
`programs/` — **UI skeleton only**: no backend, no business logic; they call
`/api/public/*` endpoints that do not exist yet and render an empty state.

---

## Incomplete work / shortcuts / technical debt

1. **`canAccessApplication` (external path) is inert** until the Application slice
   provides an `ApplicationApplicantResolver` bean — currently returns `false` for
   external callers. Staff path (RBAC) works. `findActiveApplicationGrant` query is
   already in place.
2. **`canReviewDocument` is a coarse RBAC check** (`nad:document:verify` OR
   `nad:document:reject`) with no per-document assignment — to be refined by the
   Document slice.
3. **No Quartz sweeps.** `expires_at` is honoured in read queries but never
   materialised to `status='EXPIRED'`; the 7/14/30-day invite-escalation job is not
   implemented; the "staff-created applicant → `UNLINKED` at interim expiry" flow is
   not implemented — `ApplicantStatus.UNLINKED` exists but is **never assigned**.
4. **Applicant audit gap.** No `nad_applicant_event` / `_history` table; profile edits
   are tracked only by `create_by`/`update_by`/`update_time` columns. `@Log` is not on
   any `/api/student/**` write, nor on the staff education/test-score/contact
   mutations.
5. **PII audit not implemented.** Masked projection for staff lacking
   `nad:applicant:pii:view` **is** implemented (`ApplicantResponse.of(entity, pii)`);
   the `docs/SECURITY.md` §6 requirement that every unmasked PII read writes a
   `sys_oper_log` row (`PiiAccessAuditTest`) is not.
6. **Role matrix is partial.** Only `ops_manager` + `case_officer` (V2) and
   `nadoumi_super_admin` (V5, cleanup) are seeded. The full 10-role matrix from
   `docs/PERMISSION_CATALOGUE.md` §3 and its `RolePermissionMatrixTest` /
   `AssignmentScopeTest` are deferred to the slices that introduce their permissions.
7. **Delegation cap not enforced.** `docs/DOMAIN_MODEL.md` §4.3 mentions a
   "configurable cap on active delegates"; `delegate` does not check one.
8. **`/login` rejection is asymmetric.** A staff-only rejection at `/login` surfaces as
   RuoYi's `AjaxResult` (HTTP 200, body `code:500` via `ServiceException`), whereas
   `/api/student/login` returns a real **403** `problem+json`. Intentional (keeps the
   RuoYi console contract) but a rough edge.
9. **Student collection endpoints are service-guarded only.**
   `GET`/`POST /api/student/applicants` have no `@PreAuthorize` (no id to gate) —
   they rely on `anyRequest().authenticated()` + a service-layer `isExternal()`
   check.
10. **Per-request `sys_user` lookup.** `CurrentCaller.userType()` hits `sys_user` on
    every authorization call — correct for "live" semantics, but not request-cached.
11. **`fk_uaa_application` is deferred** — `nad_user_applicant_access.application_id`
    is currently a nullable column with no foreign key (added by the Application
    slice, per plan).
12. **Testcontainers dependency.** The 5 full-context tests + `FlywayMigrationsIT`
    require a Docker daemon; without one they self-skip (a Docker-less
    `mvn clean verify` reports them skipped, not run). CI has Docker.
13. **Test fixture shortcut.** `AbstractNadIntegrationTest` clears `sys_user` /
    `sys_user_role` where `user_id > 3` between tests, assuming seeded accounts occupy
    ids ≤ 3 (`admin`=1, `almousleck`=3).
14. **Nothing is committed** — Phase 3 + the cleanup pass are all in the working tree
    on `master`.
