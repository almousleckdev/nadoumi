# Stack map

Which ECC rule pack / skill to consult per technology. Rule packs live in
`.claude/rules/ecc/`; skills are invoked with the `Skill` tool (ECC prefix) or
are already installed standalone.

| Technology | Rule pack | ECC skills |
|---|---|---|
| Java / Spring Boot | `ecc/java`, `ecc/common` | `springboot-patterns`, `springboot-security`, `springboot-tdd`, `springboot-verification`, `jpa-patterns`, `java-coding-standards`, `backend-patterns`, `hexagonal-architecture` |
| TypeScript / Node | `ecc/typescript` | `nodejs` patterns via `typescript`, `error-handling`, `contract-first` |
| NestJS | `ecc/typescript` | `nestjs-patterns` |
| Next.js / React | `ecc/react`, `ecc/web` | `react-patterns`, `react-performance`, `react-review`, `nextjs-turbopack`, `frontend-patterns`, `frontend-a11y` |
| Vue / Nuxt | `ecc/vue`, `ecc/nuxt`, `ecc/web` | `vue-patterns`, `vue-review`, `nuxt4-patterns`, `ui-to-vue`, `vite-patterns` |
| Flutter / Dart | `ecc/dart` | `dart-flutter-patterns`, `flutter-dart-code-review`, `compose-multiplatform-patterns`; installed: `flutter-*` skills |
| PostgreSQL | — (project uses MySQL; see `database.md`) | `postgres-patterns`, `database-migrations` |
| MySQL | `database.md` | `mysql-patterns` |
| Docker | — | `docker-patterns`, `deployment-patterns` |
| Kubernetes | — | `kubernetes-patterns`; installed: `devops-infrastructure`, `gke-basics` |
| AWS | — | `deployment-patterns`, `production-audit` |
| Alibaba Cloud | — | no dedicated skill; use `deployment-patterns` + provider docs |
| Go | `ecc/golang` | `golang-patterns`, `golang-testing`, `go-review` |
| Python | `ecc/python` | `python-patterns`, `python-testing`, `python-review` |
| FastAPI | `ecc/python/fastapi.md` | `fastapi-patterns`, `fastapi-review` |
| AI engineering | — | `ai-first-engineering`, `cost-aware-llm-pipeline`, `token-budget-advisor`, `eval-harness`, `prompt-optimizer`, `iterative-retrieval`, `recsys-pipeline-architect`; installed: `claude-api`, `mcp-builder` |

Nothing here overrides `CLAUDE.md` or the Nadoumi rule files. For this repo the
active stacks are **Java/Spring Boot + MyBatis + MySQL** (backend) and
**Nuxt/Vue** (`nadoumi-web`) + **Vue** (`ruoyi-admin`). The rest are for future
modules or reference.
