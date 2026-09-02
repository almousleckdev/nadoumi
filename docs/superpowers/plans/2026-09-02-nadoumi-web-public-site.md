# nadoumi-web Public Website + Student Experience — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn `nadoumi-web/` from a bare scaffold into a professional public website with a working authenticated student profile dashboard, wired to the existing Phase 3 Spring API through the Nitro BFF.

**Architecture:** Extend the existing Nuxt 3 (SSR) app. Add a Tailwind + design-token layer and a small hand-built `app/components/ui/` primitive set. Restructure pages into a marketing group (`layout: default`) and an auth-gated `dashboard/` group. All backend calls go through Nitro BFF routes; the student JWT lives only in an httpOnly cookie the BFF sets and attaches — no token or password ever reaches client JS beyond the single credential POST.

**Tech Stack:** Nuxt 3.21 (`future.compatibilityVersion: 4`, `app/` dir), Nitro, `@nuxtjs/i18n@9`, `@nuxtjs/tailwindcss`, `@nuxt/fonts`, Vitest + `@vue/test-utils` + `@nuxt/test-utils`, Playwright, pnpm 10, ESLint 9 flat config (`@nuxt/eslint`).

**Spec:** `docs/superpowers/specs/2026-09-02-nadoumi-web-public-site-design.md` — read it alongside this plan.

## Global Constraints

- **Public/student architecture:** Nuxt 3 (SSR) + Nitro BFF + httpOnly session cookie. No Next.js, no second SPA framework. Do not reopen.
- **Styling:** Tailwind CSS + a small hand-built shared component set with design tokens. No large component library (no Nuxt UI, no Element Plus, no PrimeVue).
- **Brand primary:** orange `#F97316` (Tailwind `orange-500`). Solid CTA = `orange-600` bg + white text ≥16px semibold. Text links / small text on light = `orange-700` (`#C2410C`). Focus ring = `orange-500`. **Never** put text smaller than 16px semibold on an `orange-500` fill.
- **Theme:** light only. Tokens defined as CSS variables so dark mode is a later additive change. Do not build dark mode.
- **Logo:** text wordmark "Nadoumi". Header markup must let a real `<img>` replace the wordmark with no layout change.
- **Typography:** headings **Plus Jakarta Sans** (600, 700); body/UI **Inter** (400, 500, 600); Arabic fallback **Noto Sans Arabic** (400, 600). Self-hosted via `@nuxt/fonts`.
- **i18n:** `en` fully populated. `fr` / `ar` / `zh` mirror the same key tree with English values. `nuxt.config` i18n `fallbackLocale: 'en'` (also already set in `i18n/i18n.config.ts`). `ar` renders RTL (`<html dir="rtl">`). All user-facing strings come from i18n keys, including validation messages.
- **Credential exposure rule:** a password is sent by the browser exactly once per action — to `POST /api/student-session` (login) or `POST /api/student-account` (register). No client code stores, re-sends, or chains a second authenticated call with raw credentials. The JWT is never in a response body, `useState`, `localStorage`, or a client log.
- **BFF route naming:** session resource is `server/api/student-session.{get,post,delete}.ts`; account creation is `server/api/student-account.post.ts`; anonymous backend endpoints not under `/api/public/**` get a named file beside `server/api/public/[...path].ts`.
- **CI:** the Playwright E2E job is a hard gate — no `continue-on-error`. Backend-startup flakiness is handled with a health-check poll before the Playwright step.
- **Catalog data:** University / Program / Scholarship list & detail pages render designed empty-states; there is no backend catalog API yet and this build adds none.
- **Every step ends green:** `pnpm lint && pnpm test && pnpm build` from `nadoumi-web/`.
- **Commits:** one per task minimum; conventional-commit messages; end the body with the two attribution lines from the repo instructions.

---

## Backend contract (verified against Phase 3 source — do not re-guess)

Anonymous:

| Method | Path | Body | Response |
| --- | --- | --- | --- |
| POST | `/api/student/register` | `{ username (2–20), password (5–20), nickName (≤30)?, email?, code?, uuid? }` | `201 { userId, username }` |
| POST | `/api/student/login` | `{ username, password, code?, uuid? }` | `200 { token }` |
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
| `api/student-account.post.ts` | `POST /api/student/register` then `POST /api/student/login` server-side; `setStudentToken`; return `{ signedIn: true }`. Passes register `problem+json` + status through on failure. |
| `api/public/captcha.get.ts` | Passthrough to backend `GET /captchaImage`. |
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

**Files:**
- Modify: `.github/workflows/ci.yml`
- Modify: `docs/FRONTEND_ARCHITECTURE.md` (§7 rewrite)
- Create: `~/.claude/projects/-Users-mac-Desktop-nadoumi/memory/nadoumi-web-stack.md`
- Modify: `~/.claude/projects/-Users-mac-Desktop-nadoumi/memory/MEMORY.md`

**Interfaces:**
- Consumes: existing `backend` job (produces `ruoyi-admin.jar` artifact) and `nadoumi-web` job.
- Produces: `nadoumi-web` job also runs `pnpm test`; a new `nadoumi-web-e2e` job `needs: [backend, nadoumi-web]` that boots MySQL + Redis services, downloads the jar artifact, starts it with the two dev-config overrides, polls `/actuator/health` (or `/` ) until ready, then runs Playwright. **No `continue-on-error`.**

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
- Registration collects a distinct **username** (2–20) in addition to email, because the backend `StudentRegisterRequest.username` is `@Size(min=2,max=20)` and login is by username — email-as-username would fail validation for most real addresses. "Sign in with email" is a deferred backend change (noted here and in spec §14 territory). The `/register` and `/login` forms therefore have a username field with the hint from `auth.usernameHint`.
- Test-scores and contacts screens are **add/delete only** (no inline edit) because the backend exposes no `PUT` for those sub-resources. Education keeps full CRUD.

---

## Execution handoff

**Plan complete and saved to `docs/superpowers/plans/2026-09-02-nadoumi-web-public-site.md`. Two execution options:**

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints.

**Which approach?**
