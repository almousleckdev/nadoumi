# Nadoumi — Architecture

Status legend: **BASELINE** = approved architecture, build to this · **EXISTING** =
present in this repository today · **PLANNED** = approved, not yet built · **OPEN** =
still needs an explicit decision.

> **Revision 3 — Phase 1 (Documentation Reconciliation & Final Design). APPROVED
> BASELINE.** All 14 decisions D1–D14 are resolved (§7). This file, `DOMAIN_MODEL.md`
> and `DATABASE_DESIGN.md` form the frozen architecture baseline for Phase 2, and the
> other ten `docs/` files plus the three new companion artifacts
> (`PERMISSION_CATALOGUE.md`, `WORKFLOW_PROGRAM_WITH_SCHOLARSHIP.md`,
> `ddl/nad_core.draft.sql`) are reconciled to it. Still **no business code, no `nad_*`
> tables, no DDL executed** — Phase 1 is documentation only.

---

## 1. Summary

**EXISTING.** This repository is the official **RuoYi-Vue** rapid-development framework
(`origin = https://github.com/yangzongzhuan/RuoYi-Vue.git`), backend artifact version
`3.9.2`, but the working branch (`master`) is **32 commits ahead of tag `v3.9.2`** and has
been customized:

- Spring Boot upgraded to **4.1.0**, `java.version = 17` (builds and runs on JDK 21 locally).
- `poi` 5.5.1, `fastjson2` 2.0.64, assorted dependency bumps.
- The **`ruoyi-ui` frontend directory was deleted** from the repo at commit `ec987ab9`
  ("update README.md", 2026-08-16). It has been **restored from tag `v3.9.2`** during this
  Phase 0 (see `docs/FRONTEND_ARCHITECTURE.md`).
- Newer-than-3.9.2 features already merged: notice read-tracking (`sys_notice_read`),
  configurable password rules (`sys_user.pwd_update_date`, `sys.account.chrtype`),
  wildcard permit-all path matching.

Nadoumi is **not yet present** in any form. There are no Nadoumi domain modules, tables,
entities, endpoints, or frontend screens. `docs/` contained empty placeholder files only.

## 2. Current runtime topology (EXISTING)

```
┌────────────┐   HTTP/JSON (Bearer JWT)   ┌───────────────────────────┐
│ ruoyi-ui   │ ─────────────────────────► │ ruoyi-admin (Spring Boot) │
│ Vue 2 SPA  │   dev proxy /dev-api        │  Tomcat 11, port 8080     │
│ Vue CLI 4  │ ◄───────────────────────── │  context-path /           │
└────────────┘                            └────────────┬──────────────┘
                                                       │
                         ┌─────────────────────────────┼───────────────────────┐
                         ▼                             ▼                        ▼
                  ┌────────────┐              ┌──────────────┐         ┌─────────────────┐
                  │ MySQL 8/9  │              │ Redis        │         │ Local filesystem│
                  │ (Druid pool)│             │ (Lettuce)    │         │ ruoyi.profile   │
                  │ schema:     │             │ token store, │         │ upload dir      │
                  │ sys_* etc.  │             │ captcha,     │         │ served /profile │
                  └────────────┘              │ dict/config  │         └─────────────────┘
                                              │ cache        │
                                              └──────────────┘
```

**Verified working in Phase 0** (JDK 21, Maven 3.9.16, Node 20.19.2, MySQL 9.7.1, Redis):
`mvn clean package` (90 MB boot jar) · app boot in ~3.6 s · `GET /captchaImage` ·
`POST /login` returns JWT · `GET /getInfo` · `GET /getRouters` · `ruoyi-ui`
`npm install` + `vue-cli-service build` + `serve`.

## 3. Maven module layout (EXISTING)

| Module | Responsibility |
| --- | --- |
| `ruoyi-admin` | Bootstrap (`RuoYiApplication`), web layer: controllers under `com.ruoyi.web.controller.{system,monitor,common,tool}`, MVC/OpenAPI config. |
| `ruoyi-common` | Cross-cutting: base domain (`AjaxResult`, `TableDataInfo`, `BaseEntity`, `SysUser`…), enums, constants, annotations (`@Log`, `@DataScope`, `@Excel`, `@RepeatSubmit`, `@Anonymous`), utils, XSS filter, exceptions, Redis cache helper. |
| `ruoyi-framework` | Security (JWT filter, entry point, logout handler), `SecurityConfig`, Druid multi-datasource + `@DataSource` AOP, AspectJ (log, data-scope, rate-limit), Redis config, async task manager, MyBatis config, resource/CORS/filter config, `SysLoginService`/`TokenService`/`SysPermissionService`. |
| `ruoyi-system` | System domain: `sys_*` entities, MyBatis mappers + XML, services for user/role/menu/dept/post/dict/config/notice/logininfor/operlog. |
| `ruoyi-generator` | Code generator: reads `information_schema`, Velocity templates → Java/Vue/XML/SQL. |
| `ruoyi-quartz` | Scheduled jobs (`sys_job`, `sys_job_log`), Quartz integration, `@RateLimiter` support tables. |

**PROPOSED** target adds a sibling aggregator `nadoumi-modules/` (see
`docs/DEVELOPMENT_GUIDELINES.md` §"Module strategy"). No microservices — modular monolith.

**EXISTING (P1).** `nadoumi-modules/` now contains, alongside the domain modules
(`nadoumi-identity`, `nadoumi-applicant`, `nadoumi-university`, `nadoumi-scholarship`,
`nadoumi-program`, …), one **foundational infrastructure module**:
`nadoumi-media` — a peer of `nadoumi-identity`, not itself a CLAUDE.md §7 business
domain. It provides the `MediaStorageService` implementation
(`CloudinaryMediaStorage`) and the `MediaGateway` façade that every domain module
depends on for image/document storage (DM8, spec
`docs/superpowers/specs/2026-09-03-media-storage-and-application-engine-design.md`
§I.2). `ruoyi-admin` depends on it so the beans are on the runtime classpath;
catalog/applicant modules depend only on the `MediaStorageService`/`MediaGateway`
interfaces in `nadoumi-common`.

## 4. Layering conventions (EXISTING, inherited)

`Controller (extends BaseController)` → `IService` / `ServiceImpl` → `Mapper` (MyBatis XML)
→ MySQL. Responses are always `AjaxResult` (map with `code`/`msg`/`data`) or
`TableDataInfo` (list + `total`) — **not** raw entities wrapped by a generic envelope type.
Pagination via PageHelper `startPage()` reading `pageNum`/`pageSize` request params.

**PROPOSED for Nadoumi:** keep the layering but introduce explicit **DTO / request /
response records per bounded context** and **MapStruct or hand-written mappers**, so
persistence entities are never serialized directly on student-facing or partner-facing
APIs (see `docs/API_DESIGN.md`, `docs/SECURITY.md` §confidentiality).

## 5. Cross-cutting infrastructure (EXISTING — reuse for Nadoumi)

- **AuthN/AuthZ:** Spring Security stateless + JWT; method security `@PreAuthorize("@ss.hasPermi('...')")`.
- **RBAC:** users ↔ roles ↔ menus/permissions; dept-tree data scope.
- **Audit:** `@Log` AOP → `sys_oper_log`; login events → `sys_logininfor`.
- **Caching:** `RedisCache` wrapper over `RedisTemplate`.
- **Config:** `sys_config` (runtime key/value, Redis-cached); `sys_dict_*` enumerations.
- **Scheduling:** Quartz via `ruoyi-quartz`.
- **Code generation:** `ruoyi-generator` (useful to scaffold Nadoumi CRUD admin screens,
  then hand-refine — do **not** ship generated code unreviewed).
- **API docs:** SpringDoc / Swagger UI at `/swagger-ui.html` (currently scans only
  `com.ruoyi.web.controller.tool`).

## 6. Key architectural risks / debt

| # | Risk | Impact | See |
| --- | --- | --- | --- |
| A1 | ~~JWT secret hard-coded weak string, HS512.~~ **RESOLVED (Phase 2):** `${TOKEN_SECRET:…}` (labelled dev default, ≥64-byte required); `jjwt 0.12.6`; `kid` header for rotation. | — | SECURITY §4 |
| A2 | ~~DB / Druid-console creds committed.~~ **RESOLVED (Phase 2):** all `${ENV_VAR:dev-default}`; `spring.config.import` for local; `config/` git-ignored. | — | DEPLOYMENT §4.1 |
| A3 | `ruoyi.profile` = `D:/ruoyi/uploadPath` (Windows path); files on local FS, no object storage. | Broken on macOS/Linux; not production-grade; no doc lifecycle. | DOCUMENT_MANAGEMENT |
| A4 | Frontend toolchain is Vue 2 + Vue CLI 4 (webpack 4); "maintenance focus has moved" per upstream README; needs `--openssl-legacy-provider` on Node ≥17. | Long-term maintainability of the admin UI. | FRONTEND_ARCHITECTURE, DECISION D1 |
| A5 | No public/student frontend of any kind; no SEO surface. | Core Nadoumi requirement unmet. | FRONTEND_ARCHITECTURE, DECISION D2 |
| A6 | RuoYi generic CRUD + single global enums/dicts encourage modelling business cases as flat records. | Conflicts with Nadoumi application-as-business-case + per-program workflow. | APPLICATION_WORKFLOW, DOMAIN_MODEL |
| A7 | ~~Quartz running with `RAMJobStore`.~~ **RESOLVED (Phase 2):** JDBC clustered `LocalDataSourceJobStore`; restart-persistence verified. | — | DEPLOYMENT §5 |
| A8 | Repo `master` diverged from upstream tag and dropped the frontend. **Mitigated (Phase 2):** `upstream` remote added + monthly-merge process (`origin` repoint is a team action). | Residual: manual merge discipline. | DEPLOYMENT §4, DECISION D3 |
| A9 | `getInfo` returns password-rule fields the restored `v3.9.2` UI does not render (`isPasswordExpired` etc.). | Cosmetic today; UI/BE version skew to track. | FRONTEND_ARCHITECTURE |
| A10 | On login RuoYi snapshots `LoginUser` (user + `permissions`) into Redis for the token lifetime. If per-applicant access grants were placed in `permissions`, revocation would lag by up to the token TTL. | Security: stale access after a grant is revoked. **Mitigation adopted:** external `User↔Applicant` access is checked **live** per request against `nad_user_applicant_access`, never cached in `LoginUser`. | SECURITY, DOMAIN_MODEL §4.3 |
| A11 | `sys_user.user_type` is a dormant column (never read in Java). Nadoumi needs it to separate staff from external identities. | Low — populating it is additive, no `ALTER`. | DOMAIN_MODEL §3, DECISION D10 |

## 7. Decision register — APPROVED (Phase 1)

All fourteen decisions are approved. Details in the linked docs. "OPEN sub-items"
listed at the end do **not** block Phase 2.

| ID | Decision | Approved outcome | Owner doc |
| --- | --- | --- | --- |
| **D1** | Admin UI baseline | **Migrate to `RuoYi-Vue3`** (Vue 3 + Element Plus + Vite + Pinia + Vue Router 4). The restored `ruoyi-ui` (Vue 2) stays in-tree as a *transitional* working admin only; **no Nadoumi admin screen is built on Vue 2**. Code-generator templates target Vue 3. Integration scheduled as Phase 3.5. | FRONTEND_ARCHITECTURE, ADMIN_ARCHITECTURE |
| **D2** | Public / student experience | **Separate Nuxt 3 app at `nadoumi-web/`** (this repo). SSR for catalog/content; consumes `/api/public` + `/api/student`. Auth via `/api/student/login`; the JWT is held **only** in an httpOnly + Secure + SameSite=Lax cookie set by Nuxt server routes (a thin BFF) — the browser JS never sees the raw token. | FRONTEND_ARCHITECTURE |
| **D3** | Upstream tracking | **Maintained fork.** `origin` → Nadoumi repo; add remote `upstream` → `yangzongzhuan/RuoYi-Vue`; monthly upstream review, security fixes cherry-picked; keep Nadoumi's own frontend line. | DEPLOYMENT, DEVELOPMENT_GUIDELINES |
| **D4** | Workflow engine | **Data-driven config tables + `WorkflowService`.** Guards are a **fixed predicate set** (`ALL_MANDATORY_TASKS_DONE`, `ALL_REQUIRED_DOCUMENTS_VERIFIED`, `DECISION_RECORDED(type)`, `PAYMENT_SETTLED(kind)`, `FIELD_SET(name)`) — **no** SpEL / expression language in v1. Instances pin `definition_version`; no auto-migration. | APPLICATION_WORKFLOW |
| **D5** | Document object storage | **SUPERSEDED by DM1 (P1, media/file-storage spec).** Original decision was an S3-compatible `DocumentStorage` SPI (`local` dev / MinIO staging / AWS S3 prod). **Built instead: Cloudinary** is the sole managed storage provider, behind the `MediaStorageService` SPI (+ `MediaGateway` façade) in `nadoumi-common`, implemented by `CloudinaryMediaStorage` in the new `nadoumi-media` module. D5's *principle* — provider-agnostic SPI, bytes never in MySQL, downloads only through an authorized API (signed URL or stream-proxy) — is retained; the provider choice and the `local`/`s3` impl split are dropped. The `DocumentStorage` SPI stub in `nadoumi-common` was deleted. | DOCUMENT_MANAGEMENT, §7.1 below |
| **D6** | Realtime + notification channels | **SSE** for v1 (`/api/{student,staff}/stream`) for notification + "new message" pings; polling fallback; WebSocket deferred. `NotificationChannel` SPI with **IN_APP + EMAIL only** built in v1 (email via an SMTP abstraction, provider chosen at deploy time). SMS / WhatsApp / PUSH: enum kept, no impl. Multi-instance fan-out via **Redis pub/sub** from day one. | COMMUNICATION_AND_NOTIFICATIONS |
| **D7** | `User ↔ Applicant` access model | **As specified in DOMAIN_MODEL §4**: `nad_user_applicant_access` grant, roles OWNER/AGENT/GUARDIAN/VIEWER + capability matrix, `capability_overrides_json`, PENDING email invites, one-active-OWNER invariant, per-application scoping, **live** (non-cached) revocation. Guardian default = no submit (staff override possible). Staff-created applicant = interim staff OWNER (`is_interim`, 30-day expiry) + PENDING invite; non-acceptance → applicant `UNLINKED`. | DOMAIN_MODEL §4, SECURITY §2 |
| **D8** | DB name + credentials | Secrets via **env vars** + optional git-ignored `config/application-local.yml` (`spring.config.import`). Committed default stays `ry-vue`/`root`/`password`; local dev DB renamed `ry_vue`→`ry-vue`. | DEPLOYMENT, DATABASE_DESIGN, DEVELOPMENT_GUIDELINES |
| **D9** | Application target | **3-type discriminator** `PROGRAM_ONLY` / `PROGRAM_WITH_SCHOLARSHIP` / `SCHOLARSHIP_LED`, nullable `program_id`/`scholarship_id`/`intake_id`, DB + service invariants INV6–INV8. | DOMAIN_MODEL §5, DATABASE_DESIGN §5.5 |
| **D10** | Identity split | Every human is a `sys_user`; `user_type` `00` staff / `10` student / `20` agent / `30` guardian; externals get **no** `sys_role`/`sys_menu`; separate `/api/student/login`. | DOMAIN_MODEL §3, §8 below |
| **D11** | Document attachment | Applicant-owned `nad_document` + `nad_application_document` M:N + `nad_document_requirement` checklist; `current_version_id` is the authoritative version. No array columns. | DOMAIN_MODEL, DOCUMENT_MANAGEMENT |
| **D12** | Task table | **One** `nad_application_task` (nullable `wf_stage_task_template_id` distinguishes engine-materialized vs ad-hoc). `nad_wf_instance_task` dropped. | DOMAIN_MODEL, APPLICATION_WORKFLOW |
| **D13** | PK generation | `bigint` **auto-increment** for v1. | DATABASE_DESIGN |
| **D14** | Migrations | **Flyway**; `V1__ruoyi_baseline.sql` = current schema (`baseline-on-migrate`, `clean-disabled`); `V2` RuoYi seed; Nadoumi from `V3`. | DATABASE_DESIGN §6, DEPLOYMENT |

**OPEN sub-items (tracked, non-blocking):** guardian-of-minor legal nuance per
destination country; API response-envelope + versioning final confirmation
(recommendation: bare bodies + `problem+json` + URI `/api/v1`); PII column-encryption
scope + data-residency; concrete email/SMS/WhatsApp vendors; runtime target
(Compose / ECS / K8s); `nad_partnership_program`; per-environment CORS origins.

### 7.1 Media & document access flow (EXISTING — DM1–DM8, P1)

Every protected or sensitive media/document access — applicant photo today,
verification documents once `nadoumi-document` (Step 7) lands — goes through the
same shape, never a stored Cloudinary URL:

```
Student / Admin request
        │
        ▼
Nadoumi authN (JWT / session)
        │
        ▼
Nadoumi authorization (ownership · grant · staff role · app visibility)
        │  deny ──► 403 + nad_media_access_log(result=DENIED, deny_reason)
        │ grant
        ▼
Media access service (MediaGateway, nadoumi-media)
        │  nad_media_access_log(result=GRANTED)
        ▼
Cloudinary (MediaStorageService: CloudinaryMediaStorage)
        │
        ├─ PROTECTED  → short-TTL signed URL  → 302 / {url, expiresAt}
        └─ SENSITIVE  → Nadoumi backend proxy → bytes streamed, no URL to client
```

PUBLIC assets (catalog imagery) skip the authorization/log step and are served
directly from Cloudinary's CDN (`secure_url`) or via the `GET /api/media/{id}`
redirect. Full model: `docs/DOCUMENT_MANAGEMENT.md` §3.2–§3.3,
`docs/SECURITY.md` "File & document storage".

## 8. Identity & authorization architecture (BASELINE · D10 APPROVED)

The single most consequential structural decision for Nadoumi. **Approved:**

- **One authentication stack.** Every authenticated human is a `sys_user` row and logs
  in through RuoYi's existing pipeline (captcha, BCrypt, 5-try/10-min lockout, JWT,
  Redis `LoginUser` session). No parallel account table, no second auth codebase.
- **`sys_user.user_type` becomes the identity discriminator:** `00` staff · `10`
  student · `20` agent · `30` guardian. It is a dormant column today (no `ALTER`
  needed).
- **Two authorization systems, cleanly separated:**
  1. **Staff (`00`)** → RuoYi **RBAC**: `sys_role` / `sys_menu` (`nad:<ctx>:<action>`
     permissions) + `sys_dept` data scope + `nad_application.assignee_user_id`.
  2. **External (`10/20/30`)** → **resource grants**: `nad_user_applicant_access`
     (DOMAIN_MODEL §4), evaluated **live per request** (never cached into
     `LoginUser.permissions` — see risk A10). External users get **no** `sys_role` /
     `sys_menu` rows, so they can never hold an admin permission.
- **Endpoint segmentation mirrors this:** `/login` (staff only) · `/api/student/login`
  (external only) · `/api/public/**` (anon) · `/api/student/**` (grants) ·
  `/api/staff/**` (RBAC) · `/api/internal/**` (service). Existing `/system|/monitor|
  /tool` stay staff-only and unchanged.
- **Defense in depth everywhere:** `@PreAuthorize("@na.can…")` **and** a service-level
  re-check **and** an explicit `authScope` parameter on every applicant-/application-
  scoped query. Frontend guards are UX only.
- **Confidentiality is structural, not cosmetic** (DOMAIN_MODEL §6): separate tables +
  a `v_scholarship_student` DB view that student mappers exclusively read + a
  single choke-point query service + type-safe `record` DTOs + a response-body
  denylist net + query-param guards + CI-blocking leak tests.

Alternative considered and **not** recommended: a dedicated `nad_account` table with a
parallel login/JWT stack for externals — doubles the auth attack surface and
maintenance for no domain benefit.
