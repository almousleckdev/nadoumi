# Nadoumi — Frontend Architecture

Status: **BASELINE** · **EXISTING** · **PLANNED** · **OPEN**.

> Reconciled with Rev 3: **D1 APPROVED** = migrate the admin to `RuoYi-Vue3`;
> **D2 APPROVED** = separate Nuxt 3 app `nadoumi-web/` with a BFF cookie.

---

## 1. State of the frontend in this repo (EXISTING)

- The `ruoyi-ui/` directory was **deleted from the repo** at commit `ec987ab9`
  ("update README.md", 2026-08-16). Tag `v3.9.2` is the last commit that contained it;
  the repo `master` is 32 commits past that tag.
- **Phase 0 action:** restored `ruoyi-ui/` from tag `v3.9.2`
  (`git checkout v3.9.2 -- ruoyi-ui`, left as an untracked working-tree directory).
  This is the **official** frontend that shipped with backend artifact `3.9.2` from the
  same repo (`yangzongzhuan/RuoYi-Vue`) — a version match, not a fork.
- Verified locally: `npm install --legacy-peer-deps` → OK;
  `vue-cli-service build` (with `NODE_OPTIONS=--openssl-legacy-provider`) → `dist/` OK;
  `vue-cli-service serve --port 8081` → "Compiled successfully", HTTP 200, and the dev
  proxy `/dev-api` → backend `:8080` reaches `/login` end to end.

## 2. What the restored admin frontend is (EXISTING)

`ruoyi-ui` = **RuoYi-Vue2** admin console:

| Aspect | Value |
| --- | --- |
| Framework | Vue **2.6.12** |
| UI library | Element UI **2.15.14** |
| Build tool | Vue CLI **4.4.6** (webpack 4) |
| State | Vuex 3.6 |
| Router | Vue Router 3.4 |
| HTTP | axios 0.30.3 (`src/utils/request.js`, injects `Authorization`) |
| Auth token | stored in cookie (`js-cookie`), sent as `Bearer` |
| Dynamic menu | `GET /getRouters` → `store/modules/permission.js` builds routes |
| Env | `.env.development` `VUE_APP_BASE_API=/dev-api`; `vue.config.js` proxies `/dev-api` and `/v3/api-docs` to `http://localhost:8080` |
| Dev port | 80 by default (`vue.config.js`); override with `--port` |
| Structure | `src/{api,views,components,store,router,layout,directive,utils,plugins}` |
| Screens | dashboard, `system/*` (user/role/menu/dept/post/dict/config/notice), `monitor/*`, `tool/*` (gen, build, swagger), login/register/lock/404 |

### Known toolchain constraints (EXISTING / risk A4)
- Node ≥ 17 requires `NODE_OPTIONS=--openssl-legacy-provider` (webpack 4 + OpenSSL 3).
  Verified on Node 20.19.2. Recommend pinning Node 16/18 via `.nvmrc`, **or** exporting
  the flag in `package.json` scripts, **or** moving to Vue 3 (see D1).
- `npm install` needs `--legacy-peer-deps` on npm 7+.
- Upstream README states Vue 2 is "维护重心已转移" (maintenance focus has moved); Vue 3
  (`RuoYi-Vue3`) is the actively-developed line.
- Backend/frontend version skew: backend `/getInfo` now returns `pwdChrtype`,
  `isDefaultModifyPwd`, `isPasswordExpired` (post-3.9.2). The restored `v3.9.2` UI
  ignores them — login works; the "force password change / expired" UX is absent until
  the frontend is updated to match.

## 3. Two frontend concerns (from CLAUDE.md §6)

### 3.1 Admin / internal operations — D1 APPROVED: `RuoYi-Vue3`

The internal console is Nadoumi's operations UI (applicants, applications, documents,
universities, programmes, scholarships, partnerships, workflow, tasks, communication,
notifications, reports, system admin — `docs/ADMIN_ARCHITECTURE.md`).

**Baseline:** adopt the official **`RuoYi-Vue3`** (Vue 3 + Element Plus + Vite + Pinia
+ Vue Router 4). Rationale: 13+ admin domains is well past "a few screens"; the Vue 2
line is in upstream maintenance-only; Vite dev server + Element Plus are the
supported path; no OpenSSL-legacy flag.

**Transition plan (integration = Phase 3.5):**
- The restored `ruoyi-ui` (Vue 2) stays in-tree **only** as a working reference / stop-
  gap admin until `RuoYi-Vue3` is wired up. **No Nadoumi admin screen is built on
  Vue 2.**
- `ruoyi-generator` templates are re-pointed to emit **Vue 3** CRUD before generating
  any Nadoumi screen.
- API-contract re-verification against `RuoYi-Vue3` is a Phase 3.5 checklist item
  (login, `getInfo`, `getRouters`, `/system/**`).
- Directory: `nadoumi-admin/` (the new Vue 3 app). `ruoyi-ui/` is removed once
  `nadoumi-admin` reaches parity.
- The RuoYi-Vue3 TypeScript variant is **not** adopted in v1 (cost); revisit later.

### 3.2 Public / student experience — D2 APPROVED: Nuxt 3 + BFF cookie

**Nothing exists yet.** Requirements: SEO-indexable university/programme/scholarship
pages, scholarship discovery, student auth, student dashboard, multi-step application
forms, document upload, realtime (SSE) notifications, messaging.

**Baseline:** a **separate Nuxt 3 app at `nadoumi-web/`** (this repo).
- **SSR** for `/api/public` catalog + content pages (SEO); SPA-style for the
  authenticated dashboard.
- **Auth:** `POST /api/student/login`. The **Nuxt server layer acts as a thin BFF** —
  it receives the JWT from the Spring API and sets it in an **httpOnly + Secure +
  SameSite=Lax cookie**; browser JS never holds the raw token. Nuxt server routes
  forward calls to `/api/student/**` attaching the bearer from the cookie.
- Never receives confidential scholarship/partnership fields — enforced server-side
  (`DOMAIN_MODEL.md` §6); the client bundle literally cannot contain them.
- Rejected: Next.js (a second framework family — no justification per CLAUDE.md §6),
  plain Vue 3 SPA (fails SEO), Thymeleaf-in-monolith (couples release cycles, weak for
  rich forms/realtime).

## 4. Frontend repo layout (BASELINE)

```
/nadoumi-admin     internal console — Vue 3 + Vite + Element Plus + Pinia + vue-i18n     [SCAFFOLDED — see §8]
/nadoumi-web       public + student — Nuxt 3 (SSR + BFF cookie)                          [SCAFFOLDED — see §7]
/ruoyi-ui          transitional Vue 2 admin — reference only, removed at admin parity
```

### 7. `nadoumi-web` — public site + student dashboard

Nuxt 3 (compat v4, `app/` layout), SSR. Full design + decisions:
`docs/superpowers/specs/2026-09-02-nadoumi-web-public-site-design.md` (incl.
**Revision 2**) and `docs/superpowers/plans/2026-09-02-nadoumi-web-public-site.md`.

**EXISTING (plan Tasks 1–17):** Tailwind + design tokens (brand `orange`, Plus
Jakarta Sans / Inter / Noto Sans Arabic, light-only); hand-built `app/components/ui/`
primitive set (no component library); `marketing/` (`SiteHeader`, `SiteFooter`) +
`dashboard/` (`DashboardShell`, `ProfileForm`, `EducationList`, …) tiers; `default`
+ `dashboard` layouts. BFF: `server/api/student-session.{get,post,delete}.ts`,
`student-account.post.ts` (server-side register+login), `student/[...path].ts`
(bearer attached from an `httpOnly+Secure+SameSite=Lax` cookie),
`public/[...path].ts`, `public/captcha.get.ts`. `useSession` + `auth`/`guest`
middleware. i18n `en` authored, `fr`/`ar`/`zh` mirrored (`fallbackLocale: 'en'`),
`ar` RTL. Dashboard screens: overview, `/dashboard/profile` (first-run create),
`/dashboard/education` (full CRUD). Catalog pages render designed empty-states.

**PLANNED — Revision 2 (plan Parts E/F/G):**
- Navbar: **Home, Scholarships, Universities, Programs, Destinations, About,
  Contact** + `Sign in` / `Create account`, → account menu when authed.
- Two-step `/register` (name + email → emailed OTP → password/**Next**) and a real
  `/forgot-password` (email → OTP → new password → `/login`), sharing
  `OtpInput` / `EmailVerifyStep` / `useOtp`.
- `/dashboard/account` = real change-password (current/new/confirm) with backend
  session revocation.
- `passwordPolicy.ts` mirrors the backend `PasswordPolicy` (8–32, class rules, ≠ current).
- New BFF passthroughs: `student-email-otp.post`, `student-email-otp-verify.post`,
  `student-password-reset.post`, `student-password.post`.
- Dashboard overview polish (welcome block, onboarding progress, `AsyncState`
  loading/error/empty; **designed empty states, never fake data**).
- Backend it consumes: spec §15 (email/OTP/password endpoints), email-first login.
- Two hard-gated Playwright journeys (register-OTP, forgot-password).

`useApi()` only ever calls the BFF, never Spring directly. CI: `pnpm lint` +
`pnpm test` + `pnpm build` + the hard-gated E2E job.

Shared: OpenAPI-generated TypeScript types once `/api/v1/**` exists; a small shared
API-client package.

## 5. Cross-cutting frontend rules (PLANNED)

- **No security in the frontend.** Route guards, hidden fields, and disabled buttons
  are UX only; every check is re-enforced server-side (`docs/SECURITY.md`).
- Student bundle must never contain university/partnership identifiers for
  scholarships — the API doesn't send them.
- i18n from day one (RuoYi admin already has an `i18n/` bundle; the public site needs
  en + likely ar/fr/zh).
- Accessibility for public pages (WCAG AA target).
- Environment config via `.env.*`; no hard-coded backend URLs beyond dev proxy.

## 6. Decision status

| Item | Status |
| --- | --- |
| **D1** Admin baseline | **APPROVED** — migrate to `RuoYi-Vue3`; integrate in Phase 3.5; `ruoyi-ui` (Vue 2) transitional only. |
| **D2** Public/student stack | **APPROVED** — Nuxt 3 `nadoumi-web/`, BFF httpOnly cookie, consumes `/api/public` + `/api/student`. |
| Code-generator target | **APPROVED** — re-point templates to Vue 3 before generating Nadoumi screens. |
| Frontend delivery | **APPROVED** — build both frontends in CI as static/SSR artifacts served by the reverse proxy; **do not** bundle `dist/` into the Spring jar for production (`DEPLOYMENT.md` §3). |
| i18n scope beyond `en` (`ar`/`fr`/`zh`) | **OPEN** — product; structure supports it from day one. |

### 8. `nadoumi-admin` scaffold (EXISTING — internal admin, English)

A lean Vue 3 admin that follows the RuoYi backend contract without vendoring the
RuoYi-Vue2/3 codebase.

- **Stack:** Vue 3 + Vite 5 + Element Plus + Pinia + Vue Router 4 + vue-i18n
  (default **en**, `zh` stub). `pnpm dev` on port **8082**, proxy `/dev-api` → `:8080`.
- **Auth:** `POST /login` → token in a cookie; `GET /getInfo` (user/roles/permissions,
  `isDefaultModifyPwd`); `GET /getRouters` → the sidebar is built live from `sys_menu`.
  `stores/permission.ts` maps each backend `component` string to a view; **any screen
  not implemented here renders `views/placeholder.vue`** (so System / Monitor / Tool
  menus resolve without 404).
- **Screens built:** login, dashboard, profile (change password),
  `views/nadoumi/applicant/index.vue` (real — lists / creates / archives against
  `/api/staff/applicants`). Everything else = placeholder until built.
- **`request.ts`** handles both response shapes: RuoYi `{code,msg,data}` (HTTP 200) and
  Nadoumi `/api/**` bare bodies + `problem+json`.
- CI: `pnpm build` (`.github/workflows/ci.yml`, `nadoumi-admin` job).
- **`V6__nadoumi_english_labels.sql`** translates the inherited RuoYi `sys_menu` /
  `sys_dict_*` / `sys_config` labels to English so the admin reads English end to end.
- Local run needs `mvn install` first (so `spring-boot:run` picks up the cleaned
  `ruoyi-*` jars, not the stale `.m2` copies). `ry-vue` dev DB has captcha disabled
  and student self-registration enabled for convenience.
