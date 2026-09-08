# Nadoumi — Security

Status: **BASELINE** · **EXISTING** · **PLANNED** · **OPEN**.

> Reconciled with Rev 3. Staff permission detail lives in
> `docs/PERMISSION_CATALOGUE.md`; the confidentiality enforcement model in
> `docs/DOMAIN_MODEL.md` §6.

---

## 1. Authentication (EXISTING)

- **Mechanism:** stateless JWT bearer tokens. Spring Security
  `SessionCreationPolicy.STATELESS`, CSRF disabled.
- **Login flow** (`SysLoginController.login` → `SysLoginService.login`):
  1. Captcha check — `sys.account.captchaEnabled` (`sys_config`, Redis-cached). Code
     stored in Redis `captcha_codes:<uuid>`, single-use, deleted on check.
  2. `loginPreCheck` — non-empty; password length 5–20 (`UserConstants`); username
     length 2–20; IP blacklist `sys.login.blackIPList`.
  3. `AuthenticationManager.authenticate` → `UserDetailsServiceImpl.loadUserByUsername`
     → BCrypt (`BCryptPasswordEncoder`) compare.
  4. Failed-attempt lockout: `user.password.maxRetryCount = 5`,
     `user.password.lockTime = 10` min (in `application.yml`, tracked in Redis).
  5. Success → `TokenService.createToken`: a random UUID is the session key; a
     `LoginUser` (user + granted authorities) is cached in Redis
     `login_tokens:<uuid>` with 30-min TTL; a JWT (HS512) carrying claim
     `login_user_key = <uuid>` is returned.
- **Per-request** (`JwtAuthenticationTokenFilter`, before
  `UsernamePasswordAuthenticationFilter`): parse JWT → read `login_user_key` → load
  `LoginUser` from Redis → populate `SecurityContext`. Sliding renewal: if <20 min to
  expiry, refresh Redis TTL.
- **Header:** `Authorization: Bearer <jwt>` (configurable `token.header`).
- **Logout:** `POST /logout` → `LogoutSuccessHandlerImpl` deletes the Redis token.
- **Registration:** `POST /register` gated by `sys.account.registerUser` config.

### Auth risks (EXISTING)

| ID | Finding | Severity |
| --- | --- | --- |
| S1 | `token.secret = abcdefghijklmnopqrstuvwxyz` hard-coded in `application.yml`; short, low-entropy, HS512. Anyone with the repo can mint valid JWT envelopes. | **High** |
| S2 | `jjwt 0.9.1` (2018) — unmaintained line, deprecated `parser().setSigningKey(String)` API. | Medium |
| S3 | JWT is only an envelope; real trust is the Redis session. Redis has **no password** (`spring.data.redis.password:` empty) and binds localhost in dev. If Redis is exposed, sessions can be read/forged. | **High** (prod) |
| S4 | Lockout + captcha state in Redis only; flushing Redis clears lockouts. | Low |
| S5 | `admin` (`user_id = 1`) bypasses every permission check (`isAdmin()` → `*:*:*`). | By-design; document + restrict use. |

**Remediation status:**
- **S1/S2 — DONE (Phase 2).** `token.secret` is now `${TOKEN_SECRET:<90-char dev
  placeholder>}` (the in-repo value is a labelled dev-only default; real envs must
  override, ≥ 64 bytes for HS512). `jjwt` upgraded `0.9.1 → 0.12.6` (split
  `jjwt-api`/`impl`/`jackson`); `TokenService` rewritten to
  `Keys.hmacShaKeyFor(secret)` + `Jwts.parser().verifyWith(key).parseSignedClaims`;
  a `kid` header (`${TOKEN_KID:v1}`) is emitted to enable future rotation (a
  multi-key resolver is deferred). Auth flow verified unchanged: `/login` → JWT →
  `/getInfo` works; a forged/expired token → `code:401` (RuoYi's 200-body-with-401
  convention, preserved). Tokens issued before the cutover are rejected → one
  re-login. **This lands before any `/api/student/login` is exposed** (Phase 3).
- **S3 — config DONE (Phase 2), enforcement per-env.** `spring.data.redis.password`
  is `${SPRING_DATA_REDIS_PASSWORD:}` and `spring.data.redis.ssl.enabled` is
  `${SPRING_DATA_REDIS_SSL_ENABLED:false}`. Non-local environments **must** set both;
  Redis must never listen on a public interface.
- **S5 — DONE (Cleanup, `V5__nadoumi_baseline_seed.sql`).** The RuoYi `admin`
  account (`user_id = 1`, still the only holder of `*:*:*` via `isAdmin(1)`) is a
  **disabled break-glass account** (`status = '1'`, nick "Break-glass administrator").
  Re-enable it only for a named individual, with MFA at the IdP/proxy and every
  action reviewed; disable it again immediately after.
  Day-to-day super-admin work uses **`almousleck`** (`user_id = 3`), a normal
  `user_type='00'` staff account holding the `nadoumi_super_admin` role
  (`data_scope = '1'`, granted every `sys_menu` row). It does **not** get the
  `isAdmin(1)` shortcut, so a handful of RuoYi conveniences reserved to id 1
  (e.g. editing the break-glass account itself) are intentionally unavailable to it.
  The `almousleck` seed password (`Nadoumi2026#`) exists **only** as a BCrypt hash in
  `V5__nadoumi_baseline_seed.sql`. **Revision 2:** the confusing RuoYi
  "your password is still the initial password" prompt is removed
  (`sys.account.initPasswordModify → 0`, the `ruoyi-ui` nag deleted,
  `SysLoginController.initPasswordIsModify` returns `false`). Forced first-login
  rotation is instead carried by the password-**expiry** path — `pwd_update_date = NULL`
  is treated as expired by `passwordIsExpiration` when
  `sys.account.passwordValidateDays > 0` (Revision 2 sets it). Every real environment
  must still reset this password.

### 4.1 Student authentication (IMPLEMENTED — Phase 3, see `docs/PHASE_3_IDENTITY_APPLICANT.md`)

`/api/student/login` reuses `SysLoginService` (captcha, 5-try/10-min lockout, JWT,
Redis session) but rejects `user_type='00'`; `/login` rejects `user_type != '00'`.
The Nuxt BFF holds the JWT only in an httpOnly + Secure + SameSite=Lax cookie
(`API_DESIGN.md` §4.1). Registration for externals is a distinct
`/api/student/register` gated by its own config flag, separate from
`sys.account.registerUser`.

### 4.2 Student email verification, OTP & password reset (IMPLEMENTED — Revision 2, spec §15)

**Chain status (verified 2026-09-02, `ARCHITECTURE_GAP_ANALYSIS.md` §1):** browser
→ Nuxt BFF → Spring → `OtpService` → Redis → `MailSender` → SMTP/Mailpit works end
to end. One open defect: a **resend inside the 60 s cooldown is a silent no-op
that still returns `{ sent: true }`**. Fix in progress — `OtpService.issue()`
returns `SENT | THROTTLED`; the controller emits
`{ sent: true, throttled: <bool>, retryAfter: <sec> }` from the live cooldown TTL,
and `<EmailVerifyStep>` distinguishes "new code sent" from "code already sent —
check spam, retry in Ns". Covered by `OtpEndToEndTest` (full-context, Redis +
`LoggingMailSender`).

**Identity:** email-first. `sys_user.user_name` for `user_type='10'` is a
server-generated handle; the verified `email` (new `sys_user.email_verified` column)
is the login key. Uniqueness among students is enforced in `StudentAuthService`;
`idx_sys_user_email` supports the lookup.

**OTP (Redis):** 6-digit numeric, `sha256`-stored, **TTL 600 s**, **single-use**
(deleted on success), **≤5 verify attempts** then invalidated, **60 s** resend
cooldown (one cooldown key per `purpose`+email, covering both the OTP and the
"account exists" branch so the response never enumerates). `@RateLimiter`:
`email-otp` **20/h/IP**, `email-otp/verify` **10/10 min/IP**. Captcha
(`sys.account.captchaEnabled`) additionally gates `email-otp`. A verified OTP mints
an opaque **single-use ticket** (Redis, TTL 600 s) consumed by `register` /
`password/reset`. **No reset tokens in URLs; no JWT-in-link.**

**Enumeration:** `email-otp` **always** returns `200 { sent: true }` (the new
`throttled` flag does **not** vary by whether the address is registered — it only
reflects the caller's own 60 s cooldown). An already-registered email in a
`REGISTER` request receives an "account exists" mail, not an OTP. `login` returns
one generic error for both "unknown email" and "wrong password". `password/reset`
with a valid ticket for a vanished account returns `204`.

**Password policy (`PasswordPolicy`, `ruoyi-common`; TS mirror in `nadoumi-web`):**
8–32 chars; ≥1 upper, ≥1 lower, ≥1 digit, ≥1 special; **≠ current**. Enforced on
register, reset, and change.

**Session revocation:** `SessionRevoker` deletes the user's `login_tokens:*` Redis
sessions. `password/reset` revokes **all**; `POST /api/student/password` revokes all
**except the caller's** and refreshes the caller's cached `LoginUser`. (RuoYi's own
`updatePwd` does **not** do this — the gap this closes.)

**Email transport:** a provider-agnostic `MailSender` port. `transport=log` (tests +
CI, no network) or `transport=smtp` (Mailpit locally via `docker-compose.yml`, Gmail
`smtp.gmail.com:587` + App Password for staging/prod). All creds are env-only
(`SPRING_MAIL_*`, `NADOUMI_MAIL_*`) — never committed (§7).

**Dev mail inbox:** `GET /api/dev/mail/latest` is `@Anonymous` and returns raw OTP /
reset codes, so it is gated independently of `transport`: it mounts only when
`nadoumi.mail.dev-inbox.enabled=true` (a dedicated opt-in — absent, and therefore
off, in every committed config; `application-test.yml` sets it for the ITs, local
E2E sets it in `config/application-local.yml`) **and** the `prod` profile is not
active (`@Profile("!prod")`). Choosing `transport=log` alone never exposes it.

## 2. Authorization (EXISTING)

- **Model:** RBAC. `sys_user` –< `sys_user_role` >– `sys_role` –< `sys_role_menu` >–
  `sys_menu`. `sys_menu.perms` tokens (`system:user:edit`).
- **Enforcement:** method-level `@PreAuthorize("@ss.hasPermi('system:user:list')")`
  (`@EnableMethodSecurity`), where `@ss` is `PermissionService`. Also `hasRole`,
  `hasAnyPermi`, `lacksPermi`.
- **Data scope:** `@DataScope(deptAlias=..., userAlias=...)` AOP injects a SQL
  `WHERE` fragment from `sys_role.data_scope` (all / custom depts / own dept /
  own dept+children / self).
- **URL rules** (`SecurityConfig`): everything authenticated except `permitAllUrl`
  (methods annotated `@Anonymous`, collected by `PermitAllUrlProperties`), `/login`,
  `/register`, `/captchaImage`, static assets, `/swagger-ui*`, `/v3/api-docs/**`,
  `/druid/**`.

### Two authorization systems (BASELINE — D7/D10)

RuoYi RBAC answers only *"may this **staff** user call this admin action?"*. Nadoumi
runs **two** non-overlapping systems (`ARCHITECTURE.md` §8):

| Caller | System | Source of truth |
| --- | --- | --- |
| Staff (`user_type='00'`) | RuoYi **RBAC** | `sys_role` / `sys_menu` `nad:<ctx>:<action>` tokens (full catalogue + role matrix in `docs/PERMISSION_CATALOGUE.md`) + `sys_dept` data scope + `nad_application.assignee_user_id` |
| External (`10/20/30`) | **resource grants** | `nad_user_applicant_access` — role OWNER/AGENT/GUARDIAN/VIEWER + capability set + `capability_overrides_json`, evaluated **live per request** |

**Live-check requirement (risk A10):** external grants are **never** written into the
JWT or the Redis-cached `LoginUser.permissions` (which live for the token TTL). The
predicate reads `nad_user_applicant_access` on every request, so a revoke takes effect
on the revoked user's next call.

**`NadoumiAccessService` (Spring bean `@na`)** exposes the predicates —
`@na.canAccessApplicant(id, capability)`, `@na.canAccessApplication(id, capability)`,
`@na.canViewScholarshipInternal()`, `@na.canReviewDocument(id)`, … Defense in depth is
mandatory, all three layers:
1. `@PreAuthorize("@na.can…")` / `@ss.hasPermi('nad:…')` on the controller;
2. an explicit re-check inside the service method;
3. an explicit `authScope` parameter on **every** applicant-/application-scoped
   MyBatis query — there is no unscoped finder on a controller path.

Externals get **no** `sys_role`/`sys_menu` rows, so a student/agent token fails every
`nad:*` and `system:*` `hasPermi` check by construction (test:
`ExternalUserHasNoStaffPermissionTest`).

## 3. Transport & headers (EXISTING)

- CORS: a `CorsFilter` bean is registered ahead of the JWT and logout filters
  (currently permissive). **BASELINE:** allowed origins become an **explicit per-
  environment list** (admin origin + `nadoumi-web` origin), sourced from config, no
  wildcard; credentials mode aligned with the httpOnly-cookie BFF. The concrete origin
  strings per environment are **OPEN** (deploy-time).
- Security headers: cache-control disabled; `frameOptions = sameOrigin`. No HSTS,
  CSP, `X-Content-Type-Options`, `Referer-Policy` set by the app (expected at the
  reverse proxy — see `docs/DEPLOYMENT.md`).
- TLS terminates at the reverse proxy (not configured in-repo).

## 4. Input handling (EXISTING)

- **XSS filter** (`xss.enabled = true`) on `/system/*`, `/monitor/*`, `/tool/*`
  (excludes `/system/notice`). Escapes HTML in request bodies.
- **SQL injection:** MyBatis parameterized mapper XML; `@DataScope` builds fragments
  from server-side role config, not user input. Audit any `${}` (vs `#{}`) usage when
  adding Nadoumi mappers.
- **Upload filter:** RuoYi blocks a denylist of extensions (incl. `.html` per commit
  history). See `docs/DOCUMENT_MANAGEMENT.md` for the Nadoumi-grade replacement.
- **`@RepeatSubmit`** AOP guards double submits; `@RateLimiter` (Redis) available.

## 5. Scholarship / University / Partnership confidentiality (BASELINE — scholarship layer BUILT & TESTED, R3)

Requirement (CLAUDE.md §8): a student must never learn which university/partnership a
scholarship is tied to, nor which universities are partners. As of R3 the
scholarship side is implemented: `nad_scholarship_internal` (PK=FK,
`university_id`→`nad_university`, `commission_model_json`, operational/confidential
notes) holds the linkage; **`v_scholarship_student`** is the only head-row object
`PublicScholarshipController` / `ScholarshipService` read, and `FlywayMigrationsIT`
asserts the view projects none of `university_id` / `partnership_id` /
`operational_notes` / `confidential_terms` / `commission_model_json` /
`internal_status`. `StaffScholarshipTest` proves an anonymous list + detail body
contains no such string on any path even after `PUT …/internal` sets the linkage,
and that `nad:scholarship:internal:*` gates the sub-resource (`case_officer` →
`200` GET, `403` PUT). The full nine-layer enforcement model is in
**`docs/DOMAIN_MODEL.md` §6.2**; summary:

1. **Storage separation** — `nad_scholarship` (safe) vs `nad_scholarship_internal` vs
   `nad_partnership`.
2. **DB view `v_scholarship_student`** — the **only** object student/public MyBatis
   mappers may read (a mis-coded join cannot reach an internal column that is not in
   the view).
3. **Single choke-point** `ScholarshipQueryService` — `findStudentView` vs
   `findInternalView` (the latter `@PreAuthorize`-gated, in a staff-only module the
   student controllers cannot reference at compile time).
4. **Type-safe DTOs** — `ScholarshipStudentView` record cannot carry a confidential
   field; explicit field-by-field mapping; a unit test asserts its component set.
5. **Response-body denylist net** on `/api/public|student` — forbidden key in a
   serialized scholarship/university body ⇒ 500 + security-log.
6. **Query-param guard** — confidential `sort`/`filter`/`groupBy` ⇒ 400.
7. **Derived data** — search index, discovery facets, exports, caches all built from
   the view; no facet groups by university/partner status.
8. **Partner-indistinguishability** — no `isPartner` flag / badge / partner-only list
   on any public university or programme representation.
9. **No route exists** — there is no public/student endpoint that returns a
   partnership field or a scholarship→university association.

Staff visibility is role-gated (`docs/PERMISSION_CATALOGUE.md` §5), **but no staff
role or combination ever causes a confidential field onto a `/api/public|student`
response** — the student boundary is structural and role-independent.

## 5a. File & document storage (BASELINE — EXISTING, P1)

`docs/superpowers/specs/2026-09-03-media-storage-and-application-engine-design.md`
Part I (**DM1–DM8**). Provider: **Cloudinary**, behind the `MediaStorageService`
SPI (+ `MediaGateway` façade) in `nadoumi-common`, implemented by
`CloudinaryMediaStorage` in the new foundational `nadoumi-media` module. This
supersedes `ARCHITECTURE.md` D5's provider choice; D5's SPI *principle* is kept.
Full model: `docs/DOCUMENT_MANAGEMENT.md` §3.2–§3.4a;
`docs/DATABASE_DESIGN.md` §5.6a.

**Authorization before access, every request.** No file is served on the
strength of URL possession, a frontend guard, or a prior grant. Every
PROTECTED/SENSITIVE request re-resolves the caller's authorization from source
(`NadoumiAccessService`, `sys_user_role`) — never a cached decision — before any
bytes or URL are returned.

**No permanent public URLs for private assets.** `nad_media_asset.secure_url` is
populated **only** when `access_class='PUBLIC'`. PROTECTED and SENSITIVE assets
never have a stored URL:
- **PROTECTED** — a fresh, single-purpose, short-TTL signed URL is issued
  per-request after authorization succeeds (`nadoumi.media.signed-url-ttl-seconds`,
  default **180s**, clamped **[60, 600]**). It expires and is not embeddable in a
  cacheable page.
- **SENSITIVE** — no URL is ever generated for the client. The Nadoumi backend
  fetches the object from Cloudinary server-side and streams it through the
  response (bounded buffer, `Cache-Control: no-store`,
  `X-Content-Type-Options: nosniff`, `Content-Disposition: attachment`).
  Reserved today for `JW202` and the Step 7 doc-type overrides `PASSPORT` /
  `VISA` / `FINANCIAL_PROOF` / `TRANSCRIPT` / `POLICE_CLEARANCE`.

**Every PROTECTED/SENSITIVE access is logged.** `nad_media_access_log`
(append-only, `REQUIRES_NEW` write so a later `403` cannot roll it back):
`access_kind` (`SIGNED_URL_ISSUED` / `STREAM_PROXY` / `METADATA`), `result`
(`GRANTED` / `DENIED`), `deny_reason`, `ttl_seconds`, actor, IP, user agent. A
public `GET /api/media/{id}` redirect for a PUBLIC asset is **not** logged.

**Revocation is immediate.** A signed URL already handed out has at most `TTL`
seconds of residual validity; SENSITIVE media has no residual window at all —
every byte re-checks authorization. Deactivating a `nad_user_applicant_access`
grant, changing a role, or archiving an applicant takes effect on the affected
user's next call, exactly as for every other resource grant (§2 above).

**Upload validation** (`MediaValidation`, boundary pipeline, before any
Cloudinary call — full detail `docs/DOCUMENT_MANAGEMENT.md` §3.4):

| Control | Rule |
| --- | --- |
| MIME allow-list | per `MediaCategory`, declared `Content-Type` must match |
| Magic-byte sniff | Apache Tika on the first 8 KiB; sniffed type must be in the allow-list **and** family-match the declared type (image↔image, pdf↔pdf) — mismatch → `422` |
| Size cap | per category (4–20 MB) + a global `NADOUMI_MEDIA_MAX_UPLOAD_MB` (default 20) ceiling; also `spring.servlet.multipart.max-file-size`/`max-request-size` |
| Filename sanitisation | strip path separators/control chars/leading dots; collapse to `[A-Za-z0-9._-]`; max 100 chars; empty → generated `upload-<uuid>` |
| Hard denylist (every category) | `text/html`, `image/svg+xml`, `application/xhtml+xml`, `application/x-msdownload`, `application/x-sh`, zip, java-archive — never accepted regardless of declared/sniffed MIME |
| Checksum | SHA-256, streamed, for document-shaped categories (dedupe + tamper evidence) |

**Legacy RuoYi `/common/*` upload/download.** Used only by the `ruoyi-ui` staff
console; `nadoumi-web` and `nadoumi-admin` do not call it. It has no per-resource
permission and `/common/download?delete=true` removes a file with only an
extension check, so the whole `CommonController` is now
`@PreAuthorize("@currentCaller.isStaff()")` — an external/student JWT is
otherwise merely `authenticated()` and would pass. New paths must upload through
`nadoumi-media` (validation pipeline above), not `/common/*`.

**`CLOUDINARY_URL` handling.** Env var only — `cloudinary://<key>:<secret>@<cloud>`.
No `application.yml` default; `CloudinaryMediaStorage`'s constructor **fails fast**
at startup if the value is absent or malformed. Never logged — `CloudinaryMediaStorage`
logs `public_id` + byte size + owner, never the URL for PROTECTED/SENSITIVE assets
and never the API secret. Rotate via the secret manager (§7).

**Scholarship confidentiality cross-reference (§5 above).** PUBLIC catalog
imagery (university logo/banner/gallery, scholarship hero/cover, programme
image) is unrelated to the confidentiality boundary — it's meant to be public.
`v_scholarship_student` (recreated `V27` to expose `hero_media_id` /
`cover_media_id`) still projects **no** confidential column and involves **no**
join to `nad_scholarship_internal` or `nad_university` — `CatalogImageRetrofitTest`
re-asserts this alongside the existing `FlywayMigrationsIT` view-shape assertion.

## 6. PII & data protection

- PII inventory: applicant name, DOB, nationality, passport number, contact details,
  guardian data, uploaded identity documents, payment references.
- **BASELINE:** masked projections — staff without `nad:applicant:pii:view` get
  masked `dob` / `passport_no` / contact detail (a `@PiiMasked` DTO variant, chosen in
  the mapper by permission); every unmasked PII read writes a `sys_oper_log` row
  (`PiiAccessAuditTest`); templates/logs never contain raw PII (`@Log` param
  scrubbing).
- **OPEN (needs security/legal input, not a Phase 2 blocker):** column-level
  encryption for `passport_no` / `national_id` (which fields, which KMS); retention &
  erasure policy for withdrawn/archived applicants; data-residency for China / Malaysia
  cross-border processing.

## 7. Secrets & configuration (BASELINE — D8)

- Committed `application*.yml` carries **no environment secret**. All of DB
  URL/user/password, Redis password, `token.secret`, Druid console creds come from
  **env vars** (relaxed binding, e.g. `SPRING_DATASOURCE_DRUID_MASTER_PASSWORD`,
  `SPRING_DATA_REDIS_PASSWORD`, `TOKEN_SECRET`) or an optional git-ignored
  `config/application-local.yml` via
  `spring.config.import=optional:file:./config/application-local.yml`.
- The committed `application-druid.yml` default stays `ry-vue` / `root` / `password`
  for a zero-config local start; the local dev DB is renamed `ry_vue`→`ry-vue` to
  match (`DEVELOPMENT_GUIDELINES.md` §1).
- **Production hardening (checklist, `DEPLOYMENT.md` §3):**
  `springdoc.swagger-ui.enabled=false`;
  Druid stat servlet disabled (or auth + IP allow-list + network policy);
  `referer.enabled=true` with real domains; explicit CORS origins; Redis AUTH+TLS;
  Actuator on a private management port.

## 8. Auditing (EXISTING — extend)

`@Log(title, businessType)` AOP → `sys_oper_log` (module, method, params, result, IP,
duration). Login events → `sys_logininfor`. **PROPOSED:** annotate all Nadoumi
state-changing endpoints; add domain events (`nad_application_event`) for business
history distinct from the technical operation log; never log PII payloads verbatim.

## 9. Security testing requirements (PLANNED — CI-blocking where noted)

- **AuthN:** token required; expired/forged/absent → 401; logout invalidates;
  `/login` rejects externals and `/api/student/login` rejects staff.
- **AuthZ (RBAC):** `RolePermissionMatrixTest` — data-driven from
  `docs/PERMISSION_CATALOGUE.md` §3/§5 (every role×token, and "only these roles" for
  each sensitive token). `AssignmentScopeTest` — case officer cannot read another
  officer's/dept's application.
- **AuthZ (grants):** `ExternalUserHasNoStaffPermissionTest`; cross-applicant /
  cross-application access denied; **revocation takes effect on the next request**
  (grant not cached in `LoginUser`); expired grant denied; per-application-scoped
  grant does not leak sibling applications.
- **Confidentiality (CI-blocking):** `ScholarshipConfidentialityTest`,
  `PartnershipExposureTest`, `UniversityPartnerLeakTest` — every public/student
  scholarship & university route (list, detail, search, facets, export) → no forbidden
  key in body or headers; internal routes → 403; confidential sort/filter → 400.
- **PII:** `PiiAccessAuditTest` — unmasked PII read writes `sys_oper_log`; masked
  projection for staff lacking `nad:applicant:pii:view`.
- **Injection / XSS** regression on new endpoints; MyBatis `${}` audit.
- **Upload:** MIME sniffing vs declared type, size (per `doc_type`), path traversal,
  infected-file (mock scanner) all rejected; `PENDING` scan blocks download.
- **Workflow:** guard bypass attempts (transition with unmet guard) → 403/409.

## 10. Employee / Finance / Payroll / Payments / Partnership data boundaries (PLANNED)

Planned architecture for the full admin/business platform
(`docs/ADMIN_ARCHITECTURE.md` §7, `docs/DOMAIN_MODEL.md` §2.2). Not implemented in the
current `nadoumi-web` build. These are **separate bounded contexts**; Finance,
Payroll, and Payments stay separate domains and integrate via read models + domain
events, not shared tables.

### 10.1 Sensitive data inventory

| Data class | Context | Examples |
| --- | --- | --- |
| Employee sensitive | Employee | national/tax ID, bank details, home address, emergency contacts, termination reason, performance notes |
| Compensation | Payroll | salary + history, payslips (gross/deductions/net/tax), payroll runs, adjustments |
| Financial | Finance | ledger entries, invoices, expenses, revenue, **commission rates & per-partner revenue** |
| Payment internals | Payments | provider references, reconciliation data, tokenized card metadata, payer contact |
| Partnership confidential | Partnership | partnership existence, terms, contacts, which universities are partners |

### 10.2 Authorization boundaries

- **Least privilege, explicit tokens.** `nad:employee:sensitive:view`,
  `nad:payroll:view` / `nad:payroll:self:view`, `nad:finance:view` /
  `nad:finance:manage`, `nad:payment:reconcile`, `nad:partnership:view` /
  `nad:partnership:terms:view`. None of these are implied by a broad operational role
  (`ops_manager`, `case_officer`, analysts).
- **No implicit super-admin reach into HR/payroll.** `nadoumi_super_admin` does not
  get payroll or employee-sensitive data by virtue of holding every `sys_menu` row;
  those tokens are granted explicitly to `payroll_admin` / `finance` / HR and every
  cross-person read is audited (`sys_oper_log`).
- **Self-service is separately scoped.** An employee sees only their own payslips
  (`employee_id = current`) and their own employee record's sensitive fields.
- **Manager scope** is limited to direct reports' **non-sensitive** fields via
  `sys_dept` data scope; cross-department browsing needs `nad:employee:view:all`.
- **Commission** rows (Finance) additionally require `nad:partnership:terms:view` —
  commission rate is a confidential partnership term.
- **Payments ≠ Finance write.** Payments emits `PaymentSettled` / `RefundIssued`;
  Finance posts ledger entries from those events. No context writes another's tables.
- **Structural public/student exclusion.** No employee, payroll, finance, payment
  internal, or partnership field appears on any `/api/public/**` or `/api/student/**`
  response, for any role or role combination — enforced the same way as scholarship
  confidentiality (§5): `@PreAuthorize` + service re-check + scoped mapper param +
  response-body denylist net on the public/student surface. A student sees only their
  **own** payment status + receipt (amount, status, date), never provider internals.
- **Append-only financial history.** Ledger entries and payslips are never updated in
  place; corrections are reversing entries / adjustment rows.

### 10.3 Tests (PLANNED — CI-blocking when each context lands)

- `PayrollAccessTest` — non-payroll roles (incl. `nadoumi_super_admin` without the
  explicit grant) → 403 on every payroll route; self-service returns only the caller's
  payslips.
- `EmployeeSensitiveFieldTest` — `nad:employee:view` without `:sensitive:view` gets a
  masked record; unmasked read writes an audit row.
- `FinanceConfidentialityTest` — commission / per-partner revenue require
  `nad:partnership:terms:view`; no finance field on public/student routes.
- `PaymentStudentScopeTest` — a student token retrieves only its own payments and
  never a provider reference / reconciliation field.
- Extend the existing `RolePermissionMatrixTest` data set with the new tokens and
  their "only these roles" assertions.
