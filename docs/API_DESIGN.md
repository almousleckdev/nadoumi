# Nadoumi — API Design

> Read `docs/PLATFORM_ARCHITECTURE.md` first — the approved enterprise-platform direction this doc rolls up to (2026-09-02).

Status: **BASELINE** · **EXISTING** · **PLANNED** · **OPEN**.

> Reconciled with Rev 3. The §7 items that were open are now decided
> (**recommended, pending your final nod** where marked).

---

## 1. Current API surface (EXISTING)

Spring MVC `@RestController`s under `ruoyi-admin/.../web/controller` + `GenController` +
`SysJob*Controller`. Base path `/` (no prefix). Consumed by `ruoyi-ui` via dev proxy
`/dev-api/*` → `:8080`.

### Auth / bootstrap
| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| GET | `/captchaImage` | anon | captcha image + uuid |
| POST | `/login` | anon | `{username,password,code,uuid}` → `{token}` |
| POST | `/register` | anon (if enabled) | self-registration |
| POST | `/logout` | bearer | invalidate token |
| GET | `/getInfo` | bearer | current user, roles, permissions, pwd-policy flags |
| GET | `/getRouters` | bearer | dynamic menu tree for the SPA |

### System / Monitor / Tool / Common
`/system/**`, `/monitor/**`, `/tool/**`, `/common/{upload,uploads,download,download/resource}`,
`/swagger-ui.html`, `/v3/api-docs/**`, `/druid/**`. (Details unchanged from Rev 1.)

## 2. Response conventions (EXISTING)

- `AjaxResult` (a `HashMap` → `{code,msg,data?}`, HTTP almost always 200) for
  commands; `TableDataInfo` (`{code,msg,rows,total}`) for lists; pagination via
  `pageNum`/`pageSize`; `GlobalExceptionHandler` maps errors to `AjaxResult`.
- Controllers extend `BaseController`.
- **Kept as-is for the existing `/system|/monitor|/tool` console endpoints** — the Vue
  admin depends on them.

## 3. Why not reuse this for Nadoumi

HTTP 200 for everything breaks clients/caching/observability; `AjaxResult extends
HashMap` is untyped and serializes entity graphs (fastjson `@type` hints visible in
`/getInfo`); no versioning; no audience separation.

## 4. Audience segmentation (BASELINE)

| Namespace | Audience | Auth | Notes |
| --- | --- | --- | --- |
| `/api/public/**` | anonymous visitors, crawlers | none | catalog (universities, programmes, scholarships **from `v_scholarship_student`**), content pages. Real 200/404, `Cache-Control` + `ETag`. SEO surface (`FRONTEND_ARCHITECTURE.md`). |
| `/api/student/**` | authenticated externals (`user_type` 10/20/30) | bearer JWT + `nad_user_applicant_access` **capability** check | profile, applications, documents, conversations, notifications, SSE stream. Every response is a student-view DTO. |
| `/api/staff/**` | staff (`user_type='00'`) | bearer JWT + RBAC token (`nad:*`) + data/assignment scope | application workbench, catalog admin, workflow, partnerships, reporting. |
| `/api/internal/**` | services / jobs / provider webhooks | network-restricted + signed/mTLS | payment + notification webhooks, batch. |

Existing `/system|/monitor|/tool` stay staff-only, unchanged. New Nadoumi work lives
under `/api/...`.

### 4.1 Authentication endpoints (IMPLEMENTED — Phase 3; Revision 2 additions marked NEW)

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/login` | staff only — **rejects `user_type != '00'`**. |
| POST | `/api/student/register` | externals only. Gated by `nad.student.register.enabled`. **NEW (Revision 2):** body `{ firstName, lastName, email, password, ticket }`; requires an OTP `ticket` proving the email was verified; `user_name` is server-generated; `email_verified=1`; password checked by `PasswordPolicy`. |
| POST | `/api/student/login` | externals only — **rejects `user_type = '00'`**. **NEW (Revision 2):** body `{ email, password, code?, uuid? }` — email-first. Unknown email returns the same generic error as a wrong password. Same `SysLoginService` pipeline (captcha, lockout, JWT, Redis session). |
| POST | `/api/student/email-otp` | **NEW.** `@Anonymous`, rate-limited (20/h/IP), captcha-aware. `{ email, purpose: REGISTER \| PASSWORD_RESET, code?, uuid? }` → **always** `200 { sent: true, throttled: <bool>, retryAfter: <sec> }`. Non-enumerating: an already-registered email gets an "account exists" mail instead of an OTP; `throttled` reflects **only** the caller's own 60 s resend cooldown (`true` ⇒ no mail was sent this call, `retryAfter` = remaining seconds), never registration state. |
| POST | `/api/student/email-otp/verify` | **NEW.** `@Anonymous`, rate-limited (10/10min/IP). `{ email, purpose, otp }` → `200 { ticket }` (opaque, single-use, ~10 min) \| `400` problem+json. |
| POST | `/api/student/password/reset` | **NEW.** `@Anonymous`, rate-limited. `{ ticket, newPassword }` (ticket from a `PASSWORD_RESET` OTP). `PasswordPolicy`, BCrypt, `pwd_update_date=now`, **all sessions revoked**, `204`. No token, no auto-login. |
| POST | `/api/student/password` | **NEW.** bearer (student). `{ currentPassword, newPassword }` → verify current, `PasswordPolicy` (incl. ≠ current), **other sessions revoked**, `204`. |
| GET | `/api/student/me` | student-shaped identity: `{ user:{id,name,locale}, accessibleApplicants:[{applicantId, accessRole, capabilities[]}] }`. **No** `roles`/`permissions`. |
| POST | `/api/student/logout` | invalidate token. |
| GET | `/api/dev/mail/latest?to=` | **NEW, non-prod.** Only mounted when `nadoumi.mail.transport=log`. Returns the last email captured for a recipient — E2E OTP retrieval. |

The Nuxt BFF (D2) sets the JWT in an httpOnly + Secure + SameSite=Lax cookie; browser
JS never holds the raw token. Revision 2 BFF passthroughs:
`student-email-otp.post`, `student-email-otp-verify.post`,
`student-password-reset.post`, `student-password.post` (cookie-authed).
Full design: `docs/superpowers/specs/2026-09-02-nadoumi-web-public-site-design.md` §15.

### 4.2 Applicant onboarding endpoints (PROPOSED — later phase)

The full guided onboarding (identity extras, residence branch, interests, languages,
work, certifications, profile photo, passport) adds a set of section endpoints under
`/api/student/applicants/{id}/…` — `profile`, `residence`, `interests`, `languages`,
`work`, `certifications`, `photo`, `passport`, `onboarding`. Full contract:
`docs/APPLICANT_ONBOARDING.md` §6. **REQUIRES BACKEND — not implemented.**

**Revision 3 (spec D-R3-3):** the `/dashboard/onboarding` wizard *shell* ships, but
it only calls the **EXISTING** endpoints — `GET/PUT /api/student/applicants/{id}`,
`.../education`, `.../contacts`. The PROPOSED section endpoints above
(`profile` extras, `residence`, `interests`, `languages`, `work`,
`certifications`, `photo`, `passport`, `onboarding`) remain PLANNED; the wizard
renders those fields disabled with a "not saved yet" note and never posts them.

### 4.3 Staff applicant endpoints (IMPLEMENTED — consumed by `nadoumi-admin`)

`/api/staff/applicants` — `StaffApplicantController`, all `@PreAuthorize`'d.

| Method | Path | Permission | Notes |
| --- | --- | --- | --- |
| GET | `/api/staff/applicants` | `nad:applicant:list` | `name`, `status`, `nationality`, `createdAfter`, `page`, `size` → `PageResponse<ApplicantResponse>`. |
| GET | `/api/staff/applicants/{id}` | `nad:applicant:view` | `ApplicantResponse` — `dob` + `passportNo` **masked** (`••••` → serialized `null`) unless the caller holds `nad:applicant:pii:view`. |
| POST | `/api/staff/applicants` | `nad:applicant:create` | interim-owner + invite flow. `201`. |
| PUT | `/api/staff/applicants/{id}` | `nad:applicant:edit` | |
| DELETE | `/api/staff/applicants/{id}` | `nad:applicant:archive` | soft archive, `204`. |
| GET / POST / PUT / DELETE | `/api/staff/applicants/{id}/education[/{eduId}]` | view / edit | `POST` → `201`; `PUT` → `200`; `DELETE` → `204`. Cross-applicant id → `404`. |
| GET / POST / PUT / DELETE | `/api/staff/applicants/{id}/test-scores[/{scoreId}]` | view / edit | idem |
| GET / POST / PUT / DELETE | `/api/staff/applicants/{id}/contacts[/{contactId}]` | view / edit | idem |
| GET | `/api/staff/applicants/{applicantId}/access` | `nad:applicant:access:view` | `List<AccessGrantResponse>` — grantee `userId` / `invitedEmail`, role, status, interim flag, granted/expires, effective capabilities (`StaffApplicantAccessController`). |
| POST / DELETE / POST `/transfer-ownership` | `/api/staff/applicants/{applicantId}/access[/{grantId}]` | `nad:applicant:access:manage` | delegate / revoke / transfer ownership. |

**Admin uses:** list + `GET {id}` + full CRUD on the three sub-resources + the
access `GET`. **Remaining:** surface grant / revoke / transfer in the UI; add a
`sys_user` join so the access list shows a name rather than `User #id`.

### 4.4 University endpoints (IMPLEMENTED — `nadoumi-university`)

**Staff** — `/api/staff/universities`, `StaffUniversityController`. Public catalog
data only; no partnership / commercial field is ever returned here. The request
body carries the full profile (§5.3 `nad_university` columns) plus three edited-whole
child lists: `rankings[]` (`source`, `rankPosition`, `rankYear?`, `note?`),
`highlights[]` (`kind` = HIGHLIGHT|ADVANTAGE, `text`) and `gallery[]` (`imageUrl`,
`caption?` — campus life / dormitory / campus view, **≤ 6**, `@Size(max=6)` on the
request, order preserved). A save replaces all three child sets inside the
scalar-update transaction. `recommended` / `featured` are optional booleans
(default `false`). `UniversityResponse` additionally returns `status`,
`publishStatus`, `remark`, audit timestamps.

| Method | Path | Permission | Notes |
| --- | --- | --- | --- |
| GET | `/api/staff/universities` | `nad:university:list` | `q` (name EN/CN, city), `country`, `province`, `city`, `type`, `status`, `page`, `size` → `PageResponse<UniversityResponse>`. |
| GET | `/api/staff/universities/{id}` | `nad:university:view` | assembles `rankings` + `highlights` + `gallery`. |
| POST | `/api/staff/universities` | `nad:university:create` | `201`. Country normalised to upper-case; `(name, country)` must be unique → `400` otherwise. |
| PUT | `/api/staff/universities/{id}` | `nad:university:edit` | same unique guard (excluding self); replaces children. |
| DELETE | `/api/staff/universities/{id}` | `nad:university:remove` | `204`. Hard delete — only while nothing references the row (FKs from programmes / partnerships will block it once those exist). Children go with `ON DELETE CASCADE`. |

Both responses also carry `logoImageUrl` / `coverImageUrl` (URL strings; the admin
uploads through RuoYi `/common/upload`, migrating to `document_id` with the
Document slice).

**Public** — `/api/public/universities`, `PublicUniversityController`, `@Anonymous`.
Serves `PublicUniversityResponse` (no `status` / `publishStatus` / `remark` / audit;
the detail body carries `rankings[]`, `highlights[]`, `gallery[]`, `logoImageUrl`,
`coverImageUrl`) and **only** rows with `publish_status='PUBLISHED' AND status='ACTIVE'`.

| Method | Path | Permission | Notes |
| --- | --- | --- | --- |
| GET | `/api/public/universities` | anonymous | `q`, `country`, `province`, `city`, `type` (PUBLIC/PRIVATE), `featured` (bool), `recommended` (bool), `page`, `size` (default 12) → `PageResponse<PublicUniversityResponse>`. Ordered featured → recommended → name. Consumed by the Home carousels + the `universities` list. |
| GET | `/api/public/universities/{id}` | anonymous | `404` if the row is not PUBLISHED + ACTIVE. Consumed by `nadoumi-web` `universities/[id].vue`. |

A `/api/public/universities/facets` (distinct province / city / type with counts) is
planned with the Universities-list redesign (PR-2).

### 4.5 Public contact endpoint (IMPLEMENTED — `nadoumi-identity`)

`PublicContactController`, `@Anonymous`, IP rate-limited (10 / hour). Persists a
`nad_contact_inquiry` row and emails `nadoumi.mail.supportInbox`; a mail failure
is logged, not surfaced. A filled honeypot (`website`) is accepted and dropped.

| Method | Path | Permission | Notes |
| --- | --- | --- | --- |
| POST | `/api/public/contact` | anonymous | body `{ firstName, lastName, email, phone?, category?, subject?, message, locale?, website? }` → `202` no body. `400` (problem+json) on validation failure. `name` is composed server-side. Consumed by `nadoumi-web` `contact.vue` via the BFF `/api/public/**` passthrough. |

### 4.6 Scholarship endpoints (IMPLEMENTED — `nadoumi-scholarship`, R3)

**Public** — `PublicScholarshipController`, `@Anonymous`. Every read is sourced
from `v_scholarship_student` + the student-safe child tables. The response
(`PublicScholarshipResponse`) carries **no** `universityId` / `partnership` /
commission / internal field on any path (`StaffScholarshipTest` enforces this).
It does carry `heroImageUrl` / `coverImageUrl` (student-safe imagery, URL strings)
on both card and detail rows. The `slug` is always derived server-side from the
title (kebab-case, lower-cased, de-duplicated); there is no client-supplied slug.
Category applicability is enforced by funding model: `SELF` carries no categories,
`PARTIAL` cannot carry CSC / CGS / government Type A–D.

**Money (V22).** Every monetary value on the detail response is presented in both
RMB and USD — `fees[]`, `applicationFee`, `serviceFee`, `stipends[]` and
`accommodation[]` each carry `amountRmb` + `amountUsd` + `currency`. The stored
figure is kept as entered; the other side is computed at a fixed display rate
(`PublicScholarshipResponse.RMB_PER_USD`, 7.10), rounded to whole units — a
display convenience, not an FX quote.

**Stipend + accommodation + non-degree duration (V22).** `stipend` (single) is
replaced by `stipends[]` — one entry per accepted education level (`level`,
amounts, `frequency`, `durationMonths?`, `conditions?`). `accommodation[]` lists
room types (`roomType` SINGLE/DOUBLE/TRIPLE/QUAD/SHARED, amounts, `note` for
amenities). `nonDegreeDuration` (`HALF_YEAR` / `ONE_YEAR`) applies when NON_DEGREE
is an accepted level. The staff request sends `levelStipends[]`, `accommodations[]`
and `nonDegreeDuration`; fee/stipend/accommodation amounts are entered once with a
currency.

**Terms (V23).** `coverage[]` (`kind` TUITION/ACCOMMODATION/STIPEND/
MEDICAL_INSURANCE/SETTLEMENT_ALLOWANCE/TRAVEL/REGISTRATION_FEE/VISA_FEE/OTHER +
`detail?`) is a structured list of what the award pays for, alongside the prose
`benefits` — detail-only. `renewalConditions` (detail-only prose) is the annual
review / renewal terms. `studyDurationMonths`, `applicationChannel`
(DIRECT_UNIVERSITY/CSC_AGENCY/NADOUMI/OTHER), `agencyNumber`,
`requiresFinancialProof`, `requiresFoundationYear` are on card + detail. The staff
request sends `coverage[]` and these scalars; `agencyNumber` is kept only when the
channel is `CSC_AGENCY`.

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/api/public/scholarships` | `q`, `country`, `province`, `city`, `field`, `language`, `funding`, `hasStipend`, `deadlineBefore`, `level` (csv), `category` (csv), `intake` (csv), `featured`, `recommended`, `hot`, `sort` (`deadline`\|`newest`\|`title`), `page`, `size` (default 12) → `PageResponse<PublicScholarshipResponse>` (card rows: + `levels`, `categories`, `intakes`). |
| GET | `/api/public/scholarships/facets` | same filters → `{ levels[], categories[], fundingModels[], teachingLanguages[] }` of `{ value, count }` — live counts for the current filter set. |
| GET | `/api/public/scholarships/{slugOrId}` | `PublicScholarshipResponse` detail (+ `benefits`/`requirements`/`policy`, `eligibility`, `fees[]`, `stipend`, `documentRequirements[]`). `404` if not PUBLISHED+ACTIVE. |

**Staff** — `StaffScholarshipController`. Student-safe CRUD under
`nad:scholarship:*`; the confidential linkage sub-resource is gated separately.

| Method | Path | Permission | Notes |
| --- | --- | --- | --- |
| GET | `/api/staff/scholarships` | `nad:scholarship:list` | `q`, `country`, `funding`, `publishStatus`, `status`, `page`, `size` → `PageResponse<ScholarshipResponse>` (`{ view, status, publishStatus, publishedAt, remark, … }`). |
| GET | `/api/staff/scholarships/{id}` | `nad:scholarship:view` | assembles all children. |
| POST | `/api/staff/scholarships` | `nad:scholarship:create` | `201`. Slug auto-derived from `title` (kebab-case, lower-cased), de-duplicated — no client slug field. Children replaced whole in one transaction; disallowed categories for the funding model are dropped. |
| PUT | `/api/staff/scholarships/{id}` | `nad:scholarship:edit` | idem; `published_at` is set on the first transition to PUBLISHED and never rewritten. |
| DELETE | `/api/staff/scholarships/{id}` | `nad:scholarship:remove` | `204`. Children + `nad_scholarship_internal` cascade. |
| GET | `/api/staff/scholarships/{id}/internal` | `nad:scholarship:internal:view` | `ScholarshipInternalResponse` (`universityId`, `universityName` via `UniversityService`, `partnershipId`, `internalStatus`, operational / confidential fields). |
| PUT | `/api/staff/scholarships/{id}/internal` | `nad:scholarship:internal:edit` | upsert; `universityId` validated via `UniversityService`. |

### 4.7 Programme endpoints (IMPLEMENTED — `nadoumi-program`, R2)

A **programme** belongs to exactly one university (`nad_program.university_id`
→ `nad_university` ON DELETE RESTRICT). The owning university is **validated on
write and its name resolved on read through `UniversityService`** — never a
cross-module SQL join. No commercial data. `ProgramResponse` (staff) carries
`status` / `publishStatus` / `remark` / audit + `universityName` + `majors` +
`intakes`; `PublicProgramResponse` drops the operational fields, and list rows
drop `majors` / `intakes` (the detail endpoint fills them).

**Staff** — `/api/staff/programs`, `StaffProgramController`.

| Method | Path | Permission | Notes |
| --- | --- | --- | --- |
| GET | `/api/staff/programs` | `nad:program:list` | `q` (name EN/CN, field), `universityId`, `type`, `language`, `field`, `status`, `page`, `size` → `PageResponse<ProgramResponse>`. |
| GET | `/api/staff/programs/{id}` | `nad:program:view` | assembles `majors` + `intakes`. |
| POST | `/api/staff/programs` | `nad:program:create` | `201`. `universityId` must resolve (`400` otherwise); `(university_id, name)` unique (`400` otherwise). Children replaced whole in one transaction. |
| PUT | `/api/staff/programs/{id}` | `nad:program:edit` | idem. |
| DELETE | `/api/staff/programs/{id}` | `nad:program:remove` | `204`. `majors` / `intakes` cascade. |

**Public** — `PublicProgramController`, `@Anonymous`. A programme is visible only
when **both** it and its university are `publish_status='PUBLISHED' AND
status='ACTIVE'`.

| Method | Path | Permission | Notes |
| --- | --- | --- | --- |
| GET | `/api/public/programs` | anonymous | `q`, `universityId`, `type`, `language`, `field`, `featured`, `hot`, `page`, `size` (default 12) → `PageResponse<PublicProgramResponse>` (cards; carries `universityId` + `universityName`). Programmes whose university has since been unpublished are dropped and the page count corrected. Consumed by the Home "Hot programmes" carousel. |
| GET | `/api/public/programs/{id}` | anonymous | adds `majors` + `intakes`. `404` unless programme + university are both published/active. Consumed by `nadoumi-web` `programs/[id].vue`. |
| GET | `/api/public/universities/{id}/programs` | anonymous | the published programmes for one published university → `List<PublicProgramResponse>` (cards). `404` if the university is not public. Consumed by `universities/[id].vue`. |

## 5. Conventions for `/api/**` endpoints (BASELINE)

- **Real HTTP status codes.** 200/201/204; 400 validation; 401 unauthenticated; 403
  authorization/confidentiality; 404 not found / not visible; 409 optimistic-lock
  conflict; 422 business-rule rejection.
- **Typed payloads.** Java `record` request/response DTOs per context. **Never** a
  MyBatis/JPA entity on the wire. Explicit field-by-field mapping (MapStruct or hand);
  no reflective copy from an entity to a confidential-adjacent DTO.
- **Response envelope — DECIDED (recommended, pending final nod):** **bare resource
  bodies** everywhere (no `ApiResponse<T>` wrapper); errors as RFC 9457
  `application/problem+json`. `AjaxResult` is not reused under `/api/**`.
- **Pagination:** `?page=0&size=20&sort=field,desc`; response
  `{ content, page, size, totalElements, totalPages }` (Spring `Page` shape).
- **Versioning — DECIDED (recommended, pending final nod):** URI prefix **`/api/v1/...`**.
- **Validation:** `jakarta.validation` on DTOs + `@Validated` controllers.
- **Idempotency:** `Idempotency-Key` header required on `POST` for payments and
  application submission.
- **Auditing:** `@Log(title, businessType)` on every state-changing endpoint; business
  history via `nad_application_event` / `nad_document_event` (distinct from
  `sys_oper_log`).
- **OpenAPI:** widen `springdoc` `packages-to-scan` to the Nadoumi controllers; one
  group per namespace; **Swagger UI disabled in production**.
- **Rate limiting:** `@RateLimiter` on auth, upload, message-post, and search
  endpoints.

## 6. Confidentiality rules on APIs (BASELINE — enforced, tested)

- `/api/public/scholarships/**` and `/api/student/scholarships/**` read
  **`v_scholarship_student`** and return `ScholarshipStudentView` only (no
  university/partnership fields — the record cannot carry them).
- Internal scholarship linkage: **only** `/api/staff/scholarships/{id}/internal`,
  `@PreAuthorize("@na.canViewScholarshipInternal()")`.
- Partnerships: **only** `/api/staff/partnerships/**`; no public/student route exists.
- A programme under a partner university is indistinguishable from one under a
  non-partner university on `/api/public/**` (no `isPartner`, no badge).
- A response-body denylist advice on `/api/public|student` aborts (500 + security-log)
  if a forbidden key (`universityId`, `partnership`, `commission`, …) appears on a
  scholarship/university payload.
- Confidential `sort`/`filter`/`groupBy` params → 400.
- CI-blocking: `ScholarshipConfidentialityTest`, `PartnershipExposureTest`,
  `UniversityPartnerLeakTest`.

## 7. Decision status

| Item | Status |
| --- | --- |
| Audience namespaces `/api/{public,student,staff,internal}` | **APPROVED** |
| Response envelope | **DECIDED** — bare bodies + `problem+json`; *final confirmation pending*. |
| Versioning | **DECIDED** — URI `/api/v1`; *final confirmation pending*. |
| `/api/public` serving | **APPROVED** — same Spring app origin; a CDN/edge cache sits in front (reverse proxy, `DEPLOYMENT.md`). |
| Error format (`problem+json`) scope | **APPROVED** — all `/api/**`; existing console endpoints keep `AjaxResult`. |
| Student auth via `/api/student/login` + capability authz | **APPROVED** (D7/D10). |
| Per-environment CORS origins | **OPEN** — deploy-time config, not design. |
