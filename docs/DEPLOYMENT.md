# Nadoumi — Deployment

Status: **BASELINE** · **EXISTING** · **PLANNED** · **OPEN**.

> Reconciled with Rev 3. **Phase 2 (enabling infrastructure) landed** — see §5.
> D3 (maintained fork), D8 (config/secrets), D14 (Flyway) implemented.

---

## 1. Build & run today (EXISTING + Phase 2)

### Backend
- **Build:** `mvn clean package` (root reactor: 10 modules incl. `nadoumi-modules`).
  Spring Boot fat jar `ruoyi-admin/target/ruoyi-admin.jar` (~94 MB), JDK 21,
  Maven 3.9.16. `mvn verify` additionally runs the Testcontainers Flyway IT
  (needs Docker; self-skips without).
- **Run:** `java -jar ruoyi-admin/target/ruoyi-admin.jar` — Tomcat 11 on **:8080**,
  context-path `/`. Boot ~5 s (Flyway migrate + Quartz JDBC init add ~1.5 s).
  On first boot against an empty DB, **Flyway executes `V1__ruoyi_baseline.sql`**
  (31 tables + seeds); against a populated DB it records a **BASELINE** marker and
  changes nothing.
- **Helper scripts:** `ry.sh {start|stop|restart|status}` (Linux, `nohup` + pidfile-less
  `ps|grep`, JVM opts `-Xms512m -Xmx1024m`, `-Duser.timezone=Asia/Shanghai`),
  `ry.bat`, `bin/*.bat` (Windows). No systemd unit, no container.
- **Profiles:** `spring.profiles.active=druid` (datasource). No `dev`/`prod` split.
- **Config:** `application.yml` + `application-druid.yml` on the classpath (baked into
  the jar). Overridable at runtime via env vars / `--` args / external
  `application.yml` next to the jar.
- **Logs:** logback → `./logs/{sys-info,sys-error,sys-user}.log`, 60-day rotation.
  (`log.path` was locally changed from `/home/ruoyi/logs` to `./logs`.)

### Frontend (`ruoyi-ui`, restored from tag v3.9.2)
- **Install:** `npm install --legacy-peer-deps` (Node 16/18 ideal; on Node ≥17 set
  `NODE_OPTIONS=--openssl-legacy-provider`).
- **Dev:** `npm run dev` (Vue CLI serve, default port 80 — override `--port`), proxies
  `/dev-api` → `http://localhost:8080`.
- **Prod build:** `npm run build:prod` → `ruoyi-ui/dist/` (static). Verified ~7 MB.
- Served by any static host / reverse proxy; RuoYi conventionally puts it behind Nginx
  with `/prod-api/` proxied to the backend, or copies `dist/` into
  `ruoyi-admin/src/main/resources/static`.

### Infra dependencies (EXISTING → BASELINE)
- **MySQL** — schema from `sql/ry_20260417.sql` + `sql/quartz.sql`. Committed config
  expects DB `ry-vue` / `root` / `password`. **D8 BASELINE:** the local dev DB is
  renamed `ry_vue`→`ry-vue` to match; all real credentials come from env vars
  (`SPRING_DATASOURCE_DRUID_MASTER_{URL,USERNAME,PASSWORD}`) or a git-ignored
  `config/application-local.yml` (`spring.config.import`). Schema management moves to
  **Flyway** (D14) — `V1` baselines the current schema, Nadoumi tables from `V3`.
- **Redis** — required (token store, captcha, dict/config cache, **and the SSE
  fan-out bus**, D6). **BASELINE:** AUTH + TLS in every non-local environment
  (`SPRING_DATA_REDIS_PASSWORD`, `ssl.enabled=true`).
- **Object storage (S3-compatible)** — **BASELINE (D5)**: MinIO (staging) / AWS S3
  (prod) for documents + chat attachments; `local` filesystem impl for dev only. No
  anonymous `/profile/**`.
- **Quartz** — **DONE (Phase 2):** `spring.quartz.job-store-type=jdbc` +
  `LocalDataSourceJobStore`, `isClustered=true`, `tablePrefix=QRTZ_`,
  `jdbc.initialize-schema=never` (tables come from `V1`). Verified: jobs written to
  `QRTZ_JOB_DETAILS` survive an app restart. RuoYi's commented-out
  `ScheduleConfig.java` stays disabled (config is now via `spring.quartz.*`).
- **Local filesystem** — `ruoyi.profile` must be a valid per-host path
  (`RUOYI_PROFILE` env); only used by the future `local` document-storage impl.

### CI/CD
- **DONE (Phase 2):** `.github/workflows/ci.yml` — JDK 21 (temurin), `mvn -B verify`
  with the Testcontainers Flyway IT, boot-jar artifact upload. No Dockerfile / deploy
  pipeline yet.

## 2. Gaps & risks — status

| ID | Gap | Status |
| --- | --- | --- |
| G1 | Secrets baked into the jar in plaintext. | **DONE (Phase 2)** — all of DB URL/user/password, Redis host/port/password/SSL, `token.secret`, `token.kid`, Druid console creds are `${ENV_VAR:dev-default}` placeholders. `config/application-local.yml` (git-ignored) via `spring.config.import` for non-datasource local tweaks. |
| G2 | No `dev`/`staging`/`prod` profile separation. | **PARTIAL** — env-var contract in place (§4); dedicated `application-<profile>.yml` files deferred to the containerization work. |
| G3 | No containerization / IaC / deploy pipeline. | CI added (Phase 2). Dockerfile + deploy + IaC still **OPEN** (runtime target undecided). |
| G4 | `ruoyi.profile` path; no object storage. | D5 — `DocumentStorage` SPI interface added (`nadoumi-common`); impls **Phase 4**. |
| G5 | Quartz `RAMJobStore`. | **DONE (Phase 2)** — JDBC clustered store, restart-persistence verified. |
| G6 | Swagger UI + Druid stat servlet on by default. | Druid console creds now env-driven; full prod-hardening switches (§3) still **PLANNED**. |
| G7 | Single-instance assumptions. | Quartz clustered store now enabled; SSE/Redis fan-out is **Phase 4**. |
| G8 | No Actuator health/readiness. | **DONE (Phase 2)** — `health,info,metrics,prometheus` exposed; `/actuator/health/**` anonymous (probes), the rest require auth via the existing Spring Security chain. |
| G9 | Forked `master`, no upstream-merge process. | **DONE (Phase 2)** — `upstream` remote added (§4). `origin` repoint is a team action. |
| G10 *(new)* | JJWT `0.9.1` unmaintained; weak in-repo secret. | **DONE (Phase 2)** — upgraded to `jjwt 0.12.6` (api/impl/jackson), `TokenService` rewritten (`Keys.hmacShaKeyFor` + `verifyWith`/`parseSignedClaims`), `kid` header emitted, 64+ char secret required. |

## 3. Proposed deployment architecture (PLANNED)

```
                    ┌──────────── Reverse proxy / LB (Nginx / ALB) ────────────┐
   Internet ───────►│  TLS termination, HSTS/CSP headers, gzip, rate limiting  │
                    └───┬───────────────┬──────────────────┬──────────────────┘
                        │ /             │ /admin           │ /api
                        ▼               ▼                  ▼
              ┌──────────────┐  ┌──────────────┐   ┌──────────────────────┐
              │ nadoumi-web  │  │ ruoyi-ui     │   │ ruoyi-admin (Spring) │  (N replicas)
              │ (Nuxt SSR)   │  │ static dist  │   │ :8080  actuator+/api │
              └──────────────┘  └──────────────┘   └───┬─────────┬────────┘
                                                       │         │
                                          ┌────────────┘         └──────────┐
                                          ▼                                 ▼
                                  ┌──────────────┐                  ┌───────────────┐
                                  │ MySQL (HA)   │                  │ Redis (HA/TLS)│
                                  └──────────────┘                  └───────────────┘
                                          │
                                          ▼
                                  ┌──────────────────────┐
                                  │ Object storage (S3)  │  documents, attachments
                                  └──────────────────────┘
```

### Recommendations
- **Containerize** `ruoyi-admin` (multi-stage: `maven` build → `eclipse-temurin:21-jre`).
  Externalize config via env vars / mounted `application-<profile>.yml` /
  `spring.config.import`.
- **Profiles:** `application-dev|staging|prod.yml`; activate with
  `SPRING_PROFILES_ACTIVE`. Keep secrets out of all of them.
- **Secrets:** environment / Docker secrets / cloud secret manager / Vault. Rotate the
  JWT secret; enable Redis AUTH + TLS.
- **Migrations:** adopt Flyway; `V1__ruoyi_baseline.sql` from `sql/*.sql`, then
  `V2+__nad_*`. No manual SQL in any environment.
- **Actuator:** enable `health`, `info`, `metrics`, `prometheus` on a separate
  management port, not publicly exposed.
- **Quartz:** configure the **JDBC clustered** job store (org.quartz.jobStore.*) so
  schedules survive restarts and scale to >1 node; verify with the `QRTZ_*` tables.
- **Hardening for prod:** `springdoc.swagger-ui.enabled=false`,
  `spring.datasource.druid.stat-view-servlet.enabled=false` (or auth+IP allow-list),
  `referer.enabled=true` with real domains, real CORS origins.
- **Storage:** MinIO/S3 for documents; drop local-filesystem uploads outside dev.
- **Frontends:** build `ruoyi-ui`/`nadoumi-web` in CI, ship as static/SSR containers;
  do **not** rely on copying `dist/` into the jar for production.
- **Observability:** ship logs (JSON encoder) to a central store; dashboards +
  alerting on error rate, latency, DB pool saturation (Druid), Redis, queue depth.
- **Pipeline:** GitHub Actions — `mvn verify` (unit + IT via Testcontainers),
  frontend build+lint, image build/push, deploy to staging, smoke test, promote.

## 4. Decision status

| Item | Status |
| --- | --- |
| **D3** Maintained fork | **DONE (Phase 2)** — `upstream` remote → `https://github.com/yangzongzhuan/RuoYi-Vue.git` added. Process: `origin` → Nadoumi's repo (team action); **monthly** review of `upstream/master`; security fixes cherry-picked with a `chore(upstream): …` commit prefix; Nadoumi keeps its own frontend line. |
| **D8** Config/secrets + DB naming | **DONE (Phase 2)** — all secrets are `${ENV_VAR:dev-default}`; `spring.config.import: optional:file:./config/application-local.yml` for local non-datasource tweaks (`config/` git-ignored, `.example` committed). Local dev DB renamed `ry_vue` → `ry-vue`; committed `application-druid.yml` default unchanged. |
| **D14** Flyway | **DONE (Phase 2)** — `V1__ruoyi_baseline.sql` in `ruoyi-admin/src/main/resources/db/migration/`; `baseline-on-migrate`, `baseline-version=1`, `clean-disabled`, `validate-on-migrate`. Fresh-DB and existing-DB paths verified (app boot + Testcontainers IT). Nadoumi migrations start at `V3` (`DATABASE_DESIGN.md` §6). Requires the `spring-boot-flyway` module (Boot 4 split — `flyway-core` alone does not auto-configure). |
| **Env-var contract** | **DONE (Phase 2)** — see §4.1. |
| Target runtime (Compose / ECS / K8s) | **OPEN** — ops decision, Phase 4+. |
| Where `nadoumi-web` is hosted | **OPEN** — with the runtime decision. |

### 4.1 Environment-variable contract (Phase 2 — implemented)

Committed `application*.yml` carries **no environment secret**; every value below is
`${VAR:dev-default}`. Real environments set these; the dev defaults keep a local
zero-config start working.

| Variable | Default (dev) | Notes |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `druid` | |
| `SPRING_DATASOURCE_DRUID_MASTER_URL` | `jdbc:mysql://localhost:3306/ry-vue?...&useSSL=true...` | full JDBC URL |
| `SPRING_DATASOURCE_DRUID_MASTER_USERNAME` | `root` | |
| `SPRING_DATASOURCE_DRUID_MASTER_PASSWORD` | `password` | |
| `SPRING_DATA_REDIS_HOST` / `_PORT` / `_DATABASE` | `localhost` / `6379` / `0` | |
| `SPRING_DATA_REDIS_PASSWORD` | *(empty)* | **required** in non-local envs |
| `SPRING_DATA_REDIS_SSL_ENABLED` | `false` | set `true` in non-local envs |
| `TOKEN_SECRET` | 90-char in-repo placeholder | **must** override; ≥ 64 bytes (HS512) |
| `TOKEN_KID` / `TOKEN_HEADER` / `TOKEN_EXPIRE_TIME` | `v1` / `Authorization` / `30` | |
| `DRUID_CONSOLE_USERNAME` / `DRUID_CONSOLE_PASSWORD` | `ruoyi` / `123456` | disable the servlet in prod |
| `RUOYI_PROFILE` | `D:/ruoyi/uploadPath` | upload dir — set on macOS/Linux |
| `NAD_STORAGE_*`, `NAD_MAIL_*` | — | reserved for Phase 4 (S3 / SMTP) |

## 5. Phase 2 status — what landed

| Step | Result |
| --- | --- |
| 1. Secrets externalized + JJWT `0.9.1 → 0.12.6` | `TokenService` rewritten; `kid` header; auth flow verified unchanged (login → getInfo, bad token → 401). |
| 2. Flyway `V1__ruoyi_baseline.sql` | fresh DB → V1 executes (31 tables + seeds); populated DB → BASELINE marker, schema/data untouched. Verified both paths. |
| 3. Quartz JDBC clustered store | `LocalDataSourceJobStore`; `QRTZ_JOB_DETAILS` rows persist across restart. Verified. |
| 4. Actuator | `health,info,metrics,prometheus`; `/actuator/health/**` anonymous, rest authenticated (one `permitAll` line in `SecurityConfig`). Same port as the app (see note). |
| 5. `nadoumi-modules` skeleton | `nadoumi-common` (SPIs: `NadoumiAccessService`, `DocumentStorage`, `NotificationChannel`, `GuardPredicate`) + empty `nadoumi-identity`. Wired into the reactor + `ruoyi-admin` deps. No `nad_*` tables, no business code. |
| 6. CI | `.github/workflows/ci.yml` — `mvn -B verify` on JDK 21 + Testcontainers Flyway IT. |
| 7. Maintained fork | `upstream` remote added. |

**Actuator port note:** the approved design (§3) wanted a separate management port.
Boot 4's separate-management-port child context is **not** covered by RuoYi's servlet
Spring Security chain by default (it would be *open*). Rather than ship an insecure
port, Phase 2 keeps Actuator on `:8080` under the existing security chain
(`metrics`/`prometheus`/`info` require auth; `health` is open for probes). A hardened
separate management port with its own auth is a later refinement.

Still-to-do items from §3 (containerization, per-env profile files, prod hardening
switches, storage, frontend delivery, observability shipping, deploy pipeline) remain
**PLANNED**.
