# Transactional email assets

Served anonymously at `${nadoumi.web.baseUrl}/email/*` and referenced by absolute
URL from every transactional email (`EmailLayout`).

| File | Purpose | Status |
|---|---|---|
| `nadoumi-logo.png` | footer logo (displayed at 120px wide) | **PLACEHOLDER — replace with final brand artwork** |
| `facebook.png` / `instagram.png` / `tiktok.png` | 24px footer social icons | **PLACEHOLDER — replace with final icons** |

An icon only appears in the footer when its URL is configured
(`nadoumi.brand.social.{facebook,instagram,tiktok}`); set the real profile URLs
via env before production.

Replacing a file needs no code change (bust the CDN cache if `static/` is fronted
by one).
