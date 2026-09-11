# Transactional email assets

Served anonymously at `${nadoumi.web.baseUrl}/email/*` and referenced by absolute
URL from every transactional email (`EmailLayout`).

| File | Purpose | Status |
|---|---|---|
| `nadoumi-logo.png` | footer logo (displayed at 120px wide) | Real brand artwork, resized from `nadoumi-web/app/assets/images/logo.jpg`. |
| `facebook.png` / `instagram.png` / `tiktok.png` | 24px footer social icons | Generic monochrome glyphs matching the email palette — swap for official platform icon art whenever the brand asset kit is available. |

An icon only appears in the footer when its URL is configured
(`nadoumi.brand.social.{facebook,instagram,tiktok}`); set the real profile URLs
via env before production — until then the icon is correctly hidden, not broken.

Replacing a file needs no code change (bust the CDN cache if `static/` is fronted
by one).

**Reachability**: these are served by this backend at `/email/*`, but every
email builds the URL as `${nadoumi.brand.baseUrl}/email/*` (defaults to
`https://nadoumi.com`, the public *site's* origin). That only resolves if the
deployment routes `/email/*` on that origin to this backend (e.g. a reverse-proxy
rule in front of both the Nuxt site and this API) — if the backend is deployed on
its own origin instead, override `nadoumi.brand.baseUrl` (`NADOUMI_WEB_BASE_URL`
is shared with `nadoumi.web.baseUrl`, so use a dedicated env var/property split
if the two origins ever diverge) to the origin that actually serves this
directory.
