# nadoumi-web

The Nadoumi public website and student experience — **Nuxt 3, SSR + BFF cookie**
(`docs/FRONTEND_ARCHITECTURE.md` §3.2). Separate from the internal admin
(`nadoumi-admin`, RuoYi-Vue3) and from the transitional `ruoyi-ui`.

## Structure

```
app/
├── pages/          home · scholarships · universities · programs · about · contact
├── components/     shared: AppHeader, AppFooter, PageHero, ContentCard
├── composables/    useApi (calls the BFF, never the Spring API directly), useSeo
├── layouts/        default shell
├── assets/css/     design tokens + base styles
└── types/          catalog DTOs (OpenAPI-generated later)
server/
├── api/public/[...path].ts     passthrough to /api/public/** (no credentials)
├── api/student/[...path].ts     passthrough to /api/student/**, bearer from cookie
└── api/student-session.*.ts     sign in / out; JWT kept in an httpOnly cookie
i18n/locales/       en (fr / ar / zh scaffolded)
```

## Rules

- The browser never holds the JWT and never calls the Spring API directly — every
  request goes through the Nitro BFF, which attaches the bearer from the
  `httpOnly + Secure + SameSite=Lax` cookie.
- The client bundle can never contain confidential scholarship fields
  (university / partnership) — the API does not send them (`docs/DOMAIN_MODEL.md` §6).
- SSR for the catalog pages (SEO); route guards / hidden fields are UX only.

## Develop

```bash
pnpm install
NUXT_BACKEND_BASE_URL=http://localhost:8080 pnpm dev     # http://localhost:3000
pnpm lint
pnpm build
```

This is architecture scaffolding: the catalog pages render empty until the
University / Program / Scholarship API slices land.
