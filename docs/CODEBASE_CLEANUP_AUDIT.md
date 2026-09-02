# Nadoumi — Codebase Cleanup & Professionalization Audit

Status: **AUDIT — no code changed yet.** Produced per the cleanup-phase step 10.
Execution is incremental, group by group, each followed by `mvn clean verify`
(+ frontend lint/build once a frontend is in scope).

Scale of the repo:

| Area | Size |
| --- | --- |
| RuoYi Java (`ruoyi-common/framework/system/quartz/generator/admin`) | 253 files — **all 253 contain Chinese** comments / Javadoc |
| Nadoumi Java (`nadoumi-common/identity/applicant`) | 63 files — **0 contain Chinese** |
| `ruoyi-ui` (Vue 2 admin) | 175 `src` files — **135 contain Chinese** |
| SQL | `V1` baseline (frozen) + `V2`–`V4` (Nadoumi) + `sql/*.sql` (source of V1) |
| Docs | 16 `docs/*.md` |

---

## 0. Decisions (confirmed 2026-09-01)

- **D-CLEAN-1 → leave `ruoyi-ui` untouched** (transitional/reference; removed at Vue 3
  admin parity). Frontend work this phase = `nadoumi-web/` scaffold + Vue 3 admin
  *architecture* only.
- **D-CLEAN-2 → functional strings only** now; wholesale RuoYi comment/Javadoc
  translation is deferred (G7, optional).
- **V2–V4 not applied to any real DB** → edit them in place (comment-strip), no new
  migration needed for that.
- **RuoYi `admin` (id 1) → disabled break-glass** (`status='1'`, keep role/menu +
  audit history, never deleted); `almousleck` is the working super-admin; admin
  user-management screens must not surface the disabled break-glass as the normal
  admin; documented in `docs/SECURITY.md`.

## 1. Two decisions that change the scope of this phase

### D-CLEAN-1 — `ruoyi-ui` is out of scope for deep cleanup

`docs/FRONTEND_ARCHITECTURE.md` **D1 (APPROVED)**: `ruoyi-ui` (Vue 2) is
**transitional / reference-only** — *"no Nadoumi admin screen is built on Vue 2"* —
and is **deleted** once `nadoumi-admin/` (RuoYi-Vue3) reaches parity.

Deep-cleaning `ruoyi-ui` (translating 135 files, consolidating Vue 2 components,
de-duplicating styles) is polishing a directory the approved architecture discards.

**Recommendation:** treat `ruoyi-ui` as frozen reference. The frontend cleanup work
(cleanup-phase steps 7–8) targets the **new** apps: scaffold `nadoumi-web/` (Nuxt 3,
public site) now with clean structure; the full `nadoumi-admin/` (RuoYi-Vue3) adoption
stays "Phase 3.5" as already documented. **Needs your confirmation.**

### D-CLEAN-2 — RuoYi source comments are *not* translated wholesale

Every RuoYi `.java` file carries Chinese comments/Javadoc. Translating all 253 =
touching every RuoYi file = exactly the "blindly rewrite RuoYi" that cleanup-phase
step 9 forbids, with real risk (i18n keys, `@Log` titles that become
`sys_oper_log.title`, user-facing exception text).

**Recommendation:** translate only **functional** Chinese now (i18n bundle, user-facing
messages, `@Log` titles, boot banner, README, scripts, config comments). Comment/Javadoc
translation inside RuoYi framework internals is a **deferred, optional, module-by-module**
pass — not part of this phase's Definition of Done. Nadoumi code is already English.
**Needs your confirmation.**

---

## 2. Chinese-language findings

| # | Location | What | Action | Risk |
| --- | --- | --- | --- | --- |
| CN-1 | `ruoyi-admin/.../i18n/messages.properties` | The only message bundle; all values Chinese; no English default, no `_zh_CN` | Make English the default `messages.properties`; move current text to `messages_zh_CN.properties` | **Low** — keyed lookups, values swap |
| CN-2 | `ruoyi-admin/.../RuoYiApplication.java` | `System.out.println("(♥◠‿◠)ﾉﾞ 若依启动成功 …")` boot banner | Delete the banner (or plain `log.info("Nadoumi API started")`) | **Low** |
| CN-3 | `ruoyi-quartz/.../task/RyTask.java` | Demo scheduled task, Chinese `System.out.println` | Delete file (see DEAD-3) | **Low** |
| CN-4 | `ruoyi-common/.../utils/html/EscapeUtil.java` `main()` | Dead debug `main` with `System.out.println` | Delete the `main` method | **Low** |
| CN-5 | `@Log(title = "…")` on RuoYi controllers (`ruoyi-admin`, ~15 files) | Chinese audit-log module names → `sys_oper_log.title` | Translate the `title` strings | **Low–Med** — changes audit labels (desired) |
| CN-6 | user-facing exceptions in `ruoyi-common` (`*Exception` `getDefaultMessage`, `MessageUtils` keys) | Chinese fallback text | Route through CN-1 bundle keys; translate literals | **Med** — must not break message keys |
| CN-7 | `application.yml`, `application-druid.yml`, `logback.xml`, `mybatis/mybatis-config.xml`, `generator.yml` | Chinese inline comments | Translate comments (no value changes) | **Low** |
| CN-8 | mapper XML (`ruoyi-system`, `ruoyi-quartz`, `ruoyi-generator`) | Chinese `<!-- comments -->` | Translate comments only; **do not touch SQL** | **Low** |
| CN-9 | `README.md` | RuoYi Chinese marketing README (gitee/oschina badges) | Replace with a Nadoumi README | **Low** |
| CN-10 | `ry.sh` | Chinese comments | Translate, keep | **Low** |
| CN-11 | `ry.bat`, `bin/*.bat`, `ruoyi-ui/bin/*.bat` | Windows scripts, GBK-mojibake Chinese | Delete (Mac/Linux team; `ry.sh` + Maven cover it) | **Low** |
| CN-12 | `doc/若依环境使用手册.docx` (428 KB) | RuoYi Chinese setup manual | Delete | **Low** |
| CN-13 | RuoYi `.java` comments/Javadoc (all 253 files) | Chinese comments | **Deferred** per D-CLEAN-2 | — |
| CN-14 | `sql/ry_20260417.sql` + `V1` migration | Chinese schema comments + demo data | `V1` is frozen; handled additively — see §5 | **Med** |
| CN-15 | `ruoyi-ui` (135 files) | Chinese UI text / comments | **Out of scope** per D-CLEAN-1 | — |

## 3. Dead / demo code

| # | Item | Evidence | Action | Risk |
| --- | --- | --- | --- | --- |
| DEAD-1 | `ruoyi-admin/.../web/controller/tool/TestController.java` | `/test/user/**`, in-memory `LinkedHashMap`, `@Tag("用户信息管理")` — RuoYi Swagger showcase | Delete file | **Low** — no caller; verify no menu row references `/tool/test` |
| DEAD-2 | `RuoYiApplication` boot banner | see CN-2 | Delete | **Low** |
| DEAD-3 | `ruoyi-quartz/.../task/RyTask.java` + demo `sys_job` rows 1–3 (`ryTask.ryNoParams` / `ryParams` / `ryMultipleParams`) | RuoYi sample cron jobs | Delete class; drop the 3 seed rows via the §5 cleanup migration | **Low** — Quartz JDBC store: also delete matching `QRTZ_*` rows if a shared DB already has them |
| DEAD-4 | `EscapeUtil.main()` | debug `main` | Delete method | **Low** |
| DEAD-5 | `SysIndexController` | returns a static "welcome, RuoYi vX running" string at `/` | Trim to a plain 200 or delete (SPA serves `/`) | **Low** |
| DEAD-6 | `ry.bat`, `bin/*.bat` | Windows-only, mojibake | Delete | **Low** |
| DEAD-7 | `doc/` dir (single `.docx`) | RuoYi manual | Delete dir | **Low** |
| DEAD-8 | `sql/ry_20260417.sql`, `sql/quartz.sql` | Superseded by `V1` migration; kept only as V1's source-of-record | Keep but add a header note pointing at `V1`; or move under `docs/ddl/` | **Low** |

No `gen_table` / `gen_table_column` demo rows exist (good — no code-generator sample tables).

## 4. Java package / responsibility findings

### 4.1 Nadoumi modules — restructure to the cleanup-phase §2 layout (SAFE)

Current vs target (all Nadoumi code, full test coverage — **Low risk**):

| Current | Problem | Target |
| --- | --- | --- |
| `identity/web/StudentAuthService.java` | a **service** under `web` | `identity/service/StudentAuthService.java` |
| `identity/web/Nad{NotFound,BadRequest,Forbidden}Exception.java`, `NadApiExceptionHandler.java` | **exceptions + advice** under `web` | `identity/exception/…` (advice stays `web/` or `exception/` — pick one) |
| `identity/grant/GrantException.java` | exception mixed with service | `identity/exception/GrantException.java` |
| `identity/grant/UserApplicantAccessService.java` | ok, but rename pkg `grant` → `service` for consistency | `identity/service/…` |
| `identity/access/{NadoumiAccessServiceImpl, CurrentCaller, StaffLoginGuard}` + `CapabilityOverrides` | service/components + a value record together | services → `service/` or keep `access/` as a cohesive sub-context; `CapabilityOverrides` → `access/` (value) — acceptable to keep `access/` as one bounded slice |
| `identity/web/dto/*` (Request + Response records mixed) | cleanup-phase wants `request` / `response` split | `identity/web/request/*`, `identity/web/response/*` |
| `identity/domain/UserApplicantAccess.java` + (enums live in `nadoumi-common/access`) | entity ok; confirm no enum in `domain` | `identity/domain/` = entities only ✔ (enums already in `common`) |
| `applicant/domain/{ApplicantStatus, ContactRelation}.java` | **enums** mixed with entities | `applicant/domain/` entities; `applicant/domain/enums/` or `applicant/type/` for enums |
| `applicant/web/dto/*` | Request + Response mixed | `applicant/web/request/*`, `applicant/web/response/*` |
| `applicant/web/{Staff,Student}ApplicantController` + no service split issue | controllers ok | keep; `ApplicantService` already in `service/` ✔ |

Proposed final shape per business module:

```
com.nadoumi.<context>
├── config
├── domain            entities (mutable persistence types)
│   └── enums          context-local enums
├── mapper            MyBatis interfaces (+ resources/mapper/<context>/*.xml)
├── service           application services
├── exception         context exceptions
└── web
    ├── <X>Controller
    ├── request        @Valid request records
    └── response       response records
```

`nadoumi-common` keeps cross-context value types (`AccessRole`, `ApplicantCapability`,
`AccessCapabilityMatrix`, SPIs). No change needed there beyond confirming it stays
Spring-free except where a slice legitimately needs it.

### 4.2 RuoYi Java — leave structure; targeted removals only

RuoYi's package layout (`config`, `controller`, `service`/`service.impl`, `mapper`,
`domain`, `common.core.*`) is coherent and **must not be reshuffled** (step 9). Only
DEAD-1/3/4/5 removals + CN translations apply.

### 4.3 Minor consolidation candidates (Low value — do only if cheap)

- Two `@MapperScan` (`ruoyi-framework/ApplicationConfig` `com.ruoyi.**.mapper` +
  `nadoumi-identity/NadoumiModuleConfiguration` `com.nadoumi.**.mapper`). Acceptable as
  is; a single scan list is marginally cleaner. **Keep** — touching `ApplicationConfig`
  is a RuoYi core edit for no behavioural gain.
- `PageResponse` (Nadoumi) vs `TableDataInfo` (RuoYi) — intentional split (`/api/**` vs
  console). **Keep both.**
- 4 Nadoumi exception types — idiomatic; **keep** (just move to `exception/`).

## 5. SQL / migrations / seed findings

| # | Item | Action | Risk |
| --- | --- | --- | --- |
| SQL-1 | `V1__ruoyi_baseline.sql` — Chinese comments + RuoYi demo business data (`sys_dept` 100–109 "若依科技", `admin`/`ry` users, `sys_post`, 2 demo `sys_role`, 3 marketing `sys_notice`, 3 demo `sys_job`, demo `sys_config`/dict) | **Frozen — do not edit.** Handle additively in a new migration | — |
| SQL-2 | New `V5__nadoumi_baseline_seed.sql` | (a) delete RuoYi demo *business* rows not needed to boot: `sys_dept` 101–109 (keep a single root, renamed), `sys_post` 1–4, `sys_notice` 1–3, `sys_job` 1–3 (+ their `QRTZ_*`), user `ry` (id 2); (b) rename dept 100 + role 1/2 display names to English/Nadoumi; (c) create super-admin **`almousleck`** with BCrypt(`Nadoumi2026#`), `pwd_update_date = NULL`, mapped to a `nadoumi_super_admin` role granted `*:*:*`; keep RuoYi `admin` (id 1) disabled or as break-glass per `docs/SECURITY.md` S5 | **Med** — must keep structural rows (all `sys_menu`, `system:*` perms, dict *types*, needed `sys_config` keys) |
| SQL-3 | Password handling | BCrypt hash lives in the migration, which is explicitly the **dev/init seed**. Document: first login forces a change (`sys.account.initPasswordModify=1` + `pwd_update_date NULL`); real deployments override via env/secret and rotate. No plaintext in any runtime code. | **Low** |
| SQL-4 | `V2`/`V3`/`V4` carry 21/14/8 comment lines | Strip to "technically necessary only" per step 6. **Only safe if V2–V4 have not executed against any shared DB** (Flyway checksum). Local dev DB is baselined at v1; confirm V2–V4 have not run, or `flyway repair` after. | **Med** — checksum validation |
| SQL-5 | `V2` menu/role seed uses English already ✔; naming follows `V<n>__<snake>.sql` ✔ | Keep the convention; document it in `docs/DATABASE_DESIGN.md` §6 (partly there) | **Low** |
| SQL-6 | `sql/ry_20260417.sql` + `sql/quartz.sql` | Superseded by `V1`. Add a one-line header "source of `V1`; do not run directly" or relocate to `docs/ddl/`. | **Low** |

## 6. Configuration / security findings

| # | Item | Action | Risk |
| --- | --- | --- | --- |
| CFG-1 | `application.yml` `ruoyi.profile: ${RUOYI_PROFILE:D:/ruoyi/uploadPath}` | Cross-platform default (`${user.home}/nadoumi/upload` or `./data/upload`) | **Low** |
| CFG-2 | `application.yml` `ruoyi.name: RuoYi`, `ruoyi.version`, `copyrightYear` | Rename to Nadoumi values | **Low** |
| CFG-3 | Chinese comments throughout `application*.yml` | Translate (CN-7) | **Low** |
| CFG-4 | Druid console `login-username/password` default `ruoyi` / `123456` | Already env-overridable; documented in `docs/SECURITY.md` / `DEPLOYMENT.md` §3. **No change this phase** beyond noting it; production disables the servlet | **Low** (dev) / High (prod, already tracked) |
| CFG-5 | `springdoc.swagger-ui` enabled by default | Tracked in `DEPLOYMENT.md` §3 (prod off). **No change** | — |
| CFG-6 | `token.secret`, Redis password, DB creds | Already externalized to `${ENV:default}` in Phase 2 (`docs/SECURITY.md` S1–S3) | — |
| CFG-7 | `.github/workflows/ci.yml` — no frontend job | Add a `nadoumi-web` lint+build job once it exists | **Low** |

No **new** security holes found beyond the S1–S5 set already in `docs/SECURITY.md`.
CFG-1/CFG-2 are professionalism, not security.

## 7. Frontend findings

`ruoyi-ui` (per D-CLEAN-1, reference-only — findings recorded, **not actioned**):
duplicated CRUD boilerplate per `views/*/index.vue`, per-view axios modules with
repeated error handling, Chinese labels inline (not all via `i18n/`), Element UI 2 +
Vue 2.6 (EOL line), `package.json` name `"ruoyi"`, needs `--openssl-legacy-provider`.
All of this is inherited RuoYi debt that disappears with the Vue 3 migration.

**In scope:** create `nadoumi-web/` (Nuxt 3) skeleton per `docs/FRONTEND_ARCHITECTURE.md`
§3.2/§4:

```
nadoumi-web/
├── nuxt.config.ts
├── app/
│   ├── pages/            home, scholarships, universities, programs, about, contact
│   ├── components/       shared enterprise components (Header, Footer, Hero, Card, …)
│   ├── composables/      useApi, useSeo, …
│   ├── server/           BFF routes → /api/public, /api/student (httpOnly cookie)
│   ├── layouts/
│   ├── assets/ styles/
│   └── types/            OpenAPI-generated later
└── i18n/                 en (+ ar/fr/zh scaffold)
```

Public sections **Home / Scholarships / Universities / Programs / About / Contact** as
pages with a shared layout + shared components; **no** duplicated UI logic; SSR for the
catalog pages; the client bundle never receives confidential scholarship fields
(enforced server-side). This is *architecture scaffolding*, not full implementation.

## 8. Execution order & status

| Group | Contents | Status |
| --- | --- | --- |
| **G1 — Nadoumi package restructure** | §4.1: services → `service/`, exceptions + advice → `exception/`, DTOs → `web/request` + `web/response`, context enums → `domain/enums/` | ✅ **done** — `mvn clean verify` green (24 tests) |
| **G2 — Dead/demo code removal** | `TestController`, `EscapeUtil.main`, boot banner, `SysIndexController` (English), root `.bat` + `bin/`, `doc/`, `sql/*` reference-only header | ✅ **done** — green |
| **G3 — functional English** | `messages.properties` → English default + `messages_zh_CN.properties`; `Constants.DEFAULT_LOCALE` → `ENGLISH`; ~120 user-facing `ServiceException` / `AjaxResult` / `log.*` Chinese literals; all `@Log(title=)`; README; `ry.sh`; `application.yml` branding + cross-platform upload path | ✅ **done** — green |
| **G4 — SQL seed cleanup** | `V5__nadoumi_baseline_seed.sql` (RuoYi demo data removed, `admin` disabled, `almousleck` + `nadoumi_super_admin` created); `RyTask` deleted; V2–V4 comment-stripped; `FlywayMigrationsIT` extended for V5 | ✅ **done** — green |
| **G5 — config / mapper-XML comments** | Chinese comments in `application*.yml`, `logback.xml`, `mybatis-config.xml`, `generator.yml`, 10 RuoYi mapper XMLs; springdoc `packages-to-scan` widened to the real controllers | ✅ **done** — green |
| **G6 — `nadoumi-web` scaffold** | Nuxt 3 skeleton at `nadoumi-web/`: 6 public pages, shared components, `useApi`/`useSeo`, Nitro BFF (httpOnly cookie) for `/api/public` + `/api/student`, i18n en/fr/ar/zh, CI job | ✅ **done** — `pnpm install` + `nuxt prepare` ok; `pnpm lint` + `pnpm build` in CI |
| **G7 (deferred, optional)** | CN-13: RuoYi framework-internal comment / Javadoc translation, module by module | **not done** — per D-CLEAN-2 |

`docs/` updates ride with each group (step "documentation matches the implementation").

## 9. Risk register

- **Flyway checksums** (SQL-4): editing an already-applied `V2`–`V4` breaks
  `validate-on-migrate`. Mitigation: confirm they are unapplied everywhere, or accept a
  one-time `flyway repair`; `V1` is never touched.
- **`@Log` title changes** (CN-5): alters `sys_oper_log.title` values going forward —
  intended, but note it in `docs/SECURITY.md` §8.
- **Message-bundle swap** (CN-1): a missing key after the swap surfaces as a raw code to
  users. Mitigation: diff key sets before/after; keep `messages_zh_CN.properties`
  complete.
- **Removing `sys_job` demo rows on a shared DB** (DEAD-3): Quartz JDBC store also holds
  `QRTZ_*` trigger rows; the cleanup migration must delete both or Quartz logs a
  missing-job warning. Fresh DBs are unaffected.
- **`ruoyi-ui` scope** (D-CLEAN-1): if you *do* want it cleaned, that is a much larger
  effort with low payoff given the planned Vue 3 replacement — call it explicitly.

## 10. Definition of done (this phase — D-CLEAN-1/2 as decided)

- [x] Nadoumi modules follow the strict package layout; `mvn clean verify` green.
- [x] No `TestController` / `RyTask` / debug `main` / boot banner / `.bat` / `doc/`.
- [x] `messages.properties` is English; `messages_zh_CN.properties` holds the Chinese;
      `Constants.DEFAULT_LOCALE = Locale.ENGLISH`.
- [x] No Chinese in: Nadoumi source, config comments, mapper XML comments, `@Log`
      titles, user-facing exception / `AjaxResult` / `log.*` strings, `README.md`, `ry.sh`.
- [x] `V5__nadoumi_baseline_seed.sql` removes RuoYi demo business data, disables
      `admin` (id 1), creates `almousleck` + `nadoumi_super_admin` (BCrypt hash only,
      forced change documented in `docs/SECURITY.md`). `V2`–`V4` comment-trimmed
      (unapplied everywhere → checksum-safe).
- [x] `nadoumi-web/` Nuxt 3 skeleton builds (`pnpm build` → `.output/`) and lints;
      6 public sections + shared components + BFF; CI job added.
- [x] `docs/` updated (DATABASE_DESIGN §6, SECURITY §S5, FRONTEND_ARCHITECTURE §4/§7,
      this file). RuoYi `.java` comment translation logged as deferred (**G7**).
- [ ] **Remaining debt:** G7 (RuoYi framework-internal Chinese comments/Javadoc — 253
      files); `nadoumi-admin` (RuoYi-Vue3) not yet created; `ruoyi-ui` inherited debt
      retired with the Vue 3 migration, not before.
