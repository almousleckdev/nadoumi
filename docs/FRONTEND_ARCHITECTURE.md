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

**UPDATE 2026-09-02 (PLATFORM_ARCHITECTURE Step 1):**
- Public navbar is **Home · Scholarships · Universities · About · Contact** +
  `Sign in` / `Create account` (→ user menu when authed). **`Programs` and
  `Destinations` are removed as top-level product areas** — programmes are
  reached through a university detail page (`docs/PLATFORM_ARCHITECTURE.md` C1).
  `app/pages/programs/` and `destinations/` deleted.
- **Onboarding is not a dashboard.** It lives at `/onboarding` with its own
  `layouts/onboarding.vue` (SiteHeader + centred content, no dashboard shell).
  Post-registration redirects there. `middleware/onboarding.ts` + `useOnboarding`
  gate: an incomplete profile is sent from `/dashboard/**` → `/onboarding`; a
  completed one is sent from `/onboarding` → `/dashboard`. Completion is an
  interim heuristic (core identity fields present) until the backend carries
  `onboarding_state` (Step 6).

**UPDATE 2026-09-02 (PLATFORM_ARCHITECTURE Step 2):**
- `app/pages/universities/[id].vue` — SSR public university profile from
  `/api/public/universities/{id}` (`useApi().publicGet` → BFF `/api/public/**`
  passthrough). Renders facts, prose sections (introduction / history / campus /
  accommodation / nearby), grouped Highlights & Advantages, a rankings table, a
  **Programmes** section (placeholder list until Step 3), and contact lines.
  Missing / unpublished → "not available" state.
- `app/pages/universities/index.vue` — richer cards (Chinese name, city/province,
  type, Featured badge), Tailwind + `NContainer` (matches the marketing shell).
- `types/catalog.ts` — `UniversityDetail` / `UniversityRanking` /
  `UniversityHighlight` / `UniversityGalleryImage`; `UniversitySummary` gains
  `nameCn` / `province` / `type` / `featured`.

**UPDATE 2026-09-03 (university gallery — V18):**
- `nad_university_gallery` (≤ 6 images, `imageUrl` + `caption?`) added as a third
  edited-whole child list on the staff + public university responses. Admin drawer
  gains a Gallery `FormSection` (thumbnail + URL + caption rows, "Add image" hidden
  at 6); admin detail renders a gallery grid. `universities/[id].vue` renders the
  gallery grid in place of the former placeholder. `UniversityGalleryImage` added
  to `types/catalog.ts`.

**UPDATE 2026-09-03 (scholarship config depth — V22):**
- `types/catalog.ts` `Money` → `{ amountRmb, amountUsd, currency }`; `ScholarshipDetail`
  gains `stipends[]` (per level, replacing the single `stipend`), `accommodation[]`
  (room types), `nonDegreeDuration`; `fees[]` carry `amountRmb`/`amountUsd`.
- `scholarships/[slug].vue` renders every amount as `¥… · $…`, a per-level stipend
  list, an accommodation table, and the non-degree duration in the header meta.
  `scholarships.room.*` / `scholarships.nonDegree.*` / `scholarships.accommodation`
  i18n added to all four locales.

**UPDATE 2026-09-03 (catalog imagery — V21):**
- `UniversitySummary` / `UniversityDetail` gain `logoImageUrl` / `coverImageUrl`;
  `ScholarshipCard` gains `heroImageUrl` / `coverImageUrl` (all optional URL
  strings). `UniversityCard` shows the cover behind the logo; `universities/[id]`
  hero uses the cover as a faded backdrop with the logo tile; `ScholarshipCard`
  shows a cover strip; `scholarships/[slug]` hero uses the hero/cover image.
  Admin uploads through RuoYi `/common/upload` (no object-storage slice yet).

**UPDATE 2026-09-03 (programmes — `nadoumi-program`, V19/V20):**
- `ProgramCard` marketing component; `types/catalog.ts` gains `ProgramCard` /
  `ProgramDetail` / `ProgramMajor` / `ProgramIntake` (replacing the unused
  `ProgramSummary` stub).
- `universities/[id].vue` — the Programmes section is now a real grid of
  `ProgramCard`s from `/api/public/universities/{id}/programs`, with an honest
  empty state (no more "arriving" placeholder).
- `pages/index.vue` — the programme-discovery `SectionPlaceholder` is replaced by
  a real "Hot programmes" `DiscoverySection` (`/api/public/programs?hot=true`).
- `pages/programs/[id].vue` — new SSR programme detail page (hero, facts,
  summary, majors, intakes, back-to-university link). Reached only from a
  university or a carousel — **no `/programs` index, no nav item**.
- i18n `program.*` namespace added to all four locales; `university.programmesEmpty`
  replaces `programmesArriving` / `programmesBody`; `home.programDiscovery.empty`
  added.

**UPDATE 2026-09-02 (public website build-out):**
- The five public pages are real, on the Tailwind + `NContainer` marketing shell.
  `PageHero.vue` / `ContentCard.vue` rebuilt on the real design tokens (the old
  undefined `nad-*` CSS is gone from every page touched).
  - **Home** (`index.vue`) — hero + dual CTA, "Why Nadoumi" (3), "How it works"
    (4 steps), a featured-universities strip from `/api/public/universities?size=6`
    with a designed empty state, closing CTA band.
  - **Scholarships / Universities** (`*/index.vue`) — `PageHero` + card grid or a
    designed "not yet" state (the scholarships endpoint 404s until Step 4).
  - **About** — real sections (who it's for, destinations, lifecycle, approach).
  - **Contact** — form wired to `useApi().publicPost('contact', …)` →
    `POST /api/public/contact`; `NField`/`NInput`/`NTextarea`, client validation
    mirroring the backend, honeypot, success / error alerts.
- `useApi` gains `publicPost<T>(path, body)`; the BFF `/api/public/[...path]`
  passthrough already forwards POST.
- i18n: new `home.*` / `about.*` / `contact.*` / `catalog.featured` keys added to
  **all four** locale files (English values in fr/ar/zh until translated — the
  `i18n-keys` parity test requires the identical key set).

**UPDATE 2026-09-03 (public website redesign — R1 / PR-1). See `docs/PUBLIC_WEBSITE_REDESIGN.md`.**
- **Imagery.** `@nuxt/image` with the `unsplash` provider (no IPX / sharp). Curated
  set in `app/data/imagery.ts` (verified-resolving ids; alt text kept broad).
- **New primitives** (`app/components/ui/`): `MediaFigure` (art-directed `<NuxtImg>`
  + aspect box + overlay), `SectionHeading` (eyebrow / title / description +
  `#actions`), `Carousel` (scroll-snap, keyboard, `prefers-reduced-motion`, edge
  state, `defineExpose` scrollPrev/Next), `CarouselArrows`.
  `app/components/marketing/`: `DiscoverySection` (heading + arrows + skeleton /
  error / empty / carousel + "View all"), `SectionPlaceholder` (honest
  "arriving with …" block — never fake cards), `UniversityCard` (shared, monogram
  tile until per-university cover art exists), `HomeHero`, `StorySplit`,
  `JourneyTimeline`, `CtaBand`.
- **Shell.** `layouts/default.vue` is now container-less (each page owns its
  rhythm). `SiteHeader` sticky + backdrop-blur + refined mobile sheet.
  `SiteFooter` restructured (brand / Explore / Company / Contact + legal row).
- **Home** (`index.vue`) rebuilt: `HomeHero` (headline, dual CTA, university
  search) → **Featured universities** carousel (real: `?featured=true`) →
  `StorySplit` → Scholarship-discovery `SectionPlaceholder` → `StorySplit` →
  Programme-discovery `SectionPlaceholder` → **Recommended universities** carousel
  (real: `?recommended=true`) → Partner-universities `SectionPlaceholder` →
  `JourneyTimeline` (the 8-step student journey) → `CtaBand` (image bg, Apply now /
  Contact us). No fake data anywhere; unbuilt sections are visibly labelled.
- **Backend (no migration):** `/api/public/universities` +
  `/api/staff/universities` gain `province` / `city` / `type` / `featured` /
  `recommended` filter params (`UniversitySearch` record).
- `home.*` i18n replaced wholesale (nested `hero` / `featuredUnis` / `journey` / …);
  parity kept across all four locales. `useLazyAsyncData` for the Home carousels.
- **R1 / PR-2 ✅ (2026-09-03).**
  - `useDiscovery<T>` — shared engine: filter + sort + page state, URL sync,
    one-page fetch via `publicGet`, loading/empty/error machine. Every discovery
    surface builds on it; pages never re-implement the fetch/paging loop.
  - `ui/`: `ResultGrid` (skeleton / error+retry / empty slot / responsive grid),
    `Pagination` (windowed), `FilterBar` (search + inline filters + mobile
    disclosure + count + chips slot), `FilterChips`.
  - **Universities list** (`universities/index.vue`) — `useDiscovery` +
    `FilterBar` (search, country / province / city / type, Featured toggle) +
    `FilterChips` + `ResultGrid` + `Pagination`. Real result count, empty vs.
    no-match states, `?`-synced filters. `UniversityCard` reused.
  - **University detail** (`universities/[id].vue`) — dark hero header
    (monogram, name, meta, badges), facts strip, 2-column body (prose sections +
    sticky sidebar with rankings + official contact), grouped highlights /
    advantages, **Gallery** (up to 6 campus-life / dormitory / campus-view images
    with captions, from `nad_university_gallery` / V18). **Programmes** still
    renders as an honest "arriving with …" block (Step 3 / V19). i18n
    `university.*` namespace.
  - **About** (`about.vue`) — image hero, who-we-are, mission + vision, four
    values (drawn from the platform's real design principles), how-we-help,
    **team / office / hours** as clearly-flagged "to be supplied by Nadoumi"
    blocks (nothing invented), `CtaBand`.
  - **Contact** (`contact.vue`) — contact-info column (real email + flagged
    office / hours) + full form: first / last name, email, phone, topic
    (`NSelect`), subject, message. Client validation mirrors the backend;
    loading / success / error / disabled states; honeypot.
  - **Backend:** `nad_contact_inquiry` gains `first_name` / `last_name` /
    `phone` / `category` (**V14**); `ContactRequest` switched to split name +
    `phone` + `category`; `name` composed server-side.
  - i18n: `about` + `contact` replaced wholesale; `catalog.*` + `university.*`
    added; parity across all four locales (429 keys).

**UPDATE 2026-09-03 (public website redesign — R3, scholarships). See `docs/PUBLIC_WEBSITE_REDESIGN.md` §R3.**
- Backend module `nadoumi-scholarship` (V16 DDL + V17 seed): `nad_scholarship`
  aggregate + 8 student-safe child tables + confidential `nad_scholarship_internal`
  + `v_scholarship_student` view. Public `/api/public/scholarships` (+ `/facets`,
  `/{slugOrId}`), staff CRUD + gated `/{id}/internal`. Confidentiality leak
  tests pass.
- `types/catalog.ts` — `ScholarshipCard` / `ScholarshipDetail` / `ScholarshipFacets`
  replace the old guessed `ScholarshipSummary`. `useDiscovery` gained no API
  change (already supports `arrayKeys` for `level` / `category`).
- **`/scholarships`** (`scholarships/index.vue`) rebuilt as a real search-and-filter
  **table**: search + country + funding + language + stipend inline, a
  `<details>` "More filters" panel with level + category checkboxes showing
  **live facet counts** (a second `useAsyncData` on `/facets` keyed to the same
  filter query), sort select, chips, `Pagination`, loading / empty / no-match /
  error states, and a per-row **Apply** → `/scholarships/[slug]`.
- **`/scholarships/[slug]`** (`scholarships/[slug].vue`) — dark hero, prose
  sections, structured eligibility grid, fee table, stipend + upfront-fee +
  intake sidebar, and a **document checklist rendered from
  `documentRequirements`** (never hard-coded). **Apply now** routes to
  `/register` (guest) or `/dashboard` (authed); the guided application is R5.
- `marketing/ScholarshipCard.vue` — carousel card. Home "New scholarships" +
  "Fully funded scholarships" `DiscoverySection`s are now real
  (`?sort=newest` / `?funding=FULLY`); the programme placeholder stays (R2).
- Dead `ContentCard.vue` deleted (last consumer removed).
- i18n: `scholarships.*` namespace (table headers, filters, detail labels, and
  enum-code label maps for level / funding / lang / intake / category / fee /
  doc) + `home.newScholarships.*` / `home.fundedScholarships.*` + `common.yes/no`,
  all four locales (533 keys).

**PLANNED — Revision 2 (plan Parts E/F/G):** *(historical; nav line superseded above)*
- Navbar: ~~Home, Scholarships, Universities, Programs, Destinations, About,
  Contact~~ + `Sign in` / `Create account`, → account menu when authed.
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

**PLANNED — Revision 3 (plan F-12…F-16, spec D-R3-*):**
- `layouts/auth.vue` — auth pages keep the real `<SiteHeader>` / `<SiteFooter>`
  (same nav + design system), centered form. Requires `nuxt.config` to register
  `~/components/marketing` with `pathPrefix: false` (otherwise `<SiteHeader>`
  resolves to `<MarketingSiteHeader>` and renders nothing).
- `/register` → **3-step wizard**: Personal (name + email + confirm email) →
  Email verification (`Email: … [Edit email]`, refined `OtpInput`, `Email verified ✓`)
  → Password (show/hide, strength, requirements, **single** Terms & Privacy checkbox).
  `Next` disabled until *verified ∧ password valid ∧ match ∧ terms*. Success →
  `/dashboard/onboarding`.
- Refined `OtpInput` / `EmailVerifyStep` — tighter boxes, `role="group"` + per-box
  `aria-label`, filled/verifying/error states, `Edit email`, reduced-motion. **One**
  implementation, reused by register + forgot-password + future email verification.
- `/dashboard/onboarding` — 7-step wizard shell (Personal · Identity · Education ·
  Interests · Location · Contact · Review) with a persistent progress indicator.
  **EXISTING** steps save via the applicant/education/contact endpoints; **PLANNED /
  REQUIRES BACKEND** steps render disabled with "not saved yet" — no fake persistence.
- `app/components/onboarding/` — `ImageCropper`, `DocumentPreview`,
  `ProfilePhotoUploadCard`, `PassportUploadCard`: client-only crop/zoom/rotate/
  preview/validate; upload disabled (REQUIRES BACKEND — Document slice). No OCR /
  face-match claims.

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

> **Port map (kills a recurring confusion):** `nadoumi-admin` = **`:8082`** (the
> Nadoumi target); `ruoyi-ui` = **`:1024`** (stock RuoYi, reference-only, frozen,
> not in the default environment "up"). Full comparison + recorded one-admin
> decision: `ARCHITECTURE_GAP_ANALYSIS.md` §2 and `ADMIN_ARCHITECTURE.md` §1.0.

- **Stack:** Vue 3 + Vite 5 + Element Plus + Pinia + Vue Router 4 + vue-i18n
  (default **en**, `zh` stub). `pnpm dev` on port **8082**, proxy `/dev-api` → `:8080`.
- **Auth:** `POST /login` → JWT in cookie `nadoumi-admin-token`; `GET /getInfo`
  → user + roles + permission tokens. **`/getRouters` is not used.**
- **Navigation:** a static manifest, `src/config/nav.ts`, is the single source of
  truth for the sidebar **and** the router (`src/router/index.ts` is built from
  the implemented paths). Each item: `{ key, path, icon, perm?, status }`.
  `status: 'implemented'` → a real screen, rendered as a link, gated by
  `userStore.hasPerm(perm)`. `status: 'planned'` → a disabled row with a
  "Planned" tag — no route, **no placeholder page**. Groups: Operations /
  Business / Growth / Platform (`ADMIN_ARCHITECTURE.md` §2.2).
- **Screens built:** login · dashboard (real applicant metrics + coming-soon for
  unbuilt domains) · **Applicants** list + detail (Overview/Education/Test
  scores/Contacts/Access, full CRUD) · **Universities** list + detail + CRUD
  (`/api/staff/universities`) · profile (change password). Planned modules have
  no page at all.
- **Shared UI:** `PageHeader`; dashboard primitives `DashboardGroup`, `StatCard`,
  `DonutStat` (SVG, no chart lib), `ComingSoonCard`, `RecentApplicants`. Vitest
  (`pnpm test`) covers the manifest, the composable and the primitives.
- **`request.ts`** handles both response shapes: RuoYi `{code,msg,data}` (HTTP 200) and
  Nadoumi `/api/**` bare bodies + `problem+json`; one deduped error toast.
- **Design tokens** (`src/assets/styles/index.scss`): Orange-500 brand
  (`#F97316` → `--el-color-primary`), navy sidebar, Inter / Plus Jakarta Sans /
  Noto Sans Arabic, `prefers-reduced-motion` honoured.
- CI: `pnpm build` (`.github/workflows/ci.yml`, `nadoumi-admin` job).
- **`V6__nadoumi_english_labels.sql`** translates the inherited RuoYi `sys_menu` /
  `sys_dict_*` / `sys_config` labels to English so the admin reads English end to end.
- Local run needs `mvn install` first (so `spring-boot:run` picks up the cleaned
  `ruoyi-*` jars, not the stale `.m2` copies). `ry-vue` dev DB has captcha disabled
  and student self-registration enabled for convenience.
