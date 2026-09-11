# Transactional email assets

Served anonymously by this backend at `/email/*` and referenced by absolute URL
(`${nadoumi.brand.assetBaseUrl}/email/*`, default `https://api.nadoumi.com`)
from every transactional email (`EmailLayout`).

`nadoumi.brand.assetBaseUrl` is deliberately separate from `nadoumi.brand.baseUrl`
(default `https://nadoumi.com`, shared with `nadoumi.web.baseUrl`): the public
site (`nadoumi-web`, Nuxt) and this API are different deployments, so CTA/content
links (which must land on the site) and these asset URLs (which must land on
this backend) cannot share one origin. If that ever changes — e.g. both are
fronted by the same reverse proxy — `assetBaseUrl` can simply be left unset,
since it falls back to `baseUrl`.

| File | Purpose | Status |
|---|---|---|
| `nadoumi-logo.png` | footer logo (displayed at 120px wide) | Real brand artwork, resized from `nadoumi-web/app/assets/images/logo.jpg`. |
| `facebook.png` / `instagram.png` / `tiktok.png` / `whatsapp.png` | 24px footer social icons | Official platform icon art, background-flattened and squared for the fixed 24×24 footer slot. |

An icon only appears in the footer when its URL is configured
(`nadoumi.brand.social.{facebook,instagram,tiktok,whatsapp}`). `tiktok` and
`whatsapp` currently default to real profile URLs baked into `application.yml`
(overridable via `NADOUMI_SOCIAL_TIKTOK` / `NADOUMI_SOCIAL_WHATSAPP`);
`facebook` and `instagram` are still blank — set
`NADOUMI_SOCIAL_FACEBOOK`/`NADOUMI_SOCIAL_INSTAGRAM` once those profiles exist.
Until then those two icons are correctly hidden, not broken.

Replacing a file needs no code change — and no manual cache-busting either.
Every URL `EmailLayout` builds for a file here carries a `?v=<content hash>`
query param, computed from the file's own bytes at first use and cached for
the life of the process. Replace a file's content and the next email
automatically gets a new URL for it, so mail providers that cache a fetched
image by URL (Gmail's image proxy, most notably — it can keep serving a stale
image against an unchanged URL more or less indefinitely) are forced to fetch
the new version instead of showing what they cached the first time this file's
name was ever used.
