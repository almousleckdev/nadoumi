# nadoumi-web Public Website + Student Experience — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn `nadoumi-web/` from a bare scaffold into a professional public website with a working authenticated student profile dashboard, wired to the existing Phase 3 Spring API through the Nitro BFF.

**Architecture:** Extend the existing Nuxt 3 (SSR) app. Add a Tailwind + design-token layer and a small hand-built `app/components/ui/` primitive set. Restructure pages into a marketing group (`layout: default`) and an auth-gated `dashboard/` group. All backend calls go through Nitro BFF routes; the student JWT lives only in an httpOnly cookie the BFF sets and attaches — no token or password ever reaches client JS beyond the single credential POST.

**Tech Stack:** Nuxt 3.21 (`future.compatibilityVersion: 4`, `app/` dir), Nitro, `@nuxtjs/i18n@9`, `@nuxtjs/tailwindcss`, `@nuxt/fonts`, Vitest + `@vue/test-utils` + `@nuxt/test-utils`, Playwright, pnpm 10, ESLint 9 flat config (`@nuxt/eslint`).

**Spec:** `docs/superpowers/specs/2026-09-02-nadoumi-web-public-site-design.md` — read it alongside this plan (including its **Revision 2** block).

---

## Revision 2 (2026-09-03)

Tasks 1–17 are shipped and stand. Revision 2 adds three new parts and re-scopes two
existing tasks. Read this before executing anything past Task 17.

- **PART E — Backend auth slice** (new). Task E-0 first: repo-root `docker-compose.yml`
  (MySQL + Redis + Mailpit) + `.env`. Then the email/SMTP abstraction, Redis OTP +
  ticket services, email-first register/login, password policy, session revocation,
  `password/reset` + `password` endpoints, admin-bootstrap migration. Blocks Part F.
- **PART F — Auth-flow rebuild + navbar + dashboard polish** (new, frontend).
  `OtpInput`/`EmailVerifyStep`/`useOtp`; four BFF passthroughs; `/register` → two
  steps; `/forgot-password` → real flow; navbar correction; `/dashboard/account` →
  real change-password; `/dashboard` overview polish + `AsyncState`.
- **PART G — Onboarding design doc** (new, docs only). `docs/APPLICANT_ONBOARDING.md`
  (PROPOSED). No code.
- **Task 19 re-scoped** — supersede note in place; the real task is F-6.
- **Task 23 re-scoped** — supersede note in place; the real task is F-9.

Execution order after Task 18: **E-0 → E-1 … E-7 → F-1 … F-10 → Task 20 → 21 → 22 →
G-1 → Task 24** (Task 24's CI + docs step now also covers the Part E/F/G additions:
CI adds a MySQL+Redis service matrix and `NADOUMI_MAIL_TRANSPORT=log` for the two
E2E specs).

---

## Revision 3 (2026-09-03) — registration UX, OTP component, onboarding

Spec **Revision 3** (D-R3-1…D-R3-4). No backend endpoint changes. New / re-scoped
frontend tasks, run after F-10:

- **F-12 — `<SiteHeader>`/`<SiteFooter>` on every layout** (bugfix, already landed
  in commit for `nuxt.config` `components: [{ path: '~/components/marketing',
  pathPrefix: false }]`). Plus `layouts/auth.vue` used by all auth pages.
- **F-13 — Refined `OtpInput` + `EmailVerifyStep`** (D-R3-2). Rewrite both,
  smaller/tighter boxes, `role="group"` + per-box `aria-label`, filled/verifying/
  error states, `Edit email` emit, reduced-motion transitions. Still the single OTP
  implementation. Update `OtpInput.test.ts` / `EmailVerifyStep.test.ts`.
- **F-14 — `/register` as a 3-step wizard** (D-R3-1). Step 1 personal + email
  (**no confirm-email** — the OTP is the ownership check) + `[Verify]` (disabled
  until both names + a valid email); step 2 verify with
  `Email: … [Edit email]` + `Email verified ✓`; step 3 password + **single**
  Terms & Privacy checkbox; `Next` gated on `verified ∧ passwordValid ∧ match ∧
  terms`; success → `navigateTo('/dashboard/onboarding')`. Collapse
  `ConsentCheckboxes` from two boxes to one (keep the component, add a `single`
  prop or a new `ConsentCheckbox`). Rewrite `register.test.ts`.
- **F-15 — Onboarding multi-step shell** (D-R3-3). `/dashboard/onboarding` wizard:
  `OnboardingProgress` (Personal · Identity · Education · Interests · Location ·
  Contact · Review), `OnboardingStep` wrapper (title, blurb, Required/Recommended/
  Optional legend), reduced-motion step transitions. Steps that map to EXISTING
  endpoints (`ProfileForm`, `EducationList`, `ContactList`) are embedded and
  actually save. PLANNED steps render disabled with a **"Coming soon — not saved
  yet"** banner — **no `$fetch`, no fake success**. Review step + "Finish" →
  `/dashboard`. `/dashboard` overview links to it when a profile exists.
- **F-16 — Document components (client-only, PLANNED — REQUIRES BACKEND)**.
  `app/components/onboarding/`: `ImageCropper` (crop/zoom/rotate on a `<canvas>`),
  `DocumentPreview` (large pan/zoom/rotate viewer), `ProfilePhotoUploadCard`
  (1:1 crop + quality guidance), `PassportUploadCard` (large readable preview,
  zoom/rotate, format/size guidance). File validation (type + size) client-side.
  **No upload endpoint** — the "Upload" action is disabled with an
  "Available once document storage is enabled" note. No OCR / face-match claims.
  Used by F-15's PLANNED photo/passport step.
- **Task 24 (docs) extended** — `docs/APPLICANT_ONBOARDING.md` rewritten to the
  7-step structure with an **EXISTING / PLANNED / REQUIRES BACKEND** column;
  matching cross-references added to `DOMAIN_MODEL.md`, `DATABASE_DESIGN.md`,
  `API_DESIGN.md`, `FRONTEND_ARCHITECTURE.md`.

Revision-3 execution order: **F-12 → F-13 → F-14 → F-16 → F-15 → docs**.

## Global Constraints

- **Public/student architecture:** Nuxt 3 (SSR) + Nitro BFF + httpOnly session cookie. No Next.js, no second SPA framework. Do not reopen.
- **Styling:** Tailwind CSS + a small hand-built shared component set with design tokens. No large component library (no Nuxt UI, no Element Plus, no PrimeVue).
- **Brand primary:** orange `#F97316` (Tailwind `orange-500`). Solid CTA = `orange-600` bg + white text ≥16px semibold. Text links / small text on light = `orange-700` (`#C2410C`). Focus ring = `orange-500`. **Never** put text smaller than 16px semibold on an `orange-500` fill.
- **Theme:** light only. Tokens defined as CSS variables so dark mode is a later additive change. Do not build dark mode.
- **Logo:** text wordmark "Nadoumi". Header markup must let a real `<img>` replace the wordmark with no layout change.
- **Typography:** headings **Plus Jakarta Sans** (600, 700); body/UI **Inter** (400, 500, 600); Arabic fallback **Noto Sans Arabic** (400, 600). Self-hosted via `@nuxt/fonts`.
- **i18n:** `en` fully populated. `fr` / `ar` / `zh` mirror the same key tree with English values. `nuxt.config` i18n `fallbackLocale: 'en'` (also already set in `i18n/i18n.config.ts`). `ar` renders RTL (`<html dir="rtl">`). All user-facing strings come from i18n keys, including validation messages.
- **Credential exposure rule:** a password is sent by the browser exactly once per action — to `POST /api/student-session` (login), `POST /api/student-account` (register), `POST /api/student-password-reset` (reset), or `POST /api/student-password` (change). No client code stores, re-sends, or chains a second authenticated call with raw credentials. The JWT is never in a response body, `useState`, `localStorage`, or a client log. The OTP `ticket` is a non-secret opaque handle and may sit in client memory between verify and register/reset.
- **BFF route naming:** session resource is `server/api/student-session.{get,post,delete}.ts`; account creation is `server/api/student-account.post.ts`; anonymous backend endpoints not under `/api/public/**` get a named file beside `server/api/public/[...path].ts`. Revision 2 adds `student-email-otp.post.ts`, `student-email-otp-verify.post.ts`, `student-password-reset.post.ts`, `student-password.post.ts`.
- **Password policy (Revision 2, shared):** 8–32 chars; ≥1 uppercase, ≥1 lowercase, ≥1 digit, ≥1 special (`` !"#$%&'()*+,-./:;<=>?@[\]^_`{|}~ ``); new ≠ current. One Java validator (`PasswordPolicy` in `ruoyi-common`), one TS mirror (`nadoumi-web/app/utils/passwordPolicy.ts`) — identical rules, i18n message keys.
- **Student identity (Revision 2):** email-first. The `/login` form field is **email**. `sys_user.user_name` for `user_type='10'` is a server-generated internal handle; `email` is unique among students and carries `email_verified`.
- **No fake data (Revision 2):** never render invented statistics, application rows, activity items, or notification items. Absent data is a designed empty state.
- **CI:** the Playwright E2E jobs are hard gates — no `continue-on-error`. Backend-startup flakiness is handled with a health-check poll before the Playwright step. The CI backend runs with `nadoumi.mail.transport=log`.
- **Catalog data:** University / Program / Scholarship list & detail pages render designed empty-states; there is no backend catalog API yet and this build adds none.
- **Every step ends green:** `pnpm lint && pnpm test && pnpm build` from `nadoumi-web/`.
- **Commits:** one per task minimum; conventional-commit messages; end the body with the two attribution lines from the repo instructions.

---

## Backend contract (verified against Phase 3 source — do not re-guess)

Anonymous:

> **Revision 2 changes `register` + `login` — see the "Revision 2 delta" table
> below.** The two rows here are the pre-Revision-2 Phase-3 shapes, kept for
> reference; Parts A–D were built against them, Part E replaces them.

| Method | Path | Body | Response |
| --- | --- | --- | --- |
| POST | `/api/student/register` | `{ username (2–20), password (5–20), nickName (≤30)?, email?, code?, uuid? }` — **superseded (Revision 2)** | `201 { userId, username }` |
| POST | `/api/student/login` | `{ username, password, code?, uuid? }` — **superseded (Revision 2)** | `200 { token }` |
| GET | `/captchaImage` | — | `200 { code: 200, captchaEnabled: boolean, uuid?, img? }` — `img` is bare base64 (no `data:` prefix), JPEG |

Bearer (`Authorization: Bearer <jwt>`):

| Method | Path | Body | Response |
| --- | --- | --- | --- |
| GET | `/api/student/me` | — | `{ userId, username, nickName, accessibleApplicants: [{ applicantId, accessRole, capabilities: string[] }] }` |
| POST | `/api/student/logout` | — | `204` |
| GET | `/api/student/applicants` | — | `ApplicantResponse[]` (plain array, **not** paged) |
| POST | `/api/student/applicants` | `SelfApplicantRequest` | `201 ApplicantResponse` |
| GET | `/api/student/applicants/{id}` | — | `ApplicantResponse` |
| PUT | `/api/student/applicants/{id}` | `SelfApplicantRequest` | `200 ApplicantResponse` |
| GET / POST | `/api/student/applicants/{id}/education` | `EducationRequest` on POST | list / `201` |
| PUT / DELETE | `/api/student/applicants/{id}/education/{educationId}` | `EducationRequest` on PUT | `200` / `204` |
| GET / POST | `/api/student/applicants/{id}/test-scores` | `TestScoreRequest` on POST | list / `201` — **no PUT** |
| DELETE | `/api/student/applicants/{id}/test-scores/{scoreId}` | — | `204` |
| GET / POST | `/api/student/applicants/{id}/contacts` | `ContactRequest` on POST | list / `201` — **no PUT** |
| DELETE | `/api/student/applicants/{id}/contacts/{contactId}` | — | `204` |

DTO shapes:

- `SelfApplicantRequest` = `{ givenName (req, ≤100), familyName (req, ≤100), dob? (ISO "YYYY-MM-DD"), nationality? (exactly 2 chars), passportNo? (≤64), email? (≤120), phone? (≤32) }`
- `ApplicantResponse` = `{ id, givenName, familyName, dob, nationality, passportNo, email, phone, status }` — `status ∈ DRAFT | ACTIVE | UNLINKED | ARCHIVED`. For a student viewing their own applicant, `dob`/`passportNo` are unmasked.
- `EducationRequest` = `{ institution (req, ≤200), level? (≤32), field? (≤120), gpa? (number), gpaScale? (number), startDate? (ISO), endDate? (ISO) }`; `EducationResponse` adds `id`.
- `TestScoreRequest` = `{ testType (req, ≤24), score (req, ≤32), subScoresJson? (string), takenOn? (ISO), expiresOn? (ISO) }`; `TestScoreResponse` adds `id`.
- `ContactRequest` = `{ relation (req, one of GUARDIAN | EMERGENCY | OTHER), name (req, ≤150), email? (≤120), phone? (≤32) }`; `ContactResponse` adds `id`, `relation` as string.

Errors on `/api/student/**` and `/api/public/**` (Nadoumi surface) are RFC 9457 `application/problem+json`: `{ type, title, status, detail }`. `detail` is a human string; on body-validation failure it is `"<field> <message>"`. There is **no** `errors` array. `$fetch` throws `FetchError` with `error.data` = the problem body and `error.statusCode` = status.

RuoYi `/login` (staff) and `/captchaImage` use the legacy envelope `{ code, msg, ... }` with HTTP 200 even on failure — but the student surface above does not.

### Backend contract — Revision 2 delta (built in PART E)

| Method | Path | Body | Response |
| --- | --- | --- | --- |
| POST | `/api/student/email-otp` | `{ email, purpose: "REGISTER" \| "PASSWORD_RESET", code?, uuid? }` | `200 { sent: true }` — **always**, regardless of whether the email exists |
| POST | `/api/student/email-otp/verify` | `{ email, purpose, otp }` | `200 { ticket }` \| `400` problem+json |
| POST | `/api/student/register` (**changed**) | `{ firstName (req, ≤100), lastName (req, ≤100), email (req, ≤120), password (policy), ticket (req) }` | `201 { userId, username }` |
| POST | `/api/student/login` (**changed**) | `{ email (req), password (req), code?, uuid? }` | `200 { token }` |
| POST | `/api/student/password/reset` | `{ ticket (req), newPassword (policy) }` | `204` — no token, no login |
| POST | `/api/student/password` (bearer) | `{ currentPassword (req), newPassword (policy) }` | `204` |

`SelfApplicantRequest` / `ApplicantResponse` etc. are **unchanged** — the applicant
profile stays at Revision 1 scope (spec D-R2-4). The full onboarding schema is
`docs/APPLICANT_ONBOARDING.md`, PROPOSED, not built.

---

## File structure

**Created — tooling / config**

| Path | Responsibility |
| --- | --- |
| `nadoumi-web/tailwind.config.ts` | Tailwind theme: brand `orange` scale aliases, `slate` neutrals, `font-display`/`font-sans` families, radius/shadow scale, container widths. |
| `nadoumi-web/app/assets/css/tokens.css` | The same values as CSS custom properties on `:root` (future theming hook). |
| `nadoumi-web/vitest.config.ts` | Vitest + `@nuxt/test-utils` environment `nuxt`, jsdom, globals. |
| `nadoumi-web/playwright.config.ts` | One project, `webServer` boots `nuxt preview` + points `NUXT_BACKEND_BASE_URL` at the CI backend, `baseURL` `http://localhost:3000`. |
| `nadoumi-web/tests/e2e/auth.spec.ts` | The single E2E journey: register → auto-login → dashboard → sign out. |

**Created — shared UI primitives (`nadoumi-web/app/components/ui/`)**

`NButton.vue`, `NInput.vue`, `NTextarea.vue`, `NSelect.vue`, `NCheckbox.vue`, `NField.vue`, `NCard.vue`, `NBadge.vue`, `NAlert.vue`, `NContainer.vue`, `NSpinner.vue`, `NModal.vue`, `NDropdown.vue`, `NAvatar.vue`, `NLocaleSwitcher.vue` — one responsibility each, no domain knowledge, keyboard-accessible, visible `:focus-visible` ring, `prefers-reduced-motion` respected.

**Created — marketing components (`nadoumi-web/app/components/marketing/`)**

`SiteHeader.vue`, `SiteFooter.vue`, `Hero.vue`, `FeatureGrid.vue`, `StatBand.vue`, `CTASection.vue`, `DestinationCard.vue`, `Testimonial.vue`, `FaqAccordion.vue`, `EmptyState.vue`.

**Created — dashboard components (`nadoumi-web/app/components/dashboard/`)**

`DashboardShell.vue`, `ApplicantSwitcher.vue`, `ProfileForm.vue`, `EducationList.vue`, `TestScoreList.vue`, `ContactList.vue`, `SectionCard.vue`.

**Created — composables / plugins / middleware (`nadoumi-web/app/`)**

| Path | Responsibility |
| --- | --- |
| `composables/useSession.ts` | `useState`-backed session: `status`, `user`, `applicants`, `activeApplicantId`; `refresh()`, `signOut()`, `setActiveApplicant()`. |
| `composables/useApplicant.ts` | Thin typed wrappers over `studentFetch` for applicant + education + test-score + contact CRUD. |
| `plugins/session.ts` | Calls `useSession().refresh()` once at app init (runs on server and client). |
| `plugins/dir.ts` | Sets `<html lang>` + `<html dir>` from the active locale. |
| `middleware/auth.ts` | Route middleware for `/dashboard/**`: guest → `/login?redirect=`. |
| `middleware/guest.ts` | Route middleware for `/login`,`/register`: authed → `/dashboard`. |

**Created — BFF routes (`nadoumi-web/server/`)**

| Path | Responsibility |
| --- | --- |
| `api/student-session.get.ts` | Read cookie → `GET /api/student/me` → `{ authenticated, user, applicants }`; 401 clears cookie. |
| `api/student-account.post.ts` | `POST /api/student/register` then `POST /api/student/login` server-side; `setStudentToken`; return `{ signedIn: true }`. Passes register `problem+json` + status through on failure. **Revision 2 (F-5):** body `{ firstName, lastName, email, password, ticket }`; login forward `{ email, password }`. |
| `api/public/captcha.get.ts` | Passthrough to backend `GET /captchaImage`. |
| `api/student-email-otp.post.ts` · `api/student-email-otp-verify.post.ts` | **Revision 2 (F-4).** Anonymous passthroughs to `POST /api/student/email-otp` and `.../verify`. |
| `api/student-password-reset.post.ts` · `api/student-password.post.ts` | **Revision 2 (F-4).** Reset = anonymous passthrough; change = cookie-authed, attaches the bearer. |
| `routes/sitemap.xml.ts` | Static list of marketing routes. |

**Created — pages (`nadoumi-web/app/pages/`)**

`login.vue`, `register.vue`, `forgot-password.vue`, `destinations/index.vue`, `privacy.vue`, `terms.vue`, `dashboard/index.vue`, `dashboard/profile.vue`, `dashboard/education.vue`, `dashboard/test-scores.vue`, `dashboard/contacts.vue`, `dashboard/account.vue`, `universities/[id].vue`, `programs/[id].vue`, `scholarships/[id].vue`.

**Modified**

| Path | Change |
| --- | --- |
| `nadoumi-web/package.json` | add deps + `test` / `test:e2e` scripts. |
| `nadoumi-web/nuxt.config.ts` | register `@nuxtjs/tailwindcss` + `@nuxt/fonts`; `fonts` config; i18n `fallbackLocale`; keep `runtimeConfig.backendBaseUrl`. |
| `nadoumi-web/app/assets/css/main.css` | replace ad-hoc CSS with `@tailwind` layers + token import + a few base rules. |
| `nadoumi-web/app/layouts/default.vue` | use new `SiteHeader` / `SiteFooter`. |
| `nadoumi-web/app/components/AppHeader.vue`, `AppFooter.vue`, `PageHero.vue`, `ContentCard.vue` | deleted, replaced by the `marketing/` set. |
| `nadoumi-web/app/pages/index.vue`, `about.vue`, `contact.vue`, `scholarships/index.vue`, `universities/index.vue`, `programs/index.vue` | rebuilt on the new components. |
| `nadoumi-web/app/composables/useApi.ts` | keep `publicGet` / `studentFetch`; add `problemMessage(err)` helper. |
| `nadoumi-web/i18n/locales/{en,fr,ar,zh}.json` | full key tree. |
| `nadoumi-web/app/types/catalog.ts` | keep; add `ApplicantDto`, `EducationDto`, `TestScoreDto`, `ContactDto`, `SessionDto`. |
| `.github/workflows/ci.yml` | `nadoumi-web` job: add `pnpm test`; new `nadoumi-web-e2e` job `needs: [backend]`. |
| `docs/FRONTEND_ARCHITECTURE.md` | rewrite §7. |
| memory `nadoumi-web-stack.md` + `MEMORY.md` index line. |

---

# PART A — Foundation & design system

Produces: a linted, building, SSR marketing shell with the brand design system, the full `ui/` primitive set (unit-tested), the site header/footer, and the i18n key tree.

---

### Task 1: Tooling — Tailwind, fonts, tokens, test runners

**Files:**
- Modify: `nadoumi-web/package.json`
- Create: `nadoumi-web/tailwind.config.ts`
- Create: `nadoumi-web/app/assets/css/tokens.css`
- Modify: `nadoumi-web/app/assets/css/main.css`
- Modify: `nadoumi-web/nuxt.config.ts`
- Create: `nadoumi-web/vitest.config.ts`
- Create: `nadoumi-web/tests/unit/smoke.test.ts`

**Interfaces:**
- Produces: Tailwind classes `font-display`, `font-sans`; color aliases `brand-50..900`, plus semantic utilities via tokens; `pnpm test` runs Vitest with the `nuxt` environment; `pnpm build` still passes.

- [ ] **Step 1: Add dependencies**

Run in `nadoumi-web/`:

```bash
pnpm add -D @nuxtjs/tailwindcss@^6.12.2 @nuxt/fonts@^0.10.2 \
  vitest@^2.1.4 @vue/test-utils@^2.4.6 @nuxt/test-utils@^3.14.4 \
  happy-dom@^15.7.4 playwright@^1.48.0 @playwright/test@^1.48.0
```

- [ ] **Step 2: Add scripts to `package.json`**

In the `"scripts"` block add:

```json
"test": "vitest run",
"test:watch": "vitest",
"test:e2e": "playwright test"
```

- [ ] **Step 3: Write `tailwind.config.ts`**

```ts
import type { Config } from 'tailwindcss'

export default <Partial<Config>>{
  content: [
    './app/**/*.{vue,ts}',
    './app/components/**/*.{vue,ts}',
    './app/pages/**/*.vue',
    './app/layouts/**/*.vue',
  ],
  theme: {
    extend: {
      colors: {
        // brand === Tailwind orange; aliased so intent reads in markup
        brand: {
          50: '#fff7ed', 100: '#ffedd5', 200: '#fed7aa', 300: '#fdba74',
          400: '#fb923c', 500: '#f97316', 600: '#ea580c', 700: '#c2410c',
          800: '#9a3412', 900: '#7c2d12',
        },
      },
      fontFamily: {
        display: ['"Plus Jakarta Sans"', 'Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        arabic: ['"Noto Sans Arabic"', 'Inter', 'ui-sans-serif', 'sans-serif'],
      },
      borderRadius: { sm: '6px', md: '10px', lg: '16px' },
      boxShadow: {
        xs: '0 1px 2px 0 rgb(15 23 42 / 0.05)',
        sm: '0 1px 3px 0 rgb(15 23 42 / 0.08), 0 1px 2px -1px rgb(15 23 42 / 0.08)',
        md: '0 6px 16px -4px rgb(15 23 42 / 0.10)',
      },
      maxWidth: { marketing: '72rem', app: '64rem' },
    },
  },
}
```

- [ ] **Step 4: Write `app/assets/css/tokens.css`**

```css
:root {
  --brand-500: #f97316;
  --brand-600: #ea580c;
  --brand-700: #c2410c;
  --text: #0f172a;          /* slate-900 */
  --text-muted: #475569;    /* slate-600 */
  --border: #e2e8f0;        /* slate-200 */
  --surface: #ffffff;
  --surface-subtle: #f8fafc; /* slate-50 */
  --focus: #f97316;
  --radius-md: 10px;
}
```

- [ ] **Step 5: Replace `app/assets/css/main.css`**

```css
@import './tokens.css';
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  html { color: var(--text); background: var(--surface); }
  body { @apply font-sans antialiased; }
  h1, h2, h3, h4 { @apply font-display; text-wrap: balance; }
  a { color: var(--brand-700); }
  a:hover { text-decoration: underline; }
  :where(button, a, input, select, textarea):focus-visible {
    outline: 2px solid var(--focus);
    outline-offset: 2px;
  }
  html[lang="ar"] body { @apply font-arabic; }
}

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { animation-duration: .01ms !important; transition-duration: .01ms !important; }
}
```

- [ ] **Step 6: Update `nuxt.config.ts`**

Add `'@nuxtjs/tailwindcss'` and `'@nuxt/fonts'` to `modules` (before `'@nuxtjs/i18n'`). Add:

```ts
  fonts: {
    families: [
      { name: 'Plus Jakarta Sans', provider: 'google', weights: [600, 700] },
      { name: 'Inter', provider: 'google', weights: [400, 500, 600] },
      { name: 'Noto Sans Arabic', provider: 'google', weights: [400, 600] },
    ],
  },
  tailwindcss: { cssPath: '~/assets/css/main.css' },
```

Keep `css: ['~/assets/css/main.css']` (Tailwind module also picks it up; harmless). Keep `runtimeConfig.backendBaseUrl`.

- [ ] **Step 7: Write `vitest.config.ts`**

```ts
import { defineVitestConfig } from '@nuxt/test-utils/config'

export default defineVitestConfig({
  test: {
    environment: 'nuxt',
    environmentOptions: { nuxt: { domEnvironment: 'happy-dom' } },
    globals: true,
    include: ['tests/unit/**/*.test.ts'],
  },
})
```

- [ ] **Step 8: Write `tests/unit/smoke.test.ts`**

```ts
import { describe, it, expect } from 'vitest'

describe('toolchain', () => {
  it('runs vitest', () => {
    expect(1 + 1).toBe(2)
  })
})
```

- [ ] **Step 9: Run the gate**

Run: `pnpm install && pnpm test && pnpm lint && pnpm build`
Expected: Vitest 1 passed; ESLint clean; Nuxt build succeeds.

- [ ] **Step 10: Commit**

```bash
git add nadoumi-web
git commit -m "chore(web): add Tailwind, fonts, design tokens, Vitest + Playwright"
```

---

### Task 2: `NButton` — primitive pattern-setter (full TDD)

**Files:**
- Create: `nadoumi-web/app/components/ui/NButton.vue`
- Create: `nadoumi-web/tests/unit/ui/NButton.test.ts`

**Interfaces:**
- Produces: `<NButton>` props `variant?: 'primary' | 'secondary' | 'ghost' | 'link'` (default `primary`), `size?: 'sm' | 'md' | 'lg'` (default `md`), `type?: 'button' | 'submit'` (default `button`), `loading?: boolean`, `disabled?: boolean`, `block?: boolean`, `to?: string`. When `to` is set it renders `<NuxtLink>`, else `<button>`. `loading` implies disabled and shows `<NSpinner>` (until `NSpinner` exists, an inline SVG; swapped in Task 5 — leave a `<!-- spinner -->` comment span with class `n-btn__spin`). Default slot = label.

- [ ] **Step 1: Write the failing test**

```ts
// tests/unit/ui/NButton.test.ts
import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import NButton from '~/components/ui/NButton.vue'

describe('NButton', () => {
  it('renders a button with the label', async () => {
    const w = await mountSuspended(NButton, { slots: { default: () => 'Save' } })
    expect(w.find('button').exists()).toBe(true)
    expect(w.text()).toContain('Save')
  })

  it('applies the primary variant classes by default', async () => {
    const w = await mountSuspended(NButton, { slots: { default: () => 'Go' } })
    expect(w.find('button').classes().join(' ')).toContain('bg-brand-600')
  })

  it('renders a NuxtLink when "to" is set', async () => {
    const w = await mountSuspended(NButton, { props: { to: '/x' }, slots: { default: () => 'Link' } })
    expect(w.find('a').exists()).toBe(true)
    expect(w.find('button').exists()).toBe(false)
  })

  it('is disabled and shows the spinner while loading', async () => {
    const w = await mountSuspended(NButton, { props: { loading: true }, slots: { default: () => 'X' } })
    expect(w.find('button').attributes('disabled')).toBeDefined()
    expect(w.find('.n-btn__spin').exists()).toBe(true)
  })

  it('sets aria-disabled and blocks the link role when disabled with "to"', async () => {
    const w = await mountSuspended(NButton, { props: { to: '/x', disabled: true }, slots: { default: () => 'X' } })
    expect(w.find('a').attributes('aria-disabled')).toBe('true')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `pnpm test -- NButton`
Expected: FAIL — cannot resolve `~/components/ui/NButton.vue`.

- [ ] **Step 3: Write `NButton.vue`**

```vue
<script setup lang="ts">
const props = withDefaults(defineProps<{
  variant?: 'primary' | 'secondary' | 'ghost' | 'link'
  size?: 'sm' | 'md' | 'lg'
  type?: 'button' | 'submit'
  loading?: boolean
  disabled?: boolean
  block?: boolean
  to?: string
}>(), { variant: 'primary', size: 'md', type: 'button' })

const isDisabled = computed(() => props.disabled || props.loading)

const base =
  'inline-flex items-center justify-center gap-2 rounded-md font-semibold ' +
  'transition-colors disabled:opacity-60 disabled:pointer-events-none'

const sizes: Record<string, string> = {
  sm: 'text-sm px-3 py-1.5',
  md: 'text-base px-4 py-2',
  lg: 'text-base px-5 py-2.5',
}

const variants: Record<string, string> = {
  primary: 'bg-brand-600 text-white hover:bg-brand-700',
  secondary: 'bg-white text-slate-900 border border-slate-200 hover:bg-slate-50',
  ghost: 'bg-transparent text-slate-700 hover:bg-slate-100',
  link: 'bg-transparent text-brand-700 hover:underline px-0 py-0',
}

const classes = computed(() => [
  base, sizes[props.size], variants[props.variant],
  props.block ? 'w-full' : '',
])
</script>

<template>
  <NuxtLink
    v-if="to"
    :to="isDisabled ? undefined : to"
    :aria-disabled="isDisabled ? 'true' : undefined"
    :tabindex="isDisabled ? -1 : undefined"
    :class="classes"
  >
    <span v-if="loading" class="n-btn__spin" aria-hidden="true">
      <svg class="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
      </svg>
    </span>
    <slot />
  </NuxtLink>
  <button v-else :type="type" :disabled="isDisabled" :class="classes">
    <span v-if="loading" class="n-btn__spin" aria-hidden="true">
      <svg class="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
      </svg>
    </span>
    <slot />
  </button>
</template>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `pnpm test -- NButton`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add nadoumi-web/app/components/ui/NButton.vue nadoumi-web/tests/unit/ui/NButton.test.ts
git commit -m "feat(web): NButton primitive"
```

---

### Task 3: Form-control primitives — `NField`, `NInput`, `NTextarea`, `NSelect`, `NCheckbox`

**Files:**
- Create: `nadoumi-web/app/components/ui/NField.vue`
- Create: `nadoumi-web/app/components/ui/NInput.vue`
- Create: `nadoumi-web/app/components/ui/NTextarea.vue`
- Create: `nadoumi-web/app/components/ui/NSelect.vue`
- Create: `nadoumi-web/app/components/ui/NCheckbox.vue`
- Create: `nadoumi-web/tests/unit/ui/NField.test.ts`
- Create: `nadoumi-web/tests/unit/ui/NInput.test.ts`

**Interfaces:**
- Produces:
  - `<NField>` props `label: string`, `hint?: string`, `error?: string`, `required?: boolean`, `for?: string`. Renders a `<label>` bound to `for`, an optional required asterisk, the default slot (the control), a hint `<p>` and — when `error` is truthy — an error `<p id="{for}-error">` with `role="alert"`. Exposes nothing; controls wire `aria-describedby` themselves using the same `${id}-error` / `${id}-hint` convention.
  - `<NInput>` `v-model: string`, props `id`, `type?` (default `text`), `invalid?: boolean`, `autocomplete?`, `placeholder?`, `maxlength?`, `disabled?`. Emits `update:modelValue`.
  - `<NTextarea>` same as NInput plus `rows?` (default 4).
  - `<NSelect>` `v-model: string`, props `id`, `options: { value: string; label: string }[]`, `invalid?`, `disabled?`, `placeholder?`.
  - `<NCheckbox>` `v-model: boolean`, props `id`, default slot = label text.

- [ ] **Step 1: Write `tests/unit/ui/NField.test.ts`**

```ts
import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import NField from '~/components/ui/NField.vue'

describe('NField', () => {
  it('renders the label bound to the control id', async () => {
    const w = await mountSuspended(NField, { props: { label: 'Email', for: 'email' } })
    const label = w.find('label')
    expect(label.text()).toContain('Email')
    expect(label.attributes('for')).toBe('email')
  })

  it('shows a required marker when required', async () => {
    const w = await mountSuspended(NField, { props: { label: 'Email', for: 'email', required: true } })
    expect(w.text()).toContain('*')
  })

  it('renders the hint text', async () => {
    const w = await mountSuspended(NField, { props: { label: 'X', for: 'x', hint: 'help me' } })
    expect(w.find('#x-hint').text()).toBe('help me')
  })

  it('renders an alert error with the id the control points to', async () => {
    const w = await mountSuspended(NField, { props: { label: 'X', for: 'x', error: 'is required' } })
    const err = w.find('#x-error')
    expect(err.exists()).toBe(true)
    expect(err.attributes('role')).toBe('alert')
    expect(err.text()).toBe('is required')
  })

  it('hides the error node when there is no error', async () => {
    const w = await mountSuspended(NField, { props: { label: 'X', for: 'x' } })
    expect(w.find('#x-error').exists()).toBe(false)
  })
})
```

- [ ] **Step 2: Write `tests/unit/ui/NInput.test.ts`**

```ts
import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import NInput from '~/components/ui/NInput.vue'

describe('NInput', () => {
  it('emits update:modelValue on input', async () => {
    const w = await mountSuspended(NInput, { props: { id: 'a', modelValue: '' } })
    await w.find('input').setValue('hello')
    expect(w.emitted('update:modelValue')?.[0]).toEqual(['hello'])
  })

  it('wires aria-describedby to the error id when invalid', async () => {
    const w = await mountSuspended(NInput, { props: { id: 'a', modelValue: '', invalid: true } })
    expect(w.find('input').attributes('aria-describedby')).toContain('a-error')
    expect(w.find('input').attributes('aria-invalid')).toBe('true')
  })
})
```

- [ ] **Step 3: Run tests — verify they fail**

Run: `pnpm test -- ui/NField ui/NInput`
Expected: FAIL — components not found.

- [ ] **Step 4: Write `NField.vue`**

```vue
<script setup lang="ts">
const props = defineProps<{
  label: string
  for: string
  hint?: string
  error?: string
  required?: boolean
}>()
</script>

<template>
  <div class="grid gap-1.5">
    <label :for="props.for" class="text-sm font-medium text-slate-700">
      {{ label }}<span v-if="required" class="text-brand-700" aria-hidden="true"> *</span>
    </label>
    <slot />
    <p v-if="hint" :id="`${props.for}-hint`" class="text-xs text-slate-500">{{ hint }}</p>
    <p v-if="error" :id="`${props.for}-error`" role="alert" class="text-xs text-red-600">{{ error }}</p>
  </div>
</template>
```

- [ ] **Step 5: Write `NInput.vue`**

```vue
<script setup lang="ts">
const props = withDefaults(defineProps<{
  id: string
  modelValue: string
  type?: string
  invalid?: boolean
  autocomplete?: string
  placeholder?: string
  maxlength?: number
  disabled?: boolean
}>(), { type: 'text' })
defineEmits<{ 'update:modelValue': [value: string] }>()

const describedBy = computed(() => props.invalid ? `${props.id}-error` : undefined)
</script>

<template>
  <input
    :id="id"
    :type="type"
    :value="modelValue"
    :placeholder="placeholder"
    :maxlength="maxlength"
    :disabled="disabled"
    :autocomplete="autocomplete"
    :aria-invalid="invalid ? 'true' : undefined"
    :aria-describedby="describedBy"
    class="w-full rounded-md border px-3 py-2 text-base outline-none focus-visible:border-brand-500"
    :class="invalid ? 'border-red-400' : 'border-slate-200'"
    @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
  >
</template>
```

- [ ] **Step 6: Write `NTextarea.vue`**

```vue
<script setup lang="ts">
withDefaults(defineProps<{
  id: string
  modelValue: string
  rows?: number
  invalid?: boolean
  placeholder?: string
  maxlength?: number
  disabled?: boolean
}>(), { rows: 4 })
defineEmits<{ 'update:modelValue': [value: string] }>()
</script>

<template>
  <textarea
    :id="id"
    :rows="rows"
    :value="modelValue"
    :placeholder="placeholder"
    :maxlength="maxlength"
    :disabled="disabled"
    :aria-invalid="invalid ? 'true' : undefined"
    :aria-describedby="invalid ? `${id}-error` : undefined"
    class="w-full rounded-md border px-3 py-2 text-base outline-none focus-visible:border-brand-500"
    :class="invalid ? 'border-red-400' : 'border-slate-200'"
    @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
  />
</template>
```

- [ ] **Step 7: Write `NSelect.vue`**

```vue
<script setup lang="ts">
defineProps<{
  id: string
  modelValue: string
  options: { value: string; label: string }[]
  invalid?: boolean
  disabled?: boolean
  placeholder?: string
}>()
defineEmits<{ 'update:modelValue': [value: string] }>()
</script>

<template>
  <select
    :id="id"
    :value="modelValue"
    :disabled="disabled"
    :aria-invalid="invalid ? 'true' : undefined"
    :aria-describedby="invalid ? `${id}-error` : undefined"
    class="w-full rounded-md border px-3 py-2 text-base bg-white outline-none focus-visible:border-brand-500"
    :class="invalid ? 'border-red-400' : 'border-slate-200'"
    @change="$emit('update:modelValue', ($event.target as HTMLSelectElement).value)"
  >
    <option v-if="placeholder" value="" disabled>{{ placeholder }}</option>
    <option v-for="o in options" :key="o.value" :value="o.value">{{ o.label }}</option>
  </select>
</template>
```

- [ ] **Step 8: Write `NCheckbox.vue`**

```vue
<script setup lang="ts">
defineProps<{ id: string; modelValue: boolean }>()
defineEmits<{ 'update:modelValue': [value: boolean] }>()
</script>

<template>
  <label :for="id" class="flex items-start gap-2 text-sm text-slate-700">
    <input
      :id="id"
      type="checkbox"
      :checked="modelValue"
      class="mt-0.5 h-4 w-4 rounded border-slate-300 text-brand-600 focus-visible:outline-brand-500"
      @change="$emit('update:modelValue', ($event.target as HTMLInputElement).checked)"
    >
    <span><slot /></span>
  </label>
</template>
```

- [ ] **Step 9: Run tests — verify they pass**

Run: `pnpm test -- ui/NField ui/NInput && pnpm lint`
Expected: PASS (7 tests), lint clean.

- [ ] **Step 10: Commit**

```bash
git add nadoumi-web/app/components/ui nadoumi-web/tests/unit/ui
git commit -m "feat(web): form-control primitives (NField, NInput, NTextarea, NSelect, NCheckbox)"
```

---

### Task 4: Display primitives — `NContainer`, `NCard`, `NBadge`, `NAlert`, `NSpinner`

**Files:**
- Create: `nadoumi-web/app/components/ui/NContainer.vue`
- Create: `nadoumi-web/app/components/ui/NCard.vue`
- Create: `nadoumi-web/app/components/ui/NBadge.vue`
- Create: `nadoumi-web/app/components/ui/NAlert.vue`
- Create: `nadoumi-web/app/components/ui/NSpinner.vue`
- Create: `nadoumi-web/tests/unit/ui/NAlert.test.ts`

**Interfaces:**
- Produces:
  - `<NContainer>` prop `size?: 'marketing' | 'app'` (default `marketing`) → `max-w-marketing`/`max-w-app`, centered, `px-5`. Default slot.
  - `<NCard>` prop `padding?: 'sm' | 'md' | 'lg'` (default `md`); optional `#header` / `#footer` slots; default slot = body. Surface white, `border border-slate-200 rounded-lg shadow-xs`.
  - `<NBadge>` prop `tone?: 'neutral' | 'brand' | 'success' | 'warning' | 'danger'` (default `neutral`). Default slot = text.
  - `<NAlert>` props `tone?: 'info' | 'success' | 'warning' | 'danger'` (default `info`), `title?: string`, `dismissible?: boolean`; emits `dismiss`. `role="alert"` when tone is `warning`/`danger`, else `role="status"`.
  - `<NSpinner>` prop `size?: 'sm' | 'md'` (default `sm`), `label?: string` (visually-hidden, default `'Loading'`).

- [ ] **Step 1: Write `tests/unit/ui/NAlert.test.ts`**

```ts
import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import NAlert from '~/components/ui/NAlert.vue'

describe('NAlert', () => {
  it('shows the title and body slot', async () => {
    const w = await mountSuspended(NAlert, { props: { title: 'Heads up' }, slots: { default: () => 'details' } })
    expect(w.text()).toContain('Heads up')
    expect(w.text()).toContain('details')
  })

  it('uses role=alert for danger and role=status for info', async () => {
    const danger = await mountSuspended(NAlert, { props: { tone: 'danger' } })
    expect(danger.attributes('role')).toBe('alert')
    const info = await mountSuspended(NAlert, { props: { tone: 'info' } })
    expect(info.attributes('role')).toBe('status')
  })

  it('emits dismiss when the close button is clicked', async () => {
    const w = await mountSuspended(NAlert, { props: { dismissible: true } })
    await w.find('button[aria-label="Dismiss"]').trigger('click')
    expect(w.emitted('dismiss')).toHaveLength(1)
  })
})
```

- [ ] **Step 2: Run test — verify it fails**

Run: `pnpm test -- ui/NAlert`
Expected: FAIL — component not found.

- [ ] **Step 3: Write `NContainer.vue`**

```vue
<script setup lang="ts">
withDefaults(defineProps<{ size?: 'marketing' | 'app' }>(), { size: 'marketing' })
</script>

<template>
  <div class="mx-auto w-full px-5" :class="size === 'app' ? 'max-w-app' : 'max-w-marketing'">
    <slot />
  </div>
</template>
```

- [ ] **Step 4: Write `NCard.vue`**

```vue
<script setup lang="ts">
withDefaults(defineProps<{ padding?: 'sm' | 'md' | 'lg' }>(), { padding: 'md' })
const pad = { sm: 'p-4', md: 'p-6', lg: 'p-8' }
</script>

<template>
  <div class="rounded-lg border border-slate-200 bg-white shadow-xs">
    <div v-if="$slots.header" class="border-b border-slate-200 px-6 py-4 font-display font-semibold">
      <slot name="header" />
    </div>
    <div :class="pad[padding]"><slot /></div>
    <div v-if="$slots.footer" class="border-t border-slate-200 px-6 py-4"><slot name="footer" /></div>
  </div>
</template>
```

- [ ] **Step 5: Write `NBadge.vue`**

```vue
<script setup lang="ts">
withDefaults(defineProps<{ tone?: 'neutral' | 'brand' | 'success' | 'warning' | 'danger' }>(), { tone: 'neutral' })
const tones = {
  neutral: 'bg-slate-100 text-slate-700',
  brand: 'bg-brand-100 text-brand-800',
  success: 'bg-green-100 text-green-800',
  warning: 'bg-amber-100 text-amber-800',
  danger: 'bg-red-100 text-red-800',
}
</script>

<template>
  <span class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium" :class="tones[tone]">
    <slot />
  </span>
</template>
```

- [ ] **Step 6: Write `NAlert.vue`**

```vue
<script setup lang="ts">
const props = withDefaults(defineProps<{
  tone?: 'info' | 'success' | 'warning' | 'danger'
  title?: string
  dismissible?: boolean
}>(), { tone: 'info' })
defineEmits<{ dismiss: [] }>()

const role = computed(() => (props.tone === 'warning' || props.tone === 'danger' ? 'alert' : 'status'))
const tones = {
  info: 'bg-slate-50 border-slate-200 text-slate-800',
  success: 'bg-green-50 border-green-200 text-green-800',
  warning: 'bg-amber-50 border-amber-200 text-amber-900',
  danger: 'bg-red-50 border-red-200 text-red-800',
}
</script>

<template>
  <div :role="role" class="flex gap-3 rounded-md border px-4 py-3 text-sm" :class="tones[tone]">
    <div class="flex-1">
      <p v-if="title" class="font-semibold">{{ title }}</p>
      <div><slot /></div>
    </div>
    <button v-if="dismissible" type="button" aria-label="Dismiss" class="shrink-0 opacity-70 hover:opacity-100" @click="$emit('dismiss')">×</button>
  </div>
</template>
```

- [ ] **Step 7: Write `NSpinner.vue`**

```vue
<script setup lang="ts">
withDefaults(defineProps<{ size?: 'sm' | 'md'; label?: string }>(), { size: 'sm', label: 'Loading' })
const dim = { sm: 'h-4 w-4', md: 'h-6 w-6' }
</script>

<template>
  <span role="status" class="inline-flex items-center">
    <svg class="animate-spin text-current" :class="dim[size]" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
    </svg>
    <span class="sr-only">{{ label }}</span>
  </span>
</template>
```

- [ ] **Step 8: Swap the inline spinner in `NButton.vue`**

Replace each `<span v-if="loading" class="n-btn__spin" ...> ... </span>` block with:

```vue
    <NSpinner v-if="loading" size="sm" class="n-btn__spin" />
```

Keep the `.n-btn__spin` class so `NButton.test.ts` still passes.

- [ ] **Step 9: Run tests — verify pass**

Run: `pnpm test -- ui/ && pnpm lint`
Expected: PASS (NButton 5, NField 5, NInput 2, NAlert 3), lint clean.

- [ ] **Step 10: Commit**

```bash
git add nadoumi-web/app/components/ui nadoumi-web/tests/unit/ui
git commit -m "feat(web): display primitives (NContainer, NCard, NBadge, NAlert, NSpinner)"
```

---

### Task 5: Overlay primitives — `NModal`, `NDropdown`

**Files:**
- Create: `nadoumi-web/app/components/ui/NModal.vue`
- Create: `nadoumi-web/app/components/ui/NDropdown.vue`
- Create: `nadoumi-web/tests/unit/ui/NModal.test.ts`
- Create: `nadoumi-web/tests/unit/ui/NDropdown.test.ts`

**Interfaces:**
- Produces:
  - `<NModal>` `v-model: boolean`, prop `title?: string`. Teleports to `body`. Renders nothing when closed. On open: scrim, a `role="dialog" aria-modal="true"` panel, focus moves to the panel, `Escape` and scrim-click emit `update:modelValue = false`. `#default` slot = body, `#footer` slot = actions.
  - `<NDropdown>` prop `label: string` (trigger text) or `#trigger` slot. Renders a `<button aria-haspopup="menu" :aria-expanded>` and, when open, a `role="menu"` panel with the default slot. Closes on outside-click, `Escape`, and on `click` of any element inside with `[data-close]`.

- [ ] **Step 1: Write `tests/unit/ui/NModal.test.ts`**

```ts
import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import NModal from '~/components/ui/NModal.vue'

describe('NModal', () => {
  it('renders nothing when closed', async () => {
    const w = await mountSuspended(NModal, { props: { modelValue: false } })
    expect(document.querySelector('[role="dialog"]')).toBeNull()
    w.unmount()
  })

  it('renders a dialog with the title when open', async () => {
    const w = await mountSuspended(NModal, { props: { modelValue: true, title: 'Confirm' } })
    const dialog = document.querySelector('[role="dialog"]')
    expect(dialog).not.toBeNull()
    expect(dialog?.textContent).toContain('Confirm')
    w.unmount()
  })

  it('emits close on Escape', async () => {
    const w = await mountSuspended(NModal, { props: { modelValue: true } })
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await w.vm.$nextTick()
    expect(w.emitted('update:modelValue')?.at(-1)).toEqual([false])
    w.unmount()
  })
})
```

- [ ] **Step 2: Write `tests/unit/ui/NDropdown.test.ts`**

```ts
import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import NDropdown from '~/components/ui/NDropdown.vue'

describe('NDropdown', () => {
  it('toggles the menu and sets aria-expanded', async () => {
    const w = await mountSuspended(NDropdown, { props: { label: 'Account' }, slots: { default: () => '<a href="#">Item</a>' } })
    const trigger = w.find('button[aria-haspopup="menu"]')
    expect(trigger.attributes('aria-expanded')).toBe('false')
    await trigger.trigger('click')
    expect(trigger.attributes('aria-expanded')).toBe('true')
    expect(w.find('[role="menu"]').exists()).toBe(true)
  })
})
```

- [ ] **Step 3: Run tests — verify they fail**

Run: `pnpm test -- ui/NModal ui/NDropdown`
Expected: FAIL — components not found.

- [ ] **Step 4: Write `NModal.vue`**

```vue
<script setup lang="ts">
const props = defineProps<{ modelValue: boolean; title?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()
const panel = ref<HTMLElement | null>(null)

function close() { emit('update:modelValue', false) }
function onKey(e: KeyboardEvent) { if (e.key === 'Escape') close() }

watch(() => props.modelValue, async (open) => {
  if (import.meta.client) {
    document[open ? 'addEventListener' : 'removeEventListener']('keydown', onKey)
    document.body.style.overflow = open ? 'hidden' : ''
    if (open) { await nextTick(); panel.value?.focus() }
  }
})
onBeforeUnmount(() => {
  if (import.meta.client) { document.removeEventListener('keydown', onKey); document.body.style.overflow = '' }
})
</script>

<template>
  <ClientOnly>
    <Teleport to="body">
      <div v-if="modelValue" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-slate-900/40" @click="close" />
        <div
          ref="panel"
          role="dialog"
          aria-modal="true"
          tabindex="-1"
          class="relative w-full max-w-md rounded-lg bg-white p-6 shadow-md outline-none"
        >
          <h2 v-if="title" class="mb-3 font-display text-lg font-semibold">{{ title }}</h2>
          <div><slot /></div>
          <div v-if="$slots.footer" class="mt-5 flex justify-end gap-2"><slot name="footer" /></div>
        </div>
      </div>
    </Teleport>
  </ClientOnly>
</template>
```

- [ ] **Step 5: Write `NDropdown.vue`**

```vue
<script setup lang="ts">
defineProps<{ label?: string }>()
const open = ref(false)
const root = ref<HTMLElement | null>(null)

function onDocClick(e: MouseEvent) {
  if (root.value && !root.value.contains(e.target as Node)) open.value = false
}
function onKey(e: KeyboardEvent) { if (e.key === 'Escape') open.value = false }
watch(open, (v) => {
  if (!import.meta.client) return
  document[v ? 'addEventListener' : 'removeEventListener']('click', onDocClick)
  document[v ? 'addEventListener' : 'removeEventListener']('keydown', onKey)
})
onBeforeUnmount(() => {
  if (!import.meta.client) return
  document.removeEventListener('click', onDocClick)
  document.removeEventListener('keydown', onKey)
})
</script>

<template>
  <div ref="root" class="relative inline-block">
    <button
      type="button"
      aria-haspopup="menu"
      :aria-expanded="open ? 'true' : 'false'"
      class="inline-flex items-center gap-1 rounded-md px-2 py-1.5 hover:bg-slate-100"
      @click="open = !open"
    >
      <slot name="trigger">{{ label }}</slot>
    </button>
    <div
      v-if="open"
      role="menu"
      class="absolute end-0 z-40 mt-1 min-w-44 rounded-md border border-slate-200 bg-white py-1 shadow-md"
      @click="open = false"
    >
      <slot />
    </div>
  </div>
</template>
```

- [ ] **Step 6: Run tests — verify pass**

Run: `pnpm test -- ui/ && pnpm lint`
Expected: PASS, lint clean.

- [ ] **Step 7: Commit**

```bash
git add nadoumi-web/app/components/ui nadoumi-web/tests/unit/ui
git commit -m "feat(web): overlay primitives (NModal, NDropdown)"
```

---

### Task 6: `NAvatar`, `NLocaleSwitcher`, and the RTL/lang plugin

**Files:**
- Create: `nadoumi-web/app/components/ui/NAvatar.vue`
- Create: `nadoumi-web/app/components/ui/NLocaleSwitcher.vue`
- Create: `nadoumi-web/app/plugins/dir.ts`
- Create: `nadoumi-web/tests/unit/ui/NLocaleSwitcher.test.ts`

**Interfaces:**
- Produces:
  - `<NAvatar>` props `name?: string`, `size?: 'sm' | 'md'` (default `sm`). Shows up to 2 uppercase initials from `name` on a `bg-brand-100 text-brand-800` circle; empty circle if no name.
  - `<NLocaleSwitcher>` uses `useI18n().locales` + `useSwitchLocalePath()`. Renders an `NDropdown` whose menu lists each locale as a `<NuxtLink :to="switchLocalePath(code)">`; current locale marked `aria-current="true"`.
  - `plugins/dir.ts` — a Nuxt plugin that, on `useI18n().locale` change (and initial), sets `document.documentElement.lang` and `.dir` (`rtl` for `ar`, else `ltr`); on the server it uses `useHead({ htmlAttrs: { lang, dir } })`.

- [ ] **Step 1: Write `tests/unit/ui/NLocaleSwitcher.test.ts`**

```ts
import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import NLocaleSwitcher from '~/components/ui/NLocaleSwitcher.vue'

describe('NLocaleSwitcher', () => {
  it('lists every configured locale', async () => {
    const w = await mountSuspended(NLocaleSwitcher)
    await w.find('button[aria-haspopup="menu"]').trigger('click')
    const text = w.text()
    for (const label of ['English', 'Français', 'العربية', '中文']) {
      expect(text).toContain(label)
    }
  })
})
```

- [ ] **Step 2: Run test — verify it fails**

Run: `pnpm test -- ui/NLocaleSwitcher`
Expected: FAIL — component not found (and locale display names not yet configured).

- [ ] **Step 3: Add locale display names in `nuxt.config.ts`**

In the i18n `locales` array give each entry a `name`:

```ts
      { code: 'en', language: 'en', file: 'en.json', name: 'English' },
      { code: 'fr', language: 'fr', file: 'fr.json', name: 'Français' },
      { code: 'ar', language: 'ar', file: 'ar.json', dir: 'rtl', name: 'العربية' },
      { code: 'zh', language: 'zh-CN', file: 'zh.json', name: '中文' },
```

- [ ] **Step 4: Write `NAvatar.vue`**

```vue
<script setup lang="ts">
const props = withDefaults(defineProps<{ name?: string; size?: 'sm' | 'md' }>(), { size: 'sm' })
const initials = computed(() =>
  (props.name ?? '')
    .split(/\s+/).filter(Boolean).slice(0, 2).map(s => s[0]!.toUpperCase()).join(''),
)
const dim = { sm: 'h-8 w-8 text-xs', md: 'h-10 w-10 text-sm' }
</script>

<template>
  <span class="inline-flex items-center justify-center rounded-full bg-brand-100 font-semibold text-brand-800" :class="dim[size]">
    {{ initials }}
  </span>
</template>
```

- [ ] **Step 5: Write `NLocaleSwitcher.vue`**

```vue
<script setup lang="ts">
import type { LocaleObject } from '@nuxtjs/i18n'
const { locale, locales } = useI18n()
const switchLocalePath = useSwitchLocalePath()
const list = computed(() => locales.value as LocaleObject[])
</script>

<template>
  <NDropdown :label="(list.find(l => l.code === locale)?.name) ?? locale">
    <NuxtLink
      v-for="l in list"
      :key="l.code"
      :to="switchLocalePath(l.code)"
      :aria-current="l.code === locale ? 'true' : undefined"
      class="block px-3 py-2 text-sm hover:bg-slate-50"
      :class="l.code === locale ? 'font-semibold text-brand-700' : 'text-slate-700'"
    >
      {{ l.name }}
    </NuxtLink>
  </NDropdown>
</template>
```

- [ ] **Step 6: Write `plugins/dir.ts`**

```ts
export default defineNuxtPlugin(() => {
  const { locale } = useI18n()
  const dirFor = (code: string) => (code === 'ar' ? 'rtl' : 'ltr')

  useHead({ htmlAttrs: { lang: () => locale.value, dir: () => dirFor(locale.value) } })

  if (import.meta.client) {
    watch(locale, (code) => {
      document.documentElement.lang = code
      document.documentElement.dir = dirFor(code)
    }, { immediate: true })
  }
})
```

- [ ] **Step 7: Run tests — verify pass**

Run: `pnpm test -- ui/ && pnpm lint && pnpm build`
Expected: PASS, lint clean, build OK.

- [ ] **Step 8: Commit**

```bash
git add nadoumi-web/app nadoumi-web/nuxt.config.ts nadoumi-web/tests
git commit -m "feat(web): NAvatar, NLocaleSwitcher, RTL/lang plugin"
```

---

### Task 7: i18n key tree — author `en.json`, mirror `fr`/`ar`/`zh`

**Files:**
- Modify: `nadoumi-web/i18n/locales/en.json`
- Modify: `nadoumi-web/i18n/locales/fr.json`
- Modify: `nadoumi-web/i18n/locales/ar.json`
- Modify: `nadoumi-web/i18n/locales/zh.json`
- Modify: `nadoumi-web/nuxt.config.ts` (i18n `fallbackLocale`)
- Create: `nadoumi-web/tests/unit/i18n-keys.test.ts`

**Interfaces:**
- Produces: a stable key tree used by every later task. Top-level namespaces: `nav`, `footer`, `common`, `home`, `about`, `contact`, `destinations`, `catalog`, `auth`, `dashboard`, `validation`. Later tasks reference keys under these — if a needed key is missing, add it to **all four** files in the same step.

- [ ] **Step 1: Write `tests/unit/i18n-keys.test.ts`**

```ts
import { describe, it, expect } from 'vitest'
import en from '~/i18n/locales/en.json'
import fr from '~/i18n/locales/fr.json'
import ar from '~/i18n/locales/ar.json'
import zh from '~/i18n/locales/zh.json'

function keys(o: Record<string, unknown>, prefix = ''): string[] {
  return Object.entries(o).flatMap(([k, v]) =>
    v && typeof v === 'object'
      ? keys(v as Record<string, unknown>, `${prefix}${k}.`)
      : [`${prefix}${k}`],
  )
}

describe('i18n locale files', () => {
  it('fr, ar, zh have exactly the same key set as en', () => {
    const base = new Set(keys(en))
    for (const [name, loc] of [['fr', fr], ['ar', ar], ['zh', zh]] as const) {
      const got = new Set(keys(loc as Record<string, unknown>))
      expect({ name, missing: [...base].filter(k => !got.has(k)) }).toEqual({ name, missing: [] })
      expect({ name, extra: [...got].filter(k => !base.has(k)) }).toEqual({ name, extra: [] })
    }
  })
})
```

- [ ] **Step 2: Run test — verify it fails**

Run: `pnpm test -- i18n-keys`
Expected: FAIL — current files differ / lack the tree.

- [ ] **Step 3: Write `en.json` (full tree)**

```json
{
  "nav": { "home": "Home", "scholarships": "Scholarships", "universities": "Universities", "programs": "Programs", "destinations": "Destinations", "about": "About", "contact": "Contact", "signIn": "Sign in", "getStarted": "Create profile", "dashboard": "Dashboard" },
  "footer": {
    "mission": "Nadoumi helps international students discover and secure study opportunities abroad.",
    "explore": "Explore", "company": "Company", "getStarted": "Get started",
    "privacy": "Privacy", "terms": "Terms", "contactEmail": "support@nadoumi.com",
    "rights": "Nadoumi. International education platform."
  },
  "common": {
    "loading": "Loading", "save": "Save", "cancel": "Cancel", "add": "Add", "remove": "Remove",
    "edit": "Edit", "delete": "Delete", "confirm": "Confirm", "back": "Back", "search": "Search",
    "none": "Nothing here yet", "retry": "Try again", "signOut": "Sign out"
  },
  "home": {
    "title": "Study abroad with Nadoumi",
    "subtitle": "Discover universities, programmes and scholarships, then apply and track everything in one place.",
    "ctaPrimary": "Create your profile", "ctaSecondary": "Browse scholarships",
    "f1Title": "Discover", "f1Body": "Search scholarships and programmes by country, field and degree level.",
    "f2Title": "Apply", "f2Body": "Build one applicant profile and reuse it across every application.",
    "f3Title": "Track", "f3Body": "Follow each application's status, documents and decisions in your dashboard.",
    "statsIntro": "Built for students heading to China, Malaysia and beyond.",
    "ctaBandTitle": "Ready to start?", "ctaBandBody": "Create a free profile and explore your options."
  },
  "about": {
    "title": "About Nadoumi", "subtitle": "An international education platform.",
    "body": "Nadoumi connects students with universities, programmes and scholarships across China, Malaysia and other destinations, and supports them through the whole application lifecycle — profile, documents, submission, decisions and enrolment."
  },
  "contact": {
    "title": "Contact", "subtitle": "We usually reply within two business days.",
    "name": "Name", "email": "Email", "message": "Message", "send": "Send",
    "sent": "Thanks — we'll be in touch.", "email_help": "Or email support@nadoumi.com"
  },
  "destinations": {
    "title": "Destinations", "subtitle": "Where Nadoumi students study.",
    "chinaTitle": "China", "chinaBody": "Government and university scholarships across a wide range of programmes and cities.",
    "malaysiaTitle": "Malaysia", "malaysiaBody": "English-taught degrees and competitive tuition at established universities.",
    "more": "More destinations are being added."
  },
  "catalog": {
    "scholarshipsTitle": "Scholarships", "scholarshipsSubtitle": "Funding opportunities across our destination countries.",
    "universitiesTitle": "Universities", "universitiesSubtitle": "Institutions in the Nadoumi catalogue.",
    "programsTitle": "Programs", "programsSubtitle": "Degree programmes and intakes.",
    "empty": "This catalogue isn't available yet. Check back soon.",
    "emptyDetail": "We're still building this section.", "rollingDeadline": "Rolling deadline",
    "deadline": "Deadline {date}", "backToList": "Back to list"
  },
  "auth": {
    "loginTitle": "Sign in", "registerTitle": "Create your profile",
    "fullName": "Full name", "username": "Username", "usernameHint": "2–20 characters, used to sign in",
    "email": "Email", "password": "Password", "confirmPassword": "Confirm password",
    "passwordHint": "5–20 characters", "captcha": "Enter the code shown", "captchaReload": "Reload image",
    "acceptTerms": "I agree to the {terms} and {privacy}.",
    "submitLogin": "Sign in", "submitRegister": "Create profile",
    "toRegister": "New to Nadoumi? Create a profile", "toLogin": "Already have an account? Sign in",
    "forgot": "Forgot your password?",
    "forgotTitle": "Password help",
    "forgotBody": "Password reset isn't self-service yet. Email support@nadoumi.com from your registered address and we'll help you back in.",
    "genericError": "Something went wrong. Please check your details and try again.",
    "passwordMismatch": "Passwords do not match."
  },
  "dashboard": {
    "nav": { "overview": "Overview", "profile": "Profile", "education": "Education", "testScores": "Test scores", "contacts": "Contacts", "account": "Account" },
    "welcome": "Welcome, {name}", "overviewBlurb": "Complete your applicant profile so staff can start matching you to opportunities.",
    "completeness": "Profile completeness", "quickProfile": "Edit profile", "quickEducation": "Add education",
    "applicantLabel": "Applicant", "switchApplicant": "Switch applicant",
    "profileTitle": "Applicant profile", "createProfileTitle": "Create your applicant profile",
    "createProfileBlurb": "Tell us who the application is about. You can edit this any time.",
    "givenName": "Given name", "familyName": "Family name", "dob": "Date of birth", "nationality": "Nationality (ISO alpha-2)",
    "passportNo": "Passport number", "phone": "Phone",
    "eduTitle": "Education history", "eduEmpty": "No education entries yet.",
    "institution": "Institution", "level": "Level", "field": "Field of study", "gpa": "GPA", "gpaScale": "GPA scale",
    "startDate": "Start date", "endDate": "End date",
    "testTitle": "Test scores", "testEmpty": "No test scores yet.",
    "testType": "Test", "score": "Score", "takenOn": "Taken on", "expiresOn": "Expires on",
    "contactsTitle": "Contacts", "contactsEmpty": "No contacts yet.",
    "relation": "Relationship", "contactName": "Name",
    "relationGUARDIAN": "Guardian", "relationEMERGENCY": "Emergency", "relationOTHER": "Other",
    "accountTitle": "Account", "accountUser": "Signed in as", "passwordChange": "Password changes",
    "passwordChangeBody": "To change your password, email support@nadoumi.com from your registered address.",
    "savedOk": "Saved", "addedOk": "Added", "removedOk": "Removed", "removeConfirm": "Remove this entry?"
  },
  "validation": {
    "required": "This field is required.",
    "email": "Enter a valid email address.",
    "min": "Must be at least {n} characters.",
    "max": "Must be at most {n} characters.",
    "len2": "Use the 2-letter country code."
  }
}
```

- [ ] **Step 4: Mirror into `fr.json`, `ar.json`, `zh.json`**

Copy `en.json` verbatim into each of the three files (identical English values). This satisfies the fallback policy; real translations come later.

- [ ] **Step 5: Set `fallbackLocale` in `nuxt.config.ts`**

In the `i18n` block add `fallbackLocale: 'en'` (mirrors `i18n/i18n.config.ts`).

- [ ] **Step 6: Run test — verify pass**

Run: `pnpm test -- i18n-keys && pnpm lint`
Expected: PASS, lint clean.

- [ ] **Step 7: Commit**

```bash
git add nadoumi-web/i18n nadoumi-web/nuxt.config.ts nadoumi-web/tests/unit/i18n-keys.test.ts
git commit -m "feat(web): full i18n key tree (en authored, fr/ar/zh mirrored)"
```

---

### Task 8: `SiteHeader`, `SiteFooter`, `default` layout

**Files:**
- Create: `nadoumi-web/app/components/marketing/SiteHeader.vue`
- Create: `nadoumi-web/app/components/marketing/SiteFooter.vue`
- Modify: `nadoumi-web/app/layouts/default.vue`
- Delete: `nadoumi-web/app/components/AppHeader.vue`, `AppFooter.vue`
- Create: `nadoumi-web/tests/unit/marketing/SiteHeader.test.ts`

**Interfaces:**
- Consumes: `useSession()` — but that composable lands in Task 10. For this task, gate the auth area on a local `const session = { status: 'guest' as const }` placeholder **only if** `useSession` is absent; the header must import `useSession` from `~/composables/useSession` and the placeholder is removed in Task 11. To keep Part A self-contained, create a minimal stub `app/composables/useSession.ts` now that returns `{ status: ref('guest'), user: ref(null) }` and is fully replaced in Task 10.
- Produces: `<SiteHeader>` — wordmark link (`<NuxtLink :to="localePath('/')">` wrapping `<span class="site-brand">Nadoumi</span>`; a sibling `<img>` slot commented for later), primary nav from `nav.*` keys, `<NLocaleSwitcher>`, auth actions (guest: "Sign in" link + `NButton` "Create profile"; authed: `NDropdown` with Dashboard / Sign out), and a mobile disclosure `<button aria-controls="site-nav" :aria-expanded>`.
  `<SiteFooter>` — 4 columns (`footer.*`, `nav.*`), bottom bar with `© {{ year }} {{ t('footer.rights') }}`.

- [ ] **Step 1: Create the temporary `useSession` stub**

```ts
// app/composables/useSession.ts  — replaced in Task 10
export function useSession() {
  return { status: ref<'unknown' | 'guest' | 'authed'>('guest'), user: ref<null | { nickName: string }>(null) }
}
```

- [ ] **Step 2: Write `tests/unit/marketing/SiteHeader.test.ts`**

```ts
import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import SiteHeader from '~/components/marketing/SiteHeader.vue'

describe('SiteHeader', () => {
  it('shows the wordmark and the primary nav', async () => {
    const w = await mountSuspended(SiteHeader)
    expect(w.find('.site-brand').text()).toBe('Nadoumi')
    expect(w.text()).toContain('Scholarships')
    expect(w.text()).toContain('Universities')
  })

  it('shows guest auth actions when signed out', async () => {
    const w = await mountSuspended(SiteHeader)
    expect(w.text()).toContain('Sign in')
    expect(w.text()).toContain('Create profile')
  })
})
```

- [ ] **Step 3: Run test — verify it fails**

Run: `pnpm test -- marketing/SiteHeader`
Expected: FAIL — component not found.

- [ ] **Step 4: Write `SiteHeader.vue`**

```vue
<script setup lang="ts">
const { t } = useI18n()
const localePath = useLocalePath()
const { status, user } = useSession()
const open = ref(false)

const links = [
  { to: '/scholarships', key: 'nav.scholarships' },
  { to: '/universities', key: 'nav.universities' },
  { to: '/programs', key: 'nav.programs' },
  { to: '/destinations', key: 'nav.destinations' },
  { to: '/about', key: 'nav.about' },
  { to: '/contact', key: 'nav.contact' },
]
</script>

<template>
  <header class="border-b border-slate-200 bg-white">
    <NContainer>
      <div class="flex h-16 items-center justify-between gap-4">
        <NuxtLink :to="localePath('/')" class="flex items-center">
          <!-- swap this span for <img src="/logo.svg" alt="Nadoumi"> later; same box -->
          <span class="site-brand font-display text-lg font-bold text-slate-900">Nadoumi</span>
        </NuxtLink>

        <nav id="site-nav" class="hidden items-center gap-6 md:flex">
          <NuxtLink v-for="l in links" :key="l.to" :to="localePath(l.to)" class="text-sm text-slate-600 hover:text-slate-900">
            {{ t(l.key) }}
          </NuxtLink>
        </nav>

        <div class="flex items-center gap-3">
          <NLocaleSwitcher class="hidden sm:block" />
          <template v-if="status === 'authed'">
            <NDropdown :label="user?.nickName ?? t('nav.dashboard')">
              <NuxtLink :to="localePath('/dashboard')" class="block px-3 py-2 text-sm hover:bg-slate-50">{{ t('nav.dashboard') }}</NuxtLink>
              <NuxtLink :to="localePath('/dashboard/account')" class="block px-3 py-2 text-sm hover:bg-slate-50">{{ t('dashboard.nav.account') }}</NuxtLink>
            </NDropdown>
          </template>
          <template v-else>
            <NuxtLink :to="localePath('/login')" class="text-sm font-medium text-slate-700 hover:text-slate-900">{{ t('nav.signIn') }}</NuxtLink>
            <NButton :to="localePath('/register')" size="sm">{{ t('nav.getStarted') }}</NButton>
          </template>
          <button
            type="button"
            class="md:hidden rounded-md p-2 hover:bg-slate-100"
            aria-controls="site-nav"
            :aria-expanded="open ? 'true' : 'false'"
            @click="open = !open"
          >☰</button>
        </div>
      </div>

      <nav v-if="open" class="grid gap-1 pb-3 md:hidden">
        <NuxtLink v-for="l in links" :key="l.to" :to="localePath(l.to)" class="rounded px-2 py-2 text-sm text-slate-700 hover:bg-slate-50" @click="open = false">
          {{ t(l.key) }}
        </NuxtLink>
      </nav>
    </NContainer>
  </header>
</template>
```

- [ ] **Step 5: Write `SiteFooter.vue`**

```vue
<script setup lang="ts">
const { t } = useI18n()
const localePath = useLocalePath()
const year = new Date().getFullYear()
</script>

<template>
  <footer class="mt-20 border-t border-slate-200 bg-slate-50">
    <NContainer>
      <div class="grid gap-8 py-12 sm:grid-cols-2 lg:grid-cols-4">
        <div class="grid gap-3">
          <span class="font-display text-base font-bold">Nadoumi</span>
          <p class="text-sm text-slate-600">{{ t('footer.mission') }}</p>
          <NLocaleSwitcher />
        </div>
        <div class="grid content-start gap-2 text-sm">
          <p class="font-semibold text-slate-900">{{ t('footer.explore') }}</p>
          <NuxtLink :to="localePath('/scholarships')" class="text-slate-600 hover:text-slate-900">{{ t('nav.scholarships') }}</NuxtLink>
          <NuxtLink :to="localePath('/universities')" class="text-slate-600 hover:text-slate-900">{{ t('nav.universities') }}</NuxtLink>
          <NuxtLink :to="localePath('/programs')" class="text-slate-600 hover:text-slate-900">{{ t('nav.programs') }}</NuxtLink>
          <NuxtLink :to="localePath('/destinations')" class="text-slate-600 hover:text-slate-900">{{ t('nav.destinations') }}</NuxtLink>
        </div>
        <div class="grid content-start gap-2 text-sm">
          <p class="font-semibold text-slate-900">{{ t('footer.company') }}</p>
          <NuxtLink :to="localePath('/about')" class="text-slate-600 hover:text-slate-900">{{ t('nav.about') }}</NuxtLink>
          <NuxtLink :to="localePath('/contact')" class="text-slate-600 hover:text-slate-900">{{ t('nav.contact') }}</NuxtLink>
          <NuxtLink :to="localePath('/privacy')" class="text-slate-600 hover:text-slate-900">{{ t('footer.privacy') }}</NuxtLink>
          <NuxtLink :to="localePath('/terms')" class="text-slate-600 hover:text-slate-900">{{ t('footer.terms') }}</NuxtLink>
        </div>
        <div class="grid content-start gap-2 text-sm">
          <p class="font-semibold text-slate-900">{{ t('footer.getStarted') }}</p>
          <NuxtLink :to="localePath('/register')" class="text-slate-600 hover:text-slate-900">{{ t('nav.getStarted') }}</NuxtLink>
          <NuxtLink :to="localePath('/login')" class="text-slate-600 hover:text-slate-900">{{ t('nav.signIn') }}</NuxtLink>
          <a href="mailto:support@nadoumi.com" class="text-slate-600 hover:text-slate-900">{{ t('footer.contactEmail') }}</a>
        </div>
      </div>
      <div class="flex flex-col gap-2 border-t border-slate-200 py-6 text-xs text-slate-500 sm:flex-row sm:items-center sm:justify-between">
        <p>© {{ year }} {{ t('footer.rights') }}</p>
        <div class="flex gap-4">
          <NuxtLink :to="localePath('/privacy')" class="hover:text-slate-700">{{ t('footer.privacy') }}</NuxtLink>
          <NuxtLink :to="localePath('/terms')" class="hover:text-slate-700">{{ t('footer.terms') }}</NuxtLink>
        </div>
      </div>
    </NContainer>
  </footer>
</template>
```

- [ ] **Step 6: Rewrite `app/layouts/default.vue`**

```vue
<template>
  <div class="flex min-h-screen flex-col">
    <SiteHeader />
    <main class="flex-1 py-10">
      <NContainer>
        <slot />
      </NContainer>
    </main>
    <SiteFooter />
  </div>
</template>
```

- [ ] **Step 7: Delete the old shell components**

```bash
git rm nadoumi-web/app/components/AppHeader.vue nadoumi-web/app/components/AppFooter.vue
```

- [ ] **Step 8: Run the gate**

Run: `pnpm test -- marketing/SiteHeader && pnpm lint && pnpm build`
Expected: PASS; build fails only if `index.vue` etc. still reference `PageHero`/`ContentCard` — those are rebuilt in Task 20/21, so if the build breaks here, temporarily keep `PageHero.vue`/`ContentCard.vue` and defer their deletion to Task 21. (Note this in the commit.)

- [ ] **Step 9: Commit**

```bash
git add nadoumi-web
git commit -m "feat(web): SiteHeader, SiteFooter, default layout on the design system"
```

---

### Task 9: `useApi` problem-message helper + shared DTO types

**Files:**
- Modify: `nadoumi-web/app/composables/useApi.ts`
- Modify: `nadoumi-web/app/types/catalog.ts`
- Create: `nadoumi-web/tests/unit/useApi.test.ts`

**Interfaces:**
- Produces:
  - `problemMessage(err: unknown, fallback: string): string` exported from `useApi.ts` — returns `err.data.detail` when `err` is a `FetchError` carrying a `problem+json` body, else `err.data.title`, else `err.message`, else `fallback`.
  - `types/catalog.ts` gains: `ApplicantDto`, `EducationDto`, `TestScoreDto`, `ContactDto`, `SessionDto` (see shapes in the "Backend contract" section — dates are `string | null`).

- [ ] **Step 1: Write `tests/unit/useApi.test.ts`**

```ts
import { describe, it, expect } from 'vitest'
import { problemMessage } from '~/composables/useApi'

describe('problemMessage', () => {
  it('prefers problem.detail', () => {
    const err = { data: { detail: 'username must be at least 2', title: 'Bad Request' }, message: 'fail' }
    expect(problemMessage(err, 'x')).toBe('username must be at least 2')
  })

  it('falls back to title, then message, then the fallback', () => {
    expect(problemMessage({ data: { title: 'Conflict' }, message: 'm' }, 'x')).toBe('Conflict')
    expect(problemMessage({ message: 'boom' }, 'x')).toBe('boom')
    expect(problemMessage({}, 'the fallback')).toBe('the fallback')
  })
})
```

- [ ] **Step 2: Run test — verify it fails**

Run: `pnpm test -- useApi`
Expected: FAIL — `problemMessage` not exported.

- [ ] **Step 3: Extend `useApi.ts`**

Append (keep the existing `useApi` function and helpers):

```ts
interface ProblemLike { data?: { detail?: string; title?: string }; message?: string }

export function problemMessage(err: unknown, fallback: string): string {
  const e = err as ProblemLike
  return e?.data?.detail || e?.data?.title || e?.message || fallback
}
```

- [ ] **Step 4: Extend `types/catalog.ts`**

```ts
export interface SessionDto {
  authenticated: boolean
  user?: { userId: number; username: string; nickName: string | null }
  applicants?: { applicantId: number; accessRole: string; capabilities: string[] }[]
}

export interface ApplicantDto {
  id: number
  givenName: string
  familyName: string
  dob: string | null
  nationality: string | null
  passportNo: string | null
  email: string | null
  phone: string | null
  status: 'DRAFT' | 'ACTIVE' | 'UNLINKED' | 'ARCHIVED'
}

export interface EducationDto {
  id: number
  institution: string
  level: string | null
  field: string | null
  gpa: number | null
  gpaScale: number | null
  startDate: string | null
  endDate: string | null
}

export interface TestScoreDto {
  id: number
  testType: string
  score: string
  subScoresJson: string | null
  takenOn: string | null
  expiresOn: string | null
}

export interface ContactDto {
  id: number
  relation: 'GUARDIAN' | 'EMERGENCY' | 'OTHER'
  name: string
  email: string | null
  phone: string | null
}
```

- [ ] **Step 5: Run test — verify pass**

Run: `pnpm test -- useApi && pnpm lint`
Expected: PASS, lint clean.

- [ ] **Step 6: Commit**

```bash
git add nadoumi-web/app/composables/useApi.ts nadoumi-web/app/types/catalog.ts nadoumi-web/tests/unit/useApi.test.ts
git commit -m "feat(web): problemMessage helper + shared student DTO types"
```

---

# PART B — Auth & session

Produces: the BFF session/registration/captcha routes, the `useSession` composable + init plugin + route middleware, the `/login` `/register` `/forgot-password` pages, and the `dashboard` layout shell with working sign-out. After Part B a user can register, be auto-signed-in server-side, land on an (empty) dashboard, and sign out.

---

### Task 10: BFF routes — `student-session.get`, `student-account.post`, `public/captcha.get`

**Files:**
- Create: `nadoumi-web/server/api/student-session.get.ts`
- Create: `nadoumi-web/server/api/student-account.post.ts`
- Create: `nadoumi-web/server/api/public/captcha.get.ts`
- Create: `nadoumi-web/tests/unit/server/student-session-get.test.ts`
- Create: `nadoumi-web/tests/unit/server/student-account-post.test.ts`

**Interfaces:**
- Consumes: `server/utils/backend.ts` — `backendBaseUrl(event)`, `studentToken(event)`, `setStudentToken(event, token)`, `clearStudentToken(event)` (all already exist).
- Produces:
  - `GET /api/student-session` → `SessionDto`. No cookie → `{ authenticated: false }` with **no** upstream call. Cookie present → `GET {backend}/api/student/me` with the bearer; 2xx → `{ authenticated: true, user: { userId, username, nickName }, applicants: accessibleApplicants }`; 401 → `clearStudentToken` + `{ authenticated: false }`; other errors rethrow.
  - `POST /api/student-account` — body `{ fullName?, username, email?, password, code?, uuid? }`. Calls `POST {backend}/api/student/register` with `{ username, password, nickName: fullName, email, code, uuid }`. On success, calls `POST {backend}/api/student/login` with `{ username, password, code, uuid }`, `setStudentToken(event, res.token)`, returns `{ signedIn: true }`. On a register `FetchError`, `setResponseStatus(event, err.statusCode ?? 400)` and return `err.data ?? { detail: 'registration failed' }` — **no cookie set**. Never returns the token.
  - `GET /api/public/captcha` → passthrough of backend `GET /captchaImage` JSON (`{ captchaEnabled, uuid?, img? }`), no credentials.

- [ ] **Step 1: Write `tests/unit/server/student-session-get.test.ts`**

```ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import handler from '~/server/api/student-session.get'

const h3 = vi.hoisted(() => ({
  cookie: undefined as string | undefined,
  cleared: false,
  fetchImpl: vi.fn(),
}))

vi.mock('#imports', () => ({
  defineEventHandler: (fn: unknown) => fn,
  getCookie: () => h3.cookie,
  deleteCookie: () => { h3.cleared = true },
  setCookie: vi.fn(),
  useRuntimeConfig: () => ({ backendBaseUrl: 'http://backend' }),
}))
vi.stubGlobal('$fetch', h3.fetchImpl)

beforeEach(() => { h3.cookie = undefined; h3.cleared = false; h3.fetchImpl.mockReset() })

describe('GET /api/student-session', () => {
  it('returns authenticated:false and never calls the API without a cookie', async () => {
    const res = await handler({} as never)
    expect(res).toEqual({ authenticated: false })
    expect(h3.fetchImpl).not.toHaveBeenCalled()
  })

  it('maps /me into a SessionDto when the cookie is valid', async () => {
    h3.cookie = 'jwt'
    h3.fetchImpl.mockResolvedValueOnce({
      userId: 7, username: 'sam', nickName: 'Sam',
      accessibleApplicants: [{ applicantId: 3, accessRole: 'OWNER', capabilities: ['VIEW_PROFILE'] }],
    })
    const res = await handler({} as never)
    expect(res).toEqual({
      authenticated: true,
      user: { userId: 7, username: 'sam', nickName: 'Sam' },
      applicants: [{ applicantId: 3, accessRole: 'OWNER', capabilities: ['VIEW_PROFILE'] }],
    })
  })

  it('clears the cookie and returns guest on 401', async () => {
    h3.cookie = 'jwt'
    h3.fetchImpl.mockRejectedValueOnce(Object.assign(new Error('x'), { statusCode: 401 }))
    const res = await handler({} as never)
    expect(res).toEqual({ authenticated: false })
    expect(h3.cleared).toBe(true)
  })
})
```

> Note: server handlers auto-import `defineEventHandler`, `getCookie`, etc. from `#imports`. The mock above supplies them. If `@nuxt/test-utils` already provides a Nitro test harness in your Nuxt version, prefer `await import('~/server/...')` via `registerEndpoint` — but the plain unit mock is sufficient and fast.

- [ ] **Step 2: Write `tests/unit/server/student-account-post.test.ts`**

```ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import handler from '~/server/api/student-account.post'

const state = vi.hoisted(() => ({
  body: {} as Record<string, unknown>,
  token: undefined as string | undefined,
  status: 0,
  fetchImpl: vi.fn(),
}))

vi.mock('#imports', () => ({
  defineEventHandler: (fn: unknown) => fn,
  readBody: () => state.body,
  setCookie: (_e: unknown, _n: string, v: string) => { state.token = v },
  deleteCookie: vi.fn(),
  getCookie: vi.fn(),
  setResponseStatus: (_e: unknown, s: number) => { state.status = s },
  useRuntimeConfig: () => ({ backendBaseUrl: 'http://backend' }),
}))
vi.stubGlobal('$fetch', state.fetchImpl)

beforeEach(() => {
  state.body = { username: 'sam', password: 'secret1', fullName: 'Sam Lee', email: 's@x.io' }
  state.token = undefined; state.status = 0; state.fetchImpl.mockReset()
})

describe('POST /api/student-account', () => {
  it('registers then logs in server-side and sets the cookie; body carries no token', async () => {
    state.fetchImpl
      .mockResolvedValueOnce({ userId: 1, username: 'sam' })   // register
      .mockResolvedValueOnce({ token: 'JWT123' })              // login
    const res = await handler({} as never)
    expect(state.fetchImpl).toHaveBeenNthCalledWith(1, 'http://backend/api/student/register', expect.objectContaining({ method: 'POST' }))
    expect(state.fetchImpl).toHaveBeenNthCalledWith(2, 'http://backend/api/student/login', expect.objectContaining({ method: 'POST' }))
    expect(state.token).toBe('JWT123')
    expect(res).toEqual({ signedIn: true })
    expect(JSON.stringify(res)).not.toContain('JWT123')
  })

  it('passes the register problem+json + status through and sets no cookie', async () => {
    state.fetchImpl.mockRejectedValueOnce(Object.assign(new Error('dup'), {
      statusCode: 409, data: { title: 'Conflict', detail: 'username taken', status: 409 },
    }))
    const res = await handler({} as never)
    expect(state.status).toBe(409)
    expect(res).toEqual({ title: 'Conflict', detail: 'username taken', status: 409 })
    expect(state.token).toBeUndefined()
  })
})
```

- [ ] **Step 3: Run tests — verify they fail**

Run: `pnpm test -- server/`
Expected: FAIL — handlers not found.

- [ ] **Step 4: Write `server/api/student-session.get.ts`**

```ts
import type { SessionDto } from '~/types/catalog'

export default defineEventHandler(async (event): Promise<SessionDto> => {
  const token = studentToken(event)
  if (!token) return { authenticated: false }

  try {
    const me = await $fetch<{
      userId: number; username: string; nickName: string | null
      accessibleApplicants: { applicantId: number; accessRole: string; capabilities: string[] }[]
    }>(`${backendBaseUrl(event)}/api/student/me`, {
      headers: { authorization: `Bearer ${token}` },
    })
    return {
      authenticated: true,
      user: { userId: me.userId, username: me.username, nickName: me.nickName },
      applicants: me.accessibleApplicants,
    }
  }
  catch (err) {
    if ((err as { statusCode?: number }).statusCode === 401) {
      clearStudentToken(event)
      return { authenticated: false }
    }
    throw err
  }
})
```

- [ ] **Step 5: Write `server/api/student-account.post.ts`**

```ts
export default defineEventHandler(async (event) => {
  const body = await readBody<{
    fullName?: string; username: string; email?: string
    password: string; code?: string; uuid?: string
  }>(event)
  const base = backendBaseUrl(event)

  try {
    await $fetch(`${base}/api/student/register`, {
      method: 'POST',
      body: {
        username: body.username,
        password: body.password,
        nickName: body.fullName,
        email: body.email,
        code: body.code,
        uuid: body.uuid,
      },
    })
  }
  catch (err) {
    const e = err as { statusCode?: number; data?: unknown }
    setResponseStatus(event, e.statusCode ?? 400)
    return e.data ?? { detail: 'registration failed' }
  }

  const { token } = await $fetch<{ token: string }>(`${base}/api/student/login`, {
    method: 'POST',
    body: { username: body.username, password: body.password, code: body.code, uuid: body.uuid },
  })
  setStudentToken(event, token)
  return { signedIn: true }
})
```

- [ ] **Step 6: Write `server/api/public/captcha.get.ts`**

```ts
export default defineEventHandler(async (event) => {
  return $fetch(`${backendBaseUrl(event)}/captchaImage`)
})
```

- [ ] **Step 7: Run tests — verify pass**

Run: `pnpm test -- server/ && pnpm lint`
Expected: PASS (5), lint clean.

- [ ] **Step 8: Commit**

```bash
git add nadoumi-web/server nadoumi-web/tests/unit/server
git commit -m "feat(web): BFF routes for session read, server-side register+login, captcha"
```

---

### Task 11: `useSession` composable + init plugin + route middleware

**Files:**
- Replace: `nadoumi-web/app/composables/useSession.ts` (was the Task 8 stub)
- Create: `nadoumi-web/app/plugins/session.ts`
- Create: `nadoumi-web/app/middleware/auth.ts`
- Create: `nadoumi-web/app/middleware/guest.ts`
- Create: `nadoumi-web/tests/unit/useSession.test.ts`

**Interfaces:**
- Consumes: `GET /api/student-session` → `SessionDto`; `DELETE /api/student-session` (existing) → `{ signedIn: false }`.
- Produces: `useSession()` returning
  `{ status: Ref<'unknown' | 'guest' | 'authed'>, user: Ref<SessionDto['user'] | null>, applicants: Ref<NonNullable<SessionDto['applicants']>>, activeApplicantId: Ref<number | null>, refresh(): Promise<void>, signOut(): Promise<void>, setActiveApplicant(id: number): void }`.
  State is `useState` keyed `'nad-session'` (shared SSR→client). `refresh()` sets `status` to `authed`/`guest` and populates `user`/`applicants`; on `authed` with `activeApplicantId === null` it defaults to `applicants[0]?.applicantId ?? null`. `signOut()` calls the DELETE route, resets state to `guest`, `navigateTo(localePath('/'))`.
  `middleware/auth.ts`: if `status.value === 'unknown'` call `refresh()`; then if `!== 'authed'` return `navigateTo({ path: localePath('/login'), query: { redirect: to.fullPath } })`.
  `middleware/guest.ts`: mirror — if `authed`, `navigateTo(localePath('/dashboard'))`.
  `plugins/session.ts`: `await useSession().refresh()` (runs SSR + client via default plugin).

- [ ] **Step 1: Write `tests/unit/useSession.test.ts`**

```ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useSession } from '~/composables/useSession'

const nav = vi.fn()
vi.mock('#imports', async (orig) => {
  const actual = await (orig as () => Promise<Record<string, unknown>>)()
  return { ...actual, navigateTo: nav, useLocalePath: () => (p: string) => p }
})

const fetchImpl = vi.fn()
vi.stubGlobal('$fetch', fetchImpl)

beforeEach(() => {
  fetchImpl.mockReset(); nav.mockReset()
  // reset shared useState between tests
  useSession().status.value = 'unknown'
  useSession().user.value = null
  useSession().applicants.value = []
  useSession().activeApplicantId.value = null
})

describe('useSession', () => {
  it('refresh() → guest when unauthenticated', async () => {
    fetchImpl.mockResolvedValueOnce({ authenticated: false })
    await useSession().refresh()
    expect(useSession().status.value).toBe('guest')
  })

  it('refresh() → authed, populates user + default active applicant', async () => {
    fetchImpl.mockResolvedValueOnce({
      authenticated: true,
      user: { userId: 1, username: 'a', nickName: 'A' },
      applicants: [{ applicantId: 5, accessRole: 'OWNER', capabilities: [] }],
    })
    await useSession().refresh()
    expect(useSession().status.value).toBe('authed')
    expect(useSession().activeApplicantId.value).toBe(5)
  })

  it('signOut() resets to guest and navigates home', async () => {
    fetchImpl.mockResolvedValueOnce({ signedIn: false })
    await useSession().signOut()
    expect(useSession().status.value).toBe('guest')
    expect(nav).toHaveBeenCalledWith('/')
  })
})
```

- [ ] **Step 2: Run test — verify it fails**

Run: `pnpm test -- useSession`
Expected: FAIL — stub has no `refresh`/`signOut`.

- [ ] **Step 3: Write `app/composables/useSession.ts` (replace the stub)**

```ts
import type { SessionDto } from '~/types/catalog'

type Status = 'unknown' | 'guest' | 'authed'
type Applicant = NonNullable<SessionDto['applicants']>[number]

export function useSession() {
  const status = useState<Status>('nad-session-status', () => 'unknown')
  const user = useState<SessionDto['user'] | null>('nad-session-user', () => null)
  const applicants = useState<Applicant[]>('nad-session-applicants', () => [])
  const activeApplicantId = useState<number | null>('nad-session-active', () => null)
  const localePath = useLocalePath()

  async function refresh() {
    const s = await $fetch<SessionDto>('/api/student-session')
    if (s.authenticated) {
      status.value = 'authed'
      user.value = s.user ?? null
      applicants.value = s.applicants ?? []
      if (activeApplicantId.value === null) {
        activeApplicantId.value = applicants.value[0]?.applicantId ?? null
      }
    }
    else {
      status.value = 'guest'
      user.value = null
      applicants.value = []
      activeApplicantId.value = null
    }
  }

  async function signOut() {
    await $fetch('/api/student-session', { method: 'DELETE' }).catch(() => undefined)
    status.value = 'guest'
    user.value = null
    applicants.value = []
    activeApplicantId.value = null
    await navigateTo(localePath('/'))
  }

  function setActiveApplicant(id: number) {
    activeApplicantId.value = id
  }

  return { status, user, applicants, activeApplicantId, refresh, signOut, setActiveApplicant }
}
```

- [ ] **Step 4: Write `app/plugins/session.ts`**

```ts
export default defineNuxtPlugin(async () => {
  await useSession().refresh().catch(() => undefined)
})
```

- [ ] **Step 5: Write `app/middleware/auth.ts`**

```ts
export default defineNuxtRouteMiddleware(async (to) => {
  const { status, refresh } = useSession()
  const localePath = useLocalePath()
  if (status.value === 'unknown') await refresh().catch(() => undefined)
  if (status.value !== 'authed') {
    return navigateTo({ path: localePath('/login'), query: { redirect: to.fullPath } })
  }
})
```

- [ ] **Step 6: Write `app/middleware/guest.ts`**

```ts
export default defineNuxtRouteMiddleware(async () => {
  const { status, refresh } = useSession()
  const localePath = useLocalePath()
  if (status.value === 'unknown') await refresh().catch(() => undefined)
  if (status.value === 'authed') return navigateTo(localePath('/dashboard'))
})
```

- [ ] **Step 7: Run tests + build**

Run: `pnpm test && pnpm lint && pnpm build`
Expected: all unit tests pass; build OK. (`SiteHeader` now consumes the real `useSession`; `status` starts `unknown` → treated as not-authed in the template's `v-if="status === 'authed'"`, fine.)

- [ ] **Step 8: Commit**

```bash
git add nadoumi-web/app
git commit -m "feat(web): useSession composable, session init plugin, auth/guest middleware"
```

---

### Task 12: `AuthCaptcha` component + `/login` page

**Files:**
- Create: `nadoumi-web/app/components/auth/AuthCaptcha.vue`
- Create: `nadoumi-web/app/pages/login.vue`
- Create: `nadoumi-web/tests/unit/auth/AuthCaptcha.test.ts`

**Interfaces:**
- Consumes: `GET /api/public/captcha` → `{ captchaEnabled: boolean, uuid?: string, img?: string }`; `POST /api/student-session` (existing) → `{ signedIn: true }` or throws `problem+json`.
- Produces:
  - `<AuthCaptcha v-model:code="code" v-model:uuid="uuid" />` — on mount fetches the captcha; if `captchaEnabled === false` it renders nothing and emits `update:uuid` with `''`; else shows the image (`:src="data:image/jpeg;base64,${img}"`), an `NField` + `NInput` for the code, and a "reload" button that refetches. Exposes `enabled: Ref<boolean>` via `defineExpose` for the page to require the field.
  - `/login` page: `layout: default`, `middleware: 'guest'`. `NCard` with `NField`/`NInput` for username + password, `<AuthCaptcha>`, submit `NButton`. On submit → `POST /api/student-session` with `{ username, password, code, uuid }` → `useSession().refresh()` → `navigateTo(route.query.redirect as string || localePath('/dashboard'))`. Error → `NAlert` with `problemMessage(err, t('auth.genericError'))` and a captcha reload.

- [ ] **Step 1: Write `tests/unit/auth/AuthCaptcha.test.ts`**

```ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import AuthCaptcha from '~/components/auth/AuthCaptcha.vue'

const fetchImpl = vi.fn()
vi.stubGlobal('$fetch', fetchImpl)
beforeEach(() => fetchImpl.mockReset())

describe('AuthCaptcha', () => {
  it('renders nothing and clears uuid when captcha is disabled', async () => {
    fetchImpl.mockResolvedValueOnce({ captchaEnabled: false })
    const w = await mountSuspended(AuthCaptcha, { props: { code: '', uuid: '' } })
    await w.vm.$nextTick()
    expect(w.find('img').exists()).toBe(false)
    expect(w.emitted('update:uuid')?.at(-1)).toEqual([''])
  })

  it('shows the image and wires the uuid when enabled', async () => {
    fetchImpl.mockResolvedValueOnce({ captchaEnabled: true, uuid: 'u1', img: 'AAAA' })
    const w = await mountSuspended(AuthCaptcha, { props: { code: '', uuid: '' } })
    await w.vm.$nextTick()
    expect(w.find('img').attributes('src')).toBe('data:image/jpeg;base64,AAAA')
    expect(w.emitted('update:uuid')?.at(-1)).toEqual(['u1'])
  })
})
```

- [ ] **Step 2: Run test — verify it fails**

Run: `pnpm test -- auth/AuthCaptcha`
Expected: FAIL — component not found.

- [ ] **Step 3: Write `AuthCaptcha.vue`**

```vue
<script setup lang="ts">
defineProps<{ code: string; uuid: string }>()
const emit = defineEmits<{ 'update:code': [v: string]; 'update:uuid': [v: string] }>()
const { t } = useI18n()

const enabled = ref(false)
const img = ref('')

async function load() {
  const res = await $fetch<{ captchaEnabled: boolean; uuid?: string; img?: string }>('/api/public/captcha')
  enabled.value = res.captchaEnabled
  if (res.captchaEnabled) {
    img.value = res.img ?? ''
    emit('update:uuid', res.uuid ?? '')
  }
  else {
    emit('update:uuid', '')
  }
}
onMounted(load)
defineExpose({ enabled })
</script>

<template>
  <div v-if="enabled" class="grid gap-2">
    <div class="flex items-center gap-3">
      <img :src="`data:image/jpeg;base64,${img}`" alt="" class="h-10 rounded border border-slate-200">
      <button type="button" class="text-sm text-brand-700 hover:underline" @click="load">{{ t('auth.captchaReload') }}</button>
    </div>
    <NField :label="t('auth.captcha')" for="captcha">
      <NInput id="captcha" :model-value="code" autocomplete="off" @update:model-value="$emit('update:code', $event)" />
    </NField>
  </div>
</template>
```

- [ ] **Step 4: Write `app/pages/login.vue`**

```vue
<script setup lang="ts">
definePageMeta({ middleware: 'guest' })
const { t } = useI18n()
const route = useRoute()
const localePath = useLocalePath()
const { refresh } = useSession()

const form = reactive({ username: '', password: '', code: '', uuid: '' })
const error = ref('')
const busy = ref(false)

async function submit() {
  error.value = ''
  busy.value = true
  try {
    await $fetch('/api/student-session', { method: 'POST', body: { ...form } })
    await refresh()
    await navigateTo((route.query.redirect as string) || localePath('/dashboard'))
  }
  catch (err) {
    error.value = problemMessage(err, t('auth.genericError'))
  }
  finally {
    busy.value = false
  }
}

useSeo(t('auth.loginTitle'), t('auth.loginTitle'))
</script>

<template>
  <div class="mx-auto max-w-md">
    <h1 class="mb-6 font-display text-2xl font-bold">{{ t('auth.loginTitle') }}</h1>
    <NCard>
      <form class="grid gap-4" @submit.prevent="submit">
        <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
        <NField :label="t('auth.username')" for="username" required>
          <NInput id="username" v-model="form.username" autocomplete="username" />
        </NField>
        <NField :label="t('auth.password')" for="password" required>
          <NInput id="password" v-model="form.password" type="password" autocomplete="current-password" />
        </NField>
        <AuthCaptcha v-model:code="form.code" v-model:uuid="form.uuid" />
        <NButton type="submit" :loading="busy" block>{{ t('auth.submitLogin') }}</NButton>
      </form>
    </NCard>
    <div class="mt-4 flex justify-between text-sm">
      <NuxtLink :to="localePath('/register')" class="text-brand-700 hover:underline">{{ t('auth.toRegister') }}</NuxtLink>
      <NuxtLink :to="localePath('/forgot-password')" class="text-slate-500 hover:underline">{{ t('auth.forgot') }}</NuxtLink>
    </div>
  </div>
</template>
```

- [ ] **Step 5: Run test + build**

Run: `pnpm test -- auth/AuthCaptcha && pnpm lint && pnpm build`
Expected: PASS, lint clean, build OK.

- [ ] **Step 6: Commit**

```bash
git add nadoumi-web/app/components/auth nadoumi-web/app/pages/login.vue nadoumi-web/tests/unit/auth
git commit -m "feat(web): AuthCaptcha component + login page"
```

---

### Task 13: `/register` (server-side auto-login) + `/forgot-password`

**Files:**
- Create: `nadoumi-web/app/pages/register.vue`
- Create: `nadoumi-web/app/pages/forgot-password.vue`
- Create: `nadoumi-web/tests/unit/pages/register.test.ts`

**Interfaces:**
- Consumes: `POST /api/student-account` (Task 10) → `{ signedIn: true }` or a `problem+json` body with a non-2xx status; `useSession().refresh()`.
- Produces:
  - `/register` — `layout: default`, `middleware: 'guest'`. Fields: full name (`nickName`), username (hint `auth.usernameHint`), email, password, confirm password, `<AuthCaptcha>`, `<NCheckbox>` accept terms (links to `/terms` + `/privacy` via i18n interpolation). Client validation: all required except email optional; `password.length` 5–20; `username.length` 2–20; `password === confirm` else `auth.passwordMismatch`; terms checked. On submit → `POST /api/student-account` with `{ fullName, username, email, password, code, uuid }`. `$fetch` treats a non-2xx as a throw → show `problemMessage`. On `{ signedIn: true }` → `useSession().refresh()` → `navigateTo(localePath('/dashboard/profile'))`.
  - `/forgot-password` — static `NCard` rendering `auth.forgotTitle` + `auth.forgotBody`, a `mailto:` link, and a link back to `/login`. No form.

- [ ] **Step 1: Write `tests/unit/pages/register.test.ts`**

```ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import Register from '~/app/pages/register.vue'

const fetchImpl = vi.fn()
vi.stubGlobal('$fetch', fetchImpl)
const nav = vi.fn()
vi.mock('#app', async (o) => ({ ...(await (o as () => Promise<object>)()), navigateTo: nav }))

beforeEach(() => { fetchImpl.mockReset(); nav.mockReset() })

describe('register page', () => {
  it('blocks submit when passwords do not match', async () => {
    const w = await mountSuspended(Register)
    await w.find('#fullName').setValue('Sam Lee')
    await w.find('#username').setValue('sam')
    await w.find('#password').setValue('secret1')
    await w.find('#confirm').setValue('secret2')
    await w.find('#terms').setValue(true)
    await w.find('form').trigger('submit')
    expect(fetchImpl).not.toHaveBeenCalled()
    expect(w.text()).toContain('Passwords do not match')
  })

  it('posts once to /api/student-account and redirects on success', async () => {
    fetchImpl.mockResolvedValueOnce({ signedIn: true })   // account
    fetchImpl.mockResolvedValueOnce({ authenticated: true, user: { userId: 1, username: 'sam', nickName: 'Sam' }, applicants: [] }) // session refresh
    const w = await mountSuspended(Register)
    await w.find('#fullName').setValue('Sam Lee')
    await w.find('#username').setValue('sam')
    await w.find('#password').setValue('secret1')
    await w.find('#confirm').setValue('secret1')
    await w.find('#terms').setValue(true)
    await w.find('form').trigger('submit')
    await new Promise(r => setTimeout(r))
    const accountCall = fetchImpl.mock.calls.find(c => c[0] === '/api/student-account')
    expect(accountCall?.[1]).toMatchObject({ method: 'POST', body: expect.objectContaining({ username: 'sam' }) })
    expect(nav).toHaveBeenCalledWith('/dashboard/profile')
  })
})
```

> If `mountSuspended` cannot resolve `~/app/pages/...`, import from `~/pages/register.vue` (Nuxt 4 `app/` dir — the alias is `~/pages`). Adjust the import to whichever resolves; keep the assertions.

- [ ] **Step 2: Run test — verify it fails**

Run: `pnpm test -- pages/register`
Expected: FAIL — page not found.

- [ ] **Step 3: Write `app/pages/register.vue`**

```vue
<script setup lang="ts">
definePageMeta({ middleware: 'guest' })
const { t } = useI18n()
const localePath = useLocalePath()
const { refresh } = useSession()

const form = reactive({ fullName: '', username: '', email: '', password: '', confirm: '', code: '', uuid: '', terms: false })
const error = ref('')
const busy = ref(false)

function validate(): string | null {
  if (!form.fullName || !form.username || !form.password) return t('validation.required')
  if (form.username.length < 2 || form.username.length > 20) return t('auth.usernameHint')
  if (form.password.length < 5 || form.password.length > 20) return t('auth.passwordHint')
  if (form.password !== form.confirm) return t('auth.passwordMismatch')
  if (!form.terms) return t('validation.required')
  return null
}

async function submit() {
  error.value = ''
  const v = validate()
  if (v) { error.value = v; return }
  busy.value = true
  try {
    await $fetch('/api/student-account', {
      method: 'POST',
      body: {
        fullName: form.fullName, username: form.username, email: form.email || undefined,
        password: form.password, code: form.code, uuid: form.uuid,
      },
    })
    await refresh()
    await navigateTo(localePath('/dashboard/profile'))
  }
  catch (err) {
    error.value = problemMessage(err, t('auth.genericError'))
  }
  finally {
    busy.value = false
  }
}

useSeo(t('auth.registerTitle'), t('home.subtitle'))
</script>

<template>
  <div class="mx-auto max-w-md">
    <h1 class="mb-6 font-display text-2xl font-bold">{{ t('auth.registerTitle') }}</h1>
    <NCard>
      <form class="grid gap-4" @submit.prevent="submit">
        <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
        <NField :label="t('auth.fullName')" for="fullName" required>
          <NInput id="fullName" v-model="form.fullName" autocomplete="name" />
        </NField>
        <NField :label="t('auth.username')" for="username" :hint="t('auth.usernameHint')" required>
          <NInput id="username" v-model="form.username" autocomplete="username" maxlength="20" />
        </NField>
        <NField :label="t('auth.email')" for="email">
          <NInput id="email" v-model="form.email" type="email" autocomplete="email" />
        </NField>
        <NField :label="t('auth.password')" for="password" :hint="t('auth.passwordHint')" required>
          <NInput id="password" v-model="form.password" type="password" autocomplete="new-password" maxlength="20" />
        </NField>
        <NField :label="t('auth.confirmPassword')" for="confirm" required>
          <NInput id="confirm" v-model="form.confirm" type="password" autocomplete="new-password" maxlength="20" />
        </NField>
        <AuthCaptcha v-model:code="form.code" v-model:uuid="form.uuid" />
        <NCheckbox id="terms" v-model="form.terms">
          <i18n-t keypath="auth.acceptTerms">
            <template #terms><NuxtLink :to="localePath('/terms')" class="text-brand-700 hover:underline">{{ t('footer.terms') }}</NuxtLink></template>
            <template #privacy><NuxtLink :to="localePath('/privacy')" class="text-brand-700 hover:underline">{{ t('footer.privacy') }}</NuxtLink></template>
          </i18n-t>
        </NCheckbox>
        <NButton type="submit" :loading="busy" block>{{ t('auth.submitRegister') }}</NButton>
      </form>
    </NCard>
    <p class="mt-4 text-center text-sm">
      <NuxtLink :to="localePath('/login')" class="text-brand-700 hover:underline">{{ t('auth.toLogin') }}</NuxtLink>
    </p>
  </div>
</template>
```

- [ ] **Step 4: Write `app/pages/forgot-password.vue`**

```vue
<script setup lang="ts">
const { t } = useI18n()
const localePath = useLocalePath()
useSeo(t('auth.forgotTitle'), t('auth.forgotBody'))
</script>

<template>
  <div class="mx-auto max-w-md">
    <h1 class="mb-6 font-display text-2xl font-bold">{{ t('auth.forgotTitle') }}</h1>
    <NCard>
      <p class="text-sm text-slate-700">{{ t('auth.forgotBody') }}</p>
      <p class="mt-4">
        <a href="mailto:support@nadoumi.com" class="text-brand-700 hover:underline">support@nadoumi.com</a>
      </p>
    </NCard>
    <p class="mt-4 text-center text-sm">
      <NuxtLink :to="localePath('/login')" class="text-brand-700 hover:underline">{{ t('auth.toLogin') }}</NuxtLink>
    </p>
  </div>
</template>
```

- [ ] **Step 5: Run tests + build**

Run: `pnpm test && pnpm lint && pnpm build`
Expected: all pass.

- [ ] **Step 6: Commit**

```bash
git add nadoumi-web/app/pages/register.vue nadoumi-web/app/pages/forgot-password.vue nadoumi-web/tests/unit/pages
git commit -m "feat(web): register page (server-side auto-login) + forgot-password"
```

---

### Task 14: `dashboard` layout + `DashboardShell` + sign-out

**Files:**
- Create: `nadoumi-web/app/layouts/dashboard.vue`
- Create: `nadoumi-web/app/components/dashboard/DashboardShell.vue`
- Create: `nadoumi-web/app/components/dashboard/ApplicantSwitcher.vue`
- Create: `nadoumi-web/app/components/dashboard/SectionCard.vue`
- Create: `nadoumi-web/tests/unit/dashboard/DashboardShell.test.ts`

**Interfaces:**
- Consumes: `useSession()` (`user`, `applicants`, `activeApplicantId`, `setActiveApplicant`, `signOut`).
- Produces:
  - `layouts/dashboard.vue` — `<DashboardShell><slot/></DashboardShell>`.
  - `<DashboardShell>` — responsive: a sidebar with `dashboard.nav.*` links (`/dashboard`, `/dashboard/profile`, `/dashboard/education`, `/dashboard/test-scores`, `/dashboard/contacts`, `/dashboard/account`, each via `localePath`, `NuxtLink` `exact-active-class`), a topbar with the wordmark, `<ApplicantSwitcher>` (rendered only when `applicants.length > 1`), an `NDropdown` account menu with a "Sign out" `<button @click="signOut">`. Mobile: sidebar collapses behind a toggle.
  - `<ApplicantSwitcher>` — `NDropdown` listing `applicants` as "Applicant #{id} · {accessRole}", clicking calls `setActiveApplicant(id)`.
  - `<SectionCard>` — thin wrapper: `NCard` with a `#header` (`title` prop + optional `#actions` slot) and default slot; used by every dashboard screen.

- [ ] **Step 1: Write `tests/unit/dashboard/DashboardShell.test.ts`**

```ts
import { describe, it, expect, vi } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import DashboardShell from '~/components/dashboard/DashboardShell.vue'

const signOut = vi.fn()
vi.mock('~/composables/useSession', () => ({
  useSession: () => ({
    user: ref({ userId: 1, username: 'sam', nickName: 'Sam' }),
    applicants: ref([{ applicantId: 1, accessRole: 'OWNER', capabilities: [] }]),
    activeApplicantId: ref(1),
    setActiveApplicant: vi.fn(),
    signOut,
  }),
}))

describe('DashboardShell', () => {
  it('renders the nav and hides the applicant switcher for a single applicant', async () => {
    const w = await mountSuspended(DashboardShell, { slots: { default: () => 'body' } })
    expect(w.text()).toContain('Overview')
    expect(w.text()).toContain('Profile')
    expect(w.text()).not.toContain('Switch applicant')
  })

  it('calls signOut from the account menu', async () => {
    const w = await mountSuspended(DashboardShell, { slots: { default: () => 'body' } })
    await w.find('button[aria-haspopup="menu"]').trigger('click')
    await w.find('[data-test="sign-out"]').trigger('click')
    expect(signOut).toHaveBeenCalled()
  })
})
```

- [ ] **Step 2: Run test — verify it fails**

Run: `pnpm test -- dashboard/DashboardShell`
Expected: FAIL — component not found.

- [ ] **Step 3: Write `SectionCard.vue`**

```vue
<script setup lang="ts">
defineProps<{ title: string }>()
</script>

<template>
  <NCard>
    <template #header>
      <div class="flex items-center justify-between gap-3">
        <span>{{ title }}</span>
        <slot name="actions" />
      </div>
    </template>
    <slot />
  </NCard>
</template>
```

- [ ] **Step 4: Write `ApplicantSwitcher.vue`**

```vue
<script setup lang="ts">
const { t } = useI18n()
const { applicants, activeApplicantId, setActiveApplicant } = useSession()
</script>

<template>
  <NDropdown :label="`${t('dashboard.applicantLabel')} #${activeApplicantId ?? '—'}`">
    <button
      v-for="a in applicants"
      :key="a.applicantId"
      type="button"
      class="block w-full px-3 py-2 text-start text-sm hover:bg-slate-50"
      :class="a.applicantId === activeApplicantId ? 'font-semibold text-brand-700' : 'text-slate-700'"
      @click="setActiveApplicant(a.applicantId)"
    >
      {{ t('dashboard.applicantLabel') }} #{{ a.applicantId }} · {{ a.accessRole }}
    </button>
  </NDropdown>
</template>
```

- [ ] **Step 5: Write `DashboardShell.vue`**

```vue
<script setup lang="ts">
const { t } = useI18n()
const localePath = useLocalePath()
const { user, applicants, signOut } = useSession()
const open = ref(false)

const nav = [
  { to: '/dashboard', key: 'dashboard.nav.overview' },
  { to: '/dashboard/profile', key: 'dashboard.nav.profile' },
  { to: '/dashboard/education', key: 'dashboard.nav.education' },
  { to: '/dashboard/test-scores', key: 'dashboard.nav.testScores' },
  { to: '/dashboard/contacts', key: 'dashboard.nav.contacts' },
  { to: '/dashboard/account', key: 'dashboard.nav.account' },
]
</script>

<template>
  <div class="min-h-screen bg-slate-50">
    <header class="border-b border-slate-200 bg-white">
      <div class="mx-auto flex h-16 max-w-app items-center justify-between px-5">
        <div class="flex items-center gap-3">
          <button type="button" class="lg:hidden rounded p-2 hover:bg-slate-100" @click="open = !open">☰</button>
          <NuxtLink :to="localePath('/')" class="font-display text-lg font-bold">Nadoumi</NuxtLink>
        </div>
        <div class="flex items-center gap-3">
          <ApplicantSwitcher v-if="applicants.length > 1" />
          <NDropdown>
            <template #trigger>
              <NAvatar :name="user?.nickName ?? user?.username" />
            </template>
            <div class="px-3 py-2 text-xs text-slate-500">{{ user?.username }}</div>
            <NuxtLink :to="localePath('/dashboard/account')" class="block px-3 py-2 text-sm hover:bg-slate-50">{{ t('dashboard.nav.account') }}</NuxtLink>
            <button data-test="sign-out" type="button" class="block w-full px-3 py-2 text-start text-sm text-red-600 hover:bg-slate-50" @click="signOut">
              {{ t('common.signOut') }}
            </button>
          </NDropdown>
        </div>
      </div>
    </header>

    <div class="mx-auto flex max-w-app gap-6 px-5 py-8">
      <aside class="w-52 shrink-0" :class="open ? 'block' : 'hidden lg:block'">
        <nav class="grid gap-1">
          <NuxtLink
            v-for="n in nav"
            :key="n.to"
            :to="localePath(n.to)"
            exact-active-class="bg-brand-50 text-brand-700 font-semibold"
            class="rounded-md px-3 py-2 text-sm text-slate-700 hover:bg-slate-100"
            @click="open = false"
          >
            {{ t(n.key) }}
          </NuxtLink>
        </nav>
      </aside>
      <main class="min-w-0 flex-1"><slot /></main>
    </div>
  </div>
</template>
```

- [ ] **Step 6: Write `layouts/dashboard.vue`**

```vue
<template>
  <DashboardShell>
    <slot />
  </DashboardShell>
</template>
```

- [ ] **Step 7: Run tests + build**

Run: `pnpm test && pnpm lint && pnpm build`
Expected: all pass.

- [ ] **Step 8: Commit**

```bash
git add nadoumi-web/app/layouts/dashboard.vue nadoumi-web/app/components/dashboard nadoumi-web/tests/unit/dashboard
git commit -m "feat(web): dashboard layout, DashboardShell, ApplicantSwitcher, sign-out"
```

---

# PART C — Student dashboard screens

Produces: `useApplicant` data layer and the five authenticated screens (overview, profile with first-run create, education CRUD, test-scores add/delete, contacts add/delete, account). Every screen uses `layout: 'dashboard'` and `middleware: 'auth'`.

---

### Task 15: `useApplicant` data composable + `/dashboard` overview

**Files:**
- Create: `nadoumi-web/app/composables/useApplicant.ts`
- Create: `nadoumi-web/app/pages/dashboard/index.vue`
- Create: `nadoumi-web/tests/unit/useApplicant.test.ts`

**Interfaces:**
- Consumes: `studentFetch` from `useApi()` (existing — calls `/api/student/${path}` through the authenticated BFF passthrough `server/api/student/[...path].ts`); `useSession().activeApplicantId`.
- Produces: `useApplicant()` returning typed functions, each throwing the raw `FetchError` on failure:
  - `listMine(): Promise<ApplicantDto[]>` → `GET applicants`
  - `create(body: SelfApplicantBody): Promise<ApplicantDto>` → `POST applicants`
  - `get(id: number): Promise<ApplicantDto>` → `GET applicants/{id}`
  - `update(id: number, body: SelfApplicantBody): Promise<ApplicantDto>` → `PUT applicants/{id}`
  - `listEducation(id)`, `addEducation(id, body)`, `updateEducation(id, eduId, body)`, `deleteEducation(id, eduId)`
  - `listTestScores(id)`, `addTestScore(id, body)`, `deleteTestScore(id, scoreId)`
  - `listContacts(id)`, `addContact(id, body)`, `deleteContact(id, contactId)`
  - Body types exported: `SelfApplicantBody`, `EducationBody`, `TestScoreBody`, `ContactBody` (fields per the "Backend contract" DTOs; omit `undefined`/`''` before sending — the composable does this).
- `/dashboard` — `useSession().status` gate handled by middleware. Fetches `listMine()` via `useAsyncData`. If empty → CTA card linking to `/dashboard/profile` ("create your profile"). Else → welcome (`dashboard.welcome` with `user.nickName`), a simple completeness read-out (count of non-null profile fields / total), and quick-link buttons to `/dashboard/profile` and `/dashboard/education`.

- [ ] **Step 1: Write `tests/unit/useApplicant.test.ts`**

```ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useApplicant } from '~/composables/useApplicant'

const studentFetch = vi.fn()
vi.mock('~/composables/useApi', () => ({
  useApi: () => ({ studentFetch, publicGet: vi.fn() }),
  problemMessage: (_e: unknown, f: string) => f,
}))

beforeEach(() => studentFetch.mockReset())

describe('useApplicant', () => {
  it('GET applicants', async () => {
    studentFetch.mockResolvedValueOnce([{ id: 1 }])
    const rows = await useApplicant().listMine()
    expect(studentFetch).toHaveBeenCalledWith('applicants', undefined)
    expect(rows).toEqual([{ id: 1 }])
  })

  it('POST applicants strips empty strings from the body', async () => {
    studentFetch.mockResolvedValueOnce({ id: 2 })
    await useApplicant().create({ givenName: 'A', familyName: 'B', dob: '', nationality: '', passportNo: '', email: '', phone: '' })
    expect(studentFetch).toHaveBeenCalledWith('applicants', { method: 'POST', body: { givenName: 'A', familyName: 'B' } })
  })

  it('PUT applicants/{id}/education/{eduId}', async () => {
    studentFetch.mockResolvedValueOnce({ id: 9 })
    await useApplicant().updateEducation(3, 9, { institution: 'X' })
    expect(studentFetch).toHaveBeenCalledWith('applicants/3/education/9', { method: 'PUT', body: { institution: 'X' } })
  })

  it('DELETE applicants/{id}/contacts/{cid}', async () => {
    studentFetch.mockResolvedValueOnce(undefined)
    await useApplicant().deleteContact(3, 4)
    expect(studentFetch).toHaveBeenCalledWith('applicants/3/contacts/4', { method: 'DELETE' })
  })
})
```

- [ ] **Step 2: Run test — verify it fails**

Run: `pnpm test -- useApplicant`
Expected: FAIL — composable not found.

- [ ] **Step 3: Write `app/composables/useApplicant.ts`**

```ts
import type { ApplicantDto, ContactDto, EducationDto, TestScoreDto } from '~/types/catalog'

export interface SelfApplicantBody {
  givenName: string; familyName: string
  dob?: string; nationality?: string; passportNo?: string; email?: string; phone?: string
}
export interface EducationBody {
  institution: string; level?: string; field?: string
  gpa?: number; gpaScale?: number; startDate?: string; endDate?: string
}
export interface TestScoreBody {
  testType: string; score: string; subScoresJson?: string; takenOn?: string; expiresOn?: string
}
export interface ContactBody {
  relation: 'GUARDIAN' | 'EMERGENCY' | 'OTHER'; name: string; email?: string; phone?: string
}

function clean<T extends Record<string, unknown>>(body: T): Partial<T> {
  return Object.fromEntries(
    Object.entries(body).filter(([, v]) => v !== undefined && v !== '' && v !== null),
  ) as Partial<T>
}

export function useApplicant() {
  const { studentFetch } = useApi()
  const p = <T>(path: string, opts?: Parameters<typeof studentFetch>[1]) => studentFetch<T>(path, opts)

  return {
    listMine: () => p<ApplicantDto[]>('applicants', undefined),
    create: (b: SelfApplicantBody) => p<ApplicantDto>('applicants', { method: 'POST', body: clean(b) }),
    get: (id: number) => p<ApplicantDto>(`applicants/${id}`, undefined),
    update: (id: number, b: SelfApplicantBody) => p<ApplicantDto>(`applicants/${id}`, { method: 'PUT', body: clean(b) }),

    listEducation: (id: number) => p<EducationDto[]>(`applicants/${id}/education`, undefined),
    addEducation: (id: number, b: EducationBody) => p<EducationDto>(`applicants/${id}/education`, { method: 'POST', body: clean(b) }),
    updateEducation: (id: number, eduId: number, b: EducationBody) => p<EducationDto>(`applicants/${id}/education/${eduId}`, { method: 'PUT', body: clean(b) }),
    deleteEducation: (id: number, eduId: number) => p<void>(`applicants/${id}/education/${eduId}`, { method: 'DELETE' }),

    listTestScores: (id: number) => p<TestScoreDto[]>(`applicants/${id}/test-scores`, undefined),
    addTestScore: (id: number, b: TestScoreBody) => p<TestScoreDto>(`applicants/${id}/test-scores`, { method: 'POST', body: clean(b) }),
    deleteTestScore: (id: number, scoreId: number) => p<void>(`applicants/${id}/test-scores/${scoreId}`, { method: 'DELETE' }),

    listContacts: (id: number) => p<ContactDto[]>(`applicants/${id}/contacts`, undefined),
    addContact: (id: number, b: ContactBody) => p<ContactDto>(`applicants/${id}/contacts`, { method: 'POST', body: clean(b) }),
    deleteContact: (id: number, contactId: number) => p<void>(`applicants/${id}/contacts/${contactId}`, { method: 'DELETE' }),
  }
}
```

- [ ] **Step 4: Write `app/pages/dashboard/index.vue`**

```vue
<script setup lang="ts">
definePageMeta({ layout: 'dashboard', middleware: 'auth' })
const { t } = useI18n()
const localePath = useLocalePath()
const { user } = useSession()
const { listMine } = useApplicant()

const { data: applicants } = await useAsyncData('dash-applicants', () => listMine().catch(() => []))
const mine = computed(() => applicants.value ?? [])
const primary = computed(() => mine.value[0] ?? null)

function completeness(a: NonNullable<typeof primary.value>) {
  const fields = [a.givenName, a.familyName, a.dob, a.nationality, a.passportNo, a.email, a.phone]
  const filled = fields.filter(Boolean).length
  return Math.round((filled / fields.length) * 100)
}

useSeo(t('dashboard.nav.overview'), t('dashboard.overviewBlurb'))
</script>

<template>
  <div class="grid gap-6">
    <h1 class="font-display text-2xl font-bold">{{ t('dashboard.welcome', { name: user?.nickName ?? user?.username ?? '' }) }}</h1>

    <SectionCard v-if="!primary" :title="t('dashboard.createProfileTitle')">
      <p class="text-sm text-slate-600">{{ t('dashboard.createProfileBlurb') }}</p>
      <NButton class="mt-4" :to="localePath('/dashboard/profile')">{{ t('dashboard.quickProfile') }}</NButton>
    </SectionCard>

    <template v-else>
      <SectionCard :title="t('dashboard.completeness')">
        <div class="flex items-center gap-3">
          <div class="h-2 flex-1 rounded-full bg-slate-100">
            <div class="h-2 rounded-full bg-brand-500" :style="{ width: `${completeness(primary)}%` }" />
          </div>
          <span class="text-sm font-semibold">{{ completeness(primary) }}%</span>
        </div>
      </SectionCard>
      <div class="flex gap-3">
        <NButton :to="localePath('/dashboard/profile')">{{ t('dashboard.quickProfile') }}</NButton>
        <NButton variant="secondary" :to="localePath('/dashboard/education')">{{ t('dashboard.quickEducation') }}</NButton>
      </div>
    </template>
  </div>
</template>
```

- [ ] **Step 5: Run tests + build**

Run: `pnpm test -- useApplicant && pnpm lint && pnpm build`
Expected: PASS, lint clean, build OK.

- [ ] **Step 6: Commit**

```bash
git add nadoumi-web/app/composables/useApplicant.ts nadoumi-web/app/pages/dashboard/index.vue nadoumi-web/tests/unit/useApplicant.test.ts
git commit -m "feat(web): useApplicant data layer + dashboard overview"
```

---

### Task 16: `/dashboard/profile` + first-run create + `ProfileForm`

**Files:**
- Create: `nadoumi-web/app/components/dashboard/ProfileForm.vue`
- Create: `nadoumi-web/app/pages/dashboard/profile.vue`
- Create: `nadoumi-web/tests/unit/dashboard/ProfileForm.test.ts`

**Interfaces:**
- Consumes: `useApplicant()` (`listMine`, `get`, `create`, `update`), `useSession()` (`activeApplicantId`, `refresh` — call after a first-run `create` so the new applicant appears in the session).
- Produces:
  - `<ProfileForm :model-value="ApplicantDto | null" :busy="boolean" @submit="(body: SelfApplicantBody) => void">` — `NField`+`NInput` for givenName (req), familyName (req), dob (`type="date"`), nationality (`maxlength=2`, hint `validation.len2`), passportNo, email (`type="email"`), phone. Client validation: givenName & familyName required; nationality if present must be length 2. Emits `submit` with a `SelfApplicantBody` (raw strings; empty → omitted by `useApplicant`).
  - `/dashboard/profile` — `layout: 'dashboard'`, `middleware: 'auth'`. On load: `listMine()`; pick the applicant whose `id === activeApplicantId` else the first. If none → render `ProfileForm` with `null` and a "create" heading; on submit → `create(body)` → `useSession().refresh()` → toast `dashboard.savedOk`. If one exists → `get(id)` to fill, on submit → `update(id, body)` → toast. Errors → `NAlert` with `problemMessage`.

- [ ] **Step 1: Write `tests/unit/dashboard/ProfileForm.test.ts`**

```ts
import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import ProfileForm from '~/components/dashboard/ProfileForm.vue'

describe('ProfileForm', () => {
  it('requires given and family name before emitting submit', async () => {
    const w = await mountSuspended(ProfileForm, { props: { modelValue: null, busy: false } })
    await w.find('form').trigger('submit')
    expect(w.emitted('submit')).toBeUndefined()
    expect(w.text()).toContain('required')
  })

  it('emits a clean body on valid submit', async () => {
    const w = await mountSuspended(ProfileForm, { props: { modelValue: null, busy: false } })
    await w.find('#givenName').setValue('Ada')
    await w.find('#familyName').setValue('Byron')
    await w.find('#nationality').setValue('GB')
    await w.find('form').trigger('submit')
    expect(w.emitted('submit')?.[0]?.[0]).toMatchObject({ givenName: 'Ada', familyName: 'Byron', nationality: 'GB' })
  })

  it('prefills from modelValue', async () => {
    const w = await mountSuspended(ProfileForm, {
      props: { busy: false, modelValue: { id: 1, givenName: 'Sam', familyName: 'Lee', dob: '2000-01-01', nationality: 'MY', passportNo: 'X1', email: 's@x.io', phone: '123', status: 'ACTIVE' } },
    })
    expect((w.find('#givenName').element as HTMLInputElement).value).toBe('Sam')
    expect((w.find('#nationality').element as HTMLInputElement).value).toBe('MY')
  })
})
```

- [ ] **Step 2: Run test — verify it fails**

Run: `pnpm test -- dashboard/ProfileForm`
Expected: FAIL — component not found.

- [ ] **Step 3: Write `ProfileForm.vue`**

```vue
<script setup lang="ts">
import type { ApplicantDto } from '~/types/catalog'
import type { SelfApplicantBody } from '~/composables/useApplicant'

const props = defineProps<{ modelValue: ApplicantDto | null; busy: boolean }>()
const emit = defineEmits<{ submit: [body: SelfApplicantBody] }>()
const { t } = useI18n()

const form = reactive<SelfApplicantBody>({
  givenName: '', familyName: '', dob: '', nationality: '', passportNo: '', email: '', phone: '',
})
watchEffect(() => {
  const a = props.modelValue
  if (!a) return
  form.givenName = a.givenName ?? ''
  form.familyName = a.familyName ?? ''
  form.dob = a.dob ?? ''
  form.nationality = a.nationality ?? ''
  form.passportNo = a.passportNo ?? ''
  form.email = a.email ?? ''
  form.phone = a.phone ?? ''
})

const error = ref('')
function submit() {
  error.value = ''
  if (!form.givenName || !form.familyName) { error.value = t('validation.required'); return }
  if (form.nationality && form.nationality.length !== 2) { error.value = t('validation.len2'); return }
  emit('submit', { ...form })
}
</script>

<template>
  <form class="grid gap-4 sm:grid-cols-2" @submit.prevent="submit">
    <NAlert v-if="error" tone="danger" class="sm:col-span-2">{{ error }}</NAlert>
    <NField :label="t('dashboard.givenName')" for="givenName" required>
      <NInput id="givenName" v-model="form.givenName" maxlength="100" />
    </NField>
    <NField :label="t('dashboard.familyName')" for="familyName" required>
      <NInput id="familyName" v-model="form.familyName" maxlength="100" />
    </NField>
    <NField :label="t('dashboard.dob')" for="dob">
      <NInput id="dob" v-model="form.dob" type="date" />
    </NField>
    <NField :label="t('dashboard.nationality')" for="nationality" :hint="t('validation.len2')">
      <NInput id="nationality" v-model="form.nationality" maxlength="2" />
    </NField>
    <NField :label="t('dashboard.passportNo')" for="passportNo">
      <NInput id="passportNo" v-model="form.passportNo" maxlength="64" />
    </NField>
    <NField :label="t('auth.email')" for="pemail">
      <NInput id="pemail" v-model="form.email" type="email" maxlength="120" />
    </NField>
    <NField :label="t('dashboard.phone')" for="phone">
      <NInput id="phone" v-model="form.phone" maxlength="32" />
    </NField>
    <div class="sm:col-span-2">
      <NButton type="submit" :loading="busy">{{ t('common.save') }}</NButton>
    </div>
  </form>
</template>
```

> `ProfileForm.test.ts` references `#nationality` — the field id above is `nationality`; the email field uses `pemail` to avoid colliding with the login page's `email` id in any shared-DOM test run.

- [ ] **Step 4: Write `app/pages/dashboard/profile.vue`**

```vue
<script setup lang="ts">
import type { ApplicantDto } from '~/types/catalog'
import type { SelfApplicantBody } from '~/composables/useApplicant'
definePageMeta({ layout: 'dashboard', middleware: 'auth' })

const { t } = useI18n()
const { activeApplicantId, refresh } = useSession()
const { listMine, get, create, update } = useApplicant()

const current = ref<ApplicantDto | null>(null)
const busy = ref(false)
const notice = ref('')
const error = ref('')

async function load() {
  const mine = await listMine().catch(() => [])
  const chosen = mine.find(a => a.id === activeApplicantId.value) ?? mine[0] ?? null
  current.value = chosen ? await get(chosen.id) : null
}
await load()

async function onSubmit(body: SelfApplicantBody) {
  busy.value = true; error.value = ''; notice.value = ''
  try {
    if (current.value) {
      current.value = await update(current.value.id, body)
    }
    else {
      current.value = await create(body)
      await refresh()
    }
    notice.value = t('dashboard.savedOk')
  }
  catch (err) {
    error.value = problemMessage(err, t('auth.genericError'))
  }
  finally {
    busy.value = false
  }
}

useSeo(t('dashboard.profileTitle'), t('dashboard.createProfileBlurb'))
</script>

<template>
  <SectionCard :title="current ? t('dashboard.profileTitle') : t('dashboard.createProfileTitle')">
    <NAlert v-if="notice" tone="success" class="mb-4">{{ notice }}</NAlert>
    <NAlert v-if="error" tone="danger" class="mb-4">{{ error }}</NAlert>
    <p v-if="!current" class="mb-4 text-sm text-slate-600">{{ t('dashboard.createProfileBlurb') }}</p>
    <ProfileForm :model-value="current" :busy="busy" @submit="onSubmit" />
  </SectionCard>
</template>
```

- [ ] **Step 5: Run tests + build**

Run: `pnpm test && pnpm lint && pnpm build`
Expected: all pass.

- [ ] **Step 6: Commit**

```bash
git add nadoumi-web/app/components/dashboard/ProfileForm.vue nadoumi-web/app/pages/dashboard/profile.vue nadoumi-web/tests/unit/dashboard/ProfileForm.test.ts
git commit -m "feat(web): dashboard profile screen with first-run create"
```

---

### Task 17: `/dashboard/education` — full CRUD

**Files:**
- Create: `nadoumi-web/app/components/dashboard/EducationList.vue`
- Create: `nadoumi-web/app/pages/dashboard/education.vue`
- Create: `nadoumi-web/tests/unit/dashboard/EducationList.test.ts`

**Interfaces:**
- Consumes: `useApplicant()` (`listEducation`, `addEducation`, `updateEducation`, `deleteEducation`), `useSession().activeApplicantId`.
- Produces:
  - `<EducationList :items="EducationDto[]" :busy="boolean" @add="(b: EducationBody)=>void" @update="(id:number,b:EducationBody)=>void" @remove="(id:number)=>void">` — a table of existing rows (institution, level, field, gpa, dates) each with Edit + Remove. Edit swaps the row for an inline form (institution req). An "Add" `NButton` reveals a blank inline form. Remove opens `NModal` confirm (`dashboard.removeConfirm`).
  - `/dashboard/education` — `layout: 'dashboard'`, `middleware: 'auth'`. Guards on `activeApplicantId` (null → link to `/dashboard/profile`). Loads `listEducation(id)`; wires the three handlers, reloading the list and showing `dashboard.addedOk` / `dashboard.savedOk` / `dashboard.removedOk`.

- [ ] **Step 1: Write `tests/unit/dashboard/EducationList.test.ts`**

```ts
import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import EducationList from '~/components/dashboard/EducationList.vue'

const rows = [
  { id: 1, institution: 'MIT', level: 'BSc', field: 'CS', gpa: 3.9, gpaScale: 4, startDate: '2018-09-01', endDate: '2022-06-01' },
]

describe('EducationList', () => {
  it('renders existing rows', async () => {
    const w = await mountSuspended(EducationList, { props: { items: rows, busy: false } })
    expect(w.text()).toContain('MIT')
    expect(w.text()).toContain('CS')
  })

  it('emits add with the new-entry body', async () => {
    const w = await mountSuspended(EducationList, { props: { items: [], busy: false } })
    await w.find('[data-test="add"]').trigger('click')
    await w.find('#edu-institution').setValue('Oxford')
    await w.find('[data-test="save-new"]').trigger('click')
    expect(w.emitted('add')?.[0]?.[0]).toMatchObject({ institution: 'Oxford' })
  })

  it('emits remove after confirm', async () => {
    const w = await mountSuspended(EducationList, { props: { items: rows, busy: false } })
    await w.find('[data-test="remove-1"]').trigger('click')
    await w.find('[data-test="confirm-remove"]').trigger('click')
    expect(w.emitted('remove')?.[0]).toEqual([1])
  })
})
```

- [ ] **Step 2: Run test — verify it fails**

Run: `pnpm test -- dashboard/EducationList`
Expected: FAIL — component not found.

- [ ] **Step 3: Write `EducationList.vue`**

```vue
<script setup lang="ts">
import type { EducationDto } from '~/types/catalog'
import type { EducationBody } from '~/composables/useApplicant'

defineProps<{ items: EducationDto[]; busy: boolean }>()
const emit = defineEmits<{
  add: [body: EducationBody]
  update: [id: number, body: EducationBody]
  remove: [id: number]
}>()
const { t } = useI18n()

const adding = ref(false)
const editingId = ref<number | null>(null)
const removeId = ref<number | null>(null)
const blank = (): EducationBody => ({ institution: '', level: '', field: '', gpa: undefined, gpaScale: undefined, startDate: '', endDate: '' })
const draft = reactive<EducationBody>(blank())
const draftError = ref('')

function startAdd() { Object.assign(draft, blank()); adding.value = true; editingId.value = null; draftError.value = '' }
function startEdit(row: EducationDto) {
  Object.assign(draft, { institution: row.institution, level: row.level ?? '', field: row.field ?? '', gpa: row.gpa ?? undefined, gpaScale: row.gpaScale ?? undefined, startDate: row.startDate ?? '', endDate: row.endDate ?? '' })
  editingId.value = row.id; adding.value = false; draftError.value = ''
}
function saveNew() {
  if (!draft.institution) { draftError.value = t('validation.required'); return }
  emit('add', { ...draft }); adding.value = false
}
function saveEdit() {
  if (!draft.institution) { draftError.value = t('validation.required'); return }
  if (editingId.value != null) emit('update', editingId.value, { ...draft })
  editingId.value = null
}
</script>

<template>
  <div class="grid gap-4">
    <p v-if="!items.length && !adding" class="text-sm text-slate-500">{{ t('dashboard.eduEmpty') }}</p>

    <table v-if="items.length" class="w-full text-sm">
      <thead class="text-left text-slate-500">
        <tr><th class="py-2">{{ t('dashboard.institution') }}</th><th>{{ t('dashboard.level') }}</th><th>{{ t('dashboard.field') }}</th><th>{{ t('dashboard.gpa') }}</th><th></th></tr>
      </thead>
      <tbody>
        <template v-for="row in items" :key="row.id">
          <tr v-if="editingId !== row.id" class="border-t border-slate-100">
            <td class="py-2">{{ row.institution }}</td><td>{{ row.level }}</td><td>{{ row.field }}</td><td>{{ row.gpa }}</td>
            <td class="text-right">
              <button class="text-brand-700 hover:underline" @click="startEdit(row)">{{ t('common.edit') }}</button>
              <button :data-test="`remove-${row.id}`" class="ml-3 text-red-600 hover:underline" @click="removeId = row.id">{{ t('common.remove') }}</button>
            </td>
          </tr>
          <tr v-else class="border-t border-slate-100">
            <td colspan="5" class="py-3">
              <div class="grid gap-3 sm:grid-cols-2">
                <NAlert v-if="draftError" tone="danger" class="sm:col-span-2">{{ draftError }}</NAlert>
                <NField :label="t('dashboard.institution')" for="edu-institution-e" required><NInput id="edu-institution-e" v-model="draft.institution" /></NField>
                <NField :label="t('dashboard.level')" for="edu-level-e"><NInput id="edu-level-e" v-model="draft.level" /></NField>
                <NField :label="t('dashboard.field')" for="edu-field-e"><NInput id="edu-field-e" v-model="draft.field" /></NField>
                <NField :label="t('dashboard.startDate')" for="edu-start-e"><NInput id="edu-start-e" v-model="draft.startDate" type="date" /></NField>
                <NField :label="t('dashboard.endDate')" for="edu-end-e"><NInput id="edu-end-e" v-model="draft.endDate" type="date" /></NField>
              </div>
              <div class="mt-3 flex gap-2">
                <NButton size="sm" :loading="busy" @click="saveEdit">{{ t('common.save') }}</NButton>
                <NButton size="sm" variant="secondary" @click="editingId = null">{{ t('common.cancel') }}</NButton>
              </div>
            </td>
          </tr>
        </template>
      </tbody>
    </table>

    <div v-if="adding" class="rounded-md border border-slate-200 p-4">
      <div class="grid gap-3 sm:grid-cols-2">
        <NAlert v-if="draftError" tone="danger" class="sm:col-span-2">{{ draftError }}</NAlert>
        <NField :label="t('dashboard.institution')" for="edu-institution" required><NInput id="edu-institution" v-model="draft.institution" /></NField>
        <NField :label="t('dashboard.level')" for="edu-level"><NInput id="edu-level" v-model="draft.level" /></NField>
        <NField :label="t('dashboard.field')" for="edu-field"><NInput id="edu-field" v-model="draft.field" /></NField>
        <NField :label="t('dashboard.startDate')" for="edu-start"><NInput id="edu-start" v-model="draft.startDate" type="date" /></NField>
        <NField :label="t('dashboard.endDate')" for="edu-end"><NInput id="edu-end" v-model="draft.endDate" type="date" /></NField>
      </div>
      <div class="mt-3 flex gap-2">
        <NButton data-test="save-new" size="sm" :loading="busy" @click="saveNew">{{ t('common.add') }}</NButton>
        <NButton size="sm" variant="secondary" @click="adding = false">{{ t('common.cancel') }}</NButton>
      </div>
    </div>

    <div v-else>
      <NButton data-test="add" variant="secondary" size="sm" @click="startAdd">{{ t('common.add') }}</NButton>
    </div>

    <NModal :model-value="removeId !== null" :title="t('common.delete')" @update:model-value="removeId = null">
      <p class="text-sm">{{ t('dashboard.removeConfirm') }}</p>
      <template #footer>
        <NButton variant="secondary" size="sm" @click="removeId = null">{{ t('common.cancel') }}</NButton>
        <NButton data-test="confirm-remove" size="sm" @click="() => { if (removeId !== null) emit('remove', removeId); removeId = null }">{{ t('common.delete') }}</NButton>
      </template>
    </NModal>
  </div>
</template>
```

- [ ] **Step 4: Write `app/pages/dashboard/education.vue`**

```vue
<script setup lang="ts">
import type { EducationDto } from '~/types/catalog'
import type { EducationBody } from '~/composables/useApplicant'
definePageMeta({ layout: 'dashboard', middleware: 'auth' })

const { t } = useI18n()
const localePath = useLocalePath()
const { activeApplicantId } = useSession()
const { listEducation, addEducation, updateEducation, deleteEducation } = useApplicant()

const items = ref<EducationDto[]>([])
const busy = ref(false)
const notice = ref('')
const error = ref('')

async function reload() {
  if (activeApplicantId.value == null) return
  items.value = await listEducation(activeApplicantId.value).catch(() => [])
}
await reload()

async function run(fn: () => Promise<unknown>, ok: string) {
  if (activeApplicantId.value == null) return
  busy.value = true; error.value = ''; notice.value = ''
  try { await fn(); await reload(); notice.value = ok }
  catch (err) { error.value = problemMessage(err, t('auth.genericError')) }
  finally { busy.value = false }
}

const onAdd = (b: EducationBody) => run(() => addEducation(activeApplicantId.value!, b), t('dashboard.addedOk'))
const onUpdate = (id: number, b: EducationBody) => run(() => updateEducation(activeApplicantId.value!, id, b), t('dashboard.savedOk'))
const onRemove = (id: number) => run(() => deleteEducation(activeApplicantId.value!, id), t('dashboard.removedOk'))

useSeo(t('dashboard.eduTitle'), t('dashboard.eduTitle'))
</script>

<template>
  <SectionCard :title="t('dashboard.eduTitle')">
    <NAlert v-if="activeApplicantId == null" tone="warning">
      {{ t('dashboard.createProfileBlurb') }}
      <NuxtLink :to="localePath('/dashboard/profile')" class="ml-1 underline">{{ t('dashboard.quickProfile') }}</NuxtLink>
    </NAlert>
    <template v-else>
      <NAlert v-if="notice" tone="success" class="mb-4">{{ notice }}</NAlert>
      <NAlert v-if="error" tone="danger" class="mb-4">{{ error }}</NAlert>
      <EducationList :items="items" :busy="busy" @add="onAdd" @update="onUpdate" @remove="onRemove" />
    </template>
  </SectionCard>
</template>
```

- [ ] **Step 5: Run tests + build**

Run: `pnpm test && pnpm lint && pnpm build`
Expected: all pass.

- [ ] **Step 6: Commit**

```bash
git add nadoumi-web/app/components/dashboard/EducationList.vue nadoumi-web/app/pages/dashboard/education.vue nadoumi-web/tests/unit/dashboard/EducationList.test.ts
git commit -m "feat(web): dashboard education CRUD screen"
```

---

### Task 18: `/dashboard/test-scores` + `/dashboard/contacts` (add / delete only)

**Files:**
- Create: `nadoumi-web/app/components/dashboard/TestScoreList.vue`
- Create: `nadoumi-web/app/components/dashboard/ContactList.vue`
- Create: `nadoumi-web/app/pages/dashboard/test-scores.vue`
- Create: `nadoumi-web/app/pages/dashboard/contacts.vue`
- Create: `nadoumi-web/tests/unit/dashboard/ContactList.test.ts`

**Interfaces:**
- Consumes: `useApplicant()` (`listTestScores`/`addTestScore`/`deleteTestScore`, `listContacts`/`addContact`/`deleteContact`), `useSession().activeApplicantId`.
- Produces:
  - `<TestScoreList :items :busy @add(b: TestScoreBody) @remove(id)>` — table (testType, score, takenOn, expiresOn) + Remove per row; an inline add form (testType req, score req). **No edit** (backend has no PUT).
  - `<ContactList :items :busy @add(b: ContactBody) @remove(id)>` — table (relation, name, email, phone) + Remove; inline add form with `<NSelect>` for relation (`GUARDIAN`/`EMERGENCY`/`OTHER`, labels from `dashboard.relation*`), name req. **No edit.**
  - Both pages mirror Task 17's page shell (guard on `activeApplicantId`, `run()` helper, notices).

- [ ] **Step 1: Write `tests/unit/dashboard/ContactList.test.ts`**

```ts
import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import ContactList from '~/components/dashboard/ContactList.vue'

describe('ContactList', () => {
  it('renders rows and has no Edit control', async () => {
    const w = await mountSuspended(ContactList, {
      props: { items: [{ id: 1, relation: 'GUARDIAN', name: 'Pat Lee', email: 'p@x.io', phone: '1' }], busy: false },
    })
    expect(w.text()).toContain('Pat Lee')
    expect(w.text()).not.toContain('Edit')
  })

  it('emits add with relation + name', async () => {
    const w = await mountSuspended(ContactList, { props: { items: [], busy: false } })
    await w.find('[data-test="add"]').trigger('click')
    await w.find('#contact-name').setValue('Chris Doe')
    await w.find('#contact-relation').setValue('EMERGENCY')
    await w.find('[data-test="save-new"]').trigger('click')
    expect(w.emitted('add')?.[0]?.[0]).toEqual({ relation: 'EMERGENCY', name: 'Chris Doe', email: '', phone: '' })
  })

  it('emits remove after confirm', async () => {
    const w = await mountSuspended(ContactList, {
      props: { items: [{ id: 7, relation: 'OTHER', name: 'X', email: null, phone: null }], busy: false },
    })
    await w.find('[data-test="remove-7"]').trigger('click')
    await w.find('[data-test="confirm-remove"]').trigger('click')
    expect(w.emitted('remove')?.[0]).toEqual([7])
  })
})
```

- [ ] **Step 2: Run test — verify it fails**

Run: `pnpm test -- dashboard/ContactList`
Expected: FAIL — component not found.

- [ ] **Step 3: Write `TestScoreList.vue`**

```vue
<script setup lang="ts">
import type { TestScoreDto } from '~/types/catalog'
import type { TestScoreBody } from '~/composables/useApplicant'

defineProps<{ items: TestScoreDto[]; busy: boolean }>()
const emit = defineEmits<{ add: [body: TestScoreBody]; remove: [id: number] }>()
const { t } = useI18n()

const adding = ref(false)
const removeId = ref<number | null>(null)
const blank = (): TestScoreBody => ({ testType: '', score: '', takenOn: '', expiresOn: '' })
const draft = reactive<TestScoreBody>(blank())
const err = ref('')

function startAdd() { Object.assign(draft, blank()); err.value = ''; adding.value = true }
function saveNew() {
  if (!draft.testType || !draft.score) { err.value = t('validation.required'); return }
  emit('add', { ...draft }); adding.value = false
}
</script>

<template>
  <div class="grid gap-4">
    <p v-if="!items.length && !adding" class="text-sm text-slate-500">{{ t('dashboard.testEmpty') }}</p>
    <table v-if="items.length" class="w-full text-sm">
      <thead class="text-left text-slate-500"><tr><th class="py-2">{{ t('dashboard.testType') }}</th><th>{{ t('dashboard.score') }}</th><th>{{ t('dashboard.takenOn') }}</th><th>{{ t('dashboard.expiresOn') }}</th><th></th></tr></thead>
      <tbody>
        <tr v-for="row in items" :key="row.id" class="border-t border-slate-100">
          <td class="py-2">{{ row.testType }}</td><td>{{ row.score }}</td><td>{{ row.takenOn }}</td><td>{{ row.expiresOn }}</td>
          <td class="text-right"><button :data-test="`remove-${row.id}`" class="text-red-600 hover:underline" @click="removeId = row.id">{{ t('common.remove') }}</button></td>
        </tr>
      </tbody>
    </table>

    <div v-if="adding" class="rounded-md border border-slate-200 p-4">
      <div class="grid gap-3 sm:grid-cols-2">
        <NAlert v-if="err" tone="danger" class="sm:col-span-2">{{ err }}</NAlert>
        <NField :label="t('dashboard.testType')" for="ts-type" required><NInput id="ts-type" v-model="draft.testType" maxlength="24" /></NField>
        <NField :label="t('dashboard.score')" for="ts-score" required><NInput id="ts-score" v-model="draft.score" maxlength="32" /></NField>
        <NField :label="t('dashboard.takenOn')" for="ts-taken"><NInput id="ts-taken" v-model="draft.takenOn" type="date" /></NField>
        <NField :label="t('dashboard.expiresOn')" for="ts-exp"><NInput id="ts-exp" v-model="draft.expiresOn" type="date" /></NField>
      </div>
      <div class="mt-3 flex gap-2">
        <NButton data-test="save-new" size="sm" :loading="busy" @click="saveNew">{{ t('common.add') }}</NButton>
        <NButton size="sm" variant="secondary" @click="adding = false">{{ t('common.cancel') }}</NButton>
      </div>
    </div>
    <div v-else><NButton data-test="add" variant="secondary" size="sm" @click="startAdd">{{ t('common.add') }}</NButton></div>

    <NModal :model-value="removeId !== null" :title="t('common.delete')" @update:model-value="removeId = null">
      <p class="text-sm">{{ t('dashboard.removeConfirm') }}</p>
      <template #footer>
        <NButton variant="secondary" size="sm" @click="removeId = null">{{ t('common.cancel') }}</NButton>
        <NButton data-test="confirm-remove" size="sm" @click="() => { if (removeId !== null) emit('remove', removeId); removeId = null }">{{ t('common.delete') }}</NButton>
      </template>
    </NModal>
  </div>
</template>
```

- [ ] **Step 4: Write `ContactList.vue`**

```vue
<script setup lang="ts">
import type { ContactDto } from '~/types/catalog'
import type { ContactBody } from '~/composables/useApplicant'

defineProps<{ items: ContactDto[]; busy: boolean }>()
const emit = defineEmits<{ add: [body: ContactBody]; remove: [id: number] }>()
const { t } = useI18n()

const relations = computed(() => [
  { value: 'GUARDIAN', label: t('dashboard.relationGUARDIAN') },
  { value: 'EMERGENCY', label: t('dashboard.relationEMERGENCY') },
  { value: 'OTHER', label: t('dashboard.relationOTHER') },
])

const adding = ref(false)
const removeId = ref<number | null>(null)
const blank = (): ContactBody => ({ relation: 'GUARDIAN', name: '', email: '', phone: '' })
const draft = reactive<ContactBody>(blank())
const err = ref('')

function startAdd() { Object.assign(draft, blank()); err.value = ''; adding.value = true }
function saveNew() {
  if (!draft.name) { err.value = t('validation.required'); return }
  emit('add', { ...draft }); adding.value = false
}
</script>

<template>
  <div class="grid gap-4">
    <p v-if="!items.length && !adding" class="text-sm text-slate-500">{{ t('dashboard.contactsEmpty') }}</p>
    <table v-if="items.length" class="w-full text-sm">
      <thead class="text-left text-slate-500"><tr><th class="py-2">{{ t('dashboard.relation') }}</th><th>{{ t('dashboard.contactName') }}</th><th>{{ t('auth.email') }}</th><th>{{ t('dashboard.phone') }}</th><th></th></tr></thead>
      <tbody>
        <tr v-for="row in items" :key="row.id" class="border-t border-slate-100">
          <td class="py-2">{{ t(`dashboard.relation${row.relation}`) }}</td><td>{{ row.name }}</td><td>{{ row.email }}</td><td>{{ row.phone }}</td>
          <td class="text-right"><button :data-test="`remove-${row.id}`" class="text-red-600 hover:underline" @click="removeId = row.id">{{ t('common.remove') }}</button></td>
        </tr>
      </tbody>
    </table>

    <div v-if="adding" class="rounded-md border border-slate-200 p-4">
      <div class="grid gap-3 sm:grid-cols-2">
        <NAlert v-if="err" tone="danger" class="sm:col-span-2">{{ err }}</NAlert>
        <NField :label="t('dashboard.relation')" for="contact-relation">
          <NSelect id="contact-relation" v-model="draft.relation" :options="relations" />
        </NField>
        <NField :label="t('dashboard.contactName')" for="contact-name" required><NInput id="contact-name" v-model="draft.name" maxlength="150" /></NField>
        <NField :label="t('auth.email')" for="contact-email"><NInput id="contact-email" v-model="draft.email" type="email" maxlength="120" /></NField>
        <NField :label="t('dashboard.phone')" for="contact-phone"><NInput id="contact-phone" v-model="draft.phone" maxlength="32" /></NField>
      </div>
      <div class="mt-3 flex gap-2">
        <NButton data-test="save-new" size="sm" :loading="busy" @click="saveNew">{{ t('common.add') }}</NButton>
        <NButton size="sm" variant="secondary" @click="adding = false">{{ t('common.cancel') }}</NButton>
      </div>
    </div>
    <div v-else><NButton data-test="add" variant="secondary" size="sm" @click="startAdd">{{ t('common.add') }}</NButton></div>

    <NModal :model-value="removeId !== null" :title="t('common.delete')" @update:model-value="removeId = null">
      <p class="text-sm">{{ t('dashboard.removeConfirm') }}</p>
      <template #footer>
        <NButton variant="secondary" size="sm" @click="removeId = null">{{ t('common.cancel') }}</NButton>
        <NButton data-test="confirm-remove" size="sm" @click="() => { if (removeId !== null) emit('remove', removeId); removeId = null }">{{ t('common.delete') }}</NButton>
      </template>
    </NModal>
  </div>
</template>
```

- [ ] **Step 5: Write `app/pages/dashboard/test-scores.vue`**

```vue
<script setup lang="ts">
import type { TestScoreDto } from '~/types/catalog'
import type { TestScoreBody } from '~/composables/useApplicant'
definePageMeta({ layout: 'dashboard', middleware: 'auth' })

const { t } = useI18n()
const localePath = useLocalePath()
const { activeApplicantId } = useSession()
const { listTestScores, addTestScore, deleteTestScore } = useApplicant()

const items = ref<TestScoreDto[]>([])
const busy = ref(false); const notice = ref(''); const error = ref('')

async function reload() {
  if (activeApplicantId.value == null) return
  items.value = await listTestScores(activeApplicantId.value).catch(() => [])
}
await reload()

async function run(fn: () => Promise<unknown>, ok: string) {
  busy.value = true; error.value = ''; notice.value = ''
  try { await fn(); await reload(); notice.value = ok }
  catch (err) { error.value = problemMessage(err, t('auth.genericError')) }
  finally { busy.value = false }
}
const onAdd = (b: TestScoreBody) => run(() => addTestScore(activeApplicantId.value!, b), t('dashboard.addedOk'))
const onRemove = (id: number) => run(() => deleteTestScore(activeApplicantId.value!, id), t('dashboard.removedOk'))

useSeo(t('dashboard.testTitle'), t('dashboard.testTitle'))
</script>

<template>
  <SectionCard :title="t('dashboard.testTitle')">
    <NAlert v-if="activeApplicantId == null" tone="warning">
      {{ t('dashboard.createProfileBlurb') }}
      <NuxtLink :to="localePath('/dashboard/profile')" class="ml-1 underline">{{ t('dashboard.quickProfile') }}</NuxtLink>
    </NAlert>
    <template v-else>
      <NAlert v-if="notice" tone="success" class="mb-4">{{ notice }}</NAlert>
      <NAlert v-if="error" tone="danger" class="mb-4">{{ error }}</NAlert>
      <TestScoreList :items="items" :busy="busy" @add="onAdd" @remove="onRemove" />
    </template>
  </SectionCard>
</template>
```

- [ ] **Step 6: Write `app/pages/dashboard/contacts.vue`**

Same shell as Step 5, swapping the composable calls and component:

```vue
<script setup lang="ts">
import type { ContactDto } from '~/types/catalog'
import type { ContactBody } from '~/composables/useApplicant'
definePageMeta({ layout: 'dashboard', middleware: 'auth' })

const { t } = useI18n()
const localePath = useLocalePath()
const { activeApplicantId } = useSession()
const { listContacts, addContact, deleteContact } = useApplicant()

const items = ref<ContactDto[]>([])
const busy = ref(false); const notice = ref(''); const error = ref('')

async function reload() {
  if (activeApplicantId.value == null) return
  items.value = await listContacts(activeApplicantId.value).catch(() => [])
}
await reload()

async function run(fn: () => Promise<unknown>, ok: string) {
  busy.value = true; error.value = ''; notice.value = ''
  try { await fn(); await reload(); notice.value = ok }
  catch (err) { error.value = problemMessage(err, t('auth.genericError')) }
  finally { busy.value = false }
}
const onAdd = (b: ContactBody) => run(() => addContact(activeApplicantId.value!, b), t('dashboard.addedOk'))
const onRemove = (id: number) => run(() => deleteContact(activeApplicantId.value!, id), t('dashboard.removedOk'))

useSeo(t('dashboard.contactsTitle'), t('dashboard.contactsTitle'))
</script>

<template>
  <SectionCard :title="t('dashboard.contactsTitle')">
    <NAlert v-if="activeApplicantId == null" tone="warning">
      {{ t('dashboard.createProfileBlurb') }}
      <NuxtLink :to="localePath('/dashboard/profile')" class="ml-1 underline">{{ t('dashboard.quickProfile') }}</NuxtLink>
    </NAlert>
    <template v-else>
      <NAlert v-if="notice" tone="success" class="mb-4">{{ notice }}</NAlert>
      <NAlert v-if="error" tone="danger" class="mb-4">{{ error }}</NAlert>
      <ContactList :items="items" :busy="busy" @add="onAdd" @remove="onRemove" />
    </template>
  </SectionCard>
</template>
```

- [ ] **Step 7: Run tests + build**

Run: `pnpm test && pnpm lint && pnpm build`
Expected: all pass.

- [ ] **Step 8: Commit**

```bash
git add nadoumi-web/app/components/dashboard/TestScoreList.vue nadoumi-web/app/components/dashboard/ContactList.vue nadoumi-web/app/pages/dashboard/test-scores.vue nadoumi-web/app/pages/dashboard/contacts.vue nadoumi-web/tests/unit/dashboard/ContactList.test.ts
git commit -m "feat(web): dashboard test-scores + contacts screens (add/delete)"
```

---

### Task 19: `/dashboard/account`

> **SUPERSEDED by Revision 2 → Task F-6.** Do **not** build the "link to support"
> version below. `/dashboard/account` is a real change-password form
> (current / new / confirm) calling `POST /api/student-password`, with session
> revocation. The task body below is kept for history only. Skip to Task F-6.

**Files:**
- Create: `nadoumi-web/app/pages/dashboard/account.vue`
- Create: `nadoumi-web/tests/unit/pages/account.test.ts`

**Interfaces:**
- Consumes: `useSession()` (`user`, `signOut`).
- Produces: `/dashboard/account` — `layout: 'dashboard'`, `middleware: 'auth'`. Shows `dashboard.accountUser` + `user.username`; a "Password changes" `SectionCard` rendering `dashboard.passwordChangeBody` + a `mailto:` link (no self-service — spec §14); a "Sign out" `NButton` calling `signOut()`.

- [ ] **Step 1: Write `tests/unit/pages/account.test.ts`**

```ts
import { describe, it, expect, vi } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import Account from '~/pages/dashboard/account.vue'

const signOut = vi.fn()
vi.mock('~/composables/useSession', () => ({
  useSession: () => ({ user: ref({ userId: 1, username: 'sam', nickName: 'Sam' }), signOut }),
}))

describe('account page', () => {
  it('shows the username and a working sign-out button', async () => {
    const w = await mountSuspended(Account)
    expect(w.text()).toContain('sam')
    await w.find('[data-test="sign-out"]').trigger('click')
    expect(signOut).toHaveBeenCalled()
  })
})
```

- [ ] **Step 2: Run test — verify it fails**

Run: `pnpm test -- pages/account`
Expected: FAIL — page not found.

- [ ] **Step 3: Write `app/pages/dashboard/account.vue`**

```vue
<script setup lang="ts">
definePageMeta({ layout: 'dashboard', middleware: 'auth' })
const { t } = useI18n()
const { user, signOut } = useSession()
useSeo(t('dashboard.accountTitle'), t('dashboard.accountTitle'))
</script>

<template>
  <div class="grid gap-6">
    <SectionCard :title="t('dashboard.accountTitle')">
      <p class="text-sm text-slate-600">{{ t('dashboard.accountUser') }}</p>
      <p class="font-medium">{{ user?.username }}</p>
    </SectionCard>

    <SectionCard :title="t('dashboard.passwordChange')">
      <p class="text-sm text-slate-600">{{ t('dashboard.passwordChangeBody') }}</p>
      <p class="mt-2"><a href="mailto:support@nadoumi.com" class="text-brand-700 hover:underline">support@nadoumi.com</a></p>
    </SectionCard>

    <div>
      <NButton data-test="sign-out" variant="secondary" @click="signOut">{{ t('common.signOut') }}</NButton>
    </div>
  </div>
</template>
```

- [ ] **Step 4: Run tests + build**

Run: `pnpm test && pnpm lint && pnpm build`
Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git add nadoumi-web/app/pages/dashboard/account.vue nadoumi-web/tests/unit/pages/account.test.ts
git commit -m "feat(web): dashboard account screen"
```

---

# PART D — Marketing pages, catalog empty-states, SEO, E2E, CI, docs

Produces: the polished marketing surface, catalog list/detail pages with designed empty-states, `robots.txt` + `sitemap.xml`, the Playwright E2E gate, CI wiring, and the docs/memory update. After Part D the build is complete.

---

### Task 20: Marketing block components

**Files:**
- Create: `nadoumi-web/app/components/marketing/Hero.vue`
- Create: `nadoumi-web/app/components/marketing/FeatureGrid.vue`
- Create: `nadoumi-web/app/components/marketing/StatBand.vue`
- Create: `nadoumi-web/app/components/marketing/CTASection.vue`
- Create: `nadoumi-web/app/components/marketing/DestinationCard.vue`
- Create: `nadoumi-web/app/components/marketing/Testimonial.vue`
- Create: `nadoumi-web/app/components/marketing/FaqAccordion.vue`
- Create: `nadoumi-web/app/components/marketing/EmptyState.vue`
- Create: `nadoumi-web/tests/unit/marketing/FaqAccordion.test.ts`

**Interfaces:**
- Produces:
  - `<Hero>` props `title`, `subtitle?`, `#actions` slot. Full-width band (`bg-slate-50`), big `font-display` heading, constrained subtitle, actions row.
  - `<FeatureGrid :items="{ icon?: string; title: string; body: string }[]">` — responsive 3-col grid of `NCard`s.
  - `<StatBand :text="string">` — a single centered line on `bg-brand-50`, brand-toned text. (No fake numbers — spec §7.1 "placeholder-safe copy".)
  - `<CTASection title body #actions>` — centered card with `bg-brand-600 text-white` and an actions slot.
  - `<DestinationCard title body :to?>` — `NCard` variant; whole card is a link when `to`.
  - `<Testimonial quote author>` — blockquote styling.
  - `<FaqAccordion :items="{ q: string; a: string }[]">` — each item a `<button aria-expanded>` toggling an answer panel; only markup + local open-state.
  - `<EmptyState title body #actions?>` — centered muted block with an optional actions slot; used by catalog pages.

- [ ] **Step 1: Write `tests/unit/marketing/FaqAccordion.test.ts`**

```ts
import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import FaqAccordion from '~/components/marketing/FaqAccordion.vue'

describe('FaqAccordion', () => {
  it('toggles an answer and sets aria-expanded', async () => {
    const w = await mountSuspended(FaqAccordion, { props: { items: [{ q: 'How much?', a: 'Free to start.' }] } })
    const btn = w.find('button')
    expect(btn.attributes('aria-expanded')).toBe('false')
    expect(w.text()).not.toContain('Free to start.')
    await btn.trigger('click')
    expect(btn.attributes('aria-expanded')).toBe('true')
    expect(w.text()).toContain('Free to start.')
  })
})
```

- [ ] **Step 2: Run test — verify it fails**

Run: `pnpm test -- marketing/FaqAccordion`
Expected: FAIL — component not found.

- [ ] **Step 3: Write the eight components**

`Hero.vue`:

```vue
<script setup lang="ts">
defineProps<{ title: string; subtitle?: string }>()
</script>

<template>
  <section class="-mx-5 mb-12 bg-slate-50 px-5 py-16">
    <div class="mx-auto max-w-marketing">
      <h1 class="font-display text-4xl font-bold text-slate-900 sm:text-5xl">{{ title }}</h1>
      <p v-if="subtitle" class="mt-4 max-w-2xl text-lg text-slate-600">{{ subtitle }}</p>
      <div v-if="$slots.actions" class="mt-8 flex flex-wrap gap-3"><slot name="actions" /></div>
    </div>
  </section>
</template>
```

`FeatureGrid.vue`:

```vue
<script setup lang="ts">
defineProps<{ items: { title: string; body: string }[] }>()
</script>

<template>
  <div class="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
    <NCard v-for="it in items" :key="it.title">
      <h3 class="font-display text-lg font-semibold">{{ it.title }}</h3>
      <p class="mt-2 text-sm text-slate-600">{{ it.body }}</p>
    </NCard>
  </div>
</template>
```

`StatBand.vue`:

```vue
<script setup lang="ts">
defineProps<{ text: string }>()
</script>

<template>
  <section class="-mx-5 my-12 bg-brand-50 px-5 py-10 text-center">
    <p class="mx-auto max-w-2xl font-display text-lg font-semibold text-brand-800">{{ text }}</p>
  </section>
</template>
```

`CTASection.vue`:

```vue
<script setup lang="ts">
defineProps<{ title: string; body: string }>()
</script>

<template>
  <section class="my-12 rounded-lg bg-brand-600 px-8 py-12 text-center text-white">
    <h2 class="font-display text-2xl font-bold">{{ title }}</h2>
    <p class="mx-auto mt-2 max-w-xl text-white/90">{{ body }}</p>
    <div v-if="$slots.actions" class="mt-6 flex justify-center gap-3"><slot name="actions" /></div>
  </section>
</template>
```

`DestinationCard.vue`:

```vue
<script setup lang="ts">
defineProps<{ title: string; body: string; to?: string }>()
</script>

<template>
  <component :is="to ? 'NuxtLink' : 'div'" :to="to" class="block rounded-lg border border-slate-200 bg-white p-6 shadow-xs hover:border-brand-300">
    <h3 class="font-display text-lg font-semibold">{{ title }}</h3>
    <p class="mt-2 text-sm text-slate-600">{{ body }}</p>
  </component>
</template>
```

`Testimonial.vue`:

```vue
<script setup lang="ts">
defineProps<{ quote: string; author: string }>()
</script>

<template>
  <figure class="rounded-lg border border-slate-200 bg-white p-6">
    <blockquote class="text-slate-800">“{{ quote }}”</blockquote>
    <figcaption class="mt-3 text-sm text-slate-500">— {{ author }}</figcaption>
  </figure>
</template>
```

`FaqAccordion.vue`:

```vue
<script setup lang="ts">
defineProps<{ items: { q: string; a: string }[] }>()
const open = ref<number | null>(null)
</script>

<template>
  <div class="divide-y divide-slate-200 rounded-lg border border-slate-200 bg-white">
    <div v-for="(it, i) in items" :key="i">
      <button
        type="button"
        class="flex w-full items-center justify-between gap-4 px-5 py-4 text-left font-medium"
        :aria-expanded="open === i ? 'true' : 'false'"
        @click="open = open === i ? null : i"
      >
        {{ it.q }}
        <span aria-hidden="true">{{ open === i ? '−' : '+' }}</span>
      </button>
      <p v-if="open === i" class="px-5 pb-4 text-sm text-slate-600">{{ it.a }}</p>
    </div>
  </div>
</template>
```

`EmptyState.vue`:

```vue
<script setup lang="ts">
defineProps<{ title: string; body: string }>()
</script>

<template>
  <div class="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-6 py-16 text-center">
    <p class="font-display text-lg font-semibold text-slate-700">{{ title }}</p>
    <p class="mx-auto mt-2 max-w-md text-sm text-slate-500">{{ body }}</p>
    <div v-if="$slots.actions" class="mt-6 flex justify-center gap-3"><slot name="actions" /></div>
  </div>
</template>
```

- [ ] **Step 4: Run tests + build**

Run: `pnpm test -- marketing/ && pnpm lint && pnpm build`
Expected: PASS, lint clean, build OK.

- [ ] **Step 5: Commit**

```bash
git add nadoumi-web/app/components/marketing nadoumi-web/tests/unit/marketing
git commit -m "feat(web): marketing block components"
```

---

### Task 21: Rebuild `/`, `/about`, `/contact`, `/destinations`, `/privacy`, `/terms`

**Files:**
- Modify: `nadoumi-web/app/pages/index.vue`
- Modify: `nadoumi-web/app/pages/about.vue`
- Modify: `nadoumi-web/app/pages/contact.vue`
- Create: `nadoumi-web/app/pages/destinations/index.vue`
- Create: `nadoumi-web/app/pages/privacy.vue`
- Create: `nadoumi-web/app/pages/terms.vue`
- Delete: `nadoumi-web/app/components/PageHero.vue`, `nadoumi-web/app/components/ContentCard.vue`
- Create: `nadoumi-web/tests/unit/pages/index.test.ts`

**Interfaces:**
- Consumes: marketing components (Task 20), i18n keys (Task 7), `useSeo`.
- Produces: fully rebuilt marketing pages, all copy from i18n keys, no references to `PageHero`/`ContentCard`.

- [ ] **Step 1: Write `tests/unit/pages/index.test.ts`**

```ts
import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import Index from '~/pages/index.vue'

describe('home page', () => {
  it('renders the hero headline and both CTAs', async () => {
    const w = await mountSuspended(Index)
    expect(w.text()).toContain('Study abroad with Nadoumi')
    expect(w.text()).toContain('Create your profile')
    expect(w.text()).toContain('Browse scholarships')
  })
})
```

- [ ] **Step 2: Run test — verify it fails**

Run: `pnpm test -- pages/index`
Expected: FAIL — current `index.vue` uses `PageHero`/`ContentCard` and different copy.

- [ ] **Step 3: Rewrite `app/pages/index.vue`**

```vue
<script setup lang="ts">
const { t } = useI18n()
const localePath = useLocalePath()
useSeo(t('home.title'), t('home.subtitle'))

const features = computed(() => [
  { title: t('home.f1Title'), body: t('home.f1Body') },
  { title: t('home.f2Title'), body: t('home.f2Body') },
  { title: t('home.f3Title'), body: t('home.f3Body') },
])
</script>

<template>
  <div>
    <Hero :title="t('home.title')" :subtitle="t('home.subtitle')">
      <template #actions>
        <NButton :to="localePath('/register')">{{ t('home.ctaPrimary') }}</NButton>
        <NButton variant="secondary" :to="localePath('/scholarships')">{{ t('home.ctaSecondary') }}</NButton>
      </template>
    </Hero>

    <FeatureGrid :items="features" />
    <StatBand :text="t('home.statsIntro')" />

    <div class="grid gap-6 sm:grid-cols-2">
      <DestinationCard :title="t('destinations.chinaTitle')" :body="t('destinations.chinaBody')" :to="localePath('/destinations')" />
      <DestinationCard :title="t('destinations.malaysiaTitle')" :body="t('destinations.malaysiaBody')" :to="localePath('/destinations')" />
    </div>

    <CTASection :title="t('home.ctaBandTitle')" :body="t('home.ctaBandBody')">
      <template #actions>
        <NButton variant="secondary" :to="localePath('/register')">{{ t('home.ctaPrimary') }}</NButton>
      </template>
    </CTASection>
  </div>
</template>
```

- [ ] **Step 4: Rewrite `app/pages/about.vue`**

```vue
<script setup lang="ts">
const { t } = useI18n()
useSeo(t('about.title'), t('about.subtitle'))
</script>

<template>
  <div>
    <Hero :title="t('about.title')" :subtitle="t('about.subtitle')" />
    <p class="max-w-2xl text-slate-700">{{ t('about.body') }}</p>
  </div>
</template>
```

- [ ] **Step 5: Rewrite `app/pages/contact.vue`**

```vue
<script setup lang="ts">
const { t } = useI18n()
useSeo(t('contact.title'), t('contact.subtitle'))

const form = reactive({ name: '', email: '', message: '' })
const sent = ref(false)
function submit() {
  // Wired to a support inbox with the Content/CMS slice; client stub for now.
  sent.value = true
}
</script>

<template>
  <div>
    <Hero :title="t('contact.title')" :subtitle="t('contact.subtitle')" />
    <NCard v-if="!sent" class="max-w-xl">
      <form class="grid gap-4" @submit.prevent="submit">
        <NField :label="t('contact.name')" for="c-name" required><NInput id="c-name" v-model="form.name" /></NField>
        <NField :label="t('contact.email')" for="c-email" required><NInput id="c-email" v-model="form.email" type="email" /></NField>
        <NField :label="t('contact.message')" for="c-msg" required><NTextarea id="c-msg" v-model="form.message" :rows="5" /></NField>
        <NButton type="submit">{{ t('contact.send') }}</NButton>
      </form>
    </NCard>
    <NAlert v-else tone="success" class="max-w-xl">{{ t('contact.sent') }}</NAlert>
    <p class="mt-4 text-sm text-slate-500">{{ t('contact.email_help') }}</p>
  </div>
</template>
```

- [ ] **Step 6: Write `app/pages/destinations/index.vue`**

```vue
<script setup lang="ts">
const { t } = useI18n()
useSeo(t('destinations.title'), t('destinations.subtitle'))
</script>

<template>
  <div>
    <Hero :title="t('destinations.title')" :subtitle="t('destinations.subtitle')" />
    <div class="grid gap-8">
      <section>
        <h2 class="font-display text-xl font-semibold">{{ t('destinations.chinaTitle') }}</h2>
        <p class="mt-2 max-w-2xl text-slate-700">{{ t('destinations.chinaBody') }}</p>
      </section>
      <section>
        <h2 class="font-display text-xl font-semibold">{{ t('destinations.malaysiaTitle') }}</h2>
        <p class="mt-2 max-w-2xl text-slate-700">{{ t('destinations.malaysiaBody') }}</p>
      </section>
      <p class="text-sm text-slate-500">{{ t('destinations.more') }}</p>
    </div>
  </div>
</template>
```

- [ ] **Step 7: Write `app/pages/privacy.vue` and `app/pages/terms.vue`**

```vue
<!-- privacy.vue -->
<script setup lang="ts">
const { t } = useI18n()
useSeo(t('footer.privacy'), t('footer.privacy'))
</script>

<template>
  <article class="prose-sm max-w-2xl">
    <h1 class="font-display text-2xl font-bold">{{ t('footer.privacy') }}</h1>
    <p class="mt-4 text-sm text-slate-500"><em>Draft — placeholder text. Replace with the reviewed privacy policy before public launch.</em></p>
    <p class="mt-4 text-slate-700">Nadoumi processes the personal data students provide (profile, education history, test scores, contacts, documents) solely to support their study-abroad applications. Data is not sold. Students may request access or deletion by emailing support@nadoumi.com.</p>
  </article>
</template>
```

```vue
<!-- terms.vue -->
<script setup lang="ts">
const { t } = useI18n()
useSeo(t('footer.terms'), t('footer.terms'))
</script>

<template>
  <article class="prose-sm max-w-2xl">
    <h1 class="font-display text-2xl font-bold">{{ t('footer.terms') }}</h1>
    <p class="mt-4 text-sm text-slate-500"><em>Draft — placeholder text. Replace with the reviewed terms of service before public launch.</em></p>
    <p class="mt-4 text-slate-700">By creating a Nadoumi profile you agree to provide accurate information and to use the platform only for legitimate study-abroad application purposes. Nadoumi provides guidance and application support and does not guarantee admission or scholarship outcomes.</p>
  </article>
</template>
```

- [ ] **Step 8: Delete the old helper components**

```bash
git rm nadoumi-web/app/components/PageHero.vue nadoumi-web/app/components/ContentCard.vue
```

- [ ] **Step 9: Run tests + build**

Run: `pnpm test && pnpm lint && pnpm build`
Expected: all pass; no unresolved `PageHero`/`ContentCard`. (`scholarships/index.vue` etc. still import `ContentCard` — Task 22 rewrites them; if the build breaks here, do Task 22 in the same commit.)

- [ ] **Step 10: Commit**

```bash
git add nadoumi-web/app
git commit -m "feat(web): rebuild marketing pages on the design system"
```

---

### Task 22: Catalog list/detail pages with empty-states + robots + sitemap

**Files:**
- Modify: `nadoumi-web/app/pages/scholarships/index.vue`
- Modify: `nadoumi-web/app/pages/universities/index.vue`
- Modify: `nadoumi-web/app/pages/programs/index.vue`
- Create: `nadoumi-web/app/pages/scholarships/[id].vue`
- Create: `nadoumi-web/app/pages/universities/[id].vue`
- Create: `nadoumi-web/app/pages/programs/[id].vue`
- Create: `nadoumi-web/public/robots.txt`
- Create: `nadoumi-web/server/routes/sitemap.xml.ts`
- Create: `nadoumi-web/tests/unit/pages/scholarships.test.ts`

**Interfaces:**
- Consumes: `useApi().publicGet` (returns `Page<T>` or throws), `EmptyState`, `Hero`, i18n `catalog.*`.
- Produces: each list page SSR-fetches its endpoint with `useAsyncData(..., () => publicGet(...).catch(() => null))`; when `null`/empty → `<EmptyState :title="t('catalog.empty')" :body="t('catalog.emptyDetail')">`; when populated → a grid of `NCard`s linking to `/{type}/{id}`. Each `[id].vue` fetches the single item (`.catch(() => null)`); `null` → `EmptyState` + a "back to list" link. `robots.txt` allows all + points at the sitemap. `server/routes/sitemap.xml.ts` returns XML listing the marketing routes (`/`, `/scholarships`, `/universities`, `/programs`, `/destinations`, `/about`, `/contact`, `/privacy`, `/terms`) at the configured site origin.

- [ ] **Step 1: Write `tests/unit/pages/scholarships.test.ts`**

```ts
import { describe, it, expect, vi } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import Scholarships from '~/pages/scholarships/index.vue'

vi.mock('~/composables/useApi', () => ({
  useApi: () => ({ publicGet: vi.fn().mockRejectedValue(new Error('no api')), studentFetch: vi.fn() }),
  problemMessage: (_e: unknown, f: string) => f,
}))

describe('scholarships list', () => {
  it('shows the empty state when the catalogue API is unavailable', async () => {
    const w = await mountSuspended(Scholarships)
    expect(w.text()).toContain("isn't available yet")
  })
})
```

- [ ] **Step 2: Run test — verify it fails**

Run: `pnpm test -- pages/scholarships`
Expected: FAIL — page still uses `ContentCard` / old copy.

- [ ] **Step 3: Rewrite `app/pages/scholarships/index.vue`**

```vue
<script setup lang="ts">
import type { Page, ScholarshipSummary } from '~/types/catalog'
const { t } = useI18n()
const localePath = useLocalePath()
const { publicGet } = useApi()
useSeo(t('catalog.scholarshipsTitle'), t('catalog.scholarshipsSubtitle'))

const { data } = await useAsyncData('scholarships', () =>
  publicGet<Page<ScholarshipSummary>>('scholarships').catch(() => null),
)
const items = computed(() => data.value?.content ?? [])
</script>

<template>
  <div>
    <Hero :title="t('catalog.scholarshipsTitle')" :subtitle="t('catalog.scholarshipsSubtitle')" />
    <div v-if="items.length" class="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
      <NuxtLink v-for="s in items" :key="s.id" :to="localePath(`/scholarships/${s.id}`)">
        <NCard>
          <h3 class="font-display font-semibold">{{ s.title }}</h3>
          <p class="mt-1 text-xs text-slate-500">{{ [s.country, s.degreeLevel, s.field].filter(Boolean).join(' · ') }}</p>
          <p class="mt-2 text-sm text-slate-600">{{ s.deadline ? t('catalog.deadline', { date: s.deadline }) : t('catalog.rollingDeadline') }}</p>
        </NCard>
      </NuxtLink>
    </div>
    <EmptyState v-else :title="t('catalog.empty')" :body="t('catalog.emptyDetail')" />
  </div>
</template>
```

- [ ] **Step 4: Rewrite `universities/index.vue` and `programs/index.vue`**

`universities/index.vue`:

```vue
<script setup lang="ts">
import type { Page, UniversitySummary } from '~/types/catalog'
const { t } = useI18n()
const localePath = useLocalePath()
const { publicGet } = useApi()
useSeo(t('catalog.universitiesTitle'), t('catalog.universitiesSubtitle'))

const { data } = await useAsyncData('universities', () =>
  publicGet<Page<UniversitySummary>>('universities').catch(() => null),
)
const items = computed(() => data.value?.content ?? [])
</script>

<template>
  <div>
    <Hero :title="t('catalog.universitiesTitle')" :subtitle="t('catalog.universitiesSubtitle')" />
    <div v-if="items.length" class="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
      <NuxtLink v-for="u in items" :key="u.id" :to="localePath(`/universities/${u.id}`)">
        <NCard>
          <h3 class="font-display font-semibold">{{ u.name }}</h3>
          <p class="mt-1 text-xs text-slate-500">{{ [u.city, u.country].filter(Boolean).join(', ') }}</p>
        </NCard>
      </NuxtLink>
    </div>
    <EmptyState v-else :title="t('catalog.empty')" :body="t('catalog.emptyDetail')" />
  </div>
</template>
```

`programs/index.vue`:

```vue
<script setup lang="ts">
import type { Page, ProgramSummary } from '~/types/catalog'
const { t } = useI18n()
const localePath = useLocalePath()
const { publicGet } = useApi()
useSeo(t('catalog.programsTitle'), t('catalog.programsSubtitle'))

const { data } = await useAsyncData('programs', () =>
  publicGet<Page<ProgramSummary>>('programs').catch(() => null),
)
const items = computed(() => data.value?.content ?? [])
</script>

<template>
  <div>
    <Hero :title="t('catalog.programsTitle')" :subtitle="t('catalog.programsSubtitle')" />
    <div v-if="items.length" class="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
      <NuxtLink v-for="p in items" :key="p.id" :to="localePath(`/programs/${p.id}`)">
        <NCard>
          <h3 class="font-display font-semibold">{{ p.name }}</h3>
          <p class="mt-1 text-xs text-slate-500">{{ [p.degreeLevel, p.field, p.language].filter(Boolean).join(' · ') }}</p>
        </NCard>
      </NuxtLink>
    </div>
    <EmptyState v-else :title="t('catalog.empty')" :body="t('catalog.emptyDetail')" />
  </div>
</template>
```

- [ ] **Step 5: Write the three `[id].vue` detail pages**

All three share this shape (`scholarships/[id].vue` shown; make `universities/[id].vue` and `programs/[id].vue` identical but for the endpoint segment and the `useAsyncData` key):

```vue
<script setup lang="ts">
const { t } = useI18n()
const localePath = useLocalePath()
const route = useRoute()
const { publicGet } = useApi()

const { data } = await useAsyncData(`scholarship-${route.params.id}`, () =>
  publicGet<Record<string, unknown>>(`scholarships/${route.params.id}`).catch(() => null),
)
useSeo(t('catalog.scholarshipsTitle'), t('catalog.scholarshipsSubtitle'))
</script>

<template>
  <div>
    <EmptyState :title="t('catalog.empty')" :body="t('catalog.emptyDetail')" />
    <p class="mt-4">
      <NuxtLink :to="localePath('/scholarships')" class="text-brand-700 hover:underline">{{ t('catalog.backToList') }}</NuxtLink>
    </p>
  </div>
</template>
```

- [ ] **Step 6: Write `public/robots.txt`**

```
User-agent: *
Allow: /

Sitemap: /sitemap.xml
```

- [ ] **Step 7: Write `server/routes/sitemap.xml.ts`**

```ts
export default defineEventHandler((event) => {
  const routes = ['/', '/scholarships', '/universities', '/programs', '/destinations', '/about', '/contact', '/privacy', '/terms']
  const origin = getRequestURL(event).origin
  const urls = routes.map(r => `  <url><loc>${origin}${r}</loc></url>`).join('\n')
  setHeader(event, 'content-type', 'application/xml')
  return `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${urls}\n</urlset>\n`
})
```

- [ ] **Step 8: Run tests + build**

Run: `pnpm test && pnpm lint && pnpm build`
Expected: all pass. Manually: `pnpm dev` then `curl -s localhost:3000/sitemap.xml` returns XML; `/scholarships` shows the empty state.

- [ ] **Step 9: Commit**

```bash
git add nadoumi-web/app nadoumi-web/public nadoumi-web/server/routes nadoumi-web/tests/unit/pages/scholarships.test.ts
git commit -m "feat(web): catalog list/detail empty-states, robots.txt, sitemap"
```

---

### Task 23: Playwright E2E — the auth journey

> **SUPERSEDED by Revision 2 → Task F-9.** The register journey now goes through the
> two-step OTP flow and a second spec covers forgot-password. Both are hard gates.
> The `webServer`/config scaffold below is still the basis; F-9 restates it with the
> OTP steps and the `readOtp` helper. Skip to Task F-9.

**Files:**
- Create: `nadoumi-web/playwright.config.ts`
- Create: `nadoumi-web/tests/e2e/auth.spec.ts`
- Modify: `nadoumi-web/package.json` (`test:e2e` script already added in Task 1; add `test:e2e:install`)
- Create: `nadoumi-web/.gitignore` entries for `playwright-report/`, `test-results/`

**Interfaces:**
- Consumes: a running backend at `NUXT_BACKEND_BASE_URL` (default `http://localhost:8080`) with `sys.account.captchaEnabled=false` and `nad.student.register.enabled=true`; a built `nadoumi-web` served by `nuxt preview` on `:3000`.
- Produces: one spec exercising register → auto-login → dashboard → sign out.

- [ ] **Step 1: Write `playwright.config.ts`**

```ts
import { defineConfig } from '@playwright/test'

const BACKEND = process.env.NUXT_BACKEND_BASE_URL ?? 'http://localhost:8080'

export default defineConfig({
  testDir: './tests/e2e',
  timeout: 30_000,
  fullyParallel: false,
  retries: process.env.CI ? 1 : 0,
  use: { baseURL: 'http://localhost:3000', trace: 'on-first-retry' },
  webServer: {
    command: 'pnpm build && pnpm preview',
    url: 'http://localhost:3000',
    timeout: 120_000,
    reuseExistingServer: !process.env.CI,
    env: { NUXT_BACKEND_BASE_URL: BACKEND },
  },
})
```

- [ ] **Step 2: Write `tests/e2e/auth.spec.ts`**

```ts
import { test, expect } from '@playwright/test'

test('register → auto-login → dashboard → sign out', async ({ page }) => {
  const uniq = Date.now().toString().slice(-8)
  const username = `e2e${uniq}`           // ≤ 20 chars, unique per run

  await page.goto('/register')
  await page.fill('#fullName', 'E2E Tester')
  await page.fill('#username', username)
  await page.fill('#email', `${username}@example.com`)
  await page.fill('#password', 'secret123')
  await page.fill('#confirm', 'secret123')
  await page.check('#terms')
  await page.getByRole('button', { name: /create profile/i }).click()

  // server-side auto-login landed us in the dashboard
  await expect(page).toHaveURL(/\/dashboard\/profile/)
  await expect(page.getByText(/applicant profile/i)).toBeVisible()

  // sign out from the account menu
  await page.locator('button[aria-haspopup="menu"]').last().click()
  await page.locator('[data-test="sign-out"]').click()

  await expect(page).toHaveURL(/\/$/)
  await expect(page.getByRole('link', { name: /sign in/i })).toBeVisible()
})
```

- [ ] **Step 3: Local dry-run (optional but recommended)**

With MySQL + Redis + backend running (captcha disabled, registration enabled):

```bash
cd nadoumi-web
pnpm exec playwright install --with-deps chromium
pnpm test:e2e
```

Expected: 1 passed.

- [ ] **Step 4: Add report dirs to `.gitignore`**

Append `playwright-report/` and `test-results/` to `nadoumi-web/.gitignore`.

- [ ] **Step 5: Commit**

```bash
git add nadoumi-web/playwright.config.ts nadoumi-web/tests/e2e nadoumi-web/.gitignore nadoumi-web/package.json
git commit -m "test(web): Playwright E2E for the auth journey"
```

---

### Task 24: CI wiring + docs + memory

> **Revision 2 adjustments:** the `§7 rewrite` is done by Task F-10 — here just
> confirm it's current. The E2E job now runs **two** specs (`auth`,
> `forgot-password`) and the backend must be started with
> `NADOUMI_MAIL_TRANSPORT=log` so `readOtp` can read the code from
> `/api/dev/mail/latest`. The backend jar built by the `backend` job already
> contains Part E. Also verify the doc-consistency sweep (below).

**Files:**
- Modify: `.github/workflows/ci.yml`
- Verify: `docs/FRONTEND_ARCHITECTURE.md` §7 (rewritten in F-10 — check it matches the shipped state)
- Verify: `docs/API_DESIGN.md`, `docs/SECURITY.md`, `docs/PHASE_3_IDENTITY_APPLICANT.md`,
  `docs/COMMUNICATION_AND_NOTIFICATIONS.md`, `docs/ADMIN_ARCHITECTURE.md`,
  `docs/DOMAIN_MODEL.md`, `docs/DATABASE_DESIGN.md`, `docs/DEPLOYMENT.md`,
  `docs/DEVELOPMENT_GUIDELINES.md`, `docs/APPLICANT_ONBOARDING.md` — all updated in
  the Revision 2 doc pass; reconcile any drift introduced during E/F execution
  (endpoint names, migration numbers, env vars).
- Create: `~/.claude/projects/-Users-mac-Desktop-nadoumi/memory/nadoumi-web-stack.md`
- Modify: `~/.claude/projects/-Users-mac-Desktop-nadoumi/memory/MEMORY.md`

**Interfaces:**
- Consumes: existing `backend` job (produces `ruoyi-admin.jar` artifact) and `nadoumi-web` job.
- Produces: `nadoumi-web` job also runs `pnpm test`; a new `nadoumi-web-e2e` job `needs: [backend, nadoumi-web]` that boots MySQL + Redis services, downloads the jar artifact, starts it with `sys.account.captchaEnabled=false`, `nad.student.register.enabled=true`, and `NADOUMI_MAIL_TRANSPORT=log`, polls `/actuator/health` until ready, then runs **both** Playwright specs. **No `continue-on-error`.**

- [ ] **Step 1: Update the `nadoumi-web` job in `.github/workflows/ci.yml`**

Add a step after `pnpm build` (or before — order doesn't matter):

```yaml
      - run: pnpm test
```

- [ ] **Step 2: Add the `nadoumi-web-e2e` job**

```yaml
  nadoumi-web-e2e:
    runs-on: ubuntu-latest
    timeout-minutes: 25
    needs: [backend, nadoumi-web]
    services:
      mysql:
        image: mysql:8.4
        env:
          MYSQL_ROOT_PASSWORD: root
          MYSQL_DATABASE: ry-vue
        ports: ['3306:3306']
        options: >-
          --health-cmd="mysqladmin ping -proot" --health-interval=5s
          --health-timeout=5s --health-retries=20
      redis:
        image: redis:7-alpine
        ports: ['6379:6379']
        options: --health-cmd="redis-cli ping" --health-interval=5s --health-timeout=5s --health-retries=20
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '21' }

      - name: Download backend jar
        uses: actions/download-artifact@v4
        with: { name: ruoyi-admin, path: backend-jar }

      - name: Start backend
        env:
          SPRING_DATASOURCE_DRUID_MASTER_URL: jdbc:mysql://127.0.0.1:3306/ry-vue?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
          SPRING_DATASOURCE_DRUID_MASTER_USERNAME: root
          SPRING_DATASOURCE_DRUID_MASTER_PASSWORD: root
          SPRING_DATA_REDIS_HOST: 127.0.0.1
          NADOUMI_MAIL_TRANSPORT: log          # OTP mails readable via GET /api/dev/mail/latest (Revision 2)
        run: |
          java -jar backend-jar/ruoyi-admin.jar > backend.log 2>&1 &
          echo $! > backend.pid

      - name: Wait for backend
        run: |
          for i in $(seq 1 60); do
            if curl -sf http://localhost:8080/ >/dev/null; then echo "backend up"; exit 0; fi
            sleep 3
          done
          echo "backend did not start"; tail -n 200 backend.log; exit 1

      - name: Disable captcha + enable student registration
        run: |
          mysql -h127.0.0.1 -uroot -proot ry-vue -e "UPDATE sys_config SET config_value='false' WHERE config_key='sys.account.captchaEnabled'; UPDATE sys_config SET config_value='true' WHERE config_key='nad.student.register.enabled';"

      - name: Restart backend to pick up config
        env:
          SPRING_DATASOURCE_DRUID_MASTER_URL: jdbc:mysql://127.0.0.1:3306/ry-vue?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
          SPRING_DATASOURCE_DRUID_MASTER_USERNAME: root
          SPRING_DATASOURCE_DRUID_MASTER_PASSWORD: root
          SPRING_DATA_REDIS_HOST: 127.0.0.1
          NADOUMI_MAIL_TRANSPORT: log
        run: |
          kill "$(cat backend.pid)" || true
          sleep 3
          java -jar backend-jar/ruoyi-admin.jar > backend2.log 2>&1 &
          for i in $(seq 1 60); do curl -sf http://localhost:8080/ >/dev/null && break || sleep 3; done

      - uses: pnpm/action-setup@v4
        with: { version: 10 }
      - uses: actions/setup-node@v4
        with: { node-version: '20', cache: pnpm, cache-dependency-path: nadoumi-web/pnpm-lock.yaml }

      - name: E2E
        working-directory: nadoumi-web
        env:
          NUXT_BACKEND_BASE_URL: http://localhost:8080
        run: |
          pnpm install --frozen-lockfile
          pnpm exec playwright install --with-deps chromium
          pnpm test:e2e

      - name: Backend logs on failure
        if: failure()
        run: tail -n 300 backend.log backend2.log || true
```

> If `sys.account.captchaEnabled` / `nad.student.register.enabled` config caching means the SQL update alone isn't honoured, the "Restart backend" step above forces a reload. Keep both steps.

- [ ] **Step 3: Run the workflow file through a linter**

Run: `python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/ci.yml'))" && echo OK`
Expected: `OK` (valid YAML).

- [ ] **Step 4: Rewrite `docs/FRONTEND_ARCHITECTURE.md` §7**

Replace the current §7 body with sections covering: the brand token table (from this plan's Global Constraints + Task 1), the Plus Jakarta Sans / Inter / Noto Sans Arabic pairing and RTL approach, the three component tiers (`ui/` / `marketing/` / `dashboard/`) with the primitive list, the auth/session design (the BFF route table + `useSession` + middleware + the "credential exposure rule"), the full page inventory (marketing + auth + dashboard), the i18n policy (`en` authored, `fr`/`ar`/`zh` mirrored, `fallbackLocale: 'en'`), and the testing + CI additions (Vitest + the hard-gated Playwright E2E job). Mark each subsection `EXISTING` (delivered by this build) vs `PLANNED` (catalog data wiring, forgot-password backend, dark mode, real translations).

- [ ] **Step 5: Write the memory note**

`~/.claude/projects/-Users-mac-Desktop-nadoumi/memory/nadoumi-web-stack.md`:

```markdown
---
name: nadoumi-web-stack
description: nadoumi-web is the Nuxt 3 public site + student dashboard; stack, BFF, and where things live
metadata:
  type: project
---

`nadoumi-web/` = public marketing site + authenticated student profile dashboard.
Nuxt 3 (SSR, `app/` dir) + Tailwind + `@nuxt/fonts` + `@nuxtjs/i18n@9`. Hand-built
`app/components/ui/` primitives (no component library). Brand = Tailwind `orange`
(500 `#F97316`); solid UI uses `orange-600/700`. Light theme only.

Auth: the browser never holds the JWT. `server/api/student-session.{get,post,delete}.ts`
+ `server/api/student-account.post.ts` (server-side register+login) set an httpOnly
cookie; `server/api/student/[...path].ts` attaches the bearer. `useSession()` reads
`/api/student-session`. Route middleware `auth` / `guest`.

Dashboard screens hit the Phase 3 `/api/student/applicants/**` API. Catalog pages
(scholarships/universities/programs) render empty-states — no backend catalog API yet.

Tests: Vitest (`pnpm test`) + one Playwright E2E (`pnpm test:e2e`, hard CI gate).
Full design + decisions: [[phase-status]], `docs/superpowers/plans/2026-09-02-nadoumi-web-public-site.md`.
```

- [ ] **Step 6: Add the `MEMORY.md` index line**

Under the existing list in `~/.claude/projects/-Users-mac-Desktop-nadoumi/memory/MEMORY.md`:

```markdown
- [nadoumi-web stack](nadoumi-web-stack.md) — Nuxt 3 public site + student dashboard; BFF cookie auth, hand-built UI kit
```

- [ ] **Step 7: Final full gate**

Run from `nadoumi-web/`: `pnpm install && pnpm lint && pnpm test && pnpm build`
Expected: all green.

- [ ] **Step 8: Commit**

```bash
git add .github/workflows/ci.yml docs/FRONTEND_ARCHITECTURE.md
git commit -m "ci(web): run Vitest + hard-gated Playwright E2E; document nadoumi-web §7"
```

(The memory files live outside the repo — no commit needed.)

---

# PART E — Backend auth slice (Revision 2)

Produces: an SMTP-abstracted email channel, Redis OTP + ticket services, a shared
password policy, session revocation, email-first register/login, `password/reset` +
`password` endpoints, and the admin-bootstrap migration. Java 21 / Spring Boot,
MyBatis, RuoYi conventions. Each task: `mvn -q -pl <module> -am test` green, no
Checkstyle/format regressions, one conventional commit.

**Module map for Part E**

| Path | Responsibility |
| --- | --- |
| `ruoyi-common/src/main/java/com/ruoyi/common/utils/PasswordPolicy.java` | Pure static validator — length + character-class + not-equal-to-current. No Spring, no deps beyond `SecurityUtils`. |
| `nadoumi-identity/.../service/mail/MailSender.java` | Port: `send(EmailMessage)`. |
| `nadoumi-identity/.../service/mail/EmailMessage.java` | `record EmailMessage(String to, String subject, String body)`. |
| `nadoumi-identity/.../service/mail/SmtpMailSender.java` | `JavaMailSender` adapter, `@ConditionalOnProperty(nadoumi.mail.transport=smtp)`. |
| `nadoumi-identity/.../service/mail/LoggingMailSender.java` | Dev/CI adapter: append JSON to a file + keep last-per-recipient in memory. `@ConditionalOnProperty(nadoumi.mail.transport=log, matchIfMissing=true)`. |
| `nadoumi-identity/.../service/mail/MailTemplates.java` | Renders `resources/mail/*.txt` with `${var}` substitution. |
| `nadoumi-identity/.../service/otp/OtpPurpose.java` | `enum { REGISTER, PASSWORD_RESET }`. |
| `nadoumi-identity/.../service/otp/OtpService.java` | issue / verify, Redis-backed, TTL + attempt cap + cooldown. |
| `nadoumi-identity/.../service/otp/TicketService.java` | mint / consume, Redis-backed, single-use. |
| `nadoumi-identity/.../access/SessionRevoker.java` | delete RuoYi `login_tokens:*` sessions by `userId`. |
| `nadoumi-identity/.../web/StudentEmailOtpController.java` | `POST /api/student/email-otp`, `POST /api/student/email-otp/verify`. |
| `nadoumi-identity/.../web/StudentPasswordController.java` | `POST /api/student/password/reset`, `POST /api/student/password`. |
| `nadoumi-identity/.../web/DevMailController.java` | `GET /api/dev/mail/latest` — only when `transport=log`. |
| `ruoyi-admin/src/main/resources/db/migration/V7__nad_student_email_verified.sql` | `sys_user.email_verified` + `idx_sys_user_email`. |
| `ruoyi-admin/src/main/resources/db/migration/V8__drop_initial_password_nag.sql` | `initPasswordModify → 0`, `passwordValidateDays → 90`. (`almousleck`/`admin` already seeded in `V5`.) |

**Global for Part E:** all new anonymous endpoints carry `@Anonymous` (so
`PermitAllUrlProperties` allows them) and `@RateLimiter`. All error bodies are
`application/problem+json` via the existing `NadApiExceptionHandler` — throw
`NadBadRequestException` / `NadForbiddenException`, never return `AjaxResult`.

**Config strategy (senior/clean):** the domain never names a provider. All mail
settings are `${ENV:default}` placeholders in `application.yml`; a repo-root
`docker-compose.yml` (MySQL + Redis + Mailpit) plus a git-ignored `.env` supply
local values; `config/application-local.yml` (already the repo's per-dev override
mechanism, git-ignored) is the other supported override path. Gmail is just an SMTP
host set in `.env` — no code change. Tests + CI force `NADOUMI_MAIL_TRANSPORT=log`.

---

### Task E-0: local infrastructure — `docker-compose.yml` + `.env`

**Files:**
- Create: `docker-compose.yml` (repo root)
- Create: `.env.example` (repo root, committed)
- Modify: `.gitignore` (add `/.env`, `/mail-outbox.log`)
- Modify: `ruoyi-admin/src/main/resources/application.yml` (add `spring.mail` +
  `nadoumi.mail` + `nadoumi.web` blocks as `${ENV:default}` placeholders)
- Modify: `config/application-local.yml.example` (add the mail keys, commented)
- Modify: `docs/DEPLOYMENT.md` §4.1 (env-var contract — add the mail rows) and
  `docs/DEVELOPMENT_GUIDELINES.md` §1 (mention `docker compose up -d`)

**Interfaces:**
- Produces: `docker compose up -d` brings up `mysql:3306`, `redis:6379`,
  `mailpit:1025` (SMTP) + `mailpit:8025` (web UI). Backend env-var contract gains
  `NADOUMI_MAIL_TRANSPORT`, `NADOUMI_MAIL_FROM`, `NADOUMI_MAIL_LOG_FILE`,
  `SPRING_MAIL_HOST/PORT/USERNAME/PASSWORD`, `SPRING_MAIL_SMTP_AUTH`,
  `SPRING_MAIL_SMTP_STARTTLS`, `NADOUMI_WEB_LOGIN_URL`.

- [ ] **Step 1: Write `docker-compose.yml`**

```yaml
name: nadoumi

# Local dev dependencies. Values come from ./.env (git-ignored; copy .env.example).
# `docker compose up -d` then run the Spring app + nadoumi-web against localhost.
services:
  mysql:
    image: mysql:8.4
    container_name: nadoumi-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root}
      MYSQL_DATABASE: ${MYSQL_DATABASE:-ry-vue}
    ports:
      - "${MYSQL_PORT:-3306}:3306"
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "127.0.0.1", "-p${MYSQL_ROOT_PASSWORD:-root}"]
      interval: 5s
      timeout: 5s
      retries: 20
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    container_name: nadoumi-redis
    command: ["redis-server", "--save", "60", "1", "--loglevel", "warning"]
    ports:
      - "${REDIS_PORT:-6379}:6379"
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 20
    restart: unless-stopped

  # Local SMTP sink — catches every outbound mail; nothing leaves the machine.
  # Web UI: http://localhost:${MAILPIT_UI_PORT:-8025}
  mailpit:
    image: axllent/mailpit:v1.20
    container_name: nadoumi-mailpit
    environment:
      MP_MAX_MESSAGES: "500"
      MP_SMTP_AUTH_ACCEPT_ANY: "true"
      MP_SMTP_AUTH_ALLOW_INSECURE: "true"
    ports:
      - "${MAILPIT_SMTP_PORT:-1025}:1025"
      - "${MAILPIT_UI_PORT:-8025}:8025"
    restart: unless-stopped

volumes:
  mysql-data:
  redis-data:
```

- [ ] **Step 2: Write `.env.example`**

```dotenv
# Copy to .env (git-ignored). `docker compose` reads .env automatically.
# Export the backend vars into your shell, or drop them in config/application-local.yml.

# ---- docker compose services ----
MYSQL_ROOT_PASSWORD=root
MYSQL_DATABASE=ry-vue
MYSQL_PORT=3306
REDIS_PORT=6379
MAILPIT_SMTP_PORT=1025
MAILPIT_UI_PORT=8025

# ---- backend: datasource + redis (match the services above) ----
SPRING_DATASOURCE_DRUID_MASTER_URL=jdbc:mysql://localhost:3306/ry-vue?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_DRUID_MASTER_USERNAME=root
SPRING_DATASOURCE_DRUID_MASTER_PASSWORD=root
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379

# ---- backend: mail (spec Revision 2 §15) ----
# NADOUMI_MAIL_TRANSPORT: log  -> write to ./mail-outbox.log + GET /api/dev/mail/latest (no network)
#                         smtp -> send via SPRING_MAIL_* below
NADOUMI_MAIL_TRANSPORT=smtp
NADOUMI_MAIL_FROM=no-reply@nadoumi.local
NADOUMI_WEB_LOGIN_URL=http://localhost:3000/login

# default: Mailpit (no auth, no TLS) — see the compose service
SPRING_MAIL_HOST=localhost
SPRING_MAIL_PORT=1025
SPRING_MAIL_USERNAME=
SPRING_MAIL_PASSWORD=
SPRING_MAIL_SMTP_AUTH=false
SPRING_MAIL_SMTP_STARTTLS=false

# ---- Gmail (staging / production-like) ----
# Requires a Google account with 2-Step Verification and an App Password:
#   https://myaccount.google.com/apppasswords
# Then set NADOUMI_MAIL_TRANSPORT=smtp and:
# SPRING_MAIL_HOST=smtp.gmail.com
# SPRING_MAIL_PORT=587
# SPRING_MAIL_USERNAME=you@gmail.com
# SPRING_MAIL_PASSWORD=your-16-char-app-password
# SPRING_MAIL_SMTP_AUTH=true
# SPRING_MAIL_SMTP_STARTTLS=true
```

- [ ] **Step 3: `.gitignore`** — append under the `# Nadoumi` block:

```gitignore
/.env
/mail-outbox.log
```

(`*.log` already matches `mail-outbox.log`, but the explicit line documents intent.)

- [ ] **Step 4: `application.yml` — add the blocks** (inside the top-level `spring:`
  for `mail`, and a top-level `nadoumi:` sibling — note `ruoyi:` is already
  top-level, add `nadoumi:` next to it):

```yaml
spring:
  # ... existing keys ...
  mail:
    host: ${SPRING_MAIL_HOST:localhost}
    port: ${SPRING_MAIL_PORT:1025}
    username: ${SPRING_MAIL_USERNAME:}
    password: ${SPRING_MAIL_PASSWORD:}
    default-encoding: UTF-8
    properties:
      mail:
        smtp:
          auth: ${SPRING_MAIL_SMTP_AUTH:false}
          starttls:
            enable: ${SPRING_MAIL_SMTP_STARTTLS:false}
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000

nadoumi:
  mail:
    transport: ${NADOUMI_MAIL_TRANSPORT:log}   # log | smtp
    from: ${NADOUMI_MAIL_FROM:no-reply@nadoumi.local}
    logFile: ${NADOUMI_MAIL_LOG_FILE:./mail-outbox.log}
  web:
    loginUrl: ${NADOUMI_WEB_LOGIN_URL:http://localhost:3000/login}
```

- [ ] **Step 5: `config/application-local.yml.example`** — add, commented:

```yaml
# nadoumi:
#   mail:
#     transport: smtp
# spring:
#   mail:
#     host: smtp.gmail.com
#     port: 587
#     username: you@gmail.com
#     password: your-app-password
#     properties: { mail: { smtp: { auth: true, starttls: { enable: true } } } }
```

- [ ] **Step 6: Docs** — `docs/DEPLOYMENT.md` §4.1: add rows for the mail env vars
  (name, purpose, default). `docs/DEVELOPMENT_GUIDELINES.md` §1: add a line
  *"`docker compose up -d` starts MySQL + Redis + Mailpit (SMTP sink, UI
  :8025); `cp .env.example .env` first."*

- [ ] **Step 7: Verify**

```bash
docker compose config >/dev/null && echo "compose OK"
cp .env.example .env && docker compose up -d
docker compose ps          # mysql, redis, mailpit healthy
python3 -c "import yaml; yaml.safe_load(open('ruoyi-admin/src/main/resources/application.yml')); print('yaml OK')"
```

- [ ] **Step 8: Commit**

```bash
git add docker-compose.yml .env.example .gitignore \
        ruoyi-admin/src/main/resources/application.yml \
        config/application-local.yml.example \
        docs/DEPLOYMENT.md docs/DEVELOPMENT_GUIDELINES.md
git commit -m "chore(infra): docker-compose (mysql+redis+mailpit) + .env + mail config placeholders"
```

---

### Task E-1: SMTP-abstracted mail channel

**Files:**
- Modify: `nadoumi-modules/nadoumi-identity/pom.xml` (add `spring-boot-starter-mail`)
- Create: `nadoumi-modules/nadoumi-identity/src/main/java/com/nadoumi/identity/service/mail/MailSender.java`
- Create: `.../service/mail/EmailMessage.java`
- Create: `.../service/mail/SmtpMailSender.java`
- Create: `.../service/mail/LoggingMailSender.java`
- Create: `.../service/mail/MailTemplates.java`
- Create: `nadoumi-modules/nadoumi-identity/src/main/resources/mail/otp-register.txt`
- Create: `.../resources/mail/otp-password-reset.txt`
- Create: `.../resources/mail/account-exists.txt`
- Create: `nadoumi-modules/nadoumi-identity/src/test/java/com/nadoumi/identity/mail/MailTemplatesTest.java`
- Create: `.../mail/LoggingMailSenderTest.java`
- Modify: `ruoyi-admin/src/main/resources/application.yml` (add `nadoumi.mail.*` block)

**Interfaces:**
- Produces:
  - `interface MailSender { void send(EmailMessage msg); }`
  - `record EmailMessage(String to, String subject, String body)`
  - `MailTemplates.render(String template, Map<String,String> vars) → String` — loads
    `classpath:/mail/<template>.txt`, replaces every `${key}`; throws
    `IllegalArgumentException` if the file is missing or a `${key}` is left unbound.
  - `LoggingMailSender.last(String to) → Optional<EmailMessage>` (test/dev hook).
- Consumes: nothing from earlier tasks.

- [ ] **Step 1: Add the dependency**

In `nadoumi-modules/nadoumi-identity/pom.xml`, inside `<dependencies>`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

- [ ] **Step 2: Write `MailTemplatesTest`**

```java
package com.nadoumi.identity.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nadoumi.identity.service.mail.MailTemplates;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MailTemplatesTest {

    private final MailTemplates templates = new MailTemplates();

    @Test
    void renders_otp_register_with_the_code() {
        String out = templates.render("otp-register", Map.of("otp", "482913", "ttlMinutes", "10"));
        assertThat(out).contains("482913").contains("10 minutes");
    }

    @Test
    void rejects_an_unbound_placeholder() {
        assertThatThrownBy(() -> templates.render("otp-register", Map.of("ttlMinutes", "10")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("otp");
    }
}
```

- [ ] **Step 3: Run it — verify it fails**

Run: `mvn -q -pl nadoumi-modules/nadoumi-identity -am test -Dtest=MailTemplatesTest`
Expected: FAIL — `MailTemplates` does not exist.

- [ ] **Step 4: Write the port + record**

`MailSender.java`:

```java
package com.nadoumi.identity.service.mail;

/** Outbound transactional email. Adapter chosen by {@code nadoumi.mail.transport}. */
public interface MailSender {
    void send(EmailMessage msg);
}
```

`EmailMessage.java`:

```java
package com.nadoumi.identity.service.mail;

public record EmailMessage(String to, String subject, String body) {}
```

- [ ] **Step 5: Write `MailTemplates`**

```java
package com.nadoumi.identity.service.mail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/** Loads {@code classpath:/mail/<name>.txt} and substitutes every {@code ${key}}. */
@Component
public class MailTemplates {

    private static final Pattern VAR = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)}");

    public String render(String template, Map<String, String> vars) {
        String raw = load(template);
        Matcher m = VAR.matcher(raw);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            String value = vars.get(key);
            if (value == null) {
                throw new IllegalArgumentException("mail template " + template + " missing var: " + key);
            }
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        return out.toString();
    }

    private String load(String template) {
        String path = "/mail/" + template + ".txt";
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("mail template not found: " + path);
            }
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new IllegalArgumentException("mail template unreadable: " + path, e);
        }
    }
}
```

- [ ] **Step 6: Write the three templates**

`resources/mail/otp-register.txt`:

```text
Welcome to Nadoumi.

Your email verification code is ${otp}.
It expires in ${ttlMinutes} minutes and can be used once.

If you did not start creating a Nadoumi account, ignore this message.
```

`resources/mail/otp-password-reset.txt`:

```text
We received a request to reset the password for your Nadoumi account.

Your password reset code is ${otp}.
It expires in ${ttlMinutes} minutes and can be used once.

If you did not request a reset, ignore this message; your password is unchanged.
```

`resources/mail/account-exists.txt`:

```text
Someone entered this email address when creating a Nadoumi account.

An account with this address already exists. If it was you, sign in at
${loginUrl} — or use "Forgot password" to reset it.

If this was not you, no action is needed.
```

- [ ] **Step 7: Write the two adapters**

`SmtpMailSender.java`:

```java
package com.nadoumi.identity.service.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Production adapter. Requires {@code spring.mail.host}. */
@Component
@ConditionalOnProperty(name = "nadoumi.mail.transport", havingValue = "smtp")
public class SmtpMailSender implements MailSender {

    private final JavaMailSender mail;
    private final String from;

    public SmtpMailSender(JavaMailSender mail,
            @org.springframework.beans.factory.annotation.Value("${nadoumi.mail.from}") String from) {
        this.mail = mail;
        this.from = from;
    }

    @Override
    public void send(EmailMessage msg) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(from);
        m.setTo(msg.to());
        m.setSubject(msg.subject());
        m.setText(msg.body());
        mail.send(m);
    }
}
```

`LoggingMailSender.java`:

```java
package com.nadoumi.identity.service.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Dev / CI / test adapter: no network. Keeps the last message per recipient. */
@Component
@ConditionalOnProperty(name = "nadoumi.mail.transport", havingValue = "log", matchIfMissing = true)
public class LoggingMailSender implements MailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailSender.class);
    private final ObjectMapper json = new ObjectMapper();
    private final Map<String, EmailMessage> lastByRecipient = new ConcurrentHashMap<>();
    private final Path outbox;

    public LoggingMailSender(@Value("${nadoumi.mail.logFile:./mail-outbox.log}") String logFile) {
        this.outbox = Path.of(logFile);
    }

    @Override
    public void send(EmailMessage msg) {
        lastByRecipient.put(msg.to().toLowerCase(), msg);
        try {
            Files.writeString(outbox, json.writeValueAsString(msg) + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        catch (IOException e) {
            log.warn("could not append to mail outbox {}", outbox, e);
        }
        log.info("[LoggingMailSender] to={} subject={}", msg.to(), msg.subject());
    }

    public Optional<EmailMessage> last(String to) {
        return Optional.ofNullable(lastByRecipient.get(to.toLowerCase()));
    }
}
```

- [ ] **Step 8: Write `LoggingMailSenderTest`**

```java
package com.nadoumi.identity.mail;

import static org.assertj.core.api.Assertions.assertThat;

import com.nadoumi.identity.service.mail.EmailMessage;
import com.nadoumi.identity.service.mail.LoggingMailSender;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

class LoggingMailSenderTest {

    @Test
    void remembers_the_last_message_per_recipient_and_appends_to_the_outbox(@TempDir Path dir) throws Exception {
        Path outbox = dir.resolve("out.log");
        LoggingMailSender sender = new LoggingMailSender(outbox.toString());

        sender.send(new EmailMessage("A@x.com", "one", "b1"));
        sender.send(new EmailMessage("a@x.com", "two", "b2"));

        assertThat(sender.last("a@x.com")).get().extracting(EmailMessage::subject).isEqualTo("two");
        assertThat(Files.readAllLines(outbox)).hasSize(2);
    }
}
```

- [ ] **Step 9: Confirm config**

The `spring.mail` + `nadoumi.mail` + `nadoumi.web` blocks were added to
`application.yml` in **Task E-0 Step 4**. Nothing to add here — just verify
`nadoumi.mail.transport` defaults to `log` so this task's tests and CI need no
SMTP server.

- [ ] **Step 10: Run the module tests**

Run: `mvn -q -pl nadoumi-modules/nadoumi-identity -am test -Dtest=MailTemplatesTest,LoggingMailSenderTest`
Expected: PASS.

- [ ] **Step 11: Commit**

```bash
git add nadoumi-modules/nadoumi-identity/pom.xml \
        nadoumi-modules/nadoumi-identity/src/main/java/com/nadoumi/identity/service/mail \
        nadoumi-modules/nadoumi-identity/src/main/resources/mail \
        nadoumi-modules/nadoumi-identity/src/test/java/com/nadoumi/identity/mail
git commit -m "feat(identity): SMTP-abstracted mail channel (log + smtp adapters, templates)"
```

---

### Task E-2: shared password policy

**Files:**
- Create: `ruoyi-common/src/main/java/com/ruoyi/common/utils/PasswordPolicy.java`
- Create: `ruoyi-common/src/test/java/com/ruoyi/common/utils/PasswordPolicyTest.java`

**Interfaces:**
- Produces: `PasswordPolicy.violation(String raw, String currentEncodedOrNull) →
  Optional<String>` — empty when the password is acceptable; otherwise a stable
  machine key: `"password.tooShort" | "password.tooLong" | "password.needUpper" |
  "password.needLower" | "password.needDigit" | "password.needSpecial" |
  "password.sameAsCurrent"`. First failing rule wins, checked in that order.
- Consumes: `com.ruoyi.common.utils.SecurityUtils.matchesPassword`.

- [ ] **Step 1: Write `PasswordPolicyTest`**

```java
package com.ruoyi.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void accepts_a_compliant_password() {
        assertThat(PasswordPolicy.violation("Abcdef1!", null)).isEmpty();
    }

    @Test
    void flags_each_rule() {
        assertThat(PasswordPolicy.violation("Ab1!", null)).contains("password.tooShort");
        assertThat(PasswordPolicy.violation("A".repeat(20) + "b1!aaaaaaaaaaaaa", null)).contains("password.tooLong");
        assertThat(PasswordPolicy.violation("abcdef1!", null)).contains("password.needUpper");
        assertThat(PasswordPolicy.violation("ABCDEF1!", null)).contains("password.needLower");
        assertThat(PasswordPolicy.violation("Abcdefg!", null)).contains("password.needDigit");
        assertThat(PasswordPolicy.violation("Abcdefg1", null)).contains("password.needSpecial");
    }

    @Test
    void rejects_reuse_of_the_current_password() {
        String encoded = SecurityUtils.encryptPassword("Abcdef1!");
        assertThat(PasswordPolicy.violation("Abcdef1!", encoded)).contains("password.sameAsCurrent");
    }
}
```

- [ ] **Step 2: Run it — verify it fails**

Run: `mvn -q -pl ruoyi-common test -Dtest=PasswordPolicyTest`
Expected: FAIL — `PasswordPolicy` does not exist.

- [ ] **Step 3: Write `PasswordPolicy`**

```java
package com.ruoyi.common.utils;

import java.util.Optional;

/**
 * Nadoumi password rules (spec D-R2-3): 8-32 chars, at least one upper, one lower,
 * one digit, one special; and not equal to the current password. Mirrored in
 * {@code nadoumi-web/app/utils/passwordPolicy.ts} with identical keys.
 */
public final class PasswordPolicy {

    public static final int MIN = 8;
    public static final int MAX = 32;
    private static final String SPECIALS = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

    private PasswordPolicy() {}

    public static Optional<String> violation(String raw, String currentEncoded) {
        if (raw == null || raw.length() < MIN) return Optional.of("password.tooShort");
        if (raw.length() > MAX) return Optional.of("password.tooLong");
        if (raw.chars().noneMatch(Character::isUpperCase)) return Optional.of("password.needUpper");
        if (raw.chars().noneMatch(Character::isLowerCase)) return Optional.of("password.needLower");
        if (raw.chars().noneMatch(Character::isDigit)) return Optional.of("password.needDigit");
        if (raw.chars().noneMatch(c -> SPECIALS.indexOf(c) >= 0)) return Optional.of("password.needSpecial");
        if (currentEncoded != null && SecurityUtils.matchesPassword(raw, currentEncoded)) {
            return Optional.of("password.sameAsCurrent");
        }
        return Optional.empty();
    }
}
```

- [ ] **Step 4: Run it — verify it passes**

Run: `mvn -q -pl ruoyi-common test -Dtest=PasswordPolicyTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add ruoyi-common/src/main/java/com/ruoyi/common/utils/PasswordPolicy.java \
        ruoyi-common/src/test/java/com/ruoyi/common/utils/PasswordPolicyTest.java
git commit -m "feat(common): shared PasswordPolicy validator (8-32, class rules, no reuse)"
```

---

### Task E-3: OTP + ticket services (Redis)

**Files:**
- Create: `nadoumi-modules/nadoumi-identity/src/main/java/com/nadoumi/identity/service/otp/OtpPurpose.java`
- Create: `.../service/otp/OtpService.java`
- Create: `.../service/otp/TicketService.java`
- Create: `.../service/otp/OtpException.java`
- Create: `nadoumi-modules/nadoumi-identity/src/test/java/com/nadoumi/identity/otp/OtpServiceTest.java`
- Create: `.../otp/TicketServiceTest.java`

**Interfaces:**
- Consumes: `com.ruoyi.common.core.redis.RedisCache` (bean;
  `setCacheObject(key,val,timeout,unit)`, `getCacheObject`, `deleteObject`, `hasKey`);
  `MailSender` + `MailTemplates` (Task E-1).
- Produces:
  - `enum OtpPurpose { REGISTER, PASSWORD_RESET }`
  - `OtpService.issue(String email, OtpPurpose purpose)` — void; generates a 6-digit
    code, stores `nad:otp:{purpose}:{sha}` (TTL 600 s), sets a 60 s cooldown key,
    sends the mail. If a cooldown key exists, returns silently (no new code, no mail).
  - `OtpService.verify(String email, OtpPurpose purpose, String code) → String` —
    returns a fresh ticket id on success; throws `OtpException` (→ 400) on
    missing/expired/mismatch; deletes the OTP key on success or on the 5th failure.
  - `TicketService.consume(String ticketId, OtpPurpose purpose) → String` — returns
    the bound email and deletes the key; throws `OtpException` if unknown/expired.
  - `OtpException extends RuntimeException` — mapped by `NadApiExceptionHandler` to a
    `400` problem+json (add a handler branch, or make it extend
    `NadBadRequestException`). **Make `OtpException extends NadBadRequestException`.**

- [ ] **Step 1: Write `OtpServiceTest`** (RedisCache mocked; fixed code via a seam)

```java
package com.nadoumi.identity.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.identity.service.mail.EmailMessage;
import com.nadoumi.identity.service.mail.MailSender;
import com.nadoumi.identity.service.mail.MailTemplates;
import com.nadoumi.identity.service.otp.OtpPurpose;
import com.nadoumi.identity.service.otp.OtpService;
import com.nadoumi.identity.service.otp.TicketService;
import com.ruoyi.common.core.redis.RedisCache;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class OtpServiceTest {

    private final RedisCache redis = mock(RedisCache.class);
    private final MailSender mail = mock(MailSender.class);
    private final TicketService tickets = mock(TicketService.class);
    // fixed generator so the test knows the code
    private final OtpService otp = new OtpService(redis, mail, new MailTemplates(), tickets,
            "https://web.local/login", () -> "482913");

    @Test
    void issue_stores_the_hash_with_ttl_and_sends_mail_when_no_cooldown() {
        when(redis.hasKey("nad:otp:cooldown:REGISTER:" + sha("a@x.com"))).thenReturn(false);

        otp.issue("a@x.com", OtpPurpose.REGISTER);

        verify(redis).setCacheObject(eq("nad:otp:REGISTER:" + sha("a@x.com")), any(), eq(600L), eq(TimeUnit.SECONDS));
        verify(redis).setCacheObject(eq("nad:otp:cooldown:REGISTER:" + sha("a@x.com")), any(), eq(60L), eq(TimeUnit.SECONDS));
        verify(mail).send(any(EmailMessage.class));
    }

    @Test
    void issue_is_silent_during_the_cooldown() {
        when(redis.hasKey("nad:otp:cooldown:REGISTER:" + sha("a@x.com"))).thenReturn(true);
        otp.issue("a@x.com", OtpPurpose.REGISTER);
        verify(redis, never()).setCacheObject(eq("nad:otp:REGISTER:" + sha("a@x.com")), any(), anyLong(), any());
        verify(mail, never()).send(any());
    }

    @Test
    void verify_returns_a_ticket_on_match_and_clears_the_otp() {
        String key = "nad:otp:PASSWORD_RESET:" + sha("a@x.com");
        when(redis.getCacheObject(key)).thenReturn(new OtpService.Entry(sha("482913"), 0));
        when(tickets.mint("a@x.com", OtpPurpose.PASSWORD_RESET)).thenReturn("tkt_1");

        assertThat(otp.verify("a@x.com", OtpPurpose.PASSWORD_RESET, "482913")).isEqualTo("tkt_1");
        verify(redis).deleteObject(key);
    }

    @Test
    void verify_throws_on_mismatch_and_deletes_after_the_fifth_failure() {
        String key = "nad:otp:REGISTER:" + sha("a@x.com");
        when(redis.getCacheObject(key)).thenReturn(new OtpService.Entry(sha("000000"), 4));
        assertThatThrownBy(() -> otp.verify("a@x.com", OtpPurpose.REGISTER, "482913"))
                .hasMessageContaining("code");
        verify(redis).deleteObject(key); // 5th attempt -> burned
    }

    @Test
    void verify_throws_when_absent() {
        when(redis.getCacheObject(any())).thenReturn(null);
        assertThatThrownBy(() -> otp.verify("a@x.com", OtpPurpose.REGISTER, "482913"))
                .hasMessageContaining("code");
    }

    private static String sha(String s) {
        return com.nadoumi.identity.service.otp.OtpService.sha256(s);
    }
}
```

- [ ] **Step 2: Run it — verify it fails**

Run: `mvn -q -pl nadoumi-modules/nadoumi-identity -am test -Dtest=OtpServiceTest`
Expected: FAIL — types missing.

- [ ] **Step 3: Write `OtpPurpose` + `OtpException`**

```java
package com.nadoumi.identity.service.otp;

public enum OtpPurpose { REGISTER, PASSWORD_RESET }
```

```java
package com.nadoumi.identity.service.otp;

import com.nadoumi.identity.exception.NadBadRequestException;

/** Bad / expired / exhausted OTP or ticket -> 400 problem+json. */
public class OtpException extends NadBadRequestException {
    public OtpException(String message) { super(message); }
}
```

- [ ] **Step 4: Write `TicketService`**

```java
package com.nadoumi.identity.service.otp;

import com.ruoyi.common.core.redis.RedisCache;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

/** Opaque single-use handle proving an email was OTP-verified for a purpose. */
@Service
public class TicketService {

    static final long TTL_SECONDS = 600;
    private static final SecureRandom RNG = new SecureRandom();

    private final RedisCache redis;

    public TicketService(RedisCache redis) { this.redis = redis; }

    public String mint(String email, OtpPurpose purpose) {
        byte[] raw = new byte[24];
        RNG.nextBytes(raw);
        String id = "tkt_" + HexFormat.of().formatHex(raw);
        redis.setCacheObject(key(purpose, id), email.toLowerCase(), TTL_SECONDS, TimeUnit.SECONDS);
        return id;
    }

    public String consume(String ticketId, OtpPurpose purpose) {
        String k = key(purpose, ticketId);
        String email = redis.getCacheObject(k);
        if (email == null) {
            throw new OtpException("verification ticket is invalid or expired");
        }
        redis.deleteObject(k);
        return email;
    }

    private static String key(OtpPurpose p, String id) {
        return "nad:ticket:" + p.name() + ":" + id;
    }
}
```

- [ ] **Step 5: Write `OtpService`**

```java
package com.nadoumi.identity.service.otp;

import com.nadoumi.identity.service.mail.EmailMessage;
import com.nadoumi.identity.service.mail.MailSender;
import com.nadoumi.identity.service.mail.MailTemplates;
import com.ruoyi.common.core.redis.RedisCache;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** 6-digit email OTP with TTL, attempt cap, and resend cooldown. Redis-backed. */
@Service
public class OtpService {

    static final long TTL_SECONDS = 600;
    static final long COOLDOWN_SECONDS = 60;
    static final int MAX_ATTEMPTS = 5;

    public record Entry(String codeHash, int attempts) implements Serializable {}

    private final RedisCache redis;
    private final MailSender mail;
    private final MailTemplates templates;
    private final TicketService tickets;
    private final String loginUrl;
    private final Supplier<String> codeGen;

    public OtpService(RedisCache redis, MailSender mail, MailTemplates templates, TicketService tickets,
            @Value("${nadoumi.web.loginUrl:https://nadoumi.local/login}") String loginUrl) {
        this(redis, mail, templates, tickets, loginUrl, OtpService::random6);
    }

    OtpService(RedisCache redis, MailSender mail, MailTemplates templates, TicketService tickets,
            String loginUrl, Supplier<String> codeGen) {
        this.redis = redis;
        this.mail = mail;
        this.templates = templates;
        this.tickets = tickets;
        this.loginUrl = loginUrl;
        this.codeGen = codeGen;
    }

    public void issue(String email, OtpPurpose purpose) {
        String cd = cooldownKey(purpose, email);
        if (Boolean.TRUE.equals(redis.hasKey(cd))) {
            return; // still cooling down — silent
        }
        String code = codeGen.get();
        redis.setCacheObject(otpKey(purpose, email), new Entry(sha256(code), 0), TTL_SECONDS, TimeUnit.SECONDS);
        redis.setCacheObject(cd, "1", COOLDOWN_SECONDS, TimeUnit.SECONDS);
        String template = purpose == OtpPurpose.REGISTER ? "otp-register" : "otp-password-reset";
        String subject = purpose == OtpPurpose.REGISTER ? "Verify your email" : "Reset your password";
        mail.send(new EmailMessage(email, subject,
                templates.render(template, Map.of("otp", code, "ttlMinutes", String.valueOf(TTL_SECONDS / 60)))));
    }

    public String verify(String email, OtpPurpose purpose, String code) {
        String key = otpKey(purpose, email);
        Entry entry = redis.getCacheObject(key);
        if (entry == null) {
            throw new OtpException("verification code is invalid or expired");
        }
        boolean ok = MessageDigest.isEqual(
                entry.codeHash().getBytes(StandardCharsets.UTF_8),
                sha256(code).getBytes(StandardCharsets.UTF_8));
        if (!ok) {
            int next = entry.attempts() + 1;
            if (next >= MAX_ATTEMPTS) {
                redis.deleteObject(key);
            }
            else {
                redis.setCacheObject(key, new Entry(entry.codeHash(), next), TTL_SECONDS, TimeUnit.SECONDS);
            }
            throw new OtpException("verification code is invalid or expired");
        }
        redis.deleteObject(key);
        return tickets.mint(email, purpose);
    }

    public void sendAccountExists(String email) {
        mail.send(new EmailMessage(email, "Your Nadoumi account",
                templates.render("account-exists", Map.of("loginUrl", loginUrl))));
    }

    private static String otpKey(OtpPurpose p, String email) {
        return "nad:otp:" + p.name() + ":" + sha256(email.toLowerCase());
    }

    private static String cooldownKey(OtpPurpose p, String email) {
        return "nad:otp:cooldown:" + p.name() + ":" + sha256(email.toLowerCase());
    }

    private static String random6() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    public static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
```

- [ ] **Step 6: Write `TicketServiceTest`**

```java
package com.nadoumi.identity.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.identity.service.otp.OtpPurpose;
import com.nadoumi.identity.service.otp.TicketService;
import com.ruoyi.common.core.redis.RedisCache;
import org.junit.jupiter.api.Test;

class TicketServiceTest {

    private final RedisCache redis = mock(RedisCache.class);
    private final TicketService tickets = new TicketService(redis);

    @Test
    void consume_returns_the_email_then_deletes_the_key() {
        when(redis.<String>getCacheObject(org.mockito.ArgumentMatchers.startsWith("nad:ticket:REGISTER:")))
                .thenReturn("a@x.com");
        String email = tickets.consume("tkt_abc", OtpPurpose.REGISTER);
        assertThat(email).isEqualTo("a@x.com");
        verify(redis).deleteObject("nad:ticket:REGISTER:tkt_abc");
    }

    @Test
    void consume_throws_when_the_ticket_is_gone() {
        when(redis.getCacheObject(org.mockito.ArgumentMatchers.any())).thenReturn(null);
        assertThatThrownBy(() -> tickets.consume("tkt_x", OtpPurpose.PASSWORD_RESET))
                .hasMessageContaining("ticket");
    }
}
```

- [ ] **Step 7: Run the otp tests**

Run: `mvn -q -pl nadoumi-modules/nadoumi-identity -am test -Dtest=OtpServiceTest,TicketServiceTest`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add nadoumi-modules/nadoumi-identity/src/main/java/com/nadoumi/identity/service/otp \
        nadoumi-modules/nadoumi-identity/src/test/java/com/nadoumi/identity/otp
git commit -m "feat(identity): Redis OTP + single-use ticket services"
```

---

### Task E-4: session revocation helper

**Files:**
- Create: `nadoumi-modules/nadoumi-identity/src/main/java/com/nadoumi/identity/access/SessionRevoker.java`
- Create: `nadoumi-modules/nadoumi-identity/src/test/java/com/nadoumi/identity/access/SessionRevokerTest.java`

**Interfaces:**
- Consumes: `RedisCache.keys(String pattern) → Collection<String>`,
  `RedisCache.getCacheObject`, `RedisCache.deleteObject`;
  `com.ruoyi.common.constant.CacheConstants.LOGIN_TOKEN_KEY` (`"login_tokens:"`);
  `com.ruoyi.common.core.domain.model.LoginUser` (`getUserId()`, `getToken()`).
- Produces: `SessionRevoker.revokeAll(Long userId, String exceptTokenOrNull) → int`
  (count deleted).

- [ ] **Step 1: Write `SessionRevokerTest`**

```java
package com.nadoumi.identity.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import java.util.List;
import org.junit.jupiter.api.Test;

class SessionRevokerTest {

    private final RedisCache redis = mock(RedisCache.class);
    private final SessionRevoker revoker = new SessionRevoker(redis);

    private LoginUser lu(long userId, String token) {
        LoginUser u = new LoginUser();
        u.setUserId(userId);
        u.setToken(token);
        return u;
    }

    @Test
    void deletes_every_session_for_the_user_except_the_caller() {
        String a = CacheConstants.LOGIN_TOKEN_KEY + "aaa";
        String b = CacheConstants.LOGIN_TOKEN_KEY + "bbb";
        String c = CacheConstants.LOGIN_TOKEN_KEY + "ccc";
        when(redis.keys(CacheConstants.LOGIN_TOKEN_KEY + "*")).thenReturn(List.of(a, b, c));
        when(redis.getCacheObject(a)).thenReturn(lu(7L, "aaa"));
        when(redis.getCacheObject(b)).thenReturn(lu(7L, "bbb"));
        when(redis.getCacheObject(c)).thenReturn(lu(9L, "ccc"));

        int n = revoker.revokeAll(7L, "aaa");

        assertThat(n).isEqualTo(1);
        verify(redis).deleteObject(b);
        verify(redis, never()).deleteObject(a);
        verify(redis, never()).deleteObject(c);
    }
}
```

- [ ] **Step 2: Run it — verify it fails**

Run: `mvn -q -pl nadoumi-modules/nadoumi-identity -am test -Dtest=SessionRevokerTest`
Expected: FAIL — `SessionRevoker` missing.

- [ ] **Step 3: Write `SessionRevoker`**

```java
package com.nadoumi.identity.access;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import java.util.Collection;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Deletes RuoYi Redis sessions ({@code login_tokens:*}) for one user. */
@Component
public class SessionRevoker {

    private final RedisCache redis;

    public SessionRevoker(RedisCache redis) { this.redis = redis; }

    public int revokeAll(Long userId, String exceptToken) {
        Collection<String> keys = redis.keys(CacheConstants.LOGIN_TOKEN_KEY + "*");
        if (keys == null) return 0;
        int deleted = 0;
        for (String key : keys) {
            LoginUser lu = redis.getCacheObject(key);
            if (lu == null || !Objects.equals(lu.getUserId(), userId)) continue;
            if (exceptToken != null && exceptToken.equals(lu.getToken())) continue;
            redis.deleteObject(key);
            deleted++;
        }
        return deleted;
    }
}
```

- [ ] **Step 4: Run it — verify it passes**

Run: `mvn -q -pl nadoumi-modules/nadoumi-identity -am test -Dtest=SessionRevokerTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add nadoumi-modules/nadoumi-identity/src/main/java/com/nadoumi/identity/access/SessionRevoker.java \
        nadoumi-modules/nadoumi-identity/src/test/java/com/nadoumi/identity/access/SessionRevokerTest.java
git commit -m "feat(identity): SessionRevoker — kill a user's other Redis sessions"
```

---

### Task E-5: email-first register + login + `email_verified` migration

**Files:**
- Modify: `.../web/request/StudentRegisterRequest.java`
- Modify: `.../web/request/StudentLoginRequest.java`
- Modify: `.../service/StudentAuthService.java`
- Modify: `.../mapper/NadIdentityMapper.java` + `.../resources/mapper/identity/NadIdentityMapper.xml` (add `selectUserIdByEmailAndType`, `markEmailVerified`)
- Create: `ruoyi-admin/src/main/resources/db/migration/V7__nad_student_email_verified.sql`
- Modify: `nadoumi-modules/nadoumi-identity/src/test/java/com/nadoumi/identity/.../StudentAuthServiceTest.java` (create if absent)

**Interfaces:**
- Consumes: `TicketService.consume` (E-3), `PasswordPolicy.violation` (E-2),
  `OtpService.sendAccountExists` (E-3).
- Produces:
  - `StudentRegisterRequest` = `record(String firstName, String lastName, String email, String password, String ticket)` — `firstName`/`lastName` `@NotBlank @Size(max=100)`, `email` `@NotBlank @Email @Size(max=120)`, `password` `@NotBlank`, `ticket` `@NotBlank`.
  - `StudentLoginRequest` = `record(String email, String password, String code, String uuid)` — `email` `@NotBlank @Email`, `password` `@NotBlank`.
  - `StudentAuthService.register` — consumes the ticket (must match `email`), runs
    `PasswordPolicy`, generates a unique `user_name`, sets `nick_name = first + " " + last`,
    inserts, `markEmailVerified(userId)`, returns `StudentRegisterResponse` unchanged.
  - `StudentAuthService.login` — resolves `user_name` from the verified email, else
    throws the same generic bad-credentials `NadBadRequestException` used for a wrong
    password; then delegates to `SysLoginService.login`.

- [ ] **Step 1: Write `V7__nad_student_email_verified.sql`**

```sql
-- Student identity is email-first (spec Revision 2, D-R2-2).
ALTER TABLE sys_user
    ADD COLUMN email_verified tinyint(1) NOT NULL DEFAULT 0 COMMENT '1 = email confirmed via OTP';

-- Support fast lookup by email for the student login path. Not UNIQUE at the DB
-- level (staff rows may legitimately share/blank emails); uniqueness among
-- user_type='10' is enforced in StudentAuthService.register.
CREATE INDEX idx_sys_user_email ON sys_user (email);
```

- [ ] **Step 2: Update `StudentRegisterRequest`**

```java
package com.nadoumi.identity.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentRegisterRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank String password,
        @NotBlank String ticket) {
}
```

- [ ] **Step 3: Update `StudentLoginRequest`**

```java
package com.nadoumi.identity.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record StudentLoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        String code,
        String uuid) {
}
```

- [ ] **Step 4: Add mapper methods**

`NadIdentityMapper.java`:

```java
Long selectUserIdByEmailAndType(@Param("email") String email, @Param("userType") String userType);
int markEmailVerified(@Param("userId") Long userId);
```

`NadIdentityMapper.xml`:

```xml
<select id="selectUserIdByEmailAndType" resultType="java.lang.Long">
    SELECT user_id FROM sys_user
    WHERE email = #{email} AND user_type = #{userType} AND del_flag = '0'
    LIMIT 1
</select>

<update id="markEmailVerified">
    UPDATE sys_user SET email_verified = 1 WHERE user_id = #{userId}
</update>
```

- [ ] **Step 5: Rewrite `StudentAuthService.register` / `login`**

```java
@Transactional
public StudentRegisterResponse register(StudentRegisterRequest req) {
    if (!"true".equalsIgnoreCase(configService.selectConfigByKey(REGISTER_ENABLED_KEY))) {
        throw new NadForbiddenException("student registration is disabled");
    }
    String verifiedEmail = tickets.consume(req.ticket(), OtpPurpose.REGISTER);
    if (!verifiedEmail.equalsIgnoreCase(req.email())) {
        throw new NadBadRequestException("verification does not match this email");
    }
    PasswordPolicy.violation(req.password(), null)
            .ifPresent(key -> { throw new NadBadRequestException(key); });
    if (identityMapper.selectUserIdByEmailAndType(req.email().toLowerCase(), STUDENT_USER_TYPE) != null) {
        throw new NadBadRequestException("email already registered");
    }
    SysUser user = new SysUser();
    user.setUserName(generateStudentHandle(req.email()));
    user.setNickName(req.firstName().trim() + " " + req.lastName().trim());
    user.setEmail(req.email().toLowerCase());
    user.setPassword(SecurityUtils.encryptPassword(req.password()));
    user.setPwdUpdateDate(DateUtils.getNowDate());
    if (!userService.registerUser(user)) {
        throw new NadBadRequestException("registration failed");
    }
    identityMapper.updateUserType(user.getUserId(), STUDENT_USER_TYPE);
    identityMapper.markEmailVerified(user.getUserId());
    return new StudentRegisterResponse(user.getUserId(), user.getUserName());
}

@Transactional
public String login(StudentLoginRequest req) {
    String email = req.email().toLowerCase();
    Long userId = identityMapper.selectUserIdByEmailAndType(email, STUDENT_USER_TYPE);
    if (userId == null) {
        // do not disclose whether the email exists
        throw new NadBadRequestException("email or password is incorrect");
    }
    SysUser user = userService.selectUserById(userId);
    String token = loginService.login(user.getUserName(), req.password(), req.code(), req.uuid());
    grants.acceptInvitesFor(userId, user.getEmail());
    return token;
}

/** Deterministic-ish unique internal handle; never shown to the student. */
private String generateStudentHandle(String email) {
    String base = "stu_" + email.toLowerCase().replaceAll("[^a-z0-9]", "").replaceAll("^(.{0,14}).*$", "$1");
    String candidate = base;
    int n = 0;
    SysUser probe = new SysUser();
    while (true) {
        probe.setUserName(candidate);
        if (userService.checkUserNameUnique(probe)) return candidate;
        candidate = base + (++n);
    }
}
```

Add fields/imports: `private final TicketService tickets;` (constructor param),
`import com.ruoyi.common.utils.PasswordPolicy;`,
`import com.nadoumi.identity.service.otp.OtpPurpose;`,
`import com.nadoumi.identity.service.otp.TicketService;`. Remove the old
staff-rejection block that keyed off `username` — resolve `user_type` from the
looked-up `userId` instead (a student email will never resolve to a staff row given
the `user_type='10'` filter, so the check is now redundant; keep a guard that the
resolved row is `user_type != '00'`).

- [ ] **Step 6: Write / update `StudentAuthServiceTest`** (unit, mocked collaborators)

```java
package com.nadoumi.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.nadoumi.identity.service.otp.OtpException;
import com.nadoumi.identity.service.otp.OtpPurpose;
import com.nadoumi.identity.service.otp.TicketService;
import com.nadoumi.identity.web.request.StudentLoginRequest;
import com.nadoumi.identity.web.request.StudentRegisterRequest;
import com.ruoyi.common.core.domain.entity.SysUser;
import org.junit.jupiter.api.Test;

class StudentAuthServiceTest {
    // wire the SUT with all-mock collaborators; only the two methods above are exercised.
    // ... mocks: configService, userService, loginService, identityMapper, grants, tickets ...

    @Test
    void register_requires_a_ticket_that_matches_the_email() {
        // configService REGISTER_ENABLED_KEY -> "true"
        // tickets.consume("tkt", REGISTER) -> "someone-else@x.com"
        // expect NadBadRequestException "verification does not match this email"
    }

    @Test
    void register_rejects_a_weak_password() {
        // tickets.consume -> "a@x.com"; password "weak" -> policy violation -> 400 "password.tooShort"
    }

    @Test
    void register_rejects_a_duplicate_email() {
        // identityMapper.selectUserIdByEmailAndType("a@x.com","10") -> 5L -> 400 "email already registered"
    }

    @Test
    void login_with_an_unknown_email_is_the_same_error_as_a_wrong_password() {
        // identityMapper.selectUserIdByEmailAndType -> null
        // expect NadBadRequestException "email or password is incorrect"
    }

    @Test
    void login_resolves_the_handle_and_delegates_to_SysLoginService() {
        // selectUserIdByEmailAndType -> 7L; userService.selectUserById(7) -> user(userName="stu_ax")
        // loginService.login("stu_ax","pw",null,null) -> "jwt"
        // assert returns "jwt" and grants.acceptInvitesFor(7L, email) called
    }
}
```

> Fill each test body with the mock stubbing shown in the comment — the harness for
> this SUT already exists in the module's test sources for the other
> `StudentAuthService` methods; copy that setup.

- [ ] **Step 7: Run module tests + a compile of the whole backend**

Run: `mvn -q -pl nadoumi-modules/nadoumi-identity -am test -Dtest=StudentAuthServiceTest`
then `mvn -q -DskipTests package` (ensures the DTO signature change compiles
everywhere — `StudentAuthController` still passes `req` straight through, no change
needed there).
Expected: PASS / BUILD SUCCESS.

- [ ] **Step 8: Commit**

```bash
git add nadoumi-modules/nadoumi-identity/src/main/java/com/nadoumi/identity/web/request/StudentRegisterRequest.java \
        nadoumi-modules/nadoumi-identity/src/main/java/com/nadoumi/identity/web/request/StudentLoginRequest.java \
        nadoumi-modules/nadoumi-identity/src/main/java/com/nadoumi/identity/service/StudentAuthService.java \
        nadoumi-modules/nadoumi-identity/src/main/java/com/nadoumi/identity/mapper/NadIdentityMapper.java \
        nadoumi-modules/nadoumi-identity/src/main/resources/mapper/identity/NadIdentityMapper.xml \
        ruoyi-admin/src/main/resources/db/migration/V7__nad_student_email_verified.sql \
        nadoumi-modules/nadoumi-identity/src/test/java/com/nadoumi/identity/service/StudentAuthServiceTest.java
git commit -m "feat(identity): email-first student register/login + email_verified (V7)"
```

---

### Task E-6: OTP + password endpoints

**Files:**
- Create: `.../web/StudentEmailOtpController.java`
- Create: `.../web/StudentPasswordController.java`
- Create: `.../web/DevMailController.java`
- Create: `.../web/request/EmailOtpRequest.java`, `EmailOtpVerifyRequest.java`, `PasswordResetRequest.java`, `ChangePasswordRequest.java`
- Create: `.../web/response/OtpSentResponse.java`, `TicketResponse.java`
- Modify: `.../service/StudentAuthService.java` (add `changePassword`, `resetPassword`)
- Create: `.../web/StudentPasswordControllerTest.java`, `.../web/EmailOtpControllerTest.java` (full-context `*Test`, self-skipping without Docker/Redis per the module's existing pattern) — or slice tests with `@WebMvcTest` + mocked services if the module already uses that style.

**Interfaces:**
- Consumes: `OtpService` (E-3), `TicketService` (E-3), `PasswordPolicy` (E-2),
  `SessionRevoker` (E-4), `StudentAuthService`, `CurrentCaller.requireUserId()`,
  `TokenService`, `ISysUserService.resetUserPwd`.
- Produces:
  - `POST /api/student/email-otp` `@Anonymous` `@RateLimiter(count=5,time=3600)` —
    body `EmailOtpRequest { @NotBlank @Email email; @NotNull OtpPurpose purpose; String code; String uuid }`.
    When `configService.selectCaptchaEnabled()` → `loginService.validateCaptcha(email, code, uuid)`.
    If `purpose==REGISTER` and the email already exists → `otpService.sendAccountExists(email)`.
    Else `otpService.issue(email, purpose)`. Always `200 OtpSentResponse(true)`.
  - `POST /api/student/email-otp/verify` `@Anonymous` `@RateLimiter(count=10,time=600)` —
    body `EmailOtpVerifyRequest { email, purpose, @NotBlank otp }` → `200 TicketResponse(otpService.verify(...))`.
  - `POST /api/student/password/reset` `@Anonymous` `@RateLimiter(count=10,time=600)` —
    body `PasswordResetRequest { @NotBlank ticket; @NotBlank newPassword }` →
    `service.resetPassword(...)` → `204`.
  - `POST /api/student/password` (bearer) — body
    `ChangePasswordRequest { @NotBlank currentPassword; @NotBlank newPassword }` →
    `service.changePassword(...)` → `204`.
  - `GET /api/dev/mail/latest?to=` `@Anonymous` `@ConditionalOnProperty(nadoumi.mail.transport=log)` →
    `200 { to, subject, body }` or `404`.

- [ ] **Step 1: Request/response records**

```java
// EmailOtpRequest.java
public record EmailOtpRequest(@NotBlank @Email String email, @NotNull OtpPurpose purpose,
        String code, String uuid) {}
// EmailOtpVerifyRequest.java
public record EmailOtpVerifyRequest(@NotBlank @Email String email, @NotNull OtpPurpose purpose,
        @NotBlank String otp) {}
// PasswordResetRequest.java
public record PasswordResetRequest(@NotBlank String ticket, @NotBlank String newPassword) {}
// ChangePasswordRequest.java
public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {}
// OtpSentResponse.java
public record OtpSentResponse(boolean sent) {}
// TicketResponse.java
public record TicketResponse(String ticket) {}
```

- [ ] **Step 2: `StudentAuthService.resetPassword` / `changePassword`**

```java
@Transactional
public void resetPassword(String ticket, String newPassword) {
    String email = tickets.consume(ticket, OtpPurpose.PASSWORD_RESET);
    Long userId = identityMapper.selectUserIdByEmailAndType(email, STUDENT_USER_TYPE);
    if (userId == null) {
        // ticket was valid, but the account vanished — treat as done, reveal nothing
        return;
    }
    PasswordPolicy.violation(newPassword, null)
            .ifPresent(key -> { throw new NadBadRequestException(key); });
    userService.resetUserPwd(userId, SecurityUtils.encryptPassword(newPassword));
    identityMapper.touchPwdUpdateDate(userId);
    sessionRevoker.revokeAll(userId, null);
}

public void changePassword(HttpServletRequest request, String current, String next) {
    Long userId = caller.requireUserId();
    SysUser user = userService.selectUserById(userId);
    if (!SecurityUtils.matchesPassword(current, user.getPassword())) {
        throw new NadBadRequestException("current password is incorrect");
    }
    PasswordPolicy.violation(next, user.getPassword())
            .ifPresent(key -> { throw new NadBadRequestException(key); });
    userService.resetUserPwd(userId, SecurityUtils.encryptPassword(next));
    identityMapper.touchPwdUpdateDate(userId);
    LoginUser me = tokenService.getLoginUser(request);
    sessionRevoker.revokeAll(userId, me != null ? me.getToken() : null);
    if (me != null) {
        me.getUser().setPassword(SecurityUtils.encryptPassword(next));
        tokenService.setLoginUser(me);
    }
}
```

Add `touchPwdUpdateDate` to the mapper: `UPDATE sys_user SET pwd_update_date = now() WHERE user_id = #{userId}`.
Add constructor params `SessionRevoker sessionRevoker`, `OtpService otpService` where needed.

- [ ] **Step 3: `StudentEmailOtpController`**

```java
@RestController
@RequestMapping("/api/student/email-otp")
public class StudentEmailOtpController {

    private final OtpService otp;
    private final StudentAuthService auth;      // exposes emailExists(email) helper
    private final ISysConfigService config;
    private final SysLoginService login;

    // constructor ...

    @Anonymous
    @PostMapping
    @RateLimiter(count = 5, time = 3600)
    public OtpSentResponse request(@Valid @RequestBody EmailOtpRequest req) {
        if (config.selectCaptchaEnabled()) {
            login.validateCaptcha(req.email(), req.code(), req.uuid());
        }
        if (req.purpose() == OtpPurpose.REGISTER && auth.studentEmailExists(req.email())) {
            otp.sendAccountExists(req.email());
        }
        else {
            otp.issue(req.email(), req.purpose());
        }
        return new OtpSentResponse(true);
    }

    @Anonymous
    @PostMapping("/verify")
    @RateLimiter(count = 10, time = 600)
    public TicketResponse verify(@Valid @RequestBody EmailOtpVerifyRequest req) {
        return new TicketResponse(otp.verify(req.email(), req.purpose(), req.otp()));
    }
}
```

Add `StudentAuthService.studentEmailExists(String email)` =
`identityMapper.selectUserIdByEmailAndType(email.toLowerCase(), STUDENT_USER_TYPE) != null`.

- [ ] **Step 4: `StudentPasswordController`**

```java
@RestController
@RequestMapping("/api/student/password")
public class StudentPasswordController {

    private final StudentAuthService service;
    // constructor ...

    @Anonymous
    @PostMapping("/reset")
    @RateLimiter(count = 10, time = 600)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody PasswordResetRequest req) {
        service.resetPassword(req.ticket(), req.newPassword());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void change(HttpServletRequest request, @Valid @RequestBody ChangePasswordRequest req) {
        service.changePassword(request, req.currentPassword(), req.newPassword());
    }
}
```

- [ ] **Step 5: `DevMailController`**

```java
@RestController
@RequestMapping("/api/dev/mail")
@ConditionalOnProperty(name = "nadoumi.mail.transport", havingValue = "log", matchIfMissing = true)
public class DevMailController {

    private final LoggingMailSender sender;
    // constructor ...

    @Anonymous
    @GetMapping("/latest")
    public ResponseEntity<EmailMessage> latest(@RequestParam String to) {
        return sender.last(to).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
```

- [ ] **Step 6: Tests**

`EmailOtpControllerTest` (MockMvc, services mocked):

```java
@Test void existing_email_still_returns_200_sent_true() { /* auth.studentEmailExists -> true; expect 200 {"sent":true}; verify otp.issue NOT called, otp.sendAccountExists called */ }
@Test void unknown_email_issues_an_otp() { /* studentEmailExists -> false; verify otp.issue(email, REGISTER) */ }
@Test void verify_returns_the_ticket() { /* otp.verify -> "tkt_1"; expect 200 {"ticket":"tkt_1"} */ }
@Test void captcha_is_enforced_when_enabled() { /* config.selectCaptchaEnabled -> true; verify login.validateCaptcha(...) */ }
```

`StudentPasswordControllerTest`:

```java
@Test void reset_applies_the_new_password_and_returns_204() { /* verify service.resetPassword(ticket,newPw); status 204 */ }
@Test void reset_rejects_a_weak_password_with_400() { /* service throws NadBadRequestException("password.needSpecial"); expect 400 problem+json detail contains password.needSpecial */ }
@Test void change_requires_authentication() { /* no bearer -> 401 */ }
@Test void change_returns_204_on_success() { /* verify service.changePassword(...); 204 */ }
```

Service-level revocation behaviour is covered by `SessionRevokerTest` (E-4) plus a
`StudentAuthServiceTest` case: `resetPassword` calls `sessionRevoker.revokeAll(userId, null)`
and issues no token; `changePassword` calls `revokeAll(userId, callerToken)`.

- [ ] **Step 7: Run + full package**

Run: `mvn -q -pl nadoumi-modules/nadoumi-identity -am test -Dtest=EmailOtpControllerTest,StudentPasswordControllerTest,StudentAuthServiceTest`
then `mvn -q -DskipTests package`.
Expected: PASS / BUILD SUCCESS.

- [ ] **Step 8: Commit**

```bash
git add nadoumi-modules/nadoumi-identity/src/main/java/com/nadoumi/identity/web \
        nadoumi-modules/nadoumi-identity/src/main/java/com/nadoumi/identity/service/StudentAuthService.java \
        nadoumi-modules/nadoumi-identity/src/main/java/com/nadoumi/identity/mapper/NadIdentityMapper.java \
        nadoumi-modules/nadoumi-identity/src/main/resources/mapper/identity/NadIdentityMapper.xml \
        nadoumi-modules/nadoumi-identity/src/test/java/com/nadoumi/identity/web
git commit -m "feat(identity): email-otp + password reset/change endpoints with session revocation"
```

---

### Task E-7: remove the "initial password" nag

> **`almousleck` + disabled `admin` already exist** in
> `V5__nadoumi_baseline_seed.sql` (user_id 3, role `nadoumi_super_admin`; `admin`
> `status='1'`). The seed BCrypt hash **is verified** to be `Nadoumi2026#`. Do **not**
> re-seed. This task only removes the confusing RuoYi initial-password messaging and
> keeps a forced first-login rotation via the password-expiry path (spec §17).

**Files:**
- Create: `ruoyi-admin/src/main/resources/db/migration/V7b__drop_initial_password_nag.sql`
  (numbering: use the next free `V<n>` — after Task E-5's `V7`, so `V8`; check
  `db/migration/` at execution time and rename accordingly)
- Modify: `ruoyi-ui/src/store/modules/user.js` (remove the initial-password prompt block)
- Modify: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/system/SysLoginController.java` (`initPasswordIsModify` → `return false;`)
- Create: `ruoyi-admin/src/test/java/com/ruoyi/web/AdminBootstrapIT.java`

**Interfaces:**
- Produces: `getInfo` no longer emits an initial-password flag; `almousleck` is still
  forced to rotate on first login (password expiry, `pwd_update_date IS NULL`).

- [ ] **Step 1: Write the migration** (`V8__drop_initial_password_nag.sql`, or the
  next free number)

```sql
-- Revision 2 §17: remove the confusing RuoYi "your password is still the initial
-- password" prompt. Forced first-login rotation for seeded accounts (almousleck,
-- pwd_update_date IS NULL) now comes from the password-expiry path instead.
UPDATE sys_config SET config_value = '0'  WHERE config_key = 'sys.account.initPasswordModify';
UPDATE sys_config SET config_value = '90' WHERE config_key = 'sys.account.passwordValidateDays';
```

- [ ] **Step 2: Remove the initial-password prompt (`ruoyi-ui`)**

In `ruoyi-ui/src/store/modules/user.js`, delete the block guarded by the
`/* 初始密码提示 */` comment that calls
`MessageBox.confirm('您的密码还是初始密码，请修改密码！', ...)`. Leave the rest of
`GetInfo` intact.

- [ ] **Step 3: Neutralise `initPasswordIsModify`**

`SysLoginController.initPasswordIsModify(Date)` → `return false;` (keep the method so
`getInfo` still compiles; drop the `configService` read). Comment: *"Nadoumi uses
PasswordPolicy + pwd_update_date expiry instead of the RuoYi initial-password nag
(spec Revision 2 §17)."*

- [ ] **Step 4: Test**

```java
// AdminBootstrapIT — against the migrated schema (self-skipping without a DB)
@Test void admin_account_is_disabled() {
    // SELECT status FROM sys_user WHERE user_name='admin' -> '1'
}
@Test void almousleck_is_the_super_admin() {
    // sys_user_role JOIN -> role_key 'nadoumi_super_admin'; user_type='00'; status='0'
}
@Test void initial_password_modify_flag_is_off() {
    // SELECT config_value FROM sys_config WHERE config_key='sys.account.initPasswordModify' -> '0'
}
```

- [ ] **Step 5: Run**

Run: `mvn -q -pl ruoyi-admin -am -Dtest=AdminBootstrapIT test` (skips cleanly with no
DB). Start the app once against a scratch DB to confirm Flyway applies the new
migration cleanly.

- [ ] **Step 6: Commit**

```bash
git add ruoyi-admin/src/main/resources/db/migration/V8__drop_initial_password_nag.sql \
        ruoyi-ui/src/store/modules/user.js \
        ruoyi-admin/src/main/java/com/ruoyi/web/controller/system/SysLoginController.java \
        ruoyi-admin/src/test/java/com/ruoyi/web/AdminBootstrapIT.java
git commit -m "feat(admin): drop the RuoYi initial-password nag; expiry-based rotation instead"
```

---

# PART F — Auth-flow rebuild, navbar, dashboard polish (Revision 2)

Produces: the shared OTP UI, the four BFF passthroughs, the two-step `/register`, the
real `/forgot-password`, the navbar correction, the real `/dashboard/account`, and
the `/dashboard` overview polish. Nuxt/Vitest rules exactly as Parts A–D. Every task
ends green: `pnpm lint && pnpm test && pnpm build` from `nadoumi-web/`.

**New files for Part F**

| Path | Responsibility |
| --- | --- |
| `app/utils/passwordPolicy.ts` | TS mirror of `PasswordPolicy` — same rules, returns an i18n key or `null`. |
| `app/components/auth/OtpInput.vue` | 6-box code input; paste-fill; `v-model` string; `@complete`. |
| `app/components/auth/EmailVerifyStep.vue` | email field + `[Verify]` + `OtpInput` + resend cooldown; drives `useOtp`. |
| `app/composables/useOtp.ts` | `request(email, purpose)`, `verify(email, purpose, otp) → ticket`, `cooldown` ref. |
| `app/components/dashboard/PasswordChangeForm.vue` | current/new/confirm; policy validation; emits `submit`. |
| `app/components/dashboard/AsyncState.vue` | slot wrapper: `pending` / `error` / `empty` / default. |
| `server/api/student-email-otp.post.ts` | → `POST {backend}/api/student/email-otp`. |
| `server/api/student-email-otp-verify.post.ts` | → `POST {backend}/api/student/email-otp/verify`. |
| `server/api/student-password-reset.post.ts` | → `POST {backend}/api/student/password/reset`. |
| `server/api/student-password.post.ts` | authed (cookie required) → `POST {backend}/api/student/password`. |
| `tests/e2e/forgot-password.spec.ts` | second hard-gate journey. |

---

### Task F-1: TS password policy mirror

**Files:**
- Create: `nadoumi-web/app/utils/passwordPolicy.ts`
- Create: `nadoumi-web/tests/unit/passwordPolicy.test.ts`
- Modify: `nadoumi-web/i18n/locales/en.json` (+ `fr`/`ar`/`zh` mirror) — add a
  `validation.password.*` group

**Interfaces:**
- Produces: `passwordPolicyKey(value: string, current?: string): string | null` —
  returns `null` when acceptable, else one of
  `'validation.password.tooShort' | '...tooLong' | '...needUpper' | '...needLower' |
  '...needDigit' | '...needSpecial' | '...sameAsCurrent'`. Same order as the Java
  `PasswordPolicy` (Task E-2).

- [ ] **Step 1: Write `tests/unit/passwordPolicy.test.ts`**

```ts
import { describe, it, expect } from 'vitest'
import { passwordPolicyKey } from '~/utils/passwordPolicy'

describe('passwordPolicyKey', () => {
  it('accepts a compliant password', () => {
    expect(passwordPolicyKey('Abcdef1!')).toBeNull()
  })
  it('flags each rule in order', () => {
    expect(passwordPolicyKey('Ab1!')).toBe('validation.password.tooShort')
    expect(passwordPolicyKey('a'.repeat(33) + 'B1!')).toBe('validation.password.tooLong')
    expect(passwordPolicyKey('abcdef1!')).toBe('validation.password.needUpper')
    expect(passwordPolicyKey('ABCDEF1!')).toBe('validation.password.needLower')
    expect(passwordPolicyKey('Abcdefg!')).toBe('validation.password.needDigit')
    expect(passwordPolicyKey('Abcdefg1')).toBe('validation.password.needSpecial')
  })
  it('rejects reuse of the current password', () => {
    expect(passwordPolicyKey('Abcdef1!', 'Abcdef1!')).toBe('validation.password.sameAsCurrent')
  })
})
```

- [ ] **Step 2: Run it — verify it fails**

Run: `pnpm test -- passwordPolicy`
Expected: FAIL — module not found.

- [ ] **Step 3: Write `app/utils/passwordPolicy.ts`**

```ts
const SPECIALS = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"

export function passwordPolicyKey(value: string, current?: string): string | null {
  if (!value || value.length < 8) return 'validation.password.tooShort'
  if (value.length > 32) return 'validation.password.tooLong'
  if (!/[A-Z]/.test(value)) return 'validation.password.needUpper'
  if (!/[a-z]/.test(value)) return 'validation.password.needLower'
  if (!/[0-9]/.test(value)) return 'validation.password.needDigit'
  if (![...value].some(c => SPECIALS.includes(c))) return 'validation.password.needSpecial'
  if (current && value === current) return 'validation.password.sameAsCurrent'
  return null
}
```

- [ ] **Step 4: Add i18n keys** (`en.json`, then mirror the same keys verbatim into `fr`/`ar`/`zh`)

```json
"password": {
  "tooShort": "Use at least 8 characters.",
  "tooLong": "Use at most 32 characters.",
  "needUpper": "Add an uppercase letter.",
  "needLower": "Add a lowercase letter.",
  "needDigit": "Add a number.",
  "needSpecial": "Add a special character.",
  "sameAsCurrent": "Choose a password different from your current one."
}
```

(nested under the existing `"validation"` object).

- [ ] **Step 5: Run tests + build**

Run: `pnpm test -- passwordPolicy && pnpm lint && pnpm build`
Expected: PASS. (`i18n-keys.test.ts` still green — keys mirrored to all four files.)

- [ ] **Step 6: Commit**

```bash
git add nadoumi-web/app/utils/passwordPolicy.ts nadoumi-web/tests/unit/passwordPolicy.test.ts nadoumi-web/i18n/locales
git commit -m "feat(web): shared password-policy validator (mirrors backend PasswordPolicy)"
```

---

### Task F-2: `OtpInput` component

**Files:**
- Create: `nadoumi-web/app/components/auth/OtpInput.vue`
- Create: `nadoumi-web/tests/unit/auth/OtpInput.test.ts`

**Interfaces:**
- Produces: `<OtpInput v-model="string" :length="6" :disabled?="boolean" />` —
  renders `length` single-char boxes; typing advances focus; Backspace on an empty
  box steps back; pasting a 6-digit string fills all boxes; emits
  `update:modelValue` (joined) on every change and `complete` (the value) when all
  boxes are filled. Only digits accepted.

- [ ] **Step 1: Write `tests/unit/auth/OtpInput.test.ts`**

```ts
import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import OtpInput from '~/components/auth/OtpInput.vue'

describe('OtpInput', () => {
  it('fills all boxes from a pasted code and emits complete', async () => {
    const w = await mountSuspended(OtpInput, { props: { modelValue: '', length: 6 } })
    const first = w.findAll('input')[0]!
    await first.trigger('paste', { clipboardData: { getData: () => '482913' } } as unknown as ClipboardEvent)
    expect(w.emitted('update:modelValue')?.at(-1)).toEqual(['482913'])
    expect(w.emitted('complete')?.at(-1)).toEqual(['482913'])
  })

  it('ignores non-digits', async () => {
    const w = await mountSuspended(OtpInput, { props: { modelValue: '', length: 6 } })
    const first = w.findAll('input')[0]!
    await first.setValue('a')
    expect(w.emitted('update:modelValue')?.at(-1)?.[0] ?? '').not.toContain('a')
  })
})
```

- [ ] **Step 2: Run it — verify it fails**

Run: `pnpm test -- auth/OtpInput`
Expected: FAIL — component not found.

- [ ] **Step 3: Write `app/components/auth/OtpInput.vue`**

```vue
<script setup lang="ts">
const props = withDefaults(defineProps<{ modelValue: string; length?: number; disabled?: boolean }>(), { length: 6 })
const emit = defineEmits<{ 'update:modelValue': [v: string]; complete: [v: string] }>()

const boxes = ref<string[]>(Array.from({ length: props.length }, (_, i) => props.modelValue[i] ?? ''))
const inputs = ref<HTMLInputElement[]>([])

watch(() => props.modelValue, (v) => {
  boxes.value = Array.from({ length: props.length }, (_, i) => v[i] ?? '')
})

function push() {
  const v = boxes.value.join('')
  emit('update:modelValue', v)
  if (v.length === props.length) emit('complete', v)
}
function onInput(i: number, e: Event) {
  const digit = (e.target as HTMLInputElement).value.replace(/\D/g, '').slice(-1)
  boxes.value[i] = digit
  if (digit && i < props.length - 1) inputs.value[i + 1]?.focus()
  push()
}
function onKeydown(i: number, e: KeyboardEvent) {
  if (e.key === 'Backspace' && !boxes.value[i] && i > 0) inputs.value[i - 1]?.focus()
}
function onPaste(e: ClipboardEvent) {
  const text = (e.clipboardData?.getData('text') ?? '').replace(/\D/g, '').slice(0, props.length)
  if (!text) return
  e.preventDefault()
  boxes.value = Array.from({ length: props.length }, (_, i) => text[i] ?? '')
  inputs.value[Math.min(text.length, props.length - 1)]?.focus()
  push()
}
</script>

<template>
  <div class="flex gap-2" dir="ltr">
    <input
      v-for="(box, i) in boxes"
      :key="i"
      :ref="el => { if (el) inputs[i] = el as HTMLInputElement }"
      :value="box"
      :disabled="disabled"
      inputmode="numeric"
      autocomplete="one-time-code"
      maxlength="1"
      class="h-12 w-10 rounded-md border border-slate-200 text-center text-lg font-semibold focus-visible:border-brand-500 focus-visible:outline-none"
      :aria-label="`Digit ${i + 1}`"
      @input="onInput(i, $event)"
      @keydown="onKeydown(i, $event)"
      @paste="onPaste"
    >
  </div>
</template>
```

- [ ] **Step 4: Run tests + build**

Run: `pnpm test -- auth/OtpInput && pnpm lint && pnpm build`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add nadoumi-web/app/components/auth/OtpInput.vue nadoumi-web/tests/unit/auth/OtpInput.test.ts
git commit -m "feat(web): OtpInput — 6-box one-time-code field"
```

---

### Task F-3: `useOtp` composable + `EmailVerifyStep`

**Files:**
- Create: `nadoumi-web/app/composables/useOtp.ts`
- Create: `nadoumi-web/app/components/auth/EmailVerifyStep.vue`
- Create: `nadoumi-web/tests/unit/useOtp.test.ts`
- Create: `nadoumi-web/tests/unit/auth/EmailVerifyStep.test.ts`
- Modify: `nadoumi-web/i18n/locales/*.json` — `auth.otp.*` keys (title, sent, resend, resendIn, verify, verified, badCode, enterEmail)

**Interfaces:**
- Consumes: `$fetch('/api/student-email-otp', ...)`, `$fetch('/api/student-email-otp-verify', ...)`,
  `problemMessage` (Task 9).
- Produces:
  - `useOtp()` → `{ request(email, purpose): Promise<void>, verify(email, purpose, otp): Promise<string /* ticket */>, cooldown: Ref<number>, busy: Ref<boolean> }`.
    `request` starts a 60 s `cooldown` countdown (client timer) on success.
    `purpose` is `'REGISTER' | 'PASSWORD_RESET'`.
  - `<EmailVerifyStep v-model:email="string" purpose="REGISTER|PASSWORD_RESET" :emailLocked?=boolean @verified="(ticket:string)=>void">` —
    renders the email `NField` + `[Verify]`; after `request` succeeds, swaps to the
    `OtpInput` + a resend button bound to `cooldown`; on `@complete` calls
    `verify` and emits `verified(ticket)`. Shows `problemMessage` errors inline.
    Always advances to the OTP entry stage even on a non-enumerating 200.

- [ ] **Step 1: Write `tests/unit/useOtp.test.ts`**

```ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useOtp } from '~/composables/useOtp'

const fetchImpl = vi.fn()
vi.stubGlobal('$fetch', fetchImpl)
beforeEach(() => fetchImpl.mockReset())

describe('useOtp', () => {
  it('request posts purpose+email and starts the cooldown', async () => {
    fetchImpl.mockResolvedValueOnce({ sent: true })
    const otp = useOtp()
    await otp.request('a@x.com', 'REGISTER')
    expect(fetchImpl).toHaveBeenCalledWith('/api/student-email-otp', expect.objectContaining({
      method: 'POST', body: { email: 'a@x.com', purpose: 'REGISTER' },
    }))
    expect(otp.cooldown.value).toBeGreaterThan(0)
  })

  it('verify returns the ticket string', async () => {
    fetchImpl.mockResolvedValueOnce({ ticket: 'tkt_9' })
    const otp = useOtp()
    await expect(otp.verify('a@x.com', 'REGISTER', '482913')).resolves.toBe('tkt_9')
  })
})
```

- [ ] **Step 2: Run — verify fail.** `pnpm test -- useOtp` → FAIL (module missing).

- [ ] **Step 3: Write `app/composables/useOtp.ts`**

```ts
export type OtpPurpose = 'REGISTER' | 'PASSWORD_RESET'

export function useOtp() {
  const cooldown = ref(0)
  const busy = ref(false)
  let timer: ReturnType<typeof setInterval> | null = null

  function startCooldown(seconds = 60) {
    cooldown.value = seconds
    timer && clearInterval(timer)
    timer = setInterval(() => {
      cooldown.value -= 1
      if (cooldown.value <= 0 && timer) { clearInterval(timer); timer = null }
    }, 1000)
  }
  onScopeDispose(() => { if (timer) clearInterval(timer) })

  async function request(email: string, purpose: OtpPurpose) {
    busy.value = true
    try {
      await $fetch('/api/student-email-otp', { method: 'POST', body: { email, purpose } })
      startCooldown()
    }
    finally { busy.value = false }
  }

  async function verify(email: string, purpose: OtpPurpose, otp: string): Promise<string> {
    busy.value = true
    try {
      const res = await $fetch<{ ticket: string }>('/api/student-email-otp-verify', {
        method: 'POST', body: { email, purpose, otp },
      })
      return res.ticket
    }
    finally { busy.value = false }
  }

  return { request, verify, cooldown, busy }
}
```

- [ ] **Step 4: Write `EmailVerifyStep.vue`**

```vue
<script setup lang="ts">
import type { OtpPurpose } from '~/composables/useOtp'
const props = defineProps<{ email: string; purpose: OtpPurpose; emailLocked?: boolean }>()
const emit = defineEmits<{ 'update:email': [v: string]; verified: [ticket: string] }>()
const { t } = useI18n()
const { request, verify, cooldown, busy } = useOtp()

const stage = ref<'email' | 'otp'>('email')
const otp = ref('')
const error = ref('')

async function sendCode() {
  error.value = ''
  try { await request(props.email, props.purpose); stage.value = 'otp' }
  catch (e) { error.value = problemMessage(e, t('auth.genericError')) }
}
async function submitCode(code: string) {
  error.value = ''
  try { emit('verified', await verify(props.email, props.purpose, code)) }
  catch (e) { error.value = problemMessage(e, t('auth.otp.badCode')) }
}
</script>

<template>
  <div class="grid gap-4">
    <NAlert v-if="error" tone="danger">{{ error }}</NAlert>

    <template v-if="stage === 'email'">
      <NField :label="t('auth.email')" for="otp-email" required>
        <NInput id="otp-email" :model-value="email" type="email" autocomplete="email"
          :disabled="emailLocked" @update:model-value="emit('update:email', $event)" />
      </NField>
      <NButton :loading="busy" @click="sendCode">{{ t('auth.otp.verify') }}</NButton>
    </template>

    <template v-else>
      <p class="text-sm text-slate-600">{{ t('auth.otp.sent', { email }) }}</p>
      <OtpInput v-model="otp" @complete="submitCode" />
      <button type="button" class="text-start text-sm text-brand-700 disabled:text-slate-400"
        :disabled="cooldown > 0" @click="sendCode">
        {{ cooldown > 0 ? t('auth.otp.resendIn', { n: cooldown }) : t('auth.otp.resend') }}
      </button>
    </template>
  </div>
</template>
```

- [ ] **Step 5: Write `tests/unit/auth/EmailVerifyStep.test.ts`**

```ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import EmailVerifyStep from '~/components/auth/EmailVerifyStep.vue'

const fetchImpl = vi.fn()
vi.stubGlobal('$fetch', fetchImpl)
beforeEach(() => fetchImpl.mockReset())

describe('EmailVerifyStep', () => {
  it('sends a code then emits verified(ticket) after the OTP resolves', async () => {
    fetchImpl.mockResolvedValueOnce({ sent: true })            // request
    fetchImpl.mockResolvedValueOnce({ ticket: 'tkt_42' })      // verify
    const w = await mountSuspended(EmailVerifyStep, { props: { email: 'a@x.com', purpose: 'REGISTER' } })
    await w.find('button').trigger('click')                    // [Verify]
    await new Promise(r => setTimeout(r))
    const boxes = w.findAll('input')
    for (let i = 0; i < 6; i++) await boxes[i]!.setValue(String(i))
    await new Promise(r => setTimeout(r))
    expect(w.emitted('verified')?.[0]).toEqual(['tkt_42'])
  })
})
```

- [ ] **Step 6: Add `auth.otp.*` i18n keys** (en + mirror). Keys: `verify`, `sent`
  (`"We sent a code to {email}."`), `resend`, `resendIn` (`"Resend in {n}s"`),
  `verified`, `badCode` (`"That code is invalid or expired."`).

- [ ] **Step 7: Run tests + build.** `pnpm test -- "useOtp|EmailVerifyStep" && pnpm lint && pnpm build` → PASS.

- [ ] **Step 8: Commit**

```bash
git add nadoumi-web/app/composables/useOtp.ts nadoumi-web/app/components/auth/EmailVerifyStep.vue \
        nadoumi-web/tests/unit/useOtp.test.ts nadoumi-web/tests/unit/auth/EmailVerifyStep.test.ts \
        nadoumi-web/i18n/locales
git commit -m "feat(web): useOtp composable + EmailVerifyStep (shared by register + forgot-password)"
```

---

### Task F-4: BFF passthroughs for OTP + password

**Files:**
- Create: `nadoumi-web/server/api/student-email-otp.post.ts`
- Create: `nadoumi-web/server/api/student-email-otp-verify.post.ts`
- Create: `nadoumi-web/server/api/student-password-reset.post.ts`
- Create: `nadoumi-web/server/api/student-password.post.ts`
- Create: `nadoumi-web/tests/unit/server/otp-and-password-bff.test.ts`

**Interfaces:**
- Consumes: `server/utils/backend.ts` — `backendBaseUrl`, `studentToken(event)`.
- Produces: four handlers. The first three are anonymous passthroughs; the fourth
  requires the session cookie and attaches the bearer. All forward the JSON body and
  re-throw upstream `problem+json` with its status (`createError({ statusCode, data })`).

- [ ] **Step 1: Write the three anonymous passthroughs** (identical shape; endpoint differs)

```ts
// server/api/student-email-otp.post.ts
import { backendBaseUrl } from '~~/server/utils/backend'

export default defineEventHandler(async (event) => {
  const body = await readBody(event)
  try {
    return await $fetch(`${backendBaseUrl()}/api/student/email-otp`, { method: 'POST', body })
  }
  catch (err: unknown) {
    const e = err as { response?: { status?: number }, data?: unknown }
    throw createError({ statusCode: e.response?.status ?? 502, data: e.data ?? { detail: 'Upstream error' } })
  }
})
```

`student-email-otp-verify.post.ts` → `/api/student/email-otp/verify`.
`student-password-reset.post.ts` → `/api/student/password/reset`.

- [ ] **Step 2: Write the authed passthrough**

```ts
// server/api/student-password.post.ts
import { backendBaseUrl, studentToken } from '~~/server/utils/backend'

export default defineEventHandler(async (event) => {
  const token = studentToken(event)
  if (!token) throw createError({ statusCode: 401, data: { detail: 'Not signed in' } })
  const body = await readBody(event)
  try {
    return await $fetch(`${backendBaseUrl()}/api/student/password`, {
      method: 'POST', body, headers: { authorization: `Bearer ${token}` },
    })
  }
  catch (err: unknown) {
    const e = err as { response?: { status?: number }, data?: unknown }
    throw createError({ statusCode: e.response?.status ?? 502, data: e.data ?? { detail: 'Upstream error' } })
  }
})
```

- [ ] **Step 3: Write `tests/unit/server/otp-and-password-bff.test.ts`**

Follow the existing `student-session-get.test.ts` harness (globalThis `$fetch` stub +
`mockNuxtImport`). Assert:

```ts
it('student-email-otp forwards the body to the backend email-otp path', async () => { /* $fetch called with `${base}/api/student/email-otp`, method POST, same body; response returned verbatim */ })
it('student-email-otp passes an upstream 400 problem+json straight through', async () => { /* upstream throws {response:{status:400}, data:{detail:'x'}} -> handler throws createError statusCode 400 data.detail 'x' */ })
it('student-password without a cookie is 401 and never calls upstream', async () => { /* studentToken -> undefined; expect 401; $fetch not called */ })
it('student-password attaches the bearer from the cookie', async () => { /* studentToken -> 'jwt'; assert headers.authorization === 'Bearer jwt' */ })
```

- [ ] **Step 4: Run tests + build.** `pnpm test -- server/otp-and-password && pnpm lint && pnpm build` → PASS.

- [ ] **Step 5: Commit**

```bash
git add nadoumi-web/server/api/student-email-otp.post.ts nadoumi-web/server/api/student-email-otp-verify.post.ts \
        nadoumi-web/server/api/student-password-reset.post.ts nadoumi-web/server/api/student-password.post.ts \
        nadoumi-web/tests/unit/server/otp-and-password-bff.test.ts
git commit -m "feat(web): BFF passthroughs for email-otp + password reset/change"
```

---

### Task F-5: `/register` rebuilt as two steps

**Files:**
- Modify: `nadoumi-web/app/pages/register.vue`
- Modify: `nadoumi-web/server/api/student-account.post.ts` (new body shape)
- Modify: `nadoumi-web/tests/unit/pages/register.test.ts`
- Modify: `nadoumi-web/tests/unit/server/student-account-post.test.ts`
- Modify: `nadoumi-web/i18n/locales/*.json` — `auth.firstName`, `auth.lastName`,
  `auth.passportNameHint` (`"Enter your name exactly as it appears on your passport."`),
  `auth.next`, `auth.step1Title`, `auth.step2Title`

**Interfaces:**
- Consumes: `<EmailVerifyStep>` (F-3), `passwordPolicyKey` (F-1),
  `POST /api/student-account` → `{ signedIn: true }`.
- Produces: `/register` — `layout: default`, `middleware: 'guest'`. Local state
  `step: 1 | 2`, `ticket: string`. Step 1 = first name, last name, helper
  `auth.passportNameHint`, then `<EmailVerifyStep purpose="REGISTER" v-model:email>`;
  on `@verified` store the ticket and go to step 2. Step 2 = password + confirm
  (blur + submit run `passwordPolicyKey`), accept-terms `NCheckbox`, primary button
  label `auth.next`. Submit → `$fetch('/api/student-account', { method:'POST', body:
  { firstName, lastName, email, password, ticket } })` → `refresh()` →
  `navigateTo(localePath('/dashboard/profile'))`.
- `student-account.post.ts` now forwards `{ firstName, lastName, email, password, ticket }`
  to `POST /api/student/register`, then logs in server-side with `{ email, password }`
  against `POST /api/student/login`, sets the cookie, returns `{ signedIn: true }`.
  (Same two-call server-side shape as today; only the field names change.)

- [ ] **Step 1: Update `tests/unit/pages/register.test.ts`**

```ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import Register from '~/pages/register.vue'

const fetchImpl = vi.fn()
vi.stubGlobal('$fetch', fetchImpl)
const nav = vi.fn()
vi.mock('#app', async (o) => ({ ...(await (o as () => Promise<object>)()), navigateTo: nav }))
beforeEach(() => { fetchImpl.mockReset(); nav.mockReset() })

describe('register page (two-step)', () => {
  it('step 1 verifies the email, step 2 posts the account with the ticket', async () => {
    fetchImpl.mockResolvedValueOnce({ sent: true })                       // email-otp request
    fetchImpl.mockResolvedValueOnce({ ticket: 'tkt_1' })                  // email-otp verify
    fetchImpl.mockResolvedValueOnce({ signedIn: true })                   // student-account
    fetchImpl.mockResolvedValueOnce({ authenticated: true, user: { userId: 1, username: 'stu_a', nickName: 'A B' }, applicants: [] })

    const w = await mountSuspended(Register)
    await w.find('#firstName').setValue('Ada')
    await w.find('#lastName').setValue('Lovelace')
    await w.find('#otp-email').setValue('ada@example.com')
    await w.find('button').trigger('click')                               // Verify
    await new Promise(r => setTimeout(r))
    const boxes = w.findAll('input').filter(i => i.attributes('inputmode') === 'numeric')
    for (let i = 0; i < 6; i++) await boxes[i]!.setValue(String(i))
    await new Promise(r => setTimeout(r))

    await w.find('#password').setValue('Abcdef1!')
    await w.find('#confirm').setValue('Abcdef1!')
    await w.find('#terms').setValue(true)
    await w.find('form').trigger('submit')
    await new Promise(r => setTimeout(r))

    const accountCall = fetchImpl.mock.calls.find(c => c[0] === '/api/student-account')
    expect(accountCall?.[1]).toMatchObject({ method: 'POST', body: expect.objectContaining({
      firstName: 'Ada', lastName: 'Lovelace', email: 'ada@example.com', ticket: 'tkt_1',
    }) })
    expect(nav).toHaveBeenCalledWith('/dashboard/profile')
  })

  it('blocks step-2 submit when the password fails the policy', async () => {
    const w = await mountSuspended(Register)
    // force step 2
    ;(w.vm as unknown as { step: number; ticket: string }).step = 2
    ;(w.vm as unknown as { step: number; ticket: string }).ticket = 'tkt_x'
    await w.vm.$nextTick()
    await w.find('#password').setValue('weak')
    await w.find('#confirm').setValue('weak')
    await w.find('#terms').setValue(true)
    await w.find('form').trigger('submit')
    expect(fetchImpl.mock.calls.find(c => c[0] === '/api/student-account')).toBeUndefined()
    expect(w.text()).toContain('at least 8 characters')
  })
})
```

- [ ] **Step 2: Run — verify fail.** `pnpm test -- pages/register` → FAIL.

- [ ] **Step 3: Rewrite `app/pages/register.vue`**

```vue
<script setup lang="ts">
definePageMeta({ middleware: 'guest' })
const { t } = useI18n()
const localePath = useLocalePath()
const { refresh } = useSession()

const step = ref<1 | 2>(1)
const form = reactive({ firstName: '', lastName: '', email: '', password: '', confirm: '', terms: false })
const ticket = ref('')
const error = ref('')
const busy = ref(false)

function onVerified(t_: string) { ticket.value = t_; step.value = 2 }

function step1Invalid(): string | null {
  if (!form.firstName || !form.lastName) return t('validation.required')
  return null
}

async function submit() {
  error.value = ''
  if (!form.terms) { error.value = t('validation.required'); return }
  const pk = passwordPolicyKey(form.password)
  if (pk) { error.value = t(pk); return }
  if (form.password !== form.confirm) { error.value = t('auth.passwordMismatch'); return }
  busy.value = true
  try {
    await $fetch('/api/student-account', {
      method: 'POST',
      body: { firstName: form.firstName, lastName: form.lastName, email: form.email, password: form.password, ticket: ticket.value },
    })
    await refresh()
    await navigateTo(localePath('/dashboard/profile'))
  }
  catch (err) { error.value = problemMessage(err, t('auth.genericError')) }
  finally { busy.value = false }
}

useSeo(t('auth.registerTitle'), t('home.subtitle'))
</script>

<template>
  <div class="mx-auto max-w-md">
    <h1 class="mb-6 font-display text-2xl font-bold">{{ t('auth.registerTitle') }}</h1>
    <NCard>
      <div v-if="step === 1" class="grid gap-4">
        <p class="text-sm font-semibold text-slate-700">{{ t('auth.step1Title') }}</p>
        <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
        <div class="grid gap-4 sm:grid-cols-2">
          <NField :label="t('auth.firstName')" for="firstName" required>
            <NInput id="firstName" v-model="form.firstName" autocomplete="given-name" :maxlength="100" />
          </NField>
          <NField :label="t('auth.lastName')" for="lastName" required>
            <NInput id="lastName" v-model="form.lastName" autocomplete="family-name" :maxlength="100" />
          </NField>
        </div>
        <p class="text-xs text-slate-500">{{ t('auth.passportNameHint') }}</p>
        <EmailVerifyStep v-model:email="form.email" purpose="REGISTER" @verified="onVerified" />
      </div>

      <form v-else class="grid gap-4" @submit.prevent="submit">
        <p class="text-sm font-semibold text-slate-700">{{ t('auth.step2Title') }}</p>
        <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
        <NField :label="t('auth.password')" for="password" :hint="t('validation.password.needSpecial')" required>
          <NInput id="password" v-model="form.password" type="password" autocomplete="new-password" :maxlength="32" />
        </NField>
        <NField :label="t('auth.confirmPassword')" for="confirm" required>
          <NInput id="confirm" v-model="form.confirm" type="password" autocomplete="new-password" :maxlength="32" />
        </NField>
        <NCheckbox id="terms" v-model="form.terms">
          <i18n-t keypath="auth.acceptTerms">
            <template #terms><NuxtLink :to="localePath('/terms')" class="text-brand-700 hover:underline">{{ t('footer.terms') }}</NuxtLink></template>
            <template #privacy><NuxtLink :to="localePath('/privacy')" class="text-brand-700 hover:underline">{{ t('footer.privacy') }}</NuxtLink></template>
          </i18n-t>
        </NCheckbox>
        <NButton type="submit" :loading="busy" block>{{ t('auth.next') }}</NButton>
      </form>
    </NCard>
    <p class="mt-4 text-center text-sm">
      <NuxtLink :to="localePath('/login')" class="text-brand-700 hover:underline">{{ t('auth.toLogin') }}</NuxtLink>
    </p>
  </div>
</template>
```

- [ ] **Step 4: Update `server/api/student-account.post.ts`**

Change the register forward body to `{ firstName, lastName, email, password, ticket }`
and the login forward body to `{ email, password }`. Everything else (cookie set,
`{ signedIn: true }`, pass-through of a register `problem+json`) is unchanged.

- [ ] **Step 5: Update `tests/unit/server/student-account-post.test.ts`**

Adjust the asserted forward bodies to the new field names; keep the two existing
assertions (register 200 → login called + cookie set + no token in body; register
4xx → status + problem passthrough, no cookie).

- [ ] **Step 6: Run tests + build.** `pnpm test && pnpm lint && pnpm build` → PASS.

- [ ] **Step 7: Commit**

```bash
git add nadoumi-web/app/pages/register.vue nadoumi-web/server/api/student-account.post.ts \
        nadoumi-web/tests/unit/pages/register.test.ts nadoumi-web/tests/unit/server/student-account-post.test.ts \
        nadoumi-web/i18n/locales
git commit -m "feat(web): two-step register with email OTP verification"
```

---

### Task F-6: `/forgot-password` real flow + `/dashboard/account` change-password

**Files:**
- Modify: `nadoumi-web/app/pages/forgot-password.vue`
- Create: `nadoumi-web/app/pages/dashboard/account.vue` (supersedes Task 19)
- Create: `nadoumi-web/app/components/dashboard/PasswordChangeForm.vue`
- Create: `nadoumi-web/tests/unit/pages/forgot-password.test.ts`
- Create: `nadoumi-web/tests/unit/pages/account.test.ts`
- Create: `nadoumi-web/tests/unit/dashboard/PasswordChangeForm.test.ts`
- Modify: `nadoumi-web/i18n/locales/*.json` — `auth.reset.*` (title, newPassword,
  done, toLogin), `dashboard.account.*` (title, changePassword, current, new,
  confirm, updated, otherSessionsEndedNote)

**Interfaces:**
- Consumes: `<EmailVerifyStep purpose="PASSWORD_RESET">` (F-3), `passwordPolicyKey`
  (F-1), `POST /api/student-password-reset` `{ ticket, newPassword }` → `204`,
  `POST /api/student-password` `{ currentPassword, newPassword }` → `204`.
- Produces:
  - `/forgot-password` — `layout: default`. `stage: 'verify' | 'reset' | 'done'`,
    `ticket: string`. `verify` = `<EmailVerifyStep purpose="PASSWORD_RESET" v-model:email>`;
    `@verified` → store ticket → `reset`. `reset` = new password + confirm
    (`passwordPolicyKey`), submit → `$fetch('/api/student-password-reset', …)` → on
    resolve set `done`. `done` = success `NAlert` + a `NuxtLink` to `/login`.
    No auto-login.
  - `<PasswordChangeForm :busy @submit="(p:{current:string,next:string})=>void">` —
    current / new / confirm `NField`s; blur + submit run `passwordPolicyKey(next, current)`
    and the confirm match; emits `submit` only when valid; parent owns the request.
  - `/dashboard/account` — `layout: 'dashboard'`, `middleware: 'auth'`. Shows
    `user.username`; a `SectionCard` with `<PasswordChangeForm>` calling
    `POST /api/student-password`; on `204` a success `NAlert` including
    `dashboard.account.otherSessionsEndedNote`. Plus a Sign-out `NButton`.

- [ ] **Step 1: Write the three test files**

```ts
// tests/unit/dashboard/PasswordChangeForm.test.ts
import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import PasswordChangeForm from '~/components/dashboard/PasswordChangeForm.vue'

describe('PasswordChangeForm', () => {
  it('does not emit submit when the new password fails the policy', async () => {
    const w = await mountSuspended(PasswordChangeForm, { props: { busy: false } })
    await w.find('#pw-current').setValue('Whatever1!')
    await w.find('#pw-new').setValue('weak')
    await w.find('#pw-confirm').setValue('weak')
    await w.find('form').trigger('submit')
    expect(w.emitted('submit')).toBeFalsy()
    expect(w.text()).toContain('at least 8 characters')
  })

  it('emits submit with current+next when valid', async () => {
    const w = await mountSuspended(PasswordChangeForm, { props: { busy: false } })
    await w.find('#pw-current').setValue('OldPass1!')
    await w.find('#pw-new').setValue('NewPass1!')
    await w.find('#pw-confirm').setValue('NewPass1!')
    await w.find('form').trigger('submit')
    expect(w.emitted('submit')?.[0]?.[0]).toEqual({ current: 'OldPass1!', next: 'NewPass1!' })
  })
})
```

```ts
// tests/unit/pages/forgot-password.test.ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import Forgot from '~/pages/forgot-password.vue'

const fetchImpl = vi.fn()
vi.stubGlobal('$fetch', fetchImpl)
beforeEach(() => fetchImpl.mockReset())

describe('forgot-password', () => {
  it('verify → reset → done posts the reset with the ticket', async () => {
    fetchImpl.mockResolvedValueOnce({ sent: true })     // request
    fetchImpl.mockResolvedValueOnce({ ticket: 'tkt_7' }) // verify
    fetchImpl.mockResolvedValueOnce(undefined)          // reset -> 204
    const w = await mountSuspended(Forgot)
    await w.find('#otp-email').setValue('sam@example.com')
    await w.find('button').trigger('click')
    await new Promise(r => setTimeout(r))
    const boxes = w.findAll('input').filter(i => i.attributes('inputmode') === 'numeric')
    for (let i = 0; i < 6; i++) await boxes[i]!.setValue(String(i))
    await new Promise(r => setTimeout(r))
    await w.find('#reset-new').setValue('BrandNew1!')
    await w.find('#reset-confirm').setValue('BrandNew1!')
    await w.find('form').trigger('submit')
    await new Promise(r => setTimeout(r))
    const call = fetchImpl.mock.calls.find(c => c[0] === '/api/student-password-reset')
    expect(call?.[1]).toMatchObject({ method: 'POST', body: { ticket: 'tkt_7', newPassword: 'BrandNew1!' } })
    expect(w.text()).toContain('You can now sign in')
  })
})
```

```ts
// tests/unit/pages/account.test.ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import Account from '~/pages/dashboard/account.vue'

const signOut = vi.fn()
vi.mock('~/composables/useSession', () => ({
  useSession: () => ({ user: ref({ userId: 1, username: 'stu_sam', nickName: 'Sam' }), signOut }),
}))
const fetchImpl = vi.fn()
vi.stubGlobal('$fetch', fetchImpl)
beforeEach(() => { fetchImpl.mockReset(); signOut.mockReset() })

describe('account page', () => {
  it('changes the password and shows the other-sessions notice', async () => {
    fetchImpl.mockResolvedValueOnce(undefined) // 204
    const w = await mountSuspended(Account)
    await w.find('#pw-current').setValue('OldPass1!')
    await w.find('#pw-new').setValue('NewPass1!')
    await w.find('#pw-confirm').setValue('NewPass1!')
    await w.find('form').trigger('submit')
    await new Promise(r => setTimeout(r))
    expect(fetchImpl).toHaveBeenCalledWith('/api/student-password', expect.objectContaining({
      method: 'POST', body: { currentPassword: 'OldPass1!', newPassword: 'NewPass1!' },
    }))
    expect(w.text().toLowerCase()).toContain('signed out')
  })

  it('signs out', async () => {
    const w = await mountSuspended(Account)
    await w.find('[data-test="sign-out"]').trigger('click')
    expect(signOut).toHaveBeenCalled()
  })
})
```

- [ ] **Step 2: Run — verify fail.** `pnpm test -- "forgot-password|account|PasswordChangeForm"` → FAIL.

- [ ] **Step 3: Write `PasswordChangeForm.vue`**

```vue
<script setup lang="ts">
defineProps<{ busy: boolean }>()
const emit = defineEmits<{ submit: [p: { current: string; next: string }] }>()
const { t } = useI18n()
const f = reactive({ current: '', next: '', confirm: '' })
const error = ref('')

function submit() {
  error.value = ''
  if (!f.current) { error.value = t('validation.required'); return }
  const k = passwordPolicyKey(f.next, f.current)
  if (k) { error.value = t(k); return }
  if (f.next !== f.confirm) { error.value = t('auth.passwordMismatch'); return }
  emit('submit', { current: f.current, next: f.next })
}
</script>

<template>
  <form class="grid gap-4" @submit.prevent="submit">
    <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
    <NField :label="t('dashboard.account.current')" for="pw-current" required>
      <NInput id="pw-current" v-model="f.current" type="password" autocomplete="current-password" :maxlength="32" />
    </NField>
    <NField :label="t('dashboard.account.new')" for="pw-new" required>
      <NInput id="pw-new" v-model="f.next" type="password" autocomplete="new-password" :maxlength="32" />
    </NField>
    <NField :label="t('dashboard.account.confirm')" for="pw-confirm" required>
      <NInput id="pw-confirm" v-model="f.confirm" type="password" autocomplete="new-password" :maxlength="32" />
    </NField>
    <NButton type="submit" :loading="busy">{{ t('dashboard.account.changePassword') }}</NButton>
  </form>
</template>
```

- [ ] **Step 4: Rewrite `forgot-password.vue`**

```vue
<script setup lang="ts">
const { t } = useI18n()
const localePath = useLocalePath()
const stage = ref<'verify' | 'reset' | 'done'>('verify')
const email = ref('')
const ticket = ref('')
const next = reactive({ pw: '', confirm: '' })
const error = ref('')
const busy = ref(false)

function onVerified(t_: string) { ticket.value = t_; stage.value = 'reset' }

async function resetPw() {
  error.value = ''
  const k = passwordPolicyKey(next.pw)
  if (k) { error.value = t(k); return }
  if (next.pw !== next.confirm) { error.value = t('auth.passwordMismatch'); return }
  busy.value = true
  try {
    await $fetch('/api/student-password-reset', { method: 'POST', body: { ticket: ticket.value, newPassword: next.pw } })
    stage.value = 'done'
  }
  catch (e) { error.value = problemMessage(e, t('auth.genericError')) }
  finally { busy.value = false }
}

useSeo(t('auth.forgotTitle'), t('auth.reset.title'))
</script>

<template>
  <div class="mx-auto max-w-md">
    <h1 class="mb-6 font-display text-2xl font-bold">{{ t('auth.forgotTitle') }}</h1>
    <NCard>
      <EmailVerifyStep v-if="stage === 'verify'" v-model:email="email" purpose="PASSWORD_RESET" @verified="onVerified" />

      <form v-else-if="stage === 'reset'" class="grid gap-4" @submit.prevent="resetPw">
        <NAlert v-if="error" tone="danger">{{ error }}</NAlert>
        <NField :label="t('auth.reset.newPassword')" for="reset-new" required>
          <NInput id="reset-new" v-model="next.pw" type="password" autocomplete="new-password" :maxlength="32" />
        </NField>
        <NField :label="t('auth.confirmPassword')" for="reset-confirm" required>
          <NInput id="reset-confirm" v-model="next.confirm" type="password" autocomplete="new-password" :maxlength="32" />
        </NField>
        <NButton type="submit" :loading="busy" block>{{ t('auth.reset.title') }}</NButton>
      </form>

      <div v-else class="grid gap-4">
        <NAlert tone="success">{{ t('auth.reset.done') }}</NAlert>
        <NuxtLink :to="localePath('/login')" class="text-brand-700 hover:underline">{{ t('auth.reset.toLogin') }}</NuxtLink>
      </div>
    </NCard>
    <p class="mt-4 text-center text-sm">
      <NuxtLink :to="localePath('/login')" class="text-brand-700 hover:underline">{{ t('auth.toLogin') }}</NuxtLink>
    </p>
  </div>
</template>
```

`auth.reset.done` = `"Your password has been reset. You can now sign in with your new password."`

- [ ] **Step 5: Write `app/pages/dashboard/account.vue`**

```vue
<script setup lang="ts">
definePageMeta({ layout: 'dashboard', middleware: 'auth' })
const { t } = useI18n()
const { user, signOut } = useSession()
const busy = ref(false)
const notice = ref('')
const error = ref('')

async function change(p: { current: string; next: string }) {
  busy.value = true; notice.value = ''; error.value = ''
  try {
    await $fetch('/api/student-password', { method: 'POST', body: { currentPassword: p.current, newPassword: p.next } })
    notice.value = t('dashboard.account.updated') + ' ' + t('dashboard.account.otherSessionsEndedNote')
  }
  catch (e) { error.value = problemMessage(e, t('auth.genericError')) }
  finally { busy.value = false }
}

useSeo(t('dashboard.account.title'), t('dashboard.account.title'))
</script>

<template>
  <div class="grid gap-6">
    <SectionCard :title="t('dashboard.account.title')">
      <p class="text-sm text-slate-600">{{ t('dashboard.accountUser') }}</p>
      <p class="font-medium">{{ user?.username }}</p>
    </SectionCard>

    <SectionCard :title="t('dashboard.account.changePassword')">
      <NAlert v-if="notice" tone="success" class="mb-4">{{ notice }}</NAlert>
      <NAlert v-if="error" tone="danger" class="mb-4">{{ error }}</NAlert>
      <PasswordChangeForm :busy="busy" @submit="change" />
    </SectionCard>

    <div>
      <NButton data-test="sign-out" variant="secondary" @click="signOut">{{ t('common.signOut') }}</NButton>
    </div>
  </div>
</template>
```

- [ ] **Step 6: Add i18n keys** (`auth.reset.*`, `dashboard.account.*`) to `en.json`
  and mirror to `fr`/`ar`/`zh`. `dashboard.account.otherSessionsEndedNote` =
  `"You have been signed out on your other devices."`

- [ ] **Step 7: Run tests + build.** `pnpm test && pnpm lint && pnpm build` → PASS.

- [ ] **Step 8: Commit**

```bash
git add nadoumi-web/app/pages/forgot-password.vue nadoumi-web/app/pages/dashboard/account.vue \
        nadoumi-web/app/components/dashboard/PasswordChangeForm.vue nadoumi-web/tests/unit \
        nadoumi-web/i18n/locales
git commit -m "feat(web): real forgot-password flow + dashboard change-password"
```

---

### Task F-7: navbar correction

**Files:**
- Modify: `nadoumi-web/app/components/marketing/SiteHeader.vue`
- Modify: `nadoumi-web/tests/unit/marketing/SiteHeader.test.ts`
- Modify: `nadoumi-web/i18n/locales/*.json` — add `nav.home`; ensure
  `nav.createAccount` exists (label "Create account")

**Interfaces:**
- Produces: header nav array = `Home, Scholarships, Universities, Programs,
  Destinations, About, Contact` (Home first, its own `<NuxtLink :to="localePath('/')">`);
  right side = `Sign in` link + `Create account` solid `NButton` → `/register` when
  guest, account `NDropdown` when authed; one mobile `<nav id="site-nav" v-show="open">`
  (always rendered) carrying the same links.

- [ ] **Step 1: Update `tests/unit/marketing/SiteHeader.test.ts`**

Add/adjust assertions:

```ts
it('renders the full nav set including Home and Destinations', async () => {
  const w = await mountSuspended(SiteHeader)
  const text = w.text()
  for (const label of ['Home', 'Scholarships', 'Universities', 'Programs', 'Destinations', 'About', 'Contact']) {
    expect(text).toContain(label)
  }
})

it('guest sees Sign in + Create account', async () => {
  // useSession mock -> status 'guest'
  const w = await mountSuspended(SiteHeader)
  expect(w.text()).toContain('Sign in')
  expect(w.text()).toContain('Create account')
})
```

- [ ] **Step 2: Run — verify the new assertions fail.** `pnpm test -- marketing/SiteHeader` → FAIL on "Home" / "Create account".

- [ ] **Step 3: Edit `SiteHeader.vue`**

- `links` array: prepend `{ to: '/', key: 'nav.home' }`; keep the existing five plus
  `Destinations`; final order `home, scholarships, universities, programs,
  destinations, about, contact`.
- Guest CTA button label: `t('nav.createAccount')` (was `t('nav.getStarted')`).
- Mobile nav: change `<nav v-if="open" id="site-nav" …>` to
  `<nav v-show="open" id="site-nav" …>` so `aria-controls="site-nav"` always resolves.
- Keep the wordmark `<NuxtLink to="/">` as-is (it stays; "Home" is now also an
  explicit item — both are fine and expected).

- [ ] **Step 4: Add `nav.home` + `nav.createAccount`** to `en.json` and mirror.
  Keep `nav.getStarted` if still referenced elsewhere; otherwise remove it and its
  mirrors (grep first).

- [ ] **Step 5: Run tests + build.** `pnpm test && pnpm lint && pnpm build` → PASS.

- [ ] **Step 6: Commit**

```bash
git add nadoumi-web/app/components/marketing/SiteHeader.vue nadoumi-web/tests/unit/marketing/SiteHeader.test.ts nadoumi-web/i18n/locales
git commit -m "feat(web): navbar — explicit Home, Destinations kept, Create account CTA"
```

---

### Task F-8: `/dashboard` overview polish + `AsyncState`

**Files:**
- Create: `nadoumi-web/app/components/dashboard/AsyncState.vue`
- Modify: `nadoumi-web/app/pages/dashboard/index.vue`
- Create: `nadoumi-web/tests/unit/dashboard/AsyncState.test.ts`
- Modify: `nadoumi-web/tests/unit/dashboard/*` if overview has a test; else add `tests/unit/pages/dashboard-index.test.ts`
- Modify: `nadoumi-web/i18n/locales/*.json` — `dashboard.sections.*` (applications,
  activity, notifications), `dashboard.comingSoon`, `dashboard.onboardingProgress`

**Interfaces:**
- Produces:
  - `<AsyncState :pending :error?="string" :empty?="boolean">` slots
    `#loading` / `#error` / `#empty` / default. Default shows when `!pending && !error
    && !empty`. Built-in fallbacks: a `NSpinner` for loading, an `NAlert tone="danger"`
    for error, a muted line for empty.
  - `/dashboard` overview — welcome header (`dashboard.welcome` + active-applicant
    chip when `applicants.length`), a profile-completeness `SectionCard` (existing
    logic), an **onboarding-progress** `SectionCard` (same completeness number,
    framed as steps), a quick-actions row, and three **coming-soon** `SectionCard`s
    (Applications / Recent activity / Notifications) each rendering
    `dashboard.comingSoon` — **no invented data**. All data regions wrapped in
    `<AsyncState>`.

- [ ] **Step 1: Write `tests/unit/dashboard/AsyncState.test.ts`**

```ts
import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import AsyncState from '~/components/dashboard/AsyncState.vue'

describe('AsyncState', () => {
  it('shows the loading slot while pending', async () => {
    const w = await mountSuspended(AsyncState, { props: { pending: true }, slots: { default: () => 'DATA', loading: () => 'LOADING' } })
    expect(w.text()).toContain('LOADING')
    expect(w.text()).not.toContain('DATA')
  })
  it('shows the error slot when error is set', async () => {
    const w = await mountSuspended(AsyncState, { props: { pending: false, error: 'boom' }, slots: { default: () => 'DATA' } })
    expect(w.text()).toContain('boom')
  })
  it('shows default when settled and non-empty', async () => {
    const w = await mountSuspended(AsyncState, { props: { pending: false }, slots: { default: () => 'DATA' } })
    expect(w.text()).toContain('DATA')
  })
})
```

- [ ] **Step 2: Run — verify fail.** `pnpm test -- dashboard/AsyncState` → FAIL.

- [ ] **Step 3: Write `AsyncState.vue`**

```vue
<script setup lang="ts">
withDefaults(defineProps<{ pending: boolean; error?: string; empty?: boolean }>(), { error: '', empty: false })
const { t } = useI18n()
</script>

<template>
  <div>
    <slot v-if="pending" name="loading"><div class="py-6 text-center"><NSpinner /></div></slot>
    <slot v-else-if="error" name="error"><NAlert tone="danger">{{ error }}</NAlert></slot>
    <slot v-else-if="empty" name="empty"><p class="py-6 text-center text-sm text-slate-500">{{ t('common.nothingYet') }}</p></slot>
    <slot v-else />
  </div>
</template>
```

Add `common.nothingYet` = `"Nothing here yet."` to i18n (+ mirrors).

- [ ] **Step 4: Rewrite `dashboard/index.vue`** — welcome header, active-applicant
  chip, completeness card (keep the existing `completeness()`), onboarding-progress
  card, quick actions, and the three coming-soon `SectionCard`s using
  `dashboard.comingSoon` (`"This area activates once applications are open."`).
  Wrap the applicants fetch in `<AsyncState :pending="pending" :error="errorMsg">`.
  **Do not add fake counts, rows, or activity.**

- [ ] **Step 5: Add i18n keys + mirror.** `dashboard.sections.applications`,
  `.activity`, `.notifications`, `dashboard.comingSoon`,
  `dashboard.onboardingProgress`.

- [ ] **Step 6: Run tests + build.** `pnpm test && pnpm lint && pnpm build` → PASS.

- [ ] **Step 7: Commit**

```bash
git add nadoumi-web/app/components/dashboard/AsyncState.vue nadoumi-web/app/pages/dashboard/index.vue \
        nadoumi-web/tests/unit/dashboard/AsyncState.test.ts nadoumi-web/i18n/locales
git commit -m "feat(web): dashboard overview polish + AsyncState wrapper (no fake data)"
```

---

### Task F-9: Playwright E2E — two hard-gated journeys

**Files:**
- Create: `nadoumi-web/playwright.config.ts` (if Task 23 not yet executed) or extend it
- Create: `nadoumi-web/tests/e2e/auth.spec.ts`
- Create: `nadoumi-web/tests/e2e/forgot-password.spec.ts`
- Create: `nadoumi-web/tests/e2e/helpers/mail.ts`

**Interfaces:**
- Consumes: a running backend with `sys.account.captchaEnabled=false`,
  `nad.student.register.enabled=true`, `nadoumi.mail.transport=log`; `nuxt preview`
  on `:3000`.
- Produces: `readOtp(email): Promise<string>` — GETs
  `${BACKEND}/api/dev/mail/latest?to=<email>` and extracts the 6-digit code from
  `body`. Two specs, both hard gates.

- [ ] **Step 1: `tests/e2e/helpers/mail.ts`**

```ts
const BACKEND = process.env.NUXT_BACKEND_BASE_URL ?? 'http://localhost:8080'

export async function readOtp(email: string): Promise<string> {
  for (let i = 0; i < 20; i++) {
    const res = await fetch(`${BACKEND}/api/dev/mail/latest?to=${encodeURIComponent(email)}`)
    if (res.ok) {
      const { body } = await res.json() as { body: string }
      const m = body.match(/\b(\d{6})\b/)
      if (m) return m[1]!
    }
    await new Promise(r => setTimeout(r, 500))
  }
  throw new Error(`no OTP mail for ${email}`)
}
```

- [ ] **Step 2: `tests/e2e/auth.spec.ts`**

```ts
import { test, expect } from '@playwright/test'
import { readOtp } from './helpers/mail'

test('register (2-step OTP) → auto-login → dashboard → sign out', async ({ page }) => {
  const email = `e2e${Date.now().toString().slice(-9)}@example.com`

  await page.goto('/register')
  await page.fill('#firstName', 'E2E')
  await page.fill('#lastName', 'Tester')
  await page.fill('#otp-email', email)
  await page.getByRole('button', { name: /verify/i }).click()

  const code = await readOtp(email)
  const boxes = page.locator('input[inputmode="numeric"]')
  for (let i = 0; i < 6; i++) await boxes.nth(i).fill(code[i]!)

  await page.fill('#password', 'Abcdef1!')
  await page.fill('#confirm', 'Abcdef1!')
  await page.check('#terms')
  await page.getByRole('button', { name: /next/i }).click()

  await expect(page).toHaveURL(/\/dashboard\/profile/)

  await page.locator('button[aria-haspopup="menu"]').last().click()
  await page.locator('[data-test="sign-out"]').click()
  await expect(page).toHaveURL(/\/$/)
  await expect(page.getByRole('link', { name: /sign in/i })).toBeVisible()
})
```

- [ ] **Step 3: `tests/e2e/forgot-password.spec.ts`**

```ts
import { test, expect } from '@playwright/test'
import { readOtp } from './helpers/mail'

test('forgot-password → OTP → new password → login with new password', async ({ page, request }) => {
  const email = `e2e${Date.now().toString().slice(-9)}@example.com`

  // arrange: register the account through the UI once
  await page.goto('/register')
  await page.fill('#firstName', 'Reset'); await page.fill('#lastName', 'User')
  await page.fill('#otp-email', email)
  await page.getByRole('button', { name: /verify/i }).click()
  let code = await readOtp(email)
  let boxes = page.locator('input[inputmode="numeric"]')
  for (let i = 0; i < 6; i++) await boxes.nth(i).fill(code[i]!)
  await page.fill('#password', 'Abcdef1!'); await page.fill('#confirm', 'Abcdef1!')
  await page.check('#terms')
  await page.getByRole('button', { name: /next/i }).click()
  await expect(page).toHaveURL(/\/dashboard\/profile/)
  await page.locator('button[aria-haspopup="menu"]').last().click()
  await page.locator('[data-test="sign-out"]').click()

  // act: reset
  await page.goto('/forgot-password')
  await page.fill('#otp-email', email)
  await page.getByRole('button', { name: /verify/i }).click()
  code = await readOtp(email)
  boxes = page.locator('input[inputmode="numeric"]')
  for (let i = 0; i < 6; i++) await boxes.nth(i).fill(code[i]!)
  await page.fill('#reset-new', 'Zxcvbn2@'); await page.fill('#reset-confirm', 'Zxcvbn2@')
  await page.getByRole('button', { name: /reset/i }).click()
  await expect(page.getByText(/sign in with your new password/i)).toBeVisible()

  // assert: the new password works, the old one does not
  await page.goto('/login')
  await page.fill('#username', email)      // login field id stays #username; label is Email
  await page.fill('#password', 'Zxcvbn2@')
  await page.getByRole('button', { name: /sign in/i }).click()
  await expect(page).toHaveURL(/\/dashboard/)
})
```

> If Task 12's login field `id` is `#username`, keep it; only the label/`type`
> changes to email in Task F-10's login tweak. Adjust the selector to match.

- [ ] **Step 4: Config** — `playwright.config.ts` `webServer.env` adds
  `NUXT_BACKEND_BASE_URL`; `fullyParallel: false`; `retries: process.env.CI ? 1 : 0`.

- [ ] **Step 5: Local dry-run** with MySQL + Redis + backend (`transport=log`):
  `pnpm exec playwright install --with-deps chromium && pnpm test:e2e` → 2 passed.

- [ ] **Step 6: Commit**

```bash
git add nadoumi-web/playwright.config.ts nadoumi-web/tests/e2e
git commit -m "test(web): hard-gated E2E — 2-step register + forgot-password journeys"
```

---

### Task F-10: email-first login field + i18n mirror + docs pass

**Files:**
- Modify: `nadoumi-web/app/pages/login.vue` (label + `type="email"` + `autocomplete="email"`; body key stays `username`? — see below)
- Modify: `nadoumi-web/server/api/student-session.post.ts` (forward `{ email, password, code, uuid }`)
- Modify: `nadoumi-web/tests/unit/pages/*` login test if present
- Modify: `nadoumi-web/i18n/locales/fr.json` / `ar.json` / `zh.json` — mirror every
  key added across F-1…F-9 (run `i18n-keys.test.ts`)
- Modify: `docs/FRONTEND_ARCHITECTURE.md` §7 (auth-flow subsection)

**Interfaces:**
- `/login` posts `{ email, password, code, uuid }` to `/api/student-session`; the BFF
  forwards to `POST /api/student/login` (which now takes `email`). The visible field
  is Email (`type="email"`); keep the input `id` stable (`#username` → rename to
  `#email` and update Task 12's test + F-9 selector, OR keep `#username` and just
  change label/type — **pick rename to `#email` for clarity** and update both).

- [ ] **Step 1: Edit `login.vue`** — field `id="email"`, `type="email"`,
  `:label="t('auth.email')"`, `autocomplete="email"`; `form` object key `email`;
  submit body `{ email: form.email, password: form.password, code: form.code, uuid: form.uuid }`.

- [ ] **Step 2: Edit `server/api/student-session.post.ts`** — forward
  `{ email, password, code, uuid }` (rename from `username`).

- [ ] **Step 3: Update login test + F-9 `forgot-password.spec.ts` selector** to `#email`.

- [ ] **Step 4: Mirror i18n** — copy every new `en.json` key into `fr`/`ar`/`zh`
  with the English value. Run `pnpm test -- i18n-keys` → PASS (byte-parity check).

- [ ] **Step 5: Docs** — in `docs/FRONTEND_ARCHITECTURE.md` §7, update the auth-flow
  subsection: two-step register with email OTP, real forgot-password, change-password
  at `/dashboard/account`, the four new BFF routes, `OtpInput`/`EmailVerifyStep`/
  `useOtp`, `passwordPolicy.ts` mirrors backend `PasswordPolicy`. Mark `EXISTING`.

- [ ] **Step 6: Full gate.** `pnpm lint && pnpm test && pnpm build` → PASS.

- [ ] **Step 7: Commit**

```bash
git add nadoumi-web/app/pages/login.vue nadoumi-web/server/api/student-session.post.ts \
        nadoumi-web/tests nadoumi-web/i18n/locales docs/FRONTEND_ARCHITECTURE.md
git commit -m "feat(web): email-first login; mirror i18n; document Revision 2 auth flows"
```

---

# PART G — Applicant onboarding design (Revision 2, docs only)

### Task G-1: finalise `docs/APPLICANT_ONBOARDING.md` (PROPOSED)

> `docs/APPLICANT_ONBOARDING.md` and the pointers in `DATABASE_DESIGN.md` §5.2 /
> `DOMAIN_MODEL.md` §2.1 were **created alongside the Revision 2 spec/plan update**.
> G-1 is a review-and-finalise pass: re-read it against the shipped spec §16, fill
> any gap the checklist below surfaces, and add the `API_DESIGN.md` §4 PROPOSED row.

**Files:**
- Modify: `docs/APPLICANT_ONBOARDING.md` (fill gaps found in review)
- Modify: `docs/API_DESIGN.md` §4 (add an "Applicant onboarding (PROPOSED)" line
  pointing at `APPLICANT_ONBOARDING.md` §6)

**No code.** Verify the document contains, all labelled **PROPOSED**:

- [ ] **Step 1: UX flow** — the ordered onboarding screens after first login:
  Identity → Profile photo → Passport → Education & history → Education interests →
  Current location (the *"Are you currently in China?"* Yes/No branch and each
  branch's fields) → Languages & proficiency → Tests → Work experience →
  Certifications → Guardian/emergency contact → Review. Wireframe-level description
  of each screen; a persistent progress indicator; save-and-resume; the
  Required / Recommended / Optional badge on every field.

- [ ] **Step 2: Field catalogue** — a table: field, screen, type, Required/Recommended/Optional,
  validation, PII flag, notes. Covers every field enumerated in spec §16 /
  requirement #5. Explicitly state what is NOT claimed: no OCR, no passport
  authenticity check, no face match.

- [ ] **Step 3: Domain model** — new aggregates/value objects:
  `ApplicantProfile` (extends the current `Applicant`), `ApplicantResidence`
  (China-branch vs abroad-branch as a sealed shape), `ApplicantLanguage`,
  `ApplicantInterest`, `ApplicantWorkExperience`, `ApplicantCertification`,
  `ApplicantDocument` (photo + passport — **depends on the Document slice**). Show
  how it composes with the existing `nad_applicant` + child tables.

- [ ] **Step 4: Schema** — `ALTER TABLE nad_applicant ADD ...` for the scalar
  identity/residence fields; new child tables `nad_applicant_language`,
  `nad_applicant_interest`, `nad_applicant_work`, `nad_applicant_certification`;
  and the dependency note that profile photo + passport need
  `nad_document` + object storage (`docs/DOCUMENT_MANAGEMENT.md`). Migration
  numbering placeholder `V<n>` — not assigned until the phase is scheduled.

- [ ] **Step 5: API** — `GET/PUT /api/student/applicants/{id}/profile`,
  `GET/PUT .../residence`, `GET/POST/PUT/DELETE .../languages`, `.../interests`,
  `.../work`, `.../certifications`, `POST .../photo`, `POST .../passport`
  (multipart, → Document slice). Request/response DTO sketches. All PROPOSED.

- [ ] **Step 6: Sequencing** — state the hard dependency order: Document slice +
  object storage first, then this onboarding phase. Cross-link
  `docs/DOCUMENT_MANAGEMENT.md`, `docs/APPLICATION_WORKFLOW.md`.

- [ ] **Step 7: Commit**

```bash
git add docs/APPLICANT_ONBOARDING.md docs/DATABASE_DESIGN.md docs/DOMAIN_MODEL.md docs/API_DESIGN.md
git commit -m "docs: PROPOSED applicant onboarding design (UX + domain + API + schema)"
```

---

## Self-review

**Spec coverage** — every spec section maps to a task:

| Spec § | Tasks |
| --- | --- |
| §2 decisions (Nuxt/BFF, styling, brand, theme, logo, type, i18n, forgot-password, post-register, testing) | 1, 6, 7, 8, 10, 13, 23, 24 |
| §5 design system (Tailwind, tokens, type, RTL) | 1, 6 |
| §6 components (ui / marketing / dashboard tiers) | 2–6, 8, 14, 20 |
| §7.1 marketing pages | 21, 22 |
| §7.2 auth pages (register/login/forgot) | 12, 13 |
| §7.3 dashboard screens | 15–19 |
| §8 auth & session (routes, useSession, middleware, cookie, credential rule) | 10, 11 |
| §9 i18n | 7 |
| §10 footer | 8 |
| §11 testing (Vitest + Playwright + CI) | every task (Vitest), 23, 24 |
| §12 docs | 24 |
| §13 work breakdown | Parts A–D mirror the 11 groups |
| §14 risks | catalog empty-states (22), `/me` shape confirmed in this plan's contract section, student password-change → support link (19), Playwright hard gate + health poll (23, 24), legal copy marked draft (21) |

**Placeholder scan** — no `TODO`/`TBD`; every code step has real content; the `[id].vue` detail pages explicitly say "identical but for the endpoint segment" and show the full file. Legal pages contain real placeholder prose flagged `<em>Draft</em>` (a spec requirement, not a plan gap).

**Type consistency** — `SessionDto`/`ApplicantDto`/`EducationDto`/`TestScoreDto`/`ContactDto` defined in Task 9, consumed unchanged in Tasks 10/11/15–19. `SelfApplicantBody`/`EducationBody`/`TestScoreBody`/`ContactBody` defined in Task 15, consumed in 16–18. `useSession()` surface (`status`/`user`/`applicants`/`activeApplicantId`/`refresh`/`signOut`/`setActiveApplicant`) defined in Task 11, consumed in 12–19. BFF route names match the Global Constraints throughout. `useApplicant` method names (`listMine`, `create`, `get`, `update`, `listEducation`, `addEducation`, `updateEducation`, `deleteEducation`, `listTestScores`, `addTestScore`, `deleteTestScore`, `listContacts`, `addContact`, `deleteContact`) are identical in Task 15's definition and Tasks 16–18's use.

**Known deviations from the spec, intentional:**
- ~~Registration collects a distinct **username**~~ — **reversed by Revision 2.** The
  backend moves to email-first (Task E-5); `user_name` becomes a server-generated
  internal handle. `/register` and `/login` no longer show a username field.
- Test-scores and contacts screens are **add/delete only** (no inline edit) because the backend exposes no `PUT` for those sub-resources. Education keeps full CRUD.

---

## Self-review — Revision 2 addendum

**Revision 2 spec coverage:**

| Spec § (Revision 2) | Tasks |
| --- | --- |
| Revision 2 block — D-R2-1 (SMTP abstraction, Mailpit, Gmail, `.env`) | E-0, E-1 |
| D-R2-2 (email-first identity) | E-5, F-5, F-10 |
| D-R2-3 (shared password policy) | E-2, F-1 (mirror), consumed E-5/E-6/F-5/F-6 |
| D-R2-4 (profile frozen; onboarding doc only) | G-1 (doc); no profile task touched |
| D-R2-5 (navbar incl. Destinations) | F-7 |
| D-R2-6 (real forgot-password) | E-3/E-6 (backend), F-3/F-6 (frontend) |
| §4.R2 (six endpoints) | E-3, E-5, E-6; BFF F-4 |
| §11.1 additions (OtpInput, passwordPolicy, BFF, account) | F-1, F-2, F-4, F-6 |
| §11.2 (two hard-gated E2E) | F-9 |
| §15 (email + OTP slice, incl. §15.5 dev mail retrieval) | E-1…E-6, E-0 |
| §15.4 (session revocation) | E-4, consumed E-6 |
| §16 (onboarding, PROPOSED) | G-1 |
| §17 (admin bootstrap) | E-7 |
| §12 doc updates (API/SECURITY/PHASE_3/COMMS/ADMIN/DOMAIN/DATABASE/DEPLOYMENT/DEV-GUIDE/FRONTEND) | E-0 (deploy/dev), F-10 (frontend), G-1 (onboarding), Task 24 (the rest) + the standalone doc-consistency pass done alongside this revision |
| "No fake data" | F-8 (designed empty states; no invented counts/rows) |

**Revision 2 type consistency:**
- Java `PasswordPolicy.violation(...)` keys ↔ TS `passwordPolicyKey(...)` keys:
  `tooShort/tooLong/needUpper/needLower/needDigit/needSpecial/sameAsCurrent`,
  same order, TS prefixed `validation.password.`.
- `OtpPurpose` values `REGISTER | PASSWORD_RESET` identical in Java (E-3), the BFF
  bodies (F-4), and the TS `OtpPurpose` union (F-3).
- Ticket is a `String` end to end: `TicketService.mint→consume` (E-3),
  `TicketResponse.ticket` (E-6), `useOtp.verify(): Promise<string>` (F-3),
  `register`/`reset` request bodies (F-5/F-6).
- New BFF routes match the Global-Constraints naming:
  `student-email-otp.post`, `student-email-otp-verify.post`,
  `student-password-reset.post`, `student-password.post`.
- `EmailMessage` record shape `(to, subject, body)` identical in `MailSender`,
  `LoggingMailSender.last`, `DevMailController` (E-1/E-6) and the E2E `readOtp`
  helper's expected JSON (F-9).

**Revision 2 placeholder scan:** the `StudentAuthServiceTest` bodies in E-5 and the
controller-test bodies in E-6 are given as named cases with the exact mock stubbing
in an adjacent comment rather than full Java — flagged explicitly in-task
("copy that setup"), because the module's existing `StudentAuthService` test harness
is the definition of record. Not a silent TODO.

---

## Execution handoff

**Plan complete and saved to `docs/superpowers/plans/2026-09-02-nadoumi-web-public-site.md`. Two execution options:**

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints.

**Which approach?**
