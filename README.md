# Nadoumi

Nadoumi is an international education platform: students discover universities,
programmes and scholarships, build applicant profiles, upload documents, submit
applications and track their progress; staff manage applicants, applications,
partner universities and the operational workflow.

The backend is built on **RuoYi-Vue** (Spring Boot, Spring Security, MyBatis,
Quartz, MySQL) — RuoYi provides authentication, RBAC, system administration and
code generation; Nadoumi's business domain lives in the `nadoumi-*` modules.

## Repository layout

| Path | What |
| --- | --- |
| `ruoyi-common` / `ruoyi-framework` / `ruoyi-system` / `ruoyi-quartz` / `ruoyi-generator` / `ruoyi-admin` | RuoYi infrastructure (do not restructure — see `CLAUDE.md`) |
| `nadoumi-modules/nadoumi-common` | cross-context value types and SPIs |
| `nadoumi-modules/nadoumi-identity` | external-user identity, `/api/student/*`, applicant-access grants, `@na` authorization |
| `nadoumi-modules/nadoumi-applicant` | applicant profiles and the staff/student applicant APIs |
| `ruoyi-admin/src/main/resources/db/migration` | Flyway migrations (`V1` RuoYi baseline; `V2+` Nadoumi) |
| `docs/` | architecture and engineering decisions — the source of truth |
| `ruoyi-ui` | transitional Vue 2 admin (reference only; replaced by the Vue 3 admin) |

## Build & test

```bash
mvn clean verify        # builds all modules and runs unit + integration tests
```

Integration tests use Testcontainers (MySQL + Redis) and self-skip when no Docker
daemon is available.

## Run locally

Requires MySQL and Redis. Configuration is externalised — set the relevant
`SPRING_DATASOURCE_DRUID_MASTER_*`, `SPRING_DATA_REDIS_*` and `TOKEN_SECRET`
environment variables, or provide a git-ignored `config/application-local.yml`.

```bash
mvn -pl ruoyi-admin spring-boot:run
```

or run the packaged jar:

```bash
./ry.sh start        # {start|stop|restart|status}
```

## Documentation

Start with `docs/ARCHITECTURE.md`, `docs/DOMAIN_MODEL.md` and
`docs/DATABASE_DESIGN.md`. `docs/PHASE_3_IDENTITY_APPLICANT.md` describes the first
implemented vertical slice; `docs/CODEBASE_CLEANUP_AUDIT.md` tracks the
professionalisation pass.

## Licence

RuoYi is distributed under the MIT licence (`LICENSE`).
