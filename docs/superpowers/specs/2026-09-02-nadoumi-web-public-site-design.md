# nadoumi-web — Public Website + Student Experience — Design

Date: 2026-09-02
Revised: 2026-09-03 (Revisions 2 & 3 — see below)
Status: APPROVED (Revision 3)
Scope owner: nadoumi-web (Nuxt 3 app in this repo)

---

## Revision 2 (2026-09-03) — scope corrections

Revision 1 deliberately deferred forgot-password, student change-password, and any
multi-step registration ("Fixed decisions": *Forgot-password: Deferred*; §7.3
account = *link to support*; §14 = *password-change links to support this phase*).
Product review rejected those deferrals. Revision 2 folds the following into scope
and the affected sections below are rewritten. **Nothing already shipped (plan
Tasks 1–17) is thrown away** — the design system, BFF, session model, and the
profile/education screens stand. Revision 2 adds auth-flow depth, a small backend
email/OTP slice, a navbar correction, a dashboard polish pass, and a *documented
but not implemented* full onboarding design.

**Locked decisions (do not reopen):**

| # | Decision |
| --- | --- |
| D-R2-1 | **Email delivery:** a generic SMTP abstraction now (`spring-boot-starter-mail` + a `MailSender` port + an SMTP adapter). Credentials are environment-only (`spring.mail.*`, `nadoumi.mail.*` via `.env` / `config/application-local.yml`). No provider name in the domain layer. **Local dev** uses a `Mailpit` container from the repo-root `docker-compose.yml` (MySQL + Redis + Mailpit); **staging/prod** points the same adapter at **Gmail SMTP** (`smtp.gmail.com:587`, App Password) — config only, no code change. Amazon SES is an equally valid later target on the same port. Tests + CI use `nadoumi.mail.transport=log`. |
| D-R2-2 | **Student identity:** email-first authentication. `sys_user.user_name` stays as an internal/generated handle where RuoYi requires one; the student signs in with a **verified email**. |
| D-R2-3 | **Password policy (all of register / reset / change):** 8–32 chars, ≥1 uppercase, ≥1 lowercase, ≥1 digit, ≥1 special character; the new password must differ from the current one. Single shared validator, server- and client-side. |
| D-R2-4 | **Current profile scope is frozen.** `/dashboard/profile` keeps its Revision 1 field set. The full applicant onboarding is a **dedicated later phase**; its complete UX / domain / API / schema design is produced now in `docs/APPLICANT_ONBOARDING.md` and is **not implemented in this build**. |
| D-R2-5 | **Navbar** keeps `Destinations`. Final top-nav set: **Home, Scholarships, Universities, Programs, Destinations, About, Contact** + right-side **Sign in / Create account**, replaced by the account menu when authenticated. |
| D-R2-6 | **Forgot-password is the real flow:** email → OTP (emailed) → verify → new password + confirm → done → `/login`. Same visual language as the registration email-verification step. |

**Mandatory corrections carried into the sections below:**

- Forgot-password: real email → OTP → new-password → login flow (not "contact support").
- `/dashboard/account`: current password + new password + confirm password, shared
  policy, revoke other sessions on success.
- Public site must read as a real international-education platform, not a scaffold.
- **No fake statistics and no fake applications** anywhere — absent data ships as a
  designed empty state, never as invented numbers or rows.
- Keep: Tailwind + the approved Nadoumi tokens + the shared `ui/` component
  architecture; Nuxt 3 + Nitro BFF + httpOnly session cookie; clean, centered,
  responsive, accessible layouts with subtle motion (`prefers-reduced-motion`
  honoured).
- Playwright auth tests stay hard CI gates — never `continue-on-error`.

---

## Revision 3 (2026-09-03) — registration UX, OTP component, onboarding

Product review of the Revision 2 build. The auth flows work but the UX is not at
enterprise quality yet. Revision 3 is UX + design depth on top of the same
contracts (no new backend endpoints).

### D-R3-1 — Registration is a 3-step wizard

| Step | Fields / behaviour |
| --- | --- |
| **1 · Personal information** | First name, last name; helper line *"Enter your first and last name exactly as they appear on your passport."*; then **Email**; `[Verify]`. **No confirm-email field** (the OTP is the email-ownership check) and **no password fields shown.** `[Verify]` is disabled until first name, last name and a syntactically valid email are present. |
| **2 · Email verification** | Shows `Email: <address>` with an **`Edit email`** control that returns to step 1 and clears the OTP cleanly. Below it, the refined OTP interface (D-R3-2). On success: an **`Email verified ✓`** confirmation, then auto-advance to step 3. |
| **3 · Password** | Password + confirm (show/hide, strength indicator, live requirements list). A **single Terms & Privacy checkbox** ("I agree to the Terms and the Privacy Policy" with both links). The password must not contain the first name, last name, or the derived username (email local-part). **`Next` stays disabled until: email verified ∧ password valid ∧ passwords match ∧ Terms accepted.** |
| → | On submit: one `POST /api/student-account` (unchanged contract). On success the student is authenticated (server-side, httpOnly cookie) and redirected to the **onboarding flow** (`/dashboard/onboarding`, D-R3-3), not straight to `/dashboard/profile`. |

Change from Revision 2: two consent checkboxes → **one**; **confirm-email field
removed** (the emailed OTP verifies ownership); redirect target → the onboarding
wizard; explicit `Edit email` round-trip; explicit "verified" state.

### D-R3-2 — Refined `OtpInput` + `EmailVerifyStep` (one implementation)

Replace the oversized generic boxes. `app/components/auth/OtpInput.vue` and
`app/components/auth/EmailVerifyStep.vue` are the **only** OTP implementation, reused
by registration, forgot-password, and any future email-verification flow.

- `OtpInput`: 6 evenly-spaced boxes sized ~2.5rem, `text-lg`, subtle border that
  strengthens on `:focus-visible`; a filled box gets a quiet brand tint. Auto-focus
  advance, backspace-to-previous, full paste support, digits only. Wrapper
  `role="group"` + `aria-label`; each box an explicit `aria-label` ("Digit N of 6").
  Container `dir="ltr"` always. Transitions are ≤150 ms and disabled under
  `prefers-reduced-motion`.
- `EmailVerifyStep`: shows the target email + `Edit email` (emits `edit`); the
  `OtpInput`; a `Resend code` control bound to the 60 s countdown ("Resend in {n}s"
  while cooling down); a verifying/loading state on the boxes; explicit
  invalid/expired and "N attempts left" feedback from the backend error; on success
  an `Email verified ✓` line before it emits `verified(ticket)`.

### D-R3-3 — Onboarding is a multi-step flow (design + partial build)

Route `/dashboard/onboarding` with steps **Personal · Identity · Education ·
Interests · Location · Contact · Review** and a persistent progress indicator.
Centered, generously spaced, responsive, with loading/error/success states and
smooth (reduced-motion-aware) step transitions. Every field carries a
**Required / Recommended / Optional** badge.

**Persistence rule (hard):** only fields the backend already stores are actually
saved. Everything else is rendered and clearly marked **"Coming soon — not saved
yet"**. No faked persistence.

| Step | Status |
| --- | --- |
| Personal (first/last name) | **EXISTING** — writes `nickName` via the applicant record |
| Identity (given/family name, DOB, nationality, passport no, phone) + Education (institution, level, field, GPA, dates) + Contact (guardian/emergency) | **EXISTING** — the current `/dashboard/profile` + `/dashboard/education` + `/dashboard/contacts` fields, surfaced inside the wizard |
| Identity extras (gender, country of residence), Interests, Location ("Are you currently in China?" branch), languages, work, certifications | **PLANNED — REQUIRES BACKEND** (`docs/APPLICANT_ONBOARDING.md`); shown read-only / disabled with the "coming soon" note |
| Profile photo, Passport | **PLANNED — REQUIRES BACKEND** (Document slice + object storage). The `ProfilePhotoUploadCard` / `PassportUploadCard` / `DocumentPreview` / `ImageCropper` components are built as **client-only** (crop / zoom / rotate / preview / validate) with a disabled "Upload isn't available yet" state — no network call, no fake success. No claim of automated face/passport verification. |
| Review | **EXISTING** — summarises what was captured; "Finish" marks onboarding complete client-side and routes to `/dashboard` |

Full domain / schema / API design for the PLANNED parts: `docs/APPLICANT_ONBOARDING.md`
(updated to the 7-step structure with the EXISTING / PLANNED / REQUIRES BACKEND
split), cross-referenced from `DOMAIN_MODEL.md`, `DATABASE_DESIGN.md`,
`API_DESIGN.md`, `FRONTEND_ARCHITECTURE.md`.

### D-R3-4 — Auth pages keep the Nadoumi header

`layouts/auth.vue` renders the real `<SiteHeader>` + `<SiteFooter>` (same nav, same
design system) around a vertically/horizontally centered form. **Fix carried in:**
`nuxt.config` `components` must register `~/components/marketing` with
`pathPrefix: false` or the layouts' `<SiteHeader>` / `<SiteFooter>` silently render
nothing.

---

## 1. Goal

Turn `nadoumi-web/` from a bare scaffold into a professional, enterprise-grade
public website with a working authenticated student area. Deliverable of this
build:

- A real Nadoumi design system (brand tokens, typography, component set).
- Polished marketing pages.
- Student **register / login / sign-out** wired to the existing Phase 3 API.
- An authenticated **student dashboard** that manages the applicant profile
  (personal info, education, test scores, contacts) — the only student data flow
  that works end to end today.
- Docs + CI updated.

Out of scope (later phases, backend not ready): applications, documents,
messaging, notifications, payments, scholarship/university/program catalog data.
Those pages ship as polished empty-states.

---

## 2. Fixed decisions (from brainstorming)

| Decision | Value |
| --- | --- |
| Public/student architecture | **Nuxt 3 (SSR) + Nitro BFF + httpOnly session cookie** (D2, `docs/FRONTEND_ARCHITECTURE.md` §3.2). Not reopened. **No Next.js**, no second SPA framework. |
| Build strategy | Extend the existing Nuxt 3 app; do not restart it. |
| Scope | Marketing pages + auth + applicant-profile dashboard. |
| Styling | Tailwind CSS + a **small hand-built** shared component set with design tokens. No large component library. |
| Brand primary | Orange `#F97316` (Tailwind `orange-500`). |
| Brand scale | Tailwind `orange` scale. Solid CTA = `orange-600` + white text ≥16px semibold. Text links / small text on fill = `orange-700` (`#C2410C`). Focus ring = `orange-500`. |
| Theme | Light only. Tokens as CSS variables so dark mode is a later additive change. No dark mode now. |
| Logo | Text wordmark **“Nadoumi”**. Header structured so a real `<img>` logo swaps in with no layout change. |
| Typography | Headings **Plus Jakarta Sans** (600/700); body/UI **Inter** (400/500/600); Arabic fallback **Noto Sans Arabic**. Self-hosted via `@nuxt/fonts`. |
| Visual direction | Modern, trustworthy, premium education platform. Clean spacing, strong hierarchy, subtle surfaces, minimal noise. Polished SaaS quality, not a template. |
| Primary audience | International undergraduate + graduate applicants. Parents/guardians secondary. |
| Priority destinations | China and Malaysia; architecture ready for more. |
| i18n | `en` fully populated. `fr` / `ar` / `zh` structurally wired with English fallback (`fallbackLocale: 'en'`). `ar` must render RTL correctly. |
| Forgot-password | **Real flow (Revision 2, D-R2-6).** `/forgot-password`: email → emailed OTP → verify → new password + confirm → `/login`. Backend: `email-otp` + `email-otp/verify` + `password/reset` (see §15). Non-enumerating. |
| Registration | **Two steps (Revision 2, D-R2-2).** Step 1: first name, last name, email, confirm email, captcha (when enabled), `[Verify]` → emailed OTP → verified. Step 2: password + confirm (policy D-R2-3), accept terms, `[Next]`. One credential POST to the BFF, which registers (with the verified-email ticket) **and** logs in server-side and sets the httpOnly cookie. The client never re-sends the password and never receives the token. Then redirect to `/dashboard/profile`. Primary button is **Next**, not "Create profile". Name fields carry the helper: *"Enter your name exactly as it appears on your passport."* |
| Student identity | **Email-first (D-R2-2).** Login form field is **email**; `sys_user.user_name` is an internal handle. |
| Password policy | **Shared (D-R2-3):** 8–32 chars, upper + lower + digit + special; ≠ current. One validator, both tiers. |
| Change password | **Self-service (Revision 2).** `/dashboard/account` has current / new / confirm; `POST /api/student/password`; other sessions revoked on success. |
| Testing | Vitest unit/component + composable/handler/middleware tests **and** one focused Playwright E2E: register → auto-login → dashboard → sign out. Added to CI with a running-backend dependency; the E2E job is a **hard gate — no `continue-on-error`**. Keep E2E small this phase. |

---

## 3. Existing foundation (do not rebuild)

- Nuxt 3.21 (`future.compatibilityVersion: 4`, `app/` dir), SSR on.
- `@nuxtjs/i18n` configured: `en`/`fr`/`ar`/`zh`, `strategy: prefix_except_default`,
  `ar` has `dir: 'rtl'`.
- Nitro BFF already present:
  - `server/utils/backend.ts` — `backendBaseUrl` from `runtimeConfig.backendBaseUrl`
    (`NUXT_BACKEND_BASE_URL`), `nad_student_token` httpOnly + Secure + SameSite=Lax
    cookie helpers (`studentToken` / `setStudentToken` / `clearStudentToken`,
    `maxAge` 30 min).
  - `server/api/student-session.post.ts` — POST `/api/student/login`, stores cookie,
    returns `{ signedIn: true }` (token not returned to browser).
  - `server/api/student-session.delete.ts` — best-effort `/api/student/logout`,
    clears cookie.
  - `server/api/student/[...path].ts` — authenticated passthrough, attaches bearer
    from cookie, 401 if absent.
  - `server/api/public/[...path].ts` — anonymous catalog passthrough.
- `app/composables/useApi.ts` — `publicGet` / `studentFetch`, always via the BFF.
- `app/composables/useSeo.ts` — title + description + OG tags.
- Pages: `index`, `about`, `contact`, `scholarships/index`, `universities/index`,
  `programs/index` (all thin).

### 4.R2 Backend contract additions (Revision 2 — new work, see §15)

| Method | Path | Auth | Job |
| --- | --- | --- | --- |
| `POST` | `/api/student/email-otp` | `@Anonymous`, rate-limited, captcha-aware | Request an OTP for `{ email, purpose: REGISTER \| PASSWORD_RESET, code?, uuid? }`. **Always 200** `{ sent: true }` — never reveals whether the email exists. |
| `POST` | `/api/student/email-otp/verify` | `@Anonymous`, rate-limited | `{ email, purpose, otp }` → `200 { ticket }` (opaque, single-use, ~10 min, server-side reference). `400` problem+json on bad/expired/exhausted OTP. |
| `POST` | `/api/student/register` | `@Anonymous` (**modified**) | Now also requires `ticket` from a verified `REGISTER` OTP whose email matches the body; persists the email as verified. Keeps captcha + `nad.student.register.enabled`. `user_name` is derived server-side (D-R2-2). |
| `POST` | `/api/student/login` | `@Anonymous` (**modified**) | Accepts `{ email, password, code?, uuid? }`. Resolves the user by verified email. Still rejects staff. |
| `POST` | `/api/student/password/reset` | `@Anonymous`, rate-limited | `{ ticket, newPassword }` (ticket from a `PASSWORD_RESET` OTP). Applies policy D-R2-3, BCrypt, sets `pwd_update_date`, **revokes all of that user's sessions**, `204`. **No auto-login.** |
| `POST` | `/api/student/password` | bearer (student) | `{ currentPassword, newPassword }`. Verify current, apply policy, reject same-as-current, revoke **other** sessions (keep the caller's), `204`. |

BFF passthroughs (same credential-confinement rule — a raw password crosses
browser→BFF→Spring at most once and is never returned): `student-email-otp.post.ts`,
`student-email-otp-verify.post.ts`, `student-password-reset.post.ts`,
`student-password.post.ts` (authed). The `ticket` is a non-secret handle and may sit
in client memory between the verify step and the register/reset step.

## 4. Backend contract (Phase 3, already shipped — see §4.R2 for the Revision 2 delta)

- `POST /api/student/register` — `@Anonymous`, rate-limited (5/60s). Requires:
  `nad.student.register.enabled=true`; captcha when `sys.account.captchaEnabled=true`
  (`code` + `uuid`); unique username. Creates `sys_user` with `user_type='10'`.
- `POST /api/student/login` — `@Anonymous`, rate-limited (10/60s). Rejects staff.
  Returns `{ code: 200, token }`. Accepts captcha `code` + `uuid` when enabled.
- `GET /api/student/me` — bearer; returns the student identity + linked applicants.
- `POST /api/student/logout` — bearer.
- `GET /captchaImage` — RuoYi core; returns `{ uuid, img (base64), captchaEnabled }`.
- Applicant profile (bearer, `@na.canAccessApplicant`):
  - `GET/POST /api/student/applicants`, `GET/PUT /api/student/applicants/{id}`
  - child CRUD for education / test-scores / contacts under `/api/student/applicants/{id}/...`
  - `GET/POST /api/student/applicants/{id}/access` (delegation; not built in UI this
    phase beyond read-only display if trivial — optional).

---

## 5. Design system

### 5.1 Tailwind + tokens

Add `@nuxtjs/tailwindcss` and `@nuxt/fonts`. `tailwind.config.ts` extends theme;
raw values also mirrored as CSS variables in `app/assets/css/tokens.css` for
non-Tailwind contexts and future theming.

| Token | Value | Use |
| --- | --- | --- |
| `--color-brand-50..900` | Tailwind `orange` scale | brand |
| brand solid | `orange-600` `#EA580C` | primary button bg |
| brand solid hover | `orange-700` `#C2410C` | primary button hover |
| brand text | `orange-700` `#C2410C` | links, small text on light |
| focus ring | `orange-500` `#F97316` | `:focus-visible` outline |
| text | `slate-900` | body headings/text |
| text-muted | `slate-600` | secondary text |
| border | `slate-200` | hairlines, inputs |
| surface | `#ffffff` | cards, header |
| surface-subtle | `slate-50` | section bands, table zebra |
| radius | `sm 6px / md 10px / lg 16px / full` | |
| shadow | `xs / sm / md` only, low-opacity slate | subtle elevation |
| container | `max-w-6xl` marketing, `max-w-5xl` dashboard, `px-5` gutter | |
| spacing rhythm | 4px base (Tailwind default) | |

**Contrast rule:** never put text smaller than 16px semibold on an `orange-500`
fill. Solid orange UI uses `orange-600`/`700`. Verified targets: `orange-700` on
white ≈ 5.900:1 (AA/AAA body); white on `orange-600` ≈ 3.9:1 (AA large / UI only).

### 5.2 Typography

- `@nuxt/fonts` self-hosts: **Plus Jakarta Sans** (600, 700), **Inter** (400, 500,
  600), **Noto Sans Arabic** (400, 600).
- `font-display` (headings) = Plus Jakarta Sans; `font-sans` (default) = Inter.
- `html[lang="ar"]` sets `font-sans`/`font-display` to Noto Sans Arabic first.
- Type scale (rem): display 3 / 2.25 / 1.875 / 1.5 / 1.25 / 1 / 0.875. Line-height
  1.15 headings, 1.6 body. `text-wrap: balance` on headings.

### 5.3 RTL

- `nuxt.config` i18n already flags `ar` as `dir: 'rtl'`; ensure a plugin/head sets
  `<html dir>` from the active locale.
- Components use logical spacing (`ps-`/`pe-`/`ms-`/`me-`, `start`/`end`) and
  Tailwind `rtl:` variants where a mirror is needed (icons, chevrons, sidebar side).
- One Playwright/visual check that `/ar` renders right-aligned nav + sidebar.

---

## 6. Components

### 6.1 `app/components/ui/` — primitives (no domain knowledge)

| Component | Notes |
| --- | --- |
| `NButton` | variants `primary` / `secondary` / `ghost` / `link`; sizes `sm` / `md` / `lg`; `loading`, `disabled`, `block`; renders `<button>` or `<NuxtLink>` via `to`. |
| `NInput` | text/email/password/number; prefix/suffix slot; `invalid` state; `v-model`. |
| `NTextarea` | autosize optional. |
| `NSelect` | native `<select>` styled; options prop. |
| `NCheckbox` | label slot; `v-model` boolean. |
| `NField` | wraps a control: `label`, `hint`, `error`, `required` marker, `for`/`id` wiring, `aria-describedby`. |
| `NCard` | padding presets; optional header/footer slots. |
| `NBadge` | tone `neutral` / `brand` / `success` / `warning` / `danger`. |
| `NAlert` | tone + title + body + optional dismiss. |
| `NContainer` | max-width + gutter wrapper; `size` `marketing` / `app`. |
| `NSpinner` | inline + block. |
| `NModal` | focus-trap, ESC, scrim; teleported; no external dep. |
| `NDropdown` | menu (used by account menu, locale switcher). |
| `NAvatar` | initials fallback. |
| `NLocaleSwitcher` | lists configured locales, keeps current path, sets locale. |

All: keyboard accessible, visible `:focus-visible` ring, `prefers-reduced-motion`
respected, no layout shift.

### 6.2 `app/components/marketing/`

`SiteHeader`, `SiteFooter`, `Hero`, `FeatureGrid`, `StatBand`, `CTASection`,
`DestinationCard`, `Testimonial`, `FaqAccordion`, `EmptyState` (for catalog pages).

### 6.3 `app/components/dashboard/`

`DashboardShell` (responsive sidebar + topbar + account menu), `ApplicantSwitcher`
(only when >1 applicant), `ProfileForm`, `EducationList` + `EducationFormRow`,
`TestScoreList` + `TestScoreFormRow`, `ContactList` + `ContactFormRow`,
`SectionCard`.

### 6.4 Layouts

- `layouts/default.vue` — `SiteHeader` + `<slot/>` + `SiteFooter` (marketing + auth).
- `layouts/dashboard.vue` — `DashboardShell` around `<slot/>`.

---

## 7. Pages

### 7.1 Marketing (SSR, indexable, `layout: default`)

| Route | Content |
| --- | --- |
| `/` | Hero (headline + primary CTA "Create your profile" → `/register`, secondary "Browse scholarships"), FeatureGrid (Discover / Apply / Track), DestinationCard row (China, Malaysia), StatBand (placeholder-safe copy), CTASection, FAQ. |
| `/scholarships` | Filter bar (country / degree / field — non-functional stub OK) + list via `publicGet('scholarships')`; `EmptyState` when API absent/empty. |
| `/scholarships/[id]` | Detail; `EmptyState` / 404-safe until API exists. |
| `/universities`, `/universities/[id]` | Same pattern. |
| `/programs`, `/programs/[id]` | Same pattern. |
| `/destinations` | China + Malaysia as rich sections; structure ready for more countries. |
| `/about` | Mission, what Nadoumi does across the application lifecycle. |
| `/contact` | Existing form; keep client-only stub (wired with Content slice later); show support email. |
| `/privacy`, `/terms` | Static legal copy (placeholder text, clearly marked draft). |

Every marketing page calls `useSeo(...)`; add a default `og:image` asset in
`public/`; SEO discovery via a static `public/robots.txt` plus a
`server/routes/sitemap.xml.ts` that lists the marketing routes. No sitemap module
dependency.

### 7.2 Auth (`layout: default`, redirect to `/dashboard` if already signed in)

| Route | Behavior |
| --- | --- |
| `/register` | **Two steps (D-R2-2).** *Step 1* — first name, last name, email, confirm email, captcha (when enabled), helper text *"Enter your name exactly as it appears on your passport."*, `[Verify]` → `POST /api/student/email-otp` (purpose `REGISTER`). *OTP step* — `<EmailVerifyStep>` (6-box `OtpInput`, resend cooldown, attempts feedback) → `POST /api/student/email-otp/verify` → holds the `ticket`, shows "Verified". *Step 2* — password + confirm (shared policy validator D-R2-3), accept-terms checkbox, primary button **Next**. Submit = **one** request `POST /api/student-account` with `{ firstName, lastName, email, password, ticket }`. The BFF registers (passing the ticket) **and** signs in server-side and sets the httpOnly cookie (§8.1); the browser sends the password once and gets back only `{ signedIn: true }`. On success → `useSession().refresh()` → redirect `/dashboard/profile`. Errors are RFC 9457 `problem+json` `detail`, status passed through. |
| `/login` | **email** + password + captcha (when enabled). Submits `POST /api/student-session`. On success → `useSession().refresh()` → redirect to a `?redirect=` that starts with a single `/`, else `/dashboard`. Links to `/register` and `/forgot-password`. |
| `/forgot-password` | **Real flow (D-R2-6), same components as `/register` step 1+OTP.** *Step 1* — email, captcha (when enabled), `[Verify]` → `POST /api/student/email-otp` (purpose `PASSWORD_RESET`); the page always advances (non-enumerating). *OTP step* — `<EmailVerifyStep>` → `POST /api/student/email-otp/verify` → `ticket`. *Step 3* — new password + confirm (policy D-R2-3) → `POST /api/student-password-reset` (BFF → `POST /api/student/password/reset`); on `204` show success and route to `/login`. No auto-login. |

Captcha component (`AuthCaptcha`) calls `GET /api/public/captcha` (passthrough);
hidden entirely when `captchaEnabled: false`. `OtpInput` + `EmailVerifyStep`
(`app/components/auth/`) are shared by `/register` and `/forgot-password` — one
implementation, no duplicated OTP logic.

### 7.3 Dashboard (`layout: dashboard`, middleware `auth`)

| Route | Content |
| --- | --- |
| `/dashboard` | Overview (**Revision 2 polish pass, requirement #6**): welcome header block (name + active applicant chip), profile-completeness card, onboarding-progress card, quick-actions row, and **designed empty-state panels** for *Applications*, *Recent activity*, *Notifications* — each labelled "coming soon", **never populated with invented data**. Every async region uses the shared `AsyncState` wrapper (loading / error / empty). Applicant switcher when >1. |
| `/dashboard/profile` | `ProfileForm` — given/family name, DOB, nationality, gender, passport no, phone, address. `GET/PUT /api/student/applicants/{id}`. If the user has no applicant yet, first-run creates one (`POST /api/student/applicants`, "about me"). |
| `/dashboard/education` | `EducationList` CRUD → `/api/student/applicants/{id}/education`. |
| `/dashboard/test-scores` | `TestScoreList` CRUD → `.../test-scores`. |
| `/dashboard/contacts` | `ContactList` CRUD → `.../contacts`. |
| `/dashboard/account` | **Change-password form (Revision 2):** current password, new password, confirm new password → `POST /api/student-password` (BFF → `POST /api/student/password`). Shared policy validator D-R2-3; new ≠ current. On `204`: success alert stating other sessions were signed out. Plus a Sign-out `NButton` (`DELETE /api/student-session` → clear session → `/`). |

`{id}` = the active applicant from `useSession()` (first, or the one chosen in
`ApplicantSwitcher`, persisted in a cookie/`useState`).

---

## 8. Auth & session architecture

```
browser ──$fetch──▶ Nitro BFF ──bearer from httpOnly cookie──▶ Spring API
   ▲                    │
   └── never sees JWT ───┘
```

### 8.1 New server routes

Naming follows the routes already in `server/api/`: the session resource is
`student-session.{verb}.ts`; anonymous backend endpoints not under `/api/public/**`
get a named file next to the `public/[...path].ts` catch-all.

| File | Status | Job |
| --- | --- | --- |
| `server/api/student-session.get.ts` | NEW | Read cookie; if none → `{ authenticated: false }` (no API call). Else `GET /api/student/me`; 200 → `{ authenticated: true, user, applicants }`; 401 → clear cookie, `{ authenticated: false }`. Never returns the token. |
| `server/api/student-session.post.ts` | EXISTING | Login: `POST /api/student/login`, set httpOnly cookie, return `{ signedIn: true }`. |
| `server/api/student-session.delete.ts` | EXISTING | Logout: best-effort `POST /api/student/logout`, clear cookie. |
| `server/api/student-account.post.ts` | NEW | **Register + open session, entirely server-side.** Forward body to `POST /api/student/register`; on success immediately `POST /api/student/login` with the same credentials from the request, `setStudentToken(event, token)`, return `{ signedIn: true }`. On registration failure pass the `problem+json` body + status straight through and set no cookie. The password is never returned to the browser and never makes a second browser round-trip. |
| `server/api/public/captcha.get.ts` | NEW | Passthrough to backend `GET /captchaImage` (root path, not under `/api/public`). |

### 8.2 Client

| File | Job |
| --- | --- |
| `app/composables/useSession.ts` | `useState<{ status: 'unknown'\|'guest'\|'authed', user?, applicants?, activeApplicantId? }>`. `refresh()` → `GET /api/student-session`. `signOut()` → `DELETE /api/student-session` + reset state + `navigateTo('/')`. `setActiveApplicant(id)`. |
| `app/plugins/session.server.ts` (+ client) | Call `useSession().refresh()` once during app init so SSR renders the right header/menu. |
| `app/middleware/auth.ts` | On `/dashboard/**`: if `status !== 'authed'` after `refresh()`, `navigateTo('/login?redirect=' + encodeURIComponent(to.fullPath))`. |
| `app/middleware/guest.ts` | On `/login`, `/register`: if authed → `navigateTo('/dashboard')`. |

Header shows "Sign in" / "Create profile" when guest; avatar + `NDropdown`
(Dashboard, Account, Sign out) when authed.

### 8.3 Cookie lifetime

Backend JWT TTL and the 30-min cookie `maxAge` should agree. Keep 30 min for this
phase; `student-session.get` treating a 401 as guest already handles expiry
gracefully (user is bounced to `/login`). No silent refresh this phase.

### 8.4 Credential exposure rule

Passwords and the JWT are confined to the server tier:

- The browser sends a password exactly once per action — to `POST /api/student-session`
  (login) or `POST /api/student-account` (register). There is **no** client-side code
  path that stores a password, re-sends it, or chains a second authenticated call with
  raw credentials.
- The JWT is set by the BFF into the httpOnly + Secure + SameSite=Lax cookie and is
  never in a response body, `useState`, `localStorage`, or a client log.
- All authenticated data calls go through `server/api/student/[...path].ts`, which
  attaches the bearer from the cookie server-side.

---

## 9. i18n

- `i18n/locales/en.json` — full key tree: `nav`, `footer`, `home.*`, `marketing.*`
  (per page), `auth.*` (labels, errors, captcha), `dashboard.*`, `validation.*`,
  `common.*`.
- `fr.json` / `ar.json` / `zh.json` — **same keys**, English values (fallback).
- `nuxt.config` i18n: add `fallbackLocale: 'en'`, keep
  `strategy: 'prefix_except_default'`.
- Plugin sets `<html lang>` + `<html dir>` from active locale.
- `NLocaleSwitcher` in the footer (and dashboard topbar).
- Validation messages via i18n keys, not hard-coded.

---

## 10. Footer (replaces the one-line footer)

`SiteFooter`: four columns —
1. Wordmark + one-sentence mission + locale switcher.
2. **Explore** — Scholarships, Universities, Programs, Destinations.
3. **Company** — About, Contact, Privacy, Terms.
4. **Get started** — Create profile, Sign in, plus `support@nadoumi.com`.

Bottom bar: `© <year> Nadoumi. International education platform.` + Privacy / Terms.
Uses `surface-subtle` band, `border` top hairline, muted text, generous vertical
padding. Fully responsive (columns stack).

`SiteHeader` (**Revision 2, D-R2-5**): wordmark slot (`<NuxtLink to="/">` wrapping a
`<span>` today, an `<img>` later — same box); primary nav with the **explicit set
`Home, Scholarships, Universities, Programs, Destinations, About, Contact`** (Home is
its own item, not only the wordmark); locale switcher; right-side auth actions
**`Sign in`** (link) + **`Create account`** (solid `NButton` → `/register`),
swapped for the account `NDropdown` (Dashboard, Account, Sign out) when
`status === 'authed'`; a mobile disclosure menu that mirrors the same set and whose
`aria-controls` targets a `v-show` panel (always in the DOM). Professionally spaced,
`:focus-visible` on every interactive element, responsive from 320px up.

---

## 11. Testing

### 11.1 Vitest (`nadoumi-web`, jsdom)

- `NButton` — variant/size classes, `loading` disables + shows spinner, `to`
  renders link.
- `NField` — renders label/hint, shows `error`, wires `aria-describedby` + `id`.
- `NLocaleSwitcher` — lists locales, preserves path.
- `useSession` — `refresh()` sets `guest` on `{authenticated:false}`, `authed` on
  `{authenticated:true,...}`; `signOut()` resets.
- Nitro `student-session.get` — no cookie → `{authenticated:false}` without calling
  the API (mock `$fetch`); cookie + API 401 → clears cookie.
- Nitro `student-account.post` — on backend register 200 it calls login and sets the
  cookie (assert `setCookie` invoked, response body carries no token); on register
  4xx it passes the status + `problem+json` through and sets no cookie.
- `middleware/auth` — guest → redirect object to `/login?redirect=...`.
- **(Revision 2)** `OtpInput` — 6 boxes, paste fills all, emits the joined value,
  `complete` event on the 6th digit; resend button disabled during the cooldown.
- **(Revision 2)** `passwordPolicy(value, current?)` shared validator — accepts a
  compliant password; rejects each of: <8, >32, no upper, no lower, no digit, no
  special, equal to `current`.
- **(Revision 2)** BFF `student-email-otp.post` / `student-email-otp-verify.post` /
  `student-password-reset.post` / `student-password.post` — forward to the right
  backend path, pass `problem+json` + status through, never place a password or
  ticket in a log; `student-password.post` requires the session cookie.
- **(Revision 2)** `/dashboard/account` — submitting mismatched new/confirm blocks
  the call; a `204` shows the "other sessions signed out" notice.

### 11.2 Playwright E2E (`tests/e2e/`, all hard gates — no `continue-on-error`)

CI backend profile: `sys.account.captchaEnabled=false`,
`nad.student.register.enabled=true`, `nadoumi.mail.transport=log` so the OTP is
written to a retrievable place (see §15.5).

1. `auth.spec.ts` — `/register` step 1 (unique email) → read the OTP from the mail
   log → enter it → step 2 password → **Next** → land `/dashboard/profile` → open
   account menu → Sign out → `/` shows "Sign in".
2. `forgot-password.spec.ts` — register a user in setup → `/forgot-password` → email
   → OTP from the mail log → new password → redirected to `/login` → sign in with
   the **new** password → `/dashboard`.

- Config: `webServer` starts `nuxt preview` with `NUXT_BACKEND_BASE_URL` at the CI
  backend. No page-object framework yet; a tiny `readOtp(email)` helper is allowed.

### 11.3 CI (`.github/workflows/ci.yml`, `nadoumi-web` job)

- Keep `pnpm lint` + `pnpm build`.
- Add `pnpm test` (Vitest).
- Add an E2E step: start MySQL + Redis services + run the Spring boot jar
  (built by the `backend` job artifact, or `mvn -pl ruoyi-admin spring-boot:run` in
  background) with `sys.account.captchaEnabled=false` and
  `nad.student.register.enabled=true`, then `pnpm playwright test`. Gate it behind
  `needs: backend`.
- **The E2E job is a hard gate.** No `continue-on-error`. An authentication-flow
  failure fails CI like any other test. If backend startup proves flaky, fix the
  startup/wait logic (health-check poll before the Playwright step) — do not paper
  over it by letting the job pass.

---

## 12. Docs

Rewrite `docs/FRONTEND_ARCHITECTURE.md` §7 (`nadoumi-web`) to cover:

- Brand token table (from §5.1), font pairing, RTL approach.
- Component tiers (`ui/` / `marketing/` / `dashboard/`) and the primitive list.
- Auth/session design (the diagram + route table from §8).
- Full page inventory (§7).
- i18n policy (`en` authored, others fallback).
- Testing + CI additions.

Mark each subsection `EXISTING` (what's built by this change) vs `PLANNED`
(catalog data wiring, full onboarding, dark mode).

**Revision 2 also updates:**
- `docs/API_DESIGN.md` §4.1 — the six auth endpoints in §4.R2.
- `docs/SECURITY.md` §4.1 + a new §4.2 — OTP design, password policy, session
  revocation, non-enumeration, admin bootstrap.
- `docs/PHASE_3_IDENTITY_APPLICANT.md` §3 + §6 — new APIs; email-first login;
  limitation "no student password self-service" removed.
- `docs/COMMUNICATION_AND_NOTIFICATIONS.md` §1 + §4 — the email channel is now
  partially built (SMTP port + OTP mails); mark the built vs still-planned split.
- `docs/ADMIN_ARCHITECTURE.md` §1 — `almousleck` super-admin, `admin` disabled,
  initial-password prompt removed.
- `docs/DOMAIN_MODEL.md` §3 — student identity is email-first; `user_name` is an
  internal handle for externals.
- `docs/DATABASE_DESIGN.md` — `sys_user.email_verified` (migration V7), the
  `idx_sys_user_email` index, and a pointer to `APPLICANT_ONBOARDING.md` for the
  PROPOSED `nad_applicant` expansion.
- `docs/DEPLOYMENT.md` §4.1 — env-var contract gains the `SPRING_MAIL_*` /
  `NADOUMI_MAIL_*` / `NADOUMI_WEB_LOGIN_URL` rows; a note on `docker-compose.yml`.
- `docs/DEVELOPMENT_GUIDELINES.md` §1 — `docker compose up -d` (MySQL + Redis +
  Mailpit) + `cp .env.example .env`.
- `docs/APPLICANT_ONBOARDING.md` — NEW, PROPOSED (§16).

Also add a memory note (`nadoumi-web-stack`) once built.

---

## 13. Work breakdown (for the implementation plan)

1. **Tooling** — add `@nuxtjs/tailwindcss`, `@nuxt/fonts`, `tailwind.config.ts`,
   `tokens.css`, base layer; Vitest + `@vue/test-utils`; Playwright.
2. **`ui/` primitives** — TDD each (test → component), in the §6.1 order.
3. **Layouts + `SiteHeader` + `SiteFooter`** + `NLocaleSwitcher` + RTL plugin.
4. **Session** — `student-session.get` route, `useSession`, session plugin, `auth` +
   `guest` middleware, tests.
5. **Auth pages** — `AuthCaptcha`, `student-account.post` route (server-side
   register+login), `public/captcha.get` route, `/register`, `/login`,
   `/forgot-password`.
   **(Revision 2)** `OtpInput` + `EmailVerifyStep` shared components; `useOtp`
   composable; the four new BFF passthroughs (§4.R2); `/register` rebuilt as two
   steps; `/forgot-password` rebuilt as the real flow.
6. **Dashboard** — `dashboard` layout, `DashboardShell`, `ApplicantSwitcher`,
   overview, profile (+ first-run create), education, test-scores, contacts,
   account. **(Revision 2)** `/dashboard/account` = real change-password form;
   overview polish pass (welcome block, onboarding-progress, designed empty
   states); `AsyncState` wrapper.
5b. **(Revision 2) Backend auth slice** — `spring-boot-starter-mail` + `MailSender`
   port + SMTP adapter + template renderer; `OtpService` + `TicketService` (Redis);
   `email-otp` + `email-otp/verify` endpoints; `/register` + `/login` moved to
   email-first; session-revocation helper; `password/reset` + `password` endpoints;
   admin-bootstrap migration (§17). See §15.
12. **(Revision 2) Onboarding design doc** — `docs/APPLICANT_ONBOARDING.md`: the
   full UX / domain / API / schema design, marked PROPOSED, **not implemented**.
7. **Marketing** — `Hero`/`FeatureGrid`/`StatBand`/`CTASection`/`DestinationCard`/
   `Testimonial`/`FaqAccordion`/`EmptyState`; rebuild `/`, `/about`, `/contact`,
   `/destinations`, `/privacy`, `/terms`; catalog list/detail with empty-states;
   `robots.txt` + `sitemap.xml`.
8. **i18n** — author `en.json`, mirror `fr/ar/zh`, `fallbackLocale`, swap all
   literals to keys.
9. **E2E** — `auth.spec.ts` + Playwright config.
10. **CI** — `pnpm test` + E2E job with backend service.
11. **Docs** — `FRONTEND_ARCHITECTURE.md` §7 rewrite; memory note.

Each step ends green: `pnpm lint && pnpm test && pnpm build`.

---

## 14. Risks / open items (non-blocking)

- **Catalog pages stay empty** until University/Program/Scholarship API slices
  exist — accepted; empty-states are designed, not broken.
- **`GET /api/student/me` shape** — confirm exact fields (linked applicants array?
  `activeApplicantId`?) against the Phase 3 controller before wiring
  `ApplicantSwitcher`; adjust `student-session.get` mapping accordingly.
- **Change-password for students** — RESOLVED (Revision 2): `POST /api/student/password`
  is now in scope (§15.4), with session revocation.
- **Playwright in CI** needs the backend + MySQL + Redis. It is a hard gate (no
  `continue-on-error`); flakiness is handled with a backend health-check poll before
  the Playwright step, not by downgrading the job.
- **Legal copy** (`/privacy`, `/terms`) is placeholder — must be replaced with real
  text before any public launch.
- **SMTP for dev/CI** (Revision 2) — a `nadoumi.mail.transport=log` mode writes the
  rendered email (OTP included) to a file/endpoint so local dev and E2E work without
  a real SMTP server. Production uses `smtp`. The transport switch lives in the
  adapter, never in the domain.
- **OTP brute-force** (Revision 2) — mitigated by: 6-digit numeric, 10-min TTL,
  single-use, ≤5 verify attempts then invalidate, 60-s resend cooldown, `@RateLimiter`
  per-email and per-IP on both `email-otp` endpoints, captcha on `email-otp` when
  enabled. Not a CAPTCHA-free open relay.
- **`sys_user` uniqueness on email** (Revision 2) — email must be unique among
  `user_type='10'` before it can be the login key; the register path enforces it and
  a migration adds the supporting index. Staff rows are unaffected.

---

## 15. Email + OTP backend slice (Revision 2 — NEW)

The codebase has **no** mail sender, templating, or notification pipeline
(`docs/COMMUNICATION_AND_NOTIFICATIONS.md` §1). This slice builds the minimum needed
for D-R2-1/D-R2-6 and is the first concrete piece of that doc's PLANNED §4.2 email
channel. Module: extend `nadoumi-modules/nadoumi-identity` (no new module this
phase; a `nadoumi-notification` module is a later extraction point).

### 15.1 Mail abstraction (D-R2-1)

- Add `spring-boot-starter-mail` to `nadoumi-identity`.
- Port: `interface MailSender { void send(EmailMessage msg); }` in the identity
  service package. `record EmailMessage(String to, String subject, String body)`.
- Adapters (selected by `@ConditionalOnProperty(nadoumi.mail.transport)`):
  - `SmtpMailSender` — `JavaMailSender`-backed, active when `transport=smtp`. Works
    unchanged against **Mailpit** (local), **Gmail** (`smtp.gmail.com:587` + App
    Password, staging/prod), or SES later.
  - `LoggingMailSender` — appends the message as JSON to `nadoumi.mail.logFile` and
    keeps the last message per recipient in memory; active when `transport=log`
    (default; tests + CI).
- Config: `application.yml` holds `${ENV:default}` placeholders only
  (`spring.mail.host/port/username/password`, `spring.mail.properties.mail.smtp.auth`,
  `...starttls.enable`, `nadoumi.mail.transport/from/logFile`, `nadoumi.web.loginUrl`).
  Real values come from the git-ignored `.env` (consumed by `docker-compose.yml`
  and exportable to the shell) or `config/application-local.yml`. **No provider
  name in Java.**
- Infra: repo-root `docker-compose.yml` — `mysql:8.4`, `redis:7`,
  `axllent/mailpit` (SMTP `:1025`, web UI `:8025`). `docker compose up -d` +
  `cp .env.example .env` is the documented local setup.
- Templates: `MailTemplates` renders `classpath:/mail/*.txt` with `${var}`
  substitution — `otp-register.txt`, `otp-password-reset.txt`, `account-exists.txt`.
  English only this phase (i18n later).
- Send off the request thread via the existing `AsyncManager`.

### 15.2 OTP + ticket services (Redis)

- `OtpService`:
  - `issue(email, purpose)` → generates a 6-digit numeric code, stores
    `nad:otp:{purpose}:{sha256(email)}` = `{ codeHash, attempts:0, issuedAt }` with
    **TTL 600 s**; enforces a **60 s** resend cooldown (`nad:otp:cooldown:...`);
    returns nothing to the caller.
  - `verify(email, purpose, code)` → compares (constant-time), increments `attempts`;
    on the **5th** failure deletes the key; on success deletes the key and calls
    `TicketService.mint`.
- `TicketService`:
  - `mint(email, purpose)` → opaque random id, stores
    `nad:ticket:{purpose}:{id}` = `{ email, issuedAt }`, **TTL 600 s**, single-use.
  - `consume(id, purpose)` → returns the email and deletes the key, or throws.
- Rate limiting: `@RateLimiter` on the controller — `email-otp` 5/hour/email +
  20/hour/IP; `email-otp/verify` 10/10 min/IP.

### 15.3 Registration & login → email-first (D-R2-2)

- `StudentRegisterRequest` becomes `{ firstName, lastName, email, password, ticket }`
  (drop client-supplied `username`/`nickName`). Server:
  - `TicketService.consume(ticket, REGISTER)` must return an email equal to `email`.
  - `user_name` = a generated unique handle (e.g. `stu_` + base36(sequence) or a
    slug of the email local-part + collision suffix). `nick_name` = `firstName + ' ' + lastName`.
  - `email` stored; `email_verified = 1` (new column, migration V7).
  - Password validated by the shared policy (§15.4) before `encryptPassword`.
  - Unique-email check scoped to `user_type='10'`.
- `StudentLoginRequest` becomes `{ email, password, code?, uuid? }`. Server resolves
  `user_name` by verified email, then delegates to `SysLoginService.login(...)`
  unchanged. Unknown email → the same generic "bad credentials" problem as a wrong
  password (non-enumerating). Staff rejection unchanged.
- Captcha keys stay keyed by the value RuoYi expects; pass the email as the
  `username` argument to `validateCaptcha`.

### 15.4 Password policy + change + reset + session revocation (D-R2-3)

- `PasswordPolicy.validate(raw, currentEncoded?)` in `ruoyi-common` (shared by staff
  paths later): length 8–32; `[A-Z]`, `[a-z]`, `[0-9]`, and one of a documented
  special-char set all present; if `currentEncoded` given,
  `!matchesPassword(raw, currentEncoded)`. Throws `NadBadRequestException` with a
  specific `detail`. Mirror as a TS `passwordPolicy()` in `nadoumi-web`
  (`app/utils/passwordPolicy.ts`) with identical rules and message keys.
- `SessionRevoker.revokeAll(userId, exceptToken?)` — scans the RuoYi
  `login_tokens:*` Redis keyspace, deletes every `LoginUser` whose `userId` matches
  (skipping `exceptToken`). Reused by reset and change.
- `POST /api/student/password/reset` (`@Anonymous`, rate-limited):
  `TicketService.consume(ticket, PASSWORD_RESET)` → policy check → `resetUserPwd` →
  `pwd_update_date = now` → `SessionRevoker.revokeAll(userId)` → `204`. No token
  issued, no login.
- `POST /api/student/password` (bearer student): verify `currentPassword` via
  `matchesPassword`; policy check (with `currentEncoded`); `resetUserPwd`;
  `pwd_update_date = now`; `SessionRevoker.revokeAll(userId, callerToken)`; refresh
  the caller's cached `LoginUser`; `204`.

### 15.5 Dev / CI mail retrieval

`LoggingMailSender` appends each message as JSON to
`${nadoumi.mail.logFile:./mail-outbox.log}` **and** exposes the latest message for a
recipient at `GET /api/dev/mail/latest?to=...` guarded by
`nadoumi.mail.transport=log` **and** a profile check (never mounted when
`transport=smtp`). The Playwright `readOtp(email)` helper hits that endpoint.

### 15.6 Tests (backend)

- `PasswordPolicyTest` — table of accept/reject cases.
- `OtpServiceTest` / `TicketServiceTest` — TTL, single-use, attempt cap, cooldown
  (fake clock + embedded Redis or mocked `RedisCache`).
- `StudentAuthServiceTest` — register requires a matching ticket; login resolves by
  email; unknown email == wrong password (same problem body).
- `StudentPasswordControllerTest` (`*Test`, self-skipping full-context) — reset
  revokes sessions and issues no token; change keeps the caller's session and kills
  the others; wrong current password → 400.
- `EmailOtpControllerTest` — existing email still returns `200 { sent:true }`;
  rate-limit trips; captcha honoured when enabled.

---

## 16. Applicant onboarding — later phase (Revision 2, D-R2-4)

**Not implemented in this build.** `/dashboard/profile` is frozen at its Revision 1
scope. The complete proposed design — identity fields (adds gender, country of
residence), profile-photo capture (crop/zoom/rotate/replace/remove/validation),
`PassportUploadCard`, education + education-history, education interests,
current-location branch (*"Are you currently in China?"* Yes/No), languages &
proficiency, tests, work experience, certifications, guardian/emergency contact,
preferred communication language — plus its domain model, `nad_applicant` schema
expansion, REST contract, storage design (needs the Document slice + object
storage), and the Required / Recommended / Optional field classification, live in
**`docs/APPLICANT_ONBOARDING.md`** (status: PROPOSED). Sequenced after the Document
slice.

---

## 17. Admin bootstrap (Revision 2 — requirement #9)

**Already done in `V5__nadoumi_baseline_seed.sql`** (verified): `almousleck`
(`user_id=3`, `user_type='00'`, role `nadoumi_super_admin` = every menu, org-wide
data scope) is seeded with the BCrypt hash of `Nadoumi2026#` (hash only, in the
migration); the inherited RuoYi `admin` (`user_id=1`) is disabled (`status='1'`,
"Break-glass administrator"). Nothing to re-seed.

**Remaining work (small):** remove the confusing "initial password" messaging.

- New migration: `UPDATE sys_config SET config_value='0' WHERE config_key='sys.account.initPasswordModify';`
  and set `sys.account.passwordValidateDays` to `90` (from `0`) so
  `pwd_update_date = NULL` on `almousleck` forces a rotation through the
  password-**expiry** path instead of the removed nag.
- Delete the "您的密码还是初始密码" block in `ruoyi-ui/src/store/modules/user.js`.
- `SysLoginController.initPasswordIsModify(Date)` → `return false;` (keep the method
  so `getInfo` compiles).
- Test: `admin` cannot authenticate (disabled); `almousleck` authenticates and
  resolves to `nadoumi_super_admin`; `getInfo` no longer emits the initial-password
  flag.
- Passwords never appear in UI, logs, source, or production-facing docs
  (`docs/SECURITY.md` §7).
