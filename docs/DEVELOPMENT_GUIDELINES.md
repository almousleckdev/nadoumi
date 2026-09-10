# Nadoumi — Development Guidelines

Status: **BASELINE** · **EXISTING** · **PLANNED** · **OPEN**.

This document is operational guidance for contributors. It complements `CLAUDE.md`
(the master instruction) — where they overlap, `CLAUDE.md` wins. Reconciled with the
Phase 1 baseline (`ARCHITECTURE.md` §7).

---

## 1. Local environment (EXISTING — verified in Phase 0)

Prerequisites: JDK 17+ (21 works), Maven 3.9+, Node 16/18 (20 works with a flag),
MySQL 8/9, Redis. **Or** just Docker:

```bash
# 0. One command for MySQL + Redis + Mailpit (SMTP sink, UI http://localhost:8025)
cp .env.example .env        # git-ignored; compose + your shell both read it
docker compose up -d        # brings up nadoumi-mysql / nadoumi-redis / nadoumi-mailpit
```

```bash
# 1. Database  (D8: the DB name is `ry-vue`, matching committed config)
#   Skipped if you used `docker compose up -d` above (MYSQL_DATABASE creates it).
mysql -uroot -e "CREATE DATABASE \`ry-vue\` DEFAULT CHARACTER SET utf8mb4;"
#   Schema is created by Flyway on first boot (V1__ruoyi_baseline.sql).
#   Do NOT hand-load sql/*.sql — an empty database is correct.
#   If you already have a populated `ry_vue`, rename it once so Flyway baselines it:
#     mysqldump -uroot --set-gtid-purged=OFF --no-tablespaces ry_vue > /tmp/ry.sql
#     mysql -uroot -e "CREATE DATABASE \`ry-vue\`" && mysql -uroot ry-vue < /tmp/ry.sql
#     mysql -uroot -e "DROP DATABASE ry_vue"      # after confirming ry-vue works

# 2. Redis
redis-server            # or: brew services start redis

# 3. Backend
mvn clean package -DskipTests
#   DB creds via env vars (the committed default password is `password`; a local root
#   with no password / no TLS needs the override):
SPRING_DATASOURCE_DRUID_MASTER_URL='jdbc:mysql://localhost:3306/ry-vue?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=GMT%2B8' \
SPRING_DATASOURCE_DRUID_MASTER_USERNAME=root \
SPRING_DATASOURCE_DRUID_MASTER_PASSWORD= \
java -jar ruoyi-admin/target/ruoyi-admin.jar
#   → http://localhost:8080 , login admin / admin123 (captcha on by default)
#   On first boot Flyway runs V1 and Quartz registers the seed jobs in QRTZ_* tables.

# 4. Admin frontend  (transitional Vue 2 admin; nadoumi-admin/RuoYi-Vue3 arrives Phase 3.5)
cd ruoyi-ui
npm install --legacy-peer-deps
NODE_OPTIONS=--openssl-legacy-provider npm run dev -- --port 8081
```

Smoke check without the UI:
```bash
curl -s localhost:8080/actuator/health                       # {"status":"UP", ...}
curl -s localhost:8080/captchaImage | head -c 60             # captcha + uuid
# (disable captcha for scripted login: sys_config sys.account.captchaEnabled=false
#  + redis-cli DEL sys_config:sys.account.captchaEnabled)
curl -s -XPOST localhost:8080/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
```

### Local overrides
- **DB / Redis / secrets** → environment variables (see `docs/DEPLOYMENT.md` §4 for the
  full list). This is also how CI / staging / prod inject config.
- `config/application-local.yml` (git-ignored; copy from
  `config/application-local.yml.example`) is loaded via `spring.config.import` for
  convenience local tweaks. **Caveat:** it does **not** override the datasource block
  (that lives in the profile-specific `application-druid.yml`, which wins) — use the
  `SPRING_DATASOURCE_DRUID_MASTER_*` env vars for DB creds.

### Local gotchas (EXISTING)
- `ruoyi.profile` defaults to `D:/ruoyi/uploadPath` — set `RUOYI_PROFILE=/abs/path`
  before testing uploads on macOS/Linux.
- `logback.xml` `log.path` is locally set to `./logs`.
- Node ≥17 without `--openssl-legacy-provider` → `ERR_OSSL_EVP_UNSUPPORTED`.
- npm 7+ without `--legacy-peer-deps` → peer-dependency resolution errors.
- `getInfo` returns password-policy fields the v3.9.2 UI ignores — harmless.
- After the JJWT upgrade, any token issued before the upgrade / a secret change is
  rejected → users re-login once. Expected.

## 2. Repository & branching (BASELINE)

- **`upstream` remote → `https://github.com/yangzongzhuan/RuoYi-Vue.git`** is
  configured (Phase 2). **`origin` still points at the same URL** — the team must
  repoint `origin` to Nadoumi's own repository. Do not push Nadoumi commits to
  `upstream`.
- **Monthly upstream review:** `git fetch upstream && git log --oneline master..upstream/master`;
  cherry-pick security/bug fixes with a `chore(upstream): …` commit prefix; skip
  frontend-only commits (Nadoumi has its own frontend line).
- Branch from `master`: `feat/<context>-<short>`, `fix/…`, `docs/…`, `chore/…`.
- Never commit directly to `master`. PR + review + green CI (`.github/workflows/ci.yml`)
  required.
- Conventional Commit style for messages.
- Do not commit secrets, `target/`, `node_modules/`, `dist/`, `logs/`, `config/*`
  (except `config/application-local.yml.example`), IDE files.

## 3. Module strategy (PLANNED)

Keep RuoYi modules (`ruoyi-admin/common/framework/system/generator/quartz`)
**unmodified where possible** — patch only for framework-wide concerns (security,
config). Add Nadoumi code in a new aggregator:

```
nadoumi-modules/            (new Maven aggregator; ruoyi-admin depends on it)
  nadoumi-common            DTO base, problem+json errors, NadoumiAccessService (@na),
                            DocumentStorage SPI, NotificationChannel SPI, guard-predicate enum
  nadoumi-identity          nad_user_applicant_access + /api/student/login + @na predicates   [Phase 3]
  nadoumi-applicant                                                                            [Phase 3]
  nadoumi-catalog           university + program + intake  (public)                            [Phase 3]
  nadoumi-scholarship       nad_scholarship + _internal + _program + v_scholarship_student     [Phase 3]
  nadoumi-workflow          nad_wf_* + WorkflowService (data-driven, fixed guard set)          [Phase 3]
  nadoumi-application       nad_application + children; the workbench API                      [Phase 3]
  nadoumi-document          nad_document*, S3 storage impls                                    [Phase 4]
  nadoumi-communication     conversations / messages / SSE                                     [Phase 4]
  nadoumi-notification      nad_notification*, email channel, Redis SSE fan-out                [Phase 4]
  nadoumi-partnership       nad_partnership*  (staff-only)                                      [Phase 4]
  nadoumi-content / -payment / -reporting                                                      [Phase 5+]
```

- Each Nadoumi module uses this strict package layout (established in the cleanup
  pass — `nadoumi-identity` / `nadoumi-applicant` are the reference):
  ```
  com.nadoumi.<context>
  ├── config           @Configuration
  ├── domain           entities (mutable persistence types)
  │   └── enums         context-local enums
  ├── mapper           MyBatis interfaces  (+ resources/mapper/<context>/*Mapper.xml)
  ├── service          application services
  ├── exception        context exceptions + the @RestControllerAdvice
  └── web
      ├── <X>Controller
      ├── request       @Valid request records
      └── response      response records
  ```
  Do not mix interfaces, implementations, DTOs, entities, enums, records, mappers and
  services in one package. `nadoumi-common` holds only cross-context value types + SPIs.
- `ruoyi-admin` depends on the Nadoumi modules and remains the single bootable app
  (modular monolith — **no microservices** without CLAUDE.md §19 approval).
- Cross-module calls go through **service interfaces**, never mapper-to-mapper.
- Confidentiality boundary is a **compile-time** boundary: student-facing controllers
  live in modules that cannot reference `nadoumi-scholarship`'s internal service or
  `nadoumi-partnership` at all.
- Nadoumi tables: `nad_<context>_<entity>`; staff permissions: `nad:<context>:<action>`
  (full catalogue: `docs/PERMISSION_CATALOGUE.md`).

## 4. Coding standards (BASELINE, building on EXISTING RuoYi style)

- Java 17 language level. Prefer `record` for DTOs and immutable value types.
- **Never serialize persistence entities on `/api/**`.** Map to DTO records at the
  controller boundary (MapStruct or explicit mappers).
- New `/api/**` endpoints use real HTTP status codes + `problem+json` errors — not
  `AjaxResult`. Existing `/system|/monitor|/tool` endpoints keep RuoYi conventions.
- Validation with `jakarta.validation` on DTOs; `@Validated` controllers.
- `@Transactional(rollbackFor = Exception.class)` on service methods that write —
  Spring's default only rolls back unchecked exceptions, so a checked / third-party
  `Exception` after a write would otherwise commit the partial change. Read-only
  methods use `@Transactional(readOnly = true)`. Keep transactions short; no remote
  calls inside a transaction.
- Authorization: `@PreAuthorize` **and** a service-level check via
  `NadoumiAccessService` for resource-scoped access (defense in depth). Every
  applicant-/application-scoped query takes an explicit auth-scope parameter.
- Append-only history: never `UPDATE` a row in `*_history`, `*_event`, `*_decision`.
- Optimistic locking (`@Version` / `version` column) on `nad_application` and other
  multi-editor entities.
- `@Log(title=…, businessType=…)` on every state-changing endpoint.
- No secrets, PII, or full request bodies in logs. Use parameterized SLF4J.
- No `${}` in MyBatis XML except for vetted dynamic ordering; use `#{}` everywhere else.
- Small cohesive services; no god classes; no duplicated business rules; name things
  for the domain, not for CRUD.

## 5. Database changes (BASELINE — D13, D14)

- **Flyway** (`classpath:db/migration`, `baseline-on-migrate=true`,
  `baseline-version=1`, `clean-disabled=true`). `V1__ruoyi_baseline.sql` = current
  `sql/*.sql`; `V2` = RuoYi-side dict/menu seed; **Nadoumi tables from `V3`**
  (`DATABASE_DESIGN.md` §6). No hand-run SQL in shared environments.
- The reviewed draft `docs/ddl/nad_core.draft.sql` becomes `V3`–`V8` after the
  §review-checklist at the bottom of that file is cleared — it is **not** a migration
  and Flyway never sees it.
- Every new table: `bigint` auto-increment PK (D13); **explicit named FK
  constraints** (a deliberate improvement over RuoYi's FK-less style),
  `ON DELETE RESTRICT` (business rows are archived, not deleted); `NOT NULL` +
  defaults; unique + index coverage on every FK and common filter/sort column; RuoYi
  audit columns on mutable entities; explicit `status`/lifecycle column;
  append-only tables (`*_event`, `*_history`, `*_decision`, `*_version`) never
  `UPDATE`d/`DELETE`d.
- MySQL has no partial indexes — enforce "one active X" invariants with a **stored
  generated guard column + plain `UNIQUE`** plus a `SELECT … FOR UPDATE` service check
  (see `nad_user_applicant_access.owner_guard`).
- `sys_menu` / `sys_role` / `sys_role_menu` seeding is always a migration.

## 6. Testing (BASELINE — CLAUDE.md §17)

- **Unit:** domain/service logic, workflow guards, mappers-to-DTO.
- **Service/slice:** `@MyBatisTest` / `@SpringBootTest` slices against a real MySQL via
  **Testcontainers** (not H2 — RuoYi SQL is MySQL-flavoured).
- **Controller/API:** `@WebMvcTest` / `MockMvc` for status codes, validation, payload
  shape.
- **Authorization tests:** per-role permission matrix; cross-applicant /
  cross-application denial; data-scope isolation.
- **Confidentiality tests (CI-blocking):** `ScholarshipConfidentialityTest`,
  `PartnershipExposureTest` — prove student/public responses never contain
  university/partnership linkage.
- **Integration:** login → application create → workflow transition → document upload →
  notification emitted.
- New business feature ⇒ tests in the same PR. Critical security rules ⇒ automated
  tests are mandatory.

## 6a. Adding a transactional email (BASELINE)

Every Nadoumi email uses the one shared design — never hand-roll HTML/CSS
(`COMMUNICATION_AND_NOTIFICATIONS.md` §4.3).

1. **Build an `EmailContent`** (`com.nadoumi.identity.service.mail`) with
   `EmailContent.builder(heading)` — paragraphs, an optional `code(...)`,
   `keyValues(...)`, a single `cta(label, url)`, and `itemGroup(...)` for
   link lists (empty groups drop themselves). Set `showPreferencesLink(true)`
   only for non-transactional mail. URLs come from `BrandProperties.url("/path")`.
2. **Render + send:** `EmailRender r = emailLayout.render(content);`
   `mailSender.send(new EmailMessage(to, subject, r.text(), r.html()));`
   Editable copy that ops should change without a redeploy lives in
   `resources/mail/*.txt` (`${var}`, `MailTemplates`) or
   `nad_notification_template` (`{{var}}`).
3. **Notification-pipeline email** (fan-out, preferences, retries): add a
   `NotificationType`, an outbox event type + `OutboxToNotificationDispatcher`
   mapping, and `en` templates in a new `V*` migration. `EmailNotificationChannel`
   wraps the body in `EmailLayout` automatically; give the type a CTA in
   `ctaFor(...)`.
4. **Never** put PII/confidential fields in a template or an outbox payload
   (`SECURITY.md` §6). Student-facing catalog data comes from the **public**
   services only.
5. **Tests:** assert the plain-text part carries every URL/code the HTML does,
   the footer renders, and (student-facing) no confidential field leaks.

## 7. Definition of Done (EXISTING — CLAUDE.md §22)

Requirements understood → architecture documented → DB designed → backend →
frontend → authorization → validation → tests → build passes → docs updated
(in the **same** change).

## 8. Documentation (EXISTING — CLAUDE.md §5)

`docs/` is the source of truth. When architecture changes, update the relevant doc in
the same PR. Keep the status labels honest — `BASELINE` / `EXISTING` / `PLANNED` /
`OPEN` — never describe something as implemented (`EXISTING`) when it is only
`PLANNED`.

## 9. Decision index (post Phase 1)

**All approved** — see `docs/ARCHITECTURE.md` §7 for the outcomes:
D1 admin UI → RuoYi-Vue3 · D2 public/student → Nuxt 3 + BFF cookie · D3 maintained
fork · D4 workflow → data-driven + fixed guards · D5 storage → S3 SPI · D6 realtime →
SSE + email-only + Redis fan-out · D7 user↔applicant access model · D8 env-var secrets
+ DB rename · D9 3-type application target · D10 `sys_user`+`user_type` identity ·
D11 document attachment M:N + checklist · D12 single task table · D13 auto-increment
PKs · D14 Flyway.

**Still OPEN (tracked, non-blocking):** guardian-of-minor legal nuance ·
API response-envelope + versioning final confirmation · PII column-encryption scope +
data residency · concrete email/SMS/WhatsApp vendors · runtime target
(Compose/ECS/K8s) · `nad_partnership_program` · per-environment CORS origins ·
i18n locale scope.

## 10. Phase 2 — enabling infrastructure (DONE)

Implemented, `mvn clean verify` green, RuoYi behaviour preserved:

1. **Secrets externalized** — all DB / Redis / JWT / Druid-console values are
   `${ENV_VAR:dev-default}` (`docs/DEPLOYMENT.md` §4.1). **JJWT `0.9.1 → 0.12.6`**
   (`jjwt-api`/`impl`/`jackson`); `TokenService` uses `Keys.hmacShaKeyFor` +
   `verifyWith`/`parseSignedClaims`; emits a `kid` header.
2. **Flyway** — `ruoyi-admin/src/main/resources/db/migration/V1__ruoyi_baseline.sql`
   (= `sql/ry_20260417.sql` + `sql/quartz.sql`). Needs the `spring-boot-flyway`
   module. Fresh-DB (executes) and existing-DB (baselines, untouched) paths both
   verified. Nadoumi migrations start at `V3`.
3. **Quartz JDBC clustered store** via `spring.quartz.*` (`LocalDataSourceJobStore`,
   `initialize-schema=never`). Restart-persistence verified.
4. **Actuator** — `health,info,metrics,prometheus`; `/actuator/health/**` anonymous
   (one `permitAll` line in `SecurityConfig`), the rest authenticated. Same port
   `:8080` (see `DEPLOYMENT.md` §5 note).
5. **`nadoumi-modules/`** aggregator: `nadoumi-common` (SPIs
   `NadoumiAccessService`, `DocumentStorage`, `NotificationChannel`, enum
   `GuardPredicate`) + empty `nadoumi-identity`. Wired into the reactor and
   `ruoyi-admin`. **No `nad_*` tables, no business code.**
6. **CI** — `.github/workflows/ci.yml` (`mvn -B verify`, JDK 21, Testcontainers
   Flyway IT).
7. **`upstream` remote** added (§2).

## 11. Companion design artifacts (Phase 1)

- `docs/PERMISSION_CATALOGUE.md` — staff permission tokens + 10-role matrix + data
  scope + sensitive-permission matrix.
- `docs/WORKFLOW_PROGRAM_WITH_SCHOLARSHIP.md` — the first concrete
  `nad_wf_definition` (stages, transitions, guards, tasks, document requirements,
  notifications, SLAs).
- `docs/ddl/nad_core.draft.sql` — **reviewed, non-executable** DDL for the
  Identity / Applicant / University / Program / Scholarship / Application slices;
  becomes Flyway `V3`–`V8` in Phase 3 after its review checklist is cleared.
