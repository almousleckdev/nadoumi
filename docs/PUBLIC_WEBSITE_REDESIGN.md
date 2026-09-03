# Public Website Redesign — Assessment & Plan

> Read `docs/PLATFORM_ARCHITECTURE.md` first. This document is the design brief and
> phased plan for turning the public website (`nadoumi-web` — Home, Scholarships,
> Universities, About, Contact) into a premium international-education discovery
> product. Labels: **EXISTING**, **PROPOSED**, **DECISION REQUIRED**.

Date: 2026-09-03 · Owner: web + identity/university/(new) program/scholarship modules

---

## 1. What is implemented today (EXISTING)

### Public backend surface — the whole of it
| Endpoint | Module | Notes |
| --- | --- | --- |
| `GET /api/public/universities` | `nadoumi-university` | `q` (name / nameCn / city), `country`, `page`, `size`. `PublicUniversityResponse`. PUBLISHED + ACTIVE only. |
| `GET /api/public/universities/{id}` | `nadoumi-university` | full profile + `rankings[]` + `highlights[]`. 404 if not PUBLISHED+ACTIVE. |
| `POST /api/public/contact` | `nadoumi-identity` | `{name,email,subject?,message,locale?,website?}` → 202. Persists `nad_contact_inquiry`, emails support inbox. Honeypot. Rate-limited 10/h/IP. |

There is **no** public scholarship, program, partnership, ranking-search, content /
"featured feed", or gallery endpoint. Those modules do not exist yet.

### Public web (`nadoumi-web`)
- Nuxt 3 (`app/`, compat v4), SSR + Nitro BFF, httpOnly `nad_student_token`,
  `@nuxtjs/i18n@9` (en/fr/ar/zh, ar RTL), Tailwind 3, Vitest + Playwright.
- Design tokens: `app/assets/css/tokens.css` (brand orange `#f97316`), `tailwind.config`
  (`brand` scale, `font-display` = Plus Jakarta Sans, `font-sans` = Inter,
  `font-arabic` = Noto Sans Arabic, `max-w-marketing` 72rem, shadows xs/sm/md).
- Shell: `layouts/default.vue` → `marketing/SiteHeader.vue` + `SiteFooter.vue`.
  Nav = Home · Scholarships · Universities · About · Contact (correct, keep).
- Pages: `index.vue` (hero + 3 values + 4 how-steps + featured-uni strip + CTA band),
  `scholarships/index.vue` (fetch `/api/public/scholarships` → 404 → empty state),
  `universities/index.vue` (card grid from real API), `universities/[id].vue`
  (real profile: facts, prose, highlights, rankings, Programmes placeholder),
  `about.vue` (4 short prose sections), `contact.vue` (real form → `/api/public/contact`).
- UI kit (`app/components/ui/`): `NButton NCard NBadge NField NInput NTextarea
  NSelect NCheckbox NModal NDropdown NAvatar NSpinner NAlert NContainer
  NLocaleSwitcher`. Dashboard: `AsyncState` (loading/error/empty slots),
  `SectionCard`. Marketing: `PageHero`, `ContentCard`.
- `useApi` (`publicGet`, `publicPost`, `studentFetch`), `useSeo`, `useSession`,
  `useApplicant`, `useOnboarding`, `useOtp`. BFF: `server/api/public/[...path].ts`
  (forwards GET+POST to `/api/public/**`), `server/api/student/[...path].ts`.
- **No `nadoumi-web/public/` directory — zero image assets, no logo file.**
  `@nuxt/image` is not installed.

---

## 2. What is poorly designed / wrong (must change)

| # | Problem | Consequence |
| --- | --- | --- |
| P1 | **Home is a generic SaaS landing skeleton.** Hero has no imagery; body is `section → cards → section → cards`. No discovery. | Doesn't communicate "international education platform"; no path from landing → opportunity. |
| P2 | **Scholarships page is a stub** — a title and an "unavailable" note. No search, filter, category, sort, result states. | The single most important discovery surface does not exist. |
| P3 | **Universities list is a flat card grid** — one `country` text box, no province/city/type/partner/recommended/ranking filters, no result count, no sort. | Not a real directory. |
| P4 | **University detail** renders every field but has no hero imagery, no gallery, no real section rhythm; Programmes is a hard-coded placeholder sentence. | Reads like a form dump, not a profile. |
| P5 | **About is four sentences.** No story, mission/vision, values, team, location, hours. | No trust signal for students/parents. |
| P6 | **Contact form** has 3 fields (name/email/message), client-only validation, an inline "thanks" line. No first/last name, phone, category; no proper success/error/disabled states surfaced as a designed experience; no contact-info cards, address, hours, channels. | Feels unfinished. |
| P7 | **No imagery system.** No `public/`, no `@nuxt/image`, no art-direction, no aspect-ratio discipline. | Cannot look premium. |
| P8 | **`PageHero` / `ContentCard`** are thin and were only just moved off broken CSS. Not enough range for a designed site (no media slot, no eyebrow, no variants). | Every section looks identical. |
| P9 | **No shared "discovery section" / carousel / filter-bar / result-grid primitives.** Each page would re-implement paging + states. | Guaranteed duplication if we build page-by-page. |
| P10 | **`ScholarshipSummary` / `ProgramSummary` types are guesses** not tied to any real DTO. | Will be rewritten; nothing should depend on them yet. |

### What is actually fine (keep, do not touch)
- The nav set and the "no Programs / no How-It-Works in nav" IA decision.
- BFF passthrough model, httpOnly cookie, `useApi` shape, `useSeo`.
- The `AsyncState` pattern, `NField`/`NInput`/`NTextarea` form primitives, `NAlert`,
  `NBadge`, `NContainer`, i18n key-parity discipline.
- `nadoumi-university` public DTO split (`PublicUniversityResponse` — no
  status/publishStatus/remark/audit). This is the pattern to copy for scholarships.
- Onboarding ≠ dashboard separation (`/onboarding`, its own layout + gate).

---

## 3. Reuse map

| Reuse as-is | Reuse with small extension | Build new (genuinely) |
| --- | --- | --- |
| `NButton NCard NBadge NAlert NContainer NModal NDropdown NField NInput NTextarea NSelect NCheckbox NSpinner NAvatar NLocaleSwitcher` | `PageHero` (+ media / eyebrow / align / size variants), `ContentCard` → split into real card family, `SiteHeader` (sticky, mega-less dropdowns, mobile drawer), `SiteFooter` (contact block + social) | `SectionHeader` (title + description + prev/next arrows + optional "View all"), `Carousel` (scroll-snap, keyboard, reduced-motion, arrows disabled at ends), `FilterBar` + `FilterDrawer` + `FilterChips`, `ResultGrid` (loading skeleton / empty / error / paged), `Pagination`, `UniversityCard`, `ScholarshipCard`, `ProgramCard`, `StatFact`, `MediaFigure` (art-directed image + aspect ratio + caption), `JourneyTimeline` (How It Works), `CtaBand` (image bg), `ContactInfoCard`, `TeamGrid`, `EmptyState` (illustrated) |
| `AsyncState`, `useApi`, `useSeo`, `useSession` | `useApi` → add typed `publicPost` already done; add nothing else | `useDiscovery<TFilters, TItem>` — one composable that owns query-param sync, paging, sort, facet state, and the loading/empty/error machine, used by every discovery page and every Home section |
| Design tokens / Tailwind config | add `font-display` weight 800, a `--surface-inverse` token for dark CTA bands, `aspect-*` usage discipline | image assets under `public/img/` **or** `@nuxt/image` remote provider (DECISION D1) |

**Rule:** no page implements its own fetch/paging/state loop — everything data-driven
goes through `useDiscovery` + `ResultGrid` (or `Carousel` for Home strips).

---

## 4. Missing backend — what each redesigned surface needs

| Surface | Needs | Exists? | Plan |
| --- | --- | --- | --- |
| Universities list filters (province, city, type, partner, recommended, ranking source/band, sort) | extra query params on `search` + `PublicUniversityResponse` unchanged | ✗ | **B1** — additive: widen `UniversityMapper.search` + `PublicUniversityController`; add `/api/public/universities/facets` (distinct provinces/cities/types with counts). No migration. |
| University "partner" badge | derive from an ACTIVE `nad_partnership`; never a stored public flag | ✗ (no partnership module) | **B5** — deferred; show no partner badge until the partnership context exists. Documented gap. |
| University gallery | `nad_university_gallery(document_id, caption, sort)` + object storage | ✗ | **B4** — deferred to the Document slice (Step 7). Detail page ships without a gallery; layout reserves the slot. |
| University → Programmes section | `nadoumi-program`: `nad_program` (`program_type` LANGUAGE/NON_DEGREE/DIPLOMA/BACHELOR/MASTER/PHD), `nad_program_major`, `nad_program_intake`; `GET /api/public/universities/{id}/programs`, `GET /api/public/programs` (filterable), `GET /api/public/programs/{id}` | ✗ | **B2** — new module + migration **V12**. Public DTO with no internal fields. |
| Scholarship discovery (search + facets + sort + paging) | `nadoumi-scholarship`: aggregate + confidentiality split + faceted public API | ✗ | **B3** — new module + migration **V13**. See §5. |
| Home "featured / hot / recommended / new" feeds | per-entity `is_featured` / `is_recommended` already on `nad_university`; need the same on program + scholarship, plus "new" = recent `published_at`, "hot" = an explicit `is_hot` flag or a computed popularity signal | partial | **B6** — each module carries `is_featured`, `is_recommended`, `is_hot`, `published_at`; Home sections are just pre-filtered discovery queries (`?featured=true&sort=published,desc&size=12`). No separate CMS feed table in v1. |
| Contact form (first/last name, phone, category) | `first_name`,`last_name`,`phone`,`category` on `nad_contact_inquiry`; `ContactRequest` fields | ✗ | **B0** — additive migration **V14** + DTO fields. Small. |
| "Notify students when a scholarship is published" | outbox event + notification module | ✗ | **B7** — Step 5 (V15). Out of scope for the website redesign; the scholarship module raises the event, delivery lands later. |

### Confidentiality (non-negotiable, tested)
`nadoumi-scholarship` gets the same structural split as the design in
`DOMAIN_MODEL.md §6`:
- `nad_scholarship` — student-safe columns only.
- `nad_scholarship_internal` (`scholarship_id` PK=FK) — `university_id`,
  `partnership_id`, internal status, operational / confidential notes, commissions.
  **Never** joined by any `/api/public` or `/api/student` query.
- Public reads go through a DB view `v_scholarship_student` (or an explicit
  projection mapper) — never the entity.
- CI leak tests: an anonymous `GET /api/public/scholarships/{id}` response body
  must not contain `universityId`, `partnership`, `commission`, `internalNote`,
  `operationalNote`, or any `nad_scholarship_internal` column, on any code path.

---

## 5. `nadoumi-scholarship` domain (PROPOSED — migration V13)

Normalized, not JSON blobs. Money is `decimal(14,2)` + ISO-4217 currency, never float.

| Table | Purpose |
| --- | --- |
| `nad_scholarship` | `id, title, slug, summary, country, teaching_language (ENGLISH/CHINESE/BOTH), funding_model (FULLY/PARTIAL/SELF), deadline?, has_stipend, application_fee_*, service_fee_*, is_featured, is_recommended, is_hot, publish_status, published_at, status` — **student-safe only** |
| `nad_scholarship_category` | reference table, extensible: `code, name, sort` — seeded CSC / CGS / GOVERNMENT / PROVINCIAL / UNIVERSITY / PRESIDENTIAL / LANGUAGE / TYPE_A / TYPE_B / TYPE_C / TYPE_D / OTHER |
| `nad_scholarship_category_link` | M:N scholarship ↔ category |
| `nad_scholarship_level` | one row per accepted education level (NON_DEGREE / DIPLOMA / BACHELOR / MASTER / PHD) |
| `nad_scholarship_intake` | `term` (SPRING_MARCH / AUTUMN_SEPTEMBER / …), open?, close? |
| `nad_scholarship_eligibility` | `age_min?, age_max?, nationality_scope, accepted_countries?, in_china?, gpa_min?, ielts_min?, toefl_min?, duolingo_min?, hsk_min?, csca_min?, notes?` |
| `nad_scholarship_fee` | typed line items: `kind` (TUITION_BEFORE/AFTER, ACCOMMODATION_BEFORE/AFTER, REGISTRATION, APPLICATION, NADOUMI_APPLICATION, NADOUMI_SERVICE, INSURANCE, VISA, OTHER), `amount decimal(14,2)`, `currency`, `note?` |
| `nad_scholarship_stipend` | `amount, currency, frequency (MONTHLY/YEARLY/ONE_OFF), duration_months?, conditions?` (nullable — no stipend row = none) |
| `nad_scholarship_document_requirement` | `doc_type` (dict), `mandatory`, `note?` — the frontend reads this, never hard-codes |
| `nad_scholarship_field` | M:N scholarship ↔ field/discipline (dict) |
| `nad_scholarship_province` / `_city` | location filters |
| `nad_scholarship_internal` | **confidential**: `university_id?, partnership_id?, internal_status, operational_notes?, confidential_terms?, commission_model_json?` |

Public API:
- `GET /api/public/scholarships` — `q, level[], category[], language, intake[],
  province, city, field[], funding[], hasStipend, deadlineBefore, sort, page, size`
  → `PageResponse<PublicScholarshipCard>`.
- `GET /api/public/scholarships/facets` — same filters, returns counts per value so
  the UI shows live facet counts.
- `GET /api/public/scholarships/{id|slug}` → `PublicScholarshipDetail` (adds
  eligibility, fee lines, stipend, intakes, document requirements). No internal fields.
- Staff CRUD `POST/PUT/DELETE /api/staff/scholarships/**` + `/internal` (separate
  permission `nad:scholarship:internal:*`), admin screens in `nadoumi-admin`.

---

## 6. Phased implementation sequence (PROPOSED)

Each phase = migration(s) + module + `/api/staff` + `/api/public` + admin screen(s) +
web surface + tests; `mvn clean verify` + web lint/typecheck/test/build green;
owning doc section updated.

| Phase | Scope | Backend | Web |
| --- | --- | --- | --- |
| **R1 — foundations + shell + static surfaces** | Public design-system layer + honest Home + About + Contact + Universities redesign against **today's** backend. | **B0** contact fields (V14), **B1** university list filters + facets. | `@nuxt/image` (D1); rebuild `PageHero` + card family + `SectionHeader` + `Carousel` + `FilterBar`/`FilterDrawer`/`FilterChips` + `ResultGrid` + `Pagination` + `useDiscovery`; redesign `SiteHeader`/`SiteFooter`; **Home** …; **Universities** list + **detail**; **About**; **Contact**. |
| &nbsp;&nbsp;↳ **R1 / PR-1 ✅** (2026-09-03) | Foundations + shell + Home. | `province`/`city`/`type`/`featured`/`recommended` params on `/api/public\|staff/universities` (`UniversitySearch` record, no migration). | `@nuxt/image` + `app/data/imagery.ts`; `MediaFigure` / `SectionHeading` / `Carousel` / `CarouselArrows` / `DiscoverySection` / `SectionPlaceholder` / `UniversityCard` / `HomeHero` / `StorySplit` / `JourneyTimeline` / `CtaBand`; container-less `layouts/default`; sticky `SiteHeader`; restructured `SiteFooter`; **Home** rebuilt (2 real university carousels + 3 honest placeholders + journey + image CTA). `home.*` i18n replaced (4 locales). `mvn verify` + admin + web lint/typecheck/test(133)/build green. |
| &nbsp;&nbsp;↳ **R1 / PR-2** (next) | Universities list + detail redesign, About, Contact. | **B0** contact fields (V14) + `/api/public/universities/facets`. | `useDiscovery` + `ResultGrid` + `FilterBar`/`FilterDrawer`/`FilterChips` + `Pagination`; Universities list (filters/sort/paging/states); University detail (hero, section rhythm, gallery + Programmes slots reserved); About (full IA, gaps flagged); Contact (info cards + fuller form). |
| **R2 — programs** | `nadoumi-program`. Programmes appear on the university detail page + Home "Hot Programs" / "Hot Language Programs" carousels + a programme detail page reached from the university. **No** top-level Programs nav. | **B2** module + **V12**. | `ProgramCard`, university-detail Programmes section (real), Home program carousels (real), `/universities/[id]/programs/[pid]`. |
| **R3 — scholarships** | `nadoumi-scholarship` (§5) + confidentiality split + leak tests. | **B3** module + **V13**. | **Scholarships** discovery page (search, primary filters + "More filters" drawer, facet counts, chips, sort, paging, all states) using `useDiscovery` + `ResultGrid`; `ScholarshipCard`; scholarship **detail** page; Home "New Scholarships" / "Featured Scholarships" / "Self-Funded" carousels (real). |
| **R4 — content flags + partner** | `is_hot` / `published_at` everywhere; partner-university derivation once a partnership context exists. | **B6**, **B5**. | "Recommended by Nadoumi", "Partner Universities" Home sections become real. |
| **R5 — Apply Now + snapshots** | `nadoumi-application` + profile/requirement snapshots (Step 6). | Step 6 (V15–V16). | Apply Now on scholarship detail → "apply for myself / another student" → profile-prefill → scholarship-specific extra fields + document checklist from `nad_scholarship_document_requirement`. |

Notifications on publish / status change (**B7**) ride the outbox in Step 5 and are
**not** part of the website redesign — the modules raise events; delivery is separate.

---

## 7. Decisions required

- **D1 — imagery source.** (a) `@nuxt/image` + a curated remote allow-list
  (Unsplash/Pexels direct URLs, art-directed, cached by Nitro) — real photos, no
  repo weight; (b) a small hand-picked set of licensed photos committed under
  `public/img/` — fully offline, but repo weight + licensing to track;
  (c) brand-illustration only (gradient/pattern/line art, no people) — safest,
  but the brief explicitly wants students/campus imagery. **Recommend (a).**
- **D2 — sequencing.** (a) Ship **R1** first (foundations + Home/About/Contact/
  Universities on real data, everything else honestly placeholdered), then R2/R3;
  Home gets revisited as each data source lands. (b) Build R2 + R3 backend first,
  then do the whole Home + Scholarships in one pass (no Home rework, but nothing
  ships for weeks). **Recommend (a).**
- **D3 — scope of the first PR.** Foundations + shell + Home + About + Contact +
  Universities (list + detail) in one increment, or split shell+foundations+Home
  as PR-1 and Universities/About/Contact as PR-2. **Recommend the split.**
- **D4 — About content.** Confirm we render the correct IA with clearly-marked
  "supplied by Nadoumi" gaps for history / mission / vision / values / team
  photos / office address / hours / social links, and invent none of it. (Assumed
  yes unless told otherwise.)
