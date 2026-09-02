# Phase 3 — Identity & Access + Applicant (first vertical slice)

Status: **IMPLEMENTED**. Built on the frozen Revision 3 baseline (`docs/DOMAIN_MODEL.md`,
`docs/DATABASE_DESIGN.md`, `docs/SECURITY.md`, `docs/API_DESIGN.md`,
`docs/PERMISSION_CATALOGUE.md`, `docs/ddl/nad_core.draft.sql`). No University, Program,
Scholarship, Chat, Payment or advanced Workflow behaviour is included.

---

## 1. Modules

| Module | Contents |
| --- | --- |
| `nadoumi-common` | `AccessRole`, `AccessGrantStatus`, `ApplicantCapability`, `AccessCapabilityMatrix` (the frozen §4.2 matrix + override merge), `ApplicationApplicantResolver` SPI seam. Pure JDK. |
| `nadoumi-identity` | `sys_user.user_type` handling, `/api/student/{register,login,me,logout}`, `nad_user_applicant_access` grant lifecycle, `NadoumiAccessService` (`@na`) implementation, `CapabilityOverrides` JSON parsing, RFC 9457 `problem+json` advice for `/api/**`. |
| `nadoumi-applicant` | `nad_applicant` + education / test-score / contact, `/api/student/applicants/**` and `/api/staff/applicants/**`, PII masking. Depends on `nadoumi-identity`. |

`nadoumi-applicant` → `nadoumi-identity` → `nadoumi-common` → (RuoYi). No cycles.

## 2. Migrations (Flyway, `db/migration`)

- **`V2__nad_reference_and_menu_seed.sql`** — `sys_config` flag `nad.student.register.enabled`;
  dicts `nad_user_type` / `nad_access_role` / `nad_degree_level` / `nad_test_type`;
  the `Nadoumi` `sys_menu` group + `Applicants` screen + `nad:applicant:*` buttons;
  roles `ops_manager`, `case_officer` + `sys_role_menu` grants.
- **`V3__nad_identity_access.sql`** — `nad_user_applicant_access` with `owner_guard`
  (INV1: one `ACTIVE` `OWNER` / applicant) and `active_guard` (INV2: one `ACTIVE`
  grant / user+applicant+application) STORED generated columns + plain `UNIQUE`.
- **`V4__nad_applicant.sql`** — `nad_applicant` + 3 child tables + the deferred
  `fk_uaa_applicant`.

`sys_user` / `SysUserMapper` are **not** modified. `user_type` is read/written by a
Nadoumi-owned mapper (`NadIdentityMapper`), consistent with it being a previously
dormant column.

## 3. APIs

### Public / auth
| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| POST | `/api/student/register` | anonymous (`@Anonymous`) | gated by `nad.student.register.enabled`; captcha honoured when `sys.account.captchaEnabled`; creates `sys_user` with `user_type='10'`, no roles. **Revision 2 (PLANNED):** body `{ firstName, lastName, email, password, ticket }`; requires a verified-email OTP `ticket`; `user_name` server-generated; `email_verified=1`; `PasswordPolicy` enforced. |
| POST | `/api/student/login` | anonymous | rejects `user_type='00'` (403); reuses `SysLoginService` (captcha, 5-try/10-min lockout, JWT, Redis session); activates any pending invites for the caller's email. **Revision 2 (PLANNED):** body `{ email, password, code?, uuid? }` — email-first; unknown email == wrong password. |
| POST | `/api/student/email-otp` · `/api/student/email-otp/verify` | anonymous | **Revision 2 (PLANNED).** Redis OTP (6-digit, 600 s TTL, single-use, ≤5 attempts, 60 s resend cooldown) for `REGISTER` / `PASSWORD_RESET`. Non-enumerating (`email-otp` always `200 {sent:true}`). Verify → single-use `ticket`. Rate-limited; captcha-aware. |
| POST | `/api/student/password/reset` | anonymous | **Revision 2 (PLANNED).** `{ ticket, newPassword }` → policy check, reset, **all sessions revoked**, `204`, no auto-login. |
| POST | `/api/student/password` | bearer | **Revision 2 (PLANNED).** `{ currentPassword, newPassword }` → policy check (≠ current), **other sessions revoked**, `204`. |
| GET | `/api/student/me` | bearer (external) | `{ userId, username, nickName, accessibleApplicants:[{applicantId, accessRole, capabilities[]}] }`. No roles / permissions. |
| POST | `/api/student/logout` | bearer | deletes the Redis session. |
| POST | `/login` | anonymous | RuoYi staff login, now **rejects `user_type != '00'`**. |

**Revision 2 email delivery:** provider-agnostic `MailSender` port +
`spring-boot-starter-mail`; `transport=log` (tests/CI) or `transport=smtp`
(Mailpit locally via `docker-compose.yml`, Gmail for staging/prod). Full design:
`docs/superpowers/specs/2026-09-02-nadoumi-web-public-site-design.md` §15.

### Student (external users, `@na` capability checks)
`GET/POST /api/student/applicants`, `GET/PUT /api/student/applicants/{id}`,
`.../{id}/education`, `.../{id}/test-scores`, `.../{id}/contacts` (GET/POST/PUT/DELETE),
`GET/POST/DELETE /api/student/applicants/{id}/access`,
`POST /api/student/applicants/{id}/access/transfer-ownership`.

### Staff (`@ss.hasPermi('nad:applicant:*')` + service re-check)
`GET/POST /api/staff/applicants`, `GET/PUT /api/staff/applicants/{id}`,
`DELETE /api/staff/applicants/{id}` (archive), child collections,
`GET/POST/DELETE /api/staff/applicants/{id}/access`,
`POST /api/staff/applicants/{id}/access/transfer-ownership`.

Bodies are Java `record` DTOs; errors are `application/problem+json` (bare bodies on
success). The RuoYi `/system|/monitor|/tool` console keeps its `AjaxResult` envelope.

## 4. Security model

- **Two non-overlapping systems.** Staff (`user_type='00'`) → RuoYi RBAC
  (`nad:applicant:*` tokens seeded in V2) + service re-check. External users
  (`10/20/30`) → `nad_user_applicant_access` grant looked up **live on every request**
  via `NadoumiAccessService`; the result is never written into the JWT or the
  Redis-cached `LoginUser.permissions`.
- **Capability matrix** (`docs/DOMAIN_MODEL.md` §4.2) is `AccessCapabilityMatrix`;
  `capability_overrides_json` (`{"add":[…],"remove":[…]}`) is applied on top, remove
  winning. Adding a privileged capability requires a staff actor with
  `nad:applicant:access:manage` (enforced in `UserApplicantAccessService`).
- **Grant lifecycle** (`UserApplicantAccessService`): self-registration → `ACTIVE OWNER`;
  staff-created applicant → interim `ACTIVE OWNER` (`is_interim=1`, `expires_at ≈ now+30d`)
  **plus** a `PENDING OWNER` email invite; invitee sign-in atomically promotes the
  invite to `ACTIVE OWNER` and revokes the interim grant
  (`revoke_reason='ownership_transferred'`); delegate (`AGENT`/`GUARDIAN`/`VIEWER`),
  revoke (soft, row retained), transfer (atomic, never ownerless).
- **Defense in depth**: `@PreAuthorize` on the controller, an explicit re-check in the
  service, and every applicant-scoped student finder restricted to the caller's
  accessible applicant ids (`accessibleApplicantIds`).
- **PII**: staff without `nad:applicant:pii:view` get masked `dob` / `passportNo`;
  external users see their own applicants' data unmasked. (Column encryption and the
  `sys_oper_log` PII-read audit remain **OPEN** per `docs/SECURITY.md` §6.)

## 5. Tests

`mvn clean verify`:

| Test | Kind | Asserts |
| --- | --- | --- |
| `AccessCapabilityMatrixTest` (9) | unit | the §4.2 matrix + override merge |
| `CapabilityOverridesTest` (4) | unit | overrides JSON parse / round-trip / unknown-name tolerance |
| `FlywayMigrationsIT` (3) | Testcontainers MySQL | V1..V4 on a fresh DB; baseline path on a pre-existing RuoYi DB; `owner_guard` rejects a 2nd active owner |
| `LoginBoundaryTest` (2) | full context + MySQL/Redis | `/login` rejects externals; `/api/student/login` rejects staff; happy paths |
| `ApplicantIsolationTest` (2) | " | one student cannot read or list another student's applicant (403 / filtered list) — **CI-blocking** |
| `RevocationTakesEffectNextRequestTest` (1) | " | a revoked `VIEWER` is denied on the very next request, same token, no re-login — **CI-blocking** |
| `ExternalUserHasNoStaffPermissionTest` (1) | " | an external token fails every `nad:*` / `system:*` check — **CI-blocking** |
| `InterimOwnerFlowTest` (1) | " | staff create → interim owner + pending invite; invitee sign-in promotes + revokes interim; `/me` shows `OWNER` |

The full-context tests are named `*Test` (surefire / `test` phase) so
`@SpringBootConfiguration` resolves before the Spring Boot repackage; they self-skip
without a Docker daemon. `FlywayMigrationsIT` stays `*IT` (failsafe).

## 6. Known limitations / deferred

- `NadoumiAccessService.canAccessApplication` denies external callers until the
  Application slice supplies an `ApplicationApplicantResolver` bean (the grant query
  `findActiveApplicationGrant` is already in place). `canReviewDocument` is a coarse
  RBAC check pending the Document slice.
- No Quartz sweep yet for invite escalation (7/14/30 d) or `expires_at → EXPIRED` /
  `UNLINKED` flagging — the columns and query index exist.
- PII column encryption + unmasked-read audit logging: **OPEN** (`docs/SECURITY.md` §6).
- Only `ops_manager` / `case_officer` roles are seeded; the full 10-role matrix from
  `docs/PERMISSION_CATALOGUE.md` §3 is seeded per-slice as its permissions land.
- `RolePermissionMatrixTest` / `PiiAccessAuditTest` / confidentiality tests belong to
  later slices (no scholarship/partnership surface exists yet).
- **Revision 2 (planned, not yet built):** student email verification, OTP-based
  forgot-password, self-service change-password with session revocation, and the
  move to email-first login. Adds `sys_user.email_verified` (migration V7) and a
  provider-agnostic email channel. `~~No student password self-service~~` is no
  longer a permanent gap — it is scheduled. See spec §15 + plan Part E.
- Full applicant onboarding (photo/passport upload, residence branch, interests,
  languages, work history, …) is a **dedicated later phase** — `docs/APPLICANT_ONBOARDING.md`
  (PROPOSED). Phase 3's `nad_applicant` scope is unchanged by Revision 2.

## 7. Next phase

Universities + Programs + Intakes (`V5`), then Scholarships with the confidentiality
split + `v_scholarship_student` view (`V6`), then Workflow (`V7`) and the Application
business case (`V8`), wiring `ApplicationApplicantResolver` so external
application-scoped authorization goes live.
