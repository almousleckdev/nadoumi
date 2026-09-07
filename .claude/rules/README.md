# Nadoumi project rules

Two layers:

- **`*.md` in this directory** — Nadoumi-specific standards (architecture, security,
  testing, code quality, git, database). Concise and authoritative. Loaded into
  every session via the `@`-imports in `CLAUDE.local.md` at the repo root.
- **`ecc/`** — reference rule packs vendored from the ECC plugin
  (`affaan-m/ECC`), matched to this repo's stack: `common`, `java`, `typescript`,
  `react`, `vue`, `nuxt`, `python`, `golang`, `dart`, `web`. These are **on-demand
  reference**, not force-loaded. Consult the relevant pack when writing code in
  that language (the project agents point you at them).

Precedence when guidance conflicts: **Nadoumi rule files > `CLAUDE.md` domain
rules > ECC `ecc/<lang>` pack > ECC `ecc/common` pack.** `CLAUDE.md` remains the
source of truth for the domain; these files operationalize it, they do not
replace it.

Stack areas with no dedicated rule pack (PostgreSQL, Docker, AWS, Kubernetes,
Alibaba Cloud, NestJS, Next.js, FastAPI, AI engineering) are covered by ECC
*skills* — see `stack.md`.
