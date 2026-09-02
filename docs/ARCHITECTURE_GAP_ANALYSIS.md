# Nadoumi — Architecture Gap Analysis

**Date:** 2026-09-02
**Status:** authoritative snapshot. Supersedes ad-hoc notes in commit messages.
**Trigger:** implementation paused to resolve accumulated architecture / UX drift
before more product code is written.

Labels used throughout: **EXISTING** (in the codebase now, verified) ·
**BROKEN** (present but not working) · **PLANNED** (documented, not built) ·
**DECISION** (needs a call recorded here).

---

## 0. How this snapshot was taken

Every claim below was checked against the running system on 2026-09-02, not
inferred from docs:

- `curl` against the live Spring API (`:8080`), the Nuxt BFF (`:3000`), Mailpit
  (`:8025`), and `redis-cli` inside the `nadoumi-redis` container.
- `git log` / `git show` for the history of `ruoyi-ui/`, `nadoumi-admin/`,
  `nadoumi-web/`, `nadoumi-modules/`.
- Source reads of the OTP chain end to end.
- `mvn clean verify` (all suites green) + `nadoumi-admin` typecheck/lint/build.

---

## 1. Broken OTP — root cause

### 1.1 Verdict

The OTP chain is **architecturally sound and currently working end to end.**
Verified on 2026-09-02:

| Layer | Evidence |
| --- | --- |
| `POST :8080/api/student/email-otp` | `200 {"sent":true}` |
| `POST :3000/api/student-email-otp` (via Nuxt BFF) | `200 {"sent":true}` |
| Redis | `nad:otp:REGISTER:<sha256(email)>` TTL ~600s, `nad:otp:cooldown:REGISTER:<…>` TTL ~60s |
| SMTP → Mailpit | message "Verify your email" delivered to the test address |
| `POST /api/student/email-otp/verify` (wrong code) | clean `400` RFC-7807 `{"detail":"verification code is invalid or expired"}` |

So "OTP does not arrive" was **not** a permanent defect in the chain. Two
concrete causes explain the report.

### 1.2 Cause A — transient: a corrupt backend jar (now fixed)

Earlier in the session two `mvn … package` runs executed concurrently and wrote
`ruoyi-admin/target/` at the same time. The resulting fat jar threw at runtime:

```
NoClassDefFoundError: Could not initialize class com.ruoyi.common.utils.http.UserAgentUtils
```

While that jar was the running process, `/login` returned `500` and the app was
broadly unhealthy — any OTP attempt in that window failed. **Fixed:** rebuilt via
`mvn clean verify` (17 surefire suites + 1 failsafe suite, 0 failures) and
restarted the backend from the clean artifact. Login and OTP verified green
afterwards.

**Follow-up:** never run two packaging builds against the same module tree.
Document the single restart path (see §10) so this cannot recur.

### 1.2b Related — integration tests were not hermetic for mail

`application.yml` imports `optional:file:./config/application-local.yml`, and that
developer file sets `nadoumi.mail.transport=smtp` + `spring.mail.*` → Mailpit.
Because `mvn verify` runs from the repo root, the Testcontainers ITs **also**
loaded it and quietly sent OTP mail to the real host Mailpit instead of the
in-memory `LoggingMailSender`. That is why the first run of the new
`OtpEndToEndTest` failed — `DevMailController` (mounted only under
`transport=log`) 401'd. **Fixed:** `AbstractNadIntegrationTest.wire()` now forces
`nadoumi.mail.transport=log` via `@DynamicPropertySource` (highest precedence), so
the ITs are network-free for mail regardless of any local override.

### 1.3 Cause B — real defect: silent resend during the cooldown

`OtpService.issue()`:

```java
if (Boolean.TRUE.equals(redis.hasKey(cooldownKey(purpose, email)))) {
    return;                     // <-- no new code, no mail, no signal
}
```

The controller then unconditionally returns `OtpSentResponse(true)`. So a resend
inside the 60s window returns `{"sent":true}` while **nothing is emailed**. On
the happy path the frontend's own 60s countdown disables the button, but any
state desync makes it look broken:

- page reload drops the JS timer → user clicks "Send code" again → "sent", no email;
- first mail is slow → user retries → "sent", no email;
- no positive "code re-sent" vs "still on the way" distinction anywhere.

This is the piece that makes a *healthy* backend feel broken.

**Fix (this phase):**

1. `OtpService.issue()` returns an outcome (`SENT` / `THROTTLED`) instead of
   `void`; the controller maps it to `{"sent":true,"throttled":true,"retryAfter":<sec>}`
   using the live cooldown-key TTL.
2. `<EmailVerifyStep>` shows "Code already sent — check your inbox and spam. You
   can request another in Ns." for `throttled`, and "New code sent" otherwise.
3. Backend integration test (see §1.5).

### 1.4 Investigation friction — misleading rate-limiter log

`com.ruoyi.framework.aspectj.RateLimiterAspect` logs, at **INFO on every
successful call**:

```
rate limit '20' exceeded, current '6', cache key 'rate_limit:127.0.0.1-…-request'
```

The word "exceeded" is wrong — it fires on the allowed path (`current < limit`).
It actively misdirects anyone reading logs during an OTP incident. **Fix:**
reword to `rate limit check: {}/{} for key '{}'` (upstream RuoYi file; one-line,
low-risk, English-only per project rules).

### 1.5 Required integration test (new)

`ruoyi-admin/src/test/java/com/ruoyi/nadoumi/OtpEndToEndTest.java`, extending
`AbstractNadIntegrationTest` (full context, Testcontainers MySQL + Redis, profile
`{druid,test}` → `LoggingMailSender` is the active `MailSender`):

- `POST /api/student/email-otp` → `200 {"sent":true}`.
- Redis: `nad:otp:REGISTER:<sha256(email)>` present, TTL in `(0, 600]`;
  cooldown key present, TTL in `(0, 60]`.
- `GET /api/dev/mail/latest?to=<email>` → `200`, subject `Verify your email`,
  body contains a 6-digit code.
- Immediate second `POST` → `200`, **no new mail** (same message timestamp),
  response carries `throttled:true`.
- `POST /api/student/email-otp/verify` with the extracted code → `200` + a
  non-blank ticket; the Redis OTP key is gone.
- `verify` again with the same code → `400`.

This proves *generated → stored → sent → retrievable → single-use* in one run.

---

## 2. The two-admin problem

### 2.1 What actually exists (port mapping corrected)

| | `nadoumi-admin` | `ruoyi-ui` |
| --- | --- | --- |
| **Dev URL** | **`http://localhost:8082`** | **`http://localhost:1024`** (wants `:80`, falls back) |
| Stack | Vue 3 + Vite + TS + Element Plus + Pinia | Vue 2 + vue-cli-service + Element UI + Vuex |
| `package.json` name | `nadoumi-admin` | `ruoyi` (stock) |
| Title | `Nadoumi Admin` | 若依管理系统 |
| Commits touching it | **2** (baseline + a login-toast fix) | 696 (full upstream RuoYi history; **2** Nadoumi tweaks) |
| Nadoumi business screens | **Dashboard** (real, live applicant metrics) + **Applicants** (real list/create/archive → `/api/staff/applicants`) | **none** |
| RuoYi infra screens | none — all resolve to `Placeholder` | all implemented (users, roles, menus, monitor, generator, …) |
| Backend menu source | `GET /getRouters` → rendered dynamically; unknown `component` → `Placeholder` | `GET /getRouters` → rendered dynamically (stock RuoYi resolver) |
| API base | `/dev-api` → proxy `:8080` | `/dev-api` → proxy `:8080` |
| Auth | JWT in cookie `nadoumi-admin-token`; `permission.ts` guard + `fetchInfo()` | JWT in cookie `Admin-Token`; stock RuoYi guard |

> The original report had the two ports swapped. `:8082` is the Nadoumi target
> app; `:1024` is stock RuoYi.

### 2.2 Why both exist

`ruoyi-ui` is the **vendored upstream RuoYi-Vue frontend**, imported wholesale
with the backend. It is the reference implementation for every RuoYi platform
screen (RBAC, menus, dict, config, monitor, code-gen) and the fallback UI while
`nadoumi-admin` is being built out. `nadoumi-admin` is the **purpose-built
replacement** — a thin Vue 3 shell (layout + i18n + router + guard + login +
profile) that renders the same backend menu but supplies Nadoumi-designed
screens, starting with Dashboard and Applicants.

### 2.3 Duplication assessment

**Low and expected.** The two apps share *no code* (different major framework).
The only conceptual overlap is the HTTP client (`request.js` 165 lines vs
`request.ts` 80 lines) and the auth/permission bootstrap — both re-derived
idiomatically per framework, not copy-pasted. There is **no duplicated Nadoumi
business logic**, because `ruoyi-ui` contains none.

### 2.4 Decision (recorded)

- **`nadoumi-admin` is the single Nadoumi admin application.** All Nadoumi
  business UI is built there.
- **`ruoyi-ui` is reference-only and frozen.** No Nadoumi feature work, no new
  commits except security bumps. It stays runnable until `nadoumi-admin` reaches
  parity on the RuoYi platform screens that operations actually need (RBAC, dict,
  config, jobs, online users, cache) — tracked in `ADMIN_ARCHITECTURE.md` §2.
- **Not deleted yet.** Removal is a separate, later change with its own checklist.
- **Dev scripts:** the "start the environment" path starts `nadoumi-admin` on
  `:8082`. `ruoyi-ui` is opt-in (`cd ruoyi-ui && npm run dev`), not part of the
  default up.

---

## 3. Maven module architecture — current vs target

### 3.1 Physical modules that exist (EXISTING)

```
ruoyi-common  ruoyi-framework  ruoyi-system  ruoyi-quartz  ruoyi-generator  ruoyi-admin
nadoumi-modules/
├── nadoumi-common      (shared: access matrix, error types, base test support)
├── nadoumi-identity    (student auth, email OTP, password policy, session revoke, mail)
└── nadoumi-applicant   (applicant profile, education, test scores, contacts, staff list)
```

This already follows the rule "create a physical module when implementation of a
bounded context begins." No empty scaffold modules exist.

### 3.2 Logical domains → module status

| Domain | Status | Module |
| --- | --- | --- |
| Identity & Access | **EXISTING** | `nadoumi-identity` (+ RuoYi `sys_*`) |
| Applicant | **EXISTING** | `nadoumi-applicant` |
| University | PLANNED — CURRENT PHASE candidate | — |
| Program | PLANNED | — |
| Scholarship | PLANNED (student/staff split already specified) | — |
| Application | PLANNED — NEXT PHASE | — |
| Document | PLANNED | — |
| Workflow | PLANNED | — |
| Partnership | PLANNED | — |
| Communication (chat) | PLANNED | — |
| Notification | PLANNED | — |
| Content / CMS | PLANNED | — |
| Employee | PLANNED | — |
| Finance / Payroll / Payment | PLANNED | — |
| Reporting / Analytics | PLANNED (dashboard read models `rm_*`) | — |

### 3.3 Gap

None structural. The risk is **premature module creation** — the guard is: a new
`nadoumi-modules/<x>` appears only in the same change that adds its first
entity + Flyway migration + endpoint + test.

---

## 4. Admin navigation — current vs target

### 4.1 Current (EXISTING)

The menu tree comes from `sys_menu` (Flyway `V2` seed + `V6` English relabel) and
is rendered by whichever admin is open. Top level:

```
Dashboard        → nadoumi-admin: real ; ruoyi-ui: stock widgets
System           → RuoYi RBAC/dict/config     (ruoyi-ui only)
Monitoring       → RuoYi online/cache/server  (ruoyi-ui only)
Tools            → RuoYi code-gen/form-builder (ruoyi-ui only)
Nadoumi
└── Applicants   → nadoumi-admin: real ; component path nadoumi/applicant/index
```

In `nadoumi-admin` every `System` / `Monitoring` / `Tools` leaf renders
`views/placeholder.vue` ("This screen is not part of the Nadoumi admin yet").
That placeholder is **correct as a transitional state** but must not be counted
as a feature and must not ship as the final experience.

### 4.2 Target (from `ADMIN_ARCHITECTURE.md`)

Two clearly separated groups in the sidebar:

**Platform (RuoYi infrastructure)** — Users · Roles · Menus · Departments ·
Posts · Dictionaries · Config · Notices · Operation log · Login log ·
Online users · Scheduled jobs · Cache · (dev-only) Code generator / API docs.

**Nadoumi business** — Dashboard · Applicants · Applications · Universities ·
Programs · Scholarships · Documents · Partnerships · Employees · Finance ·
Payroll · Payments · Marketing/CMS · Communication · Notifications ·
Reports & Analytics.

### 4.3 Gap & rule

- Nadoumi business screens beyond Dashboard + Applicants: **not built.**
- The seed only defines the `Nadoumi → Applicants` branch; the other business
  branches are not in `sys_menu` yet. They get added per-domain, with the screen,
  in the same change.
- **Rule:** menu labels are not renamed RuoYi entries; each Nadoumi branch is a
  new `sys_menu` row with its own `nad:*` permission, added when its screen is
  real. Placeholder count ≠ feature count.

---

## 5. Public website (`nadoumi-web`) — current state

| Area | Status |
| --- | --- |
| Nuxt 3 SSR + Nitro BFF, httpOnly student cookie, i18n en/fr/ar/zh | **EXISTING** |
| Navbar `Home · Scholarships · Universities · Programs · Destinations · About · Contact` + `Sign in` / `Create account` + account dropdown | **EXISTING** — matches spec exactly (`app/components/marketing/SiteHeader.vue`) |
| Auth pages keep the site header (layout `auth.vue`), centered | **EXISTING** |
| 3-step register wizard (name → email+OTP → password+terms), no "Confirm email" | **EXISTING** |
| Shared `OtpInput` / `EmailVerifyStep` / `PasswordField` / `PasswordRequirements` / `ConsentCheckboxes` | **EXISTING** |
| `authErrorMessage()` single error-mapping util; `passwordChecks()` shared policy | **EXISTING** |
| Forgot-password real flow (email → OTP → new password → login) | **EXISTING** |
| `/dashboard/account` change-password (revokes other sessions) | **EXISTING** |
| Onboarding wizard shell (`Personal·Identity·Education·Interests·Location·Contact·Review`); EXISTING steps save, PLANNED steps show "not saved yet" | **EXISTING (shell)** |
| Client-only `ImageCropper` / `DocumentPreview` / `ProfilePhotoUploadCard` / `PassportUploadCard` (Upload disabled — no backend) | **EXISTING (client-only)** |
| Homepage value proposition, `/scholarships` `/universities` `/programs` `/destinations` content | **PARTIAL** — pages exist; content is thin, needs the real marketing pass (plan Tasks 20–22) |
| Fake stats / fake applications / fake API success | **NONE** — verified; unbuilt areas show "coming soon / not saved yet" |

### Gap

Mostly **content and polish**, not architecture: homepage + discovery pages need
the enterprise marketing rebuild. No fake data to remove.

---

## 6. Registration / password-reset gaps

| Requirement | Status |
| --- | --- |
| Step order name → "exactly as on your passport" → email → Verify | **EXISTING** |
| `Email: … [Edit email]` + OTP after Verify; email stays visible | **EXISTING** |
| `✓ Email verified` then password step | **EXISTING** |
| No `Confirm email` field anywhere | **EXISTING** (removed in `43bb62ef`) |
| `Next` disabled until email-verified ∧ password-valid ∧ match ∧ terms | **EXISTING** |
| Password 8–32 + upper/lower/digit/special; not first/last/username | **EXISTING** (`passwordChecks`, shared server+client) |
| Show/hide on both password fields; strength indicator | **EXISTING** |
| Forgot password: email → OTP → verify → new password → login; reuse `OtpInput` | **EXISTING** |
| OTP short-lived / single-use / rate-limited / non-enumerating | **EXISTING** (600s TTL, burned on verify, IP `@RateLimiter`, `/email-otp` never reveals existence) |
| **Resend gives real feedback (not silent `{sent:true}`)** | **BROKEN** — see §1.3, fix this phase |
| Account change-password revokes other sessions | **EXISTING** (`SessionRevoker.revokeAll`) |

**Net:** the flows are built and correct except the resend-feedback defect.

---

## 7. Onboarding backend gaps

The wizard shell is on the frontend; persistence is only as deep as the backend.

| Step | Frontend | Backend today | Gap |
| --- | --- | --- | --- |
| Personal / Identity | `ProfileForm` → saves | `nad_applicant` (given/family/dob/nationality/passport/email/phone/status) | none for these fields |
| Education | `EducationList` → saves | `nad_applicant_education` | none |
| Test scores | (not in wizard yet) | `nad_applicant_test_score` | wire into wizard later |
| Contacts | shown as "not saved yet" | `nad_applicant_contact` exists | **wire endpoint into the Contact step** |
| Interests | "not saved yet" | **no table** | **REQUIRES BACKEND** — `nad_applicant_interest` (fields of study, target degree, destinations, intake) |
| Location | "not saved yet" | **no columns** | **REQUIRES BACKEND** — residence country/city, timezone on `nad_applicant` |
| Passport upload | client-only preview, Upload disabled | **no document storage** | **REQUIRES BACKEND** — Document domain + object storage (see `DOCUMENT_MANAGEMENT.md`) |
| Profile photo | client-only crop, Upload disabled | **no avatar storage for applicants** | **REQUIRES BACKEND** — same |
| Review / submit | navigates to dashboard | no "onboarding complete" flag | **REQUIRES BACKEND** — `nad_applicant.onboarding_state` |

**Rule already followed:** PLANNED steps never fake a save (`no $fetch`, explicit
"not saved yet"). This must hold as steps are wired.

Owner doc: `APPLICANT_ONBOARDING.md` — its EXISTING / PLANNED / REQUIRES BACKEND
table is authoritative; keep it in sync as each step lands.

---

## 8. Duplicated code / components

| Area | Finding |
| --- | --- |
| `ruoyi-ui` vs `nadoumi-admin` | separate frameworks; no shared or copied Nadoumi logic (§2.3) |
| `nadoumi-web` OTP | **single** implementation (`OtpInput` + `EmailVerifyStep` + `useOtp`), reused by register + forgot-password |
| `nadoumi-web` password rules | **single** `passwordChecks()` util, shared by register + reset + account |
| `nadoumi-web` error mapping | **single** `authErrorMessage()` |
| `nadoumi-admin` dashboard | composed from reusable `DashboardGroup` / `StatCard` / `ComingSoonCard` / `RecentApplicants`; no ad-hoc widgets |
| Backend password policy | **single** `PasswordPolicy` (server) mirrored by one frontend util — intentional, not duplication |
| Mail sender | one `MailSender` port, two adapters chosen by property — intentional |

**No action.** The current duplication is limited to the unavoidable
cross-framework HTTP/auth bootstrap.

---

## 9. Placeholder screens

| Placeholder | Where | Legitimate? |
| --- | --- | --- |
| `nadoumi-admin/src/views/placeholder.vue` for every RuoYi infra route | admin sidebar `System`/`Monitoring`/`Tools` | **Yes, transitional** — but must read as "use the platform console" not "coming soon forever"; wire the ones operations need (§4.2) |
| Dashboard `ComingSoonCard` for Applications / Finance / Operations groups | admin dashboard | **Yes** — honest, no fake numbers; replaced per-domain |
| Onboarding "not saved yet" steps | `nadoumi-web` onboarding | **Yes** — no fake persistence |
| `/scholarships` `/universities` `/programs` thin content | `nadoumi-web` | **Partial** — real routes, needs marketing content, not a code gap |

**None are dishonest.** The rule to keep: a placeholder is never counted as a
delivered feature in status reports.

---

## 10. Recommended implementation order

**A. Stabilize (this phase — before any new product feature):**

1. ✅ Rebuild backend via `mvn clean verify`; restart from the clean jar.
2. ✅ Fix the **OTP resend feedback** defect (§1.3): `OtpService.issue()` →
   `IssueResult(sent, retryAfterSeconds)`, one cooldown key covering the
   OTP and "account exists" branches; `OtpSentResponse(sent, throttled,
   retryAfter)`; `<EmailVerifyStep>` shows "new code sent" vs "already sent —
   check spam". Verified live through the BFF.
3. ✅ Add `OtpEndToEndTest` (§1.5) — full context, Testcontainers Redis +
   `LoggingMailSender`; `AbstractNadIntegrationTest` now forces
   `nadoumi.mail.transport=log` so the ITs stay network-free (§1.2b).
4. ✅ Reword the `RateLimiterAspect` INFO log (§1.4).
5. Land the **single environment script** so the corrupt-jar race (§1.2) cannot
   recur; it must offer a `--mail=log` mode (E2E / `GET /api/dev/mail/latest`)
   and a `--mail=smtp` mode (dev, Mailpit) — document in `DEPLOYMENT.md`.
6. ✅ Record the **one-admin decision** (§2.4) in `ADMIN_ARCHITECTURE.md` §1.0;
   `ruoyi-ui` is opt-in, not in the default "up".
7. ✅ Docs updated in this change: this file, `ADMIN_ARCHITECTURE.md`,
   `FRONTEND_ARCHITECTURE.md`, `API_DESIGN.md`, `SECURITY.md`,
   `APPLICANT_ONBOARDING.md`.

**B. Admin parity + first business slice (next):**

7. `nadoumi-admin` Applicants screen polish to design system; wire dashboard
   "View all" to it.
8. Port the RuoYi platform screens operations actually need into `nadoumi-admin`
   (Users, Roles, Dict, Config, Online users, Jobs, Cache) — reusing the backend
   endpoints; no backend change.
9. Reporting read-model slice (`rm_applicant_*`, `rm_application_*`) → switch the
   dashboard Applicants group off the interim `/api/staff/applicants` source.

**C. Product domains (per approved architecture, one module per change):**

10. University → Program → Scholarship (student/staff response split) →
    Application (+ Workflow) → Document (+ object storage, unblocks onboarding
    uploads) → Communication / Notification → Finance / Payment → CMS →
    Reporting depth.

**D. Continuous:**

- Onboarding steps wired to real endpoints only as the backend lands (§7).
- `nadoumi-web` marketing rebuild (homepage + discovery pages).
- Playwright E2E stays hard-gated.

---

## Appendix — commands used for this snapshot

```bash
# OTP chain
curl -s -X POST localhost:8080/api/student/email-otp -d '{"email":"…","purpose":"REGISTER"}'
curl -s -X POST localhost:3000/api/student-email-otp  -d '{"email":"…","purpose":"REGISTER"}'
docker exec nadoumi-redis redis-cli --scan --pattern 'nad:otp:*'
curl -s localhost:8025/api/v1/messages
curl -s -X POST localhost:8080/api/student/email-otp/verify -d '{"email":"…","purpose":"REGISTER","otp":"000000"}'

# admin identity
curl -s localhost:8082/ | grep -o '<title>[^<]*</title>'   # Nadoumi Admin
curl -s localhost:1024/ | grep -o '<title>[^<]*</title>'   # 若依管理系统
git log --oneline -- ruoyi-admin/ nadoumi-admin/ ruoyi-ui/

# modules
grep -oE '<module>[^<]+</module>' pom.xml nadoumi-modules/pom.xml
```
