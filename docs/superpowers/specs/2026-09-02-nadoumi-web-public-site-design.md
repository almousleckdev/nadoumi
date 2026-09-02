# nadoumi-web — Public Website + Student Experience — Design

Date: 2026-09-02
Status: APPROVED
Scope owner: nadoumi-web (Nuxt 3 app in this repo)

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
| Forgot-password | **Deferred.** `/forgot-password` route renders a "contact support" message. No backend work now. |
| Post-registration | **Auto sign-in performed server-side.** The browser POSTs credentials **once** to the BFF; the BFF registers, then logs in against the Spring API, and sets the httpOnly session cookie itself. The client never re-sends the password and never receives the token. Then redirect to `/dashboard/profile`. |
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

## 4. Backend contract (Phase 3, already shipped — no changes except §9)

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
| `/register` | `NField` form: full name, email (username), password + confirm, captcha (when enabled), accept terms checkbox. **One** request: `POST /api/student-account`. The BFF registers **and** signs in server-side and sets the httpOnly cookie (§8.1); the browser sends the password once and gets back only `{ signedIn: true }`. On success → `useSession().refresh()` → redirect `/dashboard/profile`. Inline field errors from RFC 9457 `problem+json` (`errors`/`detail`), status passed through. |
| `/login` | email + password + captcha (when enabled). Submits `POST /api/student-session`. On success → `useSession().refresh()` → redirect to `?redirect=` or `/dashboard`. Link to `/register` and `/forgot-password`. |
| `/forgot-password` | Static: "Password reset isn't self-service yet — email support@nadoumi.com and we'll help." No form submit. |

Captcha component (`AuthCaptcha`) calls `GET /api/public/captcha` (new passthrough);
hidden entirely when `captchaEnabled: false`.

### 7.3 Dashboard (`layout: dashboard`, middleware `auth`)

| Route | Content |
| --- | --- |
| `/dashboard` | Overview: greeting, profile-completeness summary, quick links, applicant switcher if >1. |
| `/dashboard/profile` | `ProfileForm` — given/family name, DOB, nationality, gender, passport no, phone, address. `GET/PUT /api/student/applicants/{id}`. If the user has no applicant yet, first-run creates one (`POST /api/student/applicants`, "about me"). |
| `/dashboard/education` | `EducationList` CRUD → `/api/student/applicants/{id}/education`. |
| `/dashboard/test-scores` | `TestScoreList` CRUD → `.../test-scores`. |
| `/dashboard/contacts` | `ContactList` CRUD → `.../contacts`. |
| `/dashboard/account` | Change password (`/api/student/...` if available; else link to support), sign out (`DELETE /api/student-session` → clear session → `/`). |

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

`SiteHeader`: wordmark slot (`<NuxtLink to="/">` wrapping a `<span>` today, an
`<img>` later — same box), primary nav, locale switcher, auth actions, mobile
disclosure menu.

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

### 11.2 Playwright E2E (one spec, `tests/e2e/auth.spec.ts`)

Flow: open `/register` → fill unique email + password (captcha disabled in the CI
backend profile) → submit → land on `/dashboard/profile` → open account menu →
Sign out → back on `/` with "Sign in" visible.

- Config: `webServer` starts `nuxt preview` (or dev) with
  `NUXT_BACKEND_BASE_URL` pointing at the CI backend.
- Keep to this one journey; no page-object framework yet.

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
(catalog data wiring, forgot-password backend, dark mode).

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
6. **Dashboard** — `dashboard` layout, `DashboardShell`, `ApplicantSwitcher`,
   overview, profile (+ first-run create), education, test-scores, contacts,
   account.
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
- **Change-password for students** — verify whether a student endpoint exists; if
  not, `/dashboard/account` links to support for password changes this phase.
- **Playwright in CI** needs the backend + MySQL + Redis. It is a hard gate (no
  `continue-on-error`); flakiness is handled with a backend health-check poll before
  the Playwright step, not by downgrading the job.
- **Legal copy** (`/privacy`, `/terms`) is placeholder — must be replaced with real
  text before any public launch.
