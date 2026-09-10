# Nadoumi — Database Design

Status legend: **BASELINE** = approved, implement as written · **EXISTING** = in the
repo today · **PLANNED** = approved, not yet built · **OPEN** = still needs a decision.

> **Revision 3 — Phase 1. APPROVED BASELINE.** Aligned with `docs/DOMAIN_MODEL.md`
> Rev 3 and `docs/ARCHITECTURE.md` Rev 3. The §5 table sketches are the reference for
> the reviewed non-executable draft at `docs/ddl/nad_core.draft.sql`.
> **No tables are created, no DDL is executed, the RuoYi business schema is not
> modified.** Flyway (D14) and the first `nad_*` migration land in **Phase 2**.

---

## 1. Engine & tooling (EXISTING)

- **MySQL** (dev machine: MySQL 9.7.1). RuoYi DDL uses `engine=innodb`,
  `bigint(20)` display widths, `datetime`. Driver `com.mysql.cj.jdbc.Driver`.
- Pool: **Alibaba Druid 1.2.28** (`application-druid.yml`, `master` + disabled
  `slave`). `@DataSource` AOP allows per-method datasource routing.
- ORM: **MyBatis** (`mybatis-spring-boot-starter` 4.1.0) + **PageHelper 4.1.1**
  (`helperDialect: mysql`). Mapper XML `classpath*:mapper/**/*Mapper.xml`;
  type aliases `com.ruoyi.**.domain`.
- **No migration tool.** Schema is raw SQL in `sql/` (`ry_20260417.sql`,
  `quartz.sql`).

## 2. Existing schema (EXISTING)

31 live tables in local DB `ry_vue` (20 business + 11 `QRTZ_*`), current with the
scripts (incl. `sys_notice_read`), seeded (`admin` id 1, `ry` id 2). See
`docs/DOMAIN_MODEL.md` §1 for the entity list.

Reuse for Nadoumi **without modification**:
- **RBAC / data scope:** `sys_user` –`sys_user_role`– `sys_role` –`sys_role_menu`–
  `sys_menu`; `sys_role_dept` (custom scope); `sys_dept` self-referencing tree.
  `sys_menu.menu_type` `M`/`C`/`F` → `perms` token. Admin (`user_id = 1`) → `*:*:*`.
  **Staff authorization only** — see §4.1.
- **Audit columns:** `BaseEntity` (`create_by/create_time/update_by/update_time/remark`).
- **Lookups without schema:** `sys_config` (Redis-cached key/value),
  `sys_dict_type` / `sys_dict_data`. Prefer these for small controlled vocabularies.

## 3. Observations / debt (EXISTING)

- MySQL-5.x-flavoured DDL (`bigint(20)`, `int(11)` display widths — deprecated,
  harmless on 8/9). `datetime` + `serverTimezone=GMT+8` + `jackson.time-zone=GMT+8`.
- No migration tool → drift risk. Divergence already visible: repo `master` added
  `sys_notice_read` / `pwd_update_date` after tag `v3.9.2` with only a renamed dump.
- Config targets DB **`ry-vue`** + `root` / `password`; local reality is **`ry_vue`** +
  `root` / *(empty)*. Reconciled in Phase 0 via `SPRING_DATASOURCE_DRUID_MASTER_*`
  env vars only. **D8 — APPROVED, see §7.**
- ~~Quartz DDL present, but the app logged `RAMJobStore` at boot.~~ **Resolved
  (Phase 2):** `spring.quartz.job-store-type=jdbc` + `LocalDataSourceJobStore`
  (clustered, `QRTZ_` prefix, `initialize-schema=never`); restart-persistence verified.

## 4. Nadoumi schema principles (PLANNED)

1. **Namespace** `nad_<context>_<entity>`.
2. **Every business table declares:** surrogate `bigint id` PK; named FK constraints;
   explicit `NOT NULL` + defaults; unique constraints for natural keys;
   indexes on every FK and every common filter/sort column; `BaseEntity` audit
   columns; an explicit `status` / lifecycle column; a documented deletion policy.
3. **History is append-only.** `nad_*_event` / `nad_*_history` / `nad_*_decision` /
   `nad_document_version` rows are never `UPDATE`d or `DELETE`d.
4. **Confidentiality by table separation + view.** Student-safe scholarship columns in
   `nad_scholarship`; confidential linkage in `nad_scholarship_internal`;
   a read-only view `v_scholarship_student` is the *only* thing student/public mappers
   read (`docs/DOMAIN_MODEL.md` §6).
5. **Optimistic locking.** `version` column on `nad_application` and any entity edited
   by multiple staff concurrently.
6. **PII columns** (`passport_no`, `dob`, `national_id`) flagged here and in
   `docs/SECURITY.md` §6 (column-level encryption / restricted projections TBD).
7. **No table purely to satisfy a CRUD screen.**
8. **Externals are not in `sys_dept`.** RuoYi data scope applies to staff only.
   Applicant/university/partnership visibility is governed by
   `nad_user_applicant_access` and resource rules, never by `sys_dept`.

### 4.1 Authorization data model (PLANNED)

| Concern | Mechanism | Table(s) |
| --- | --- | --- |
| Staff → admin actions | RuoYi RBAC + data scope | `sys_user` (`user_type='00'`), `sys_role`, `sys_menu`, `sys_role_menu`, `sys_role_dept` |
| Staff → case assignment | `nad_application.assignee_user_id` + `nad:*` perms | `nad_application` |
| External user → applicant/application | live grant check (never cached in `LoginUser`) | `nad_user_applicant_access` |

## 5. Nadoumi core tables — indicative sketch (BASELINE, **not DDL**)

> **UPDATE 2026-09-02 — read `docs/PLATFORM_ARCHITECTURE.md` §3 first.** Live:
> `nad_user_applicant_access` (`V3`), `nad_applicant` + education / test_score /
> contact (`V4`), `nad_university` (`V9`). The **Scholarship** (§5.4) and
> **Application** (§5.5) sketches below are **superseded**: Scholarship becomes a
> configurable aggregate (category / funding_model / eligibility / typed fee
> lines / stipend / intakes / document requirements); University (§5.3) gains the
> full public profile; Application gains a **profile snapshot** + **requirement
> snapshot** captured at submit. `nad_outbox_event` is added (§C7). Each domain's
> real DDL is designed in its own step, not copied from here.

Aligned with `docs/DOMAIN_MODEL.md` Rev 3 and realized (with FKs / generated guard
columns / CHECK constraints) in `docs/ddl/nad_core.draft.sql`. `?` = nullable.
Invariants marked **INV** are enforced in both the schema
(constraints / generated columns / CHECK) and the service layer.

### 5.1 Identity & Access

| Table | Key columns | Notes / invariants |
| --- | --- | --- |
| `sys_user` (EXISTING) | + populate `user_type`; **+ `email_verified` (Revision 2, `V7`)** | `00` staff · `10` student · `20` agent · `30` guardian. No RuoYi role/menu rows for `10/20/30`. **Revision 2:** `email_verified tinyint(1) NOT NULL DEFAULT 0` + non-unique `idx_sys_user_email`. For externals the verified `email` is the login key (unique among `user_type='10'`, enforced in `StudentAuthService`); `user_name` is a generated internal handle. OTP codes/tickets live in **Redis**, not MySQL (spec §15.2). **Admin build-out (2026-09-06):** `user_type` is now modelled on RuoYi's `SysUser` (a nullable `userType` field, purely additive) and `SysUserMapper.selectUserList` / `insertUser` gained an additive `userType` `<if>`, so the stock `/system/user/*` console endpoints filter and persist it. The Nadoumi admin's **Staff** (`/staff`, `userType='00'`) and **Students** (`/students`, `userType='10'`) screens are the same Vue component parameterised by the route. No column change — the DB column already existed. |
| `nad_user_applicant_access` | `id`, `user_id`→`sys_user`, `applicant_id`→`nad_applicant`, `application_id?`→`nad_application`, `access_role`(OWNER/AGENT/GUARDIAN/VIEWER), `status`(PENDING/ACTIVE/REVOKED/EXPIRED), `invited_email?`, `capability_overrides_json?`, `is_interim`(bool, default 0), `granted_by_user_id`, `granted_at`, `revoked_by_user_id?`, `revoked_at?`, `revoke_reason?`, `expires_at?` | **INV1** partial-unique `(applicant_id)` where `access_role='OWNER' AND status='ACTIVE'` → exactly one active owner. **INV2** partial-unique `(user_id, applicant_id, application_id)` where `status='ACTIVE'`. **INV3** a user cannot have OWNER + another active role on one applicant. `is_interim=1` only on the staff-held transitional OWNER grant (`expires_at` required, ≈ now+30d). Adding a privileged capability via `capability_overrides_json` requires a staff actor with `nad:applicant:access:manage`. Index `(user_id,status)`, `(applicant_id,status)`, `(status,expires_at)` for the expiry + invite-escalation sweep. Rows never hard-deleted. (MySQL has no partial indexes — INV1/INV2 are enforced by a generated helper column, e.g. `owner_guard = CASE WHEN access_role='OWNER' AND status='ACTIVE' THEN applicant_id END` with a plain `UNIQUE(owner_guard)`, plus a service-layer check.) |

### 5.2 Applicant

| Table | Key columns | Notes |
| --- | --- | --- |
| `nad_applicant` | `id`, `given_name`, `family_name`, `dob`, `nationality`, `passport_no?`, `email?`, `phone?`, `status`(DRAFT/ACTIVE/UNLINKED/ARCHIVED), **`photo_media_id?`→`nad_media_asset` (`ON DELETE SET NULL`, `V28`, EXISTING — P1)** | PII. `UNLINKED` = staff-created applicant whose owner invite was never accepted (DOMAIN_MODEL §4.3) — readable by staff, blocked from student-side submission. `passport_no` duplicate detection is **soft (warn)**, not a unique constraint; staff can merge records later. `photo_media_id` is the applicant profile photo — category `APPLICANT_PHOTO`, access class **PROTECTED** (short-TTL signed URL only, never a stored public URL). See §5.10 Media. |
| `nad_applicant_education` | `id`, `applicant_id`, `institution`, `level`(dict), `field?`, `gpa?`, `gpa_scale?`, `start_date?`, `end_date?` | Index `(applicant_id)`. |
| `nad_applicant_test_score` | `id`, `applicant_id`, `test_type`(dict: IELTS/TOEFL/GAOKAO/SAT/GRE/…), `score`, `sub_scores_json?`, `taken_on`, `expires_on?` | Index `(applicant_id, test_type)`. |
| `nad_applicant_contact` | `id`, `applicant_id`, `relation`(GUARDIAN/EMERGENCY/OTHER), `name`, `email?`, `phone?` | **Not** a login identity; a login guardian is a `sys_user` + grant. |

**PLANNED / REQUIRES BACKEND — full onboarding expansion (`docs/APPLICANT_ONBOARDING.md`).**
The Revision 3 `/dashboard/onboarding` wizard *shell* ships, but it writes **only**
the EXISTING `nad_applicant` + child-table columns above. The new columns/tables
below stay PLANNED and the wizard never fakes saving them.
Adds scalar columns to `nad_applicant` (gender,
country of residence, residence branch fields incl. the *"currently in China?"*
split) and new child tables `nad_applicant_language`, `nad_applicant_interest`,
`nad_applicant_work`, `nad_applicant_certification`. Profile photo + passport image
depend on the **Document slice + object storage** (`docs/DOCUMENT_MANAGEMENT.md`) —
not on `nad_applicant`. Migration number assigned when the phase is scheduled.

### 5.3 University / Program / Partnership

| Table | Key columns | Notes / invariants |
| --- | --- | --- |
| `nad_university` | `id`, `name`, `name_cn?`, `country`, `type?`(PUBLIC/PRIVATE), `city?`, `province?`, `founded_year?`, `total_students?`, `international_students?`, `faculty_count?`, `website?`, `ranking_tier?`, `introduction?`, `history?`, `campus_info?`, `accommodation_info?`, `nearby_info?`, `admissions_email?`, `office_phone?`, `logo_document_id?`, `banner_document_id?`, `is_recommended`, `is_featured`, `publish_status`(DRAFT/PUBLISHED), `status`(ACTIVE/INACTIVE), **`logo_media_id?` / `banner_media_id?`→`nad_media_asset` (`ON DELETE SET NULL`, `V27`, EXISTING — P1)** | **PUBLIC.** No commercial column — `is_partner` is derived from an ACTIVE `nad_partnership`, never stored. Unique `(name, country)`. `V10` added the profile columns. Public API (`/api/public/universities`) serves only `publish_status='PUBLISHED' AND status='ACTIVE'` and never `status` / `publish_status` / `remark` / audit. **`logo_media_id` / `banner_media_id` (V27) supersede the V21 `logo_image_url` / `cover_image_url` strings** — see §5.10 and the V21/V27 migration rows in §6. `logo_image_url` / `cover_image_url` are kept as a **deprecated read-only fallback for one release** (`resolveUrl(mediaId, legacyUrl)`), removal tracked as a next-release migration. |
| `nad_university_ranking` | `id`, `university_id`→`nad_university` (**ON DELETE CASCADE**), `source`, `rank_position`, `rank_year?`, `note?` | Owned child. One row per source (QS / THE / ARWU / national…). `rank_position` (not `rank`, MySQL reserved). |
| `nad_university_highlight` | `id`, `university_id`→`nad_university` (**ON DELETE CASCADE**), `kind`(HIGHLIGHT/ADVANTAGE), `sort_order`, `text` | Owned child. Edited whole: a save replaces the set inside the scalar-update transaction. |
| `nad_university_gallery` | `id`, `university_id`→`nad_university` (**ON DELETE CASCADE**), `image_url`, `caption?`, `sort_order`, **`media_id?`→`nad_media_asset` (`ON DELETE SET NULL`, `V27`, EXISTING — P1)** | **PUBLIC.** Owned child, `V18`. Campus-life / dormitory / campus-view imagery, **≤ 6 rows/university** (enforced in `UniversityService` + a `@Size(max=6)` on the request). Edited whole: a save replaces the set. Serialised on both the staff and public university responses as `gallery[]`. `image_url` is the deprecated fallback (§5.10) once `media_id` is set. |
| `nad_program` | `id`, `university_id`→`nad_university` (**ON DELETE RESTRICT**), `name`, `name_cn?`, `program_type`(LANGUAGE/NON_DEGREE/DIPLOMA/BACHELOR/MASTER/PHD), `field?`, `teaching_language?`(ENGLISH/CHINESE/BILINGUAL), `duration_months?`, `tuition_amount decimal(12,2)?`, `tuition_currency char(3)?`, `summary?`, `is_featured`, `is_hot`, `publish_status`(DRAFT/PUBLISHED), `status`(ACTIVE/INACTIVE), **`image_media_id?`→`nad_media_asset` (`ON DELETE SET NULL`, `V27`, EXISTING — P1)**, audit | **PUBLIC**, `V19`. Belongs to exactly one university. Unique `(university_id, name)`. Indexes `(university_id)`, `(publish_status, status)`, `(program_type)`, `(is_featured, is_hot)`. The owning university name is resolved through `UniversityService`, never a join. Public reads require the programme **and** its university to be PUBLISHED + ACTIVE. `workflow_definition_code` lands with the Application/Workflow slice. `image_media_id` (V27) is a **new capability** — programmes had no image field before. |
| `nad_program_major` | `id`, `program_id`→`nad_program` (**ON DELETE CASCADE**), `name`, `name_cn?`, `sort_order` | Owned child, edited whole. `idx (program_id, sort_order)`. |
| `nad_program_intake` | `id`, `program_id`→`nad_program` (**ON DELETE CASCADE**), `term`(SPRING_MARCH/AUTUMN_SEPTEMBER/…), `application_open?`, `application_close?`, `sort_order` | Owned child, edited whole. `idx (program_id, sort_order)`. |
| `nad_partnership` | `id`, `university_id`, `status`(DRAFT/ACTIVE/SUSPENDED/TERMINATED), `tier?`, `commission_model_json?`, `contract_start?`, `contract_end?`, `agreement_document_id?`, `internal_notes?` | **CONFIDENTIAL.** **INV4** partial-unique `(university_id)` where `status='ACTIVE'` → ≤ 1 active partnership/university. Never hard-deleted (history via `TERMINATED` + `nad_partnership_event`). |
| `nad_partnership_contact` | `id`, `partnership_id`, `name`, `role?`, `email?`, `phone?` | Confidential. |
| `nad_partnership_event` | `id`, `partnership_id`, `event_type`, `detail_json?`, `actor_user_id`, `at` | Append-only. |
| `nad_partnership_program` *(optional, D-sub G-P2)* | `id`, `partnership_id`, `program_id`, `commission_override_json?` | Add **only** if per-programme commission terms are actually needed; else commission stays at partnership level. |

### 5.4 Scholarship (confidentiality split) — BUILT (V16 / V17)

| Table | Key columns | Notes / invariants |
| --- | --- | --- |
| `nad_scholarship` | `id`, `slug`(unique), `title`, `summary?`, `country`, `province?`, `city?`, `field?`, `teaching_language?`(ENGLISH/CHINESE/BOTH), `funding_model`(FULLY/PARTIAL/SELF), `has_stipend`, `deadline?`, `benefits?`/`requirements?`/`policy?` (prose), `application_fee_*`/`service_fee_*` (`decimal(14,2)`+`char(3)`), `slots?`, `is_featured`/`is_recommended`/`is_hot`, `publish_status`(DRAFT/PUBLISHED), `published_at?`, `status`(ACTIVE/INACTIVE), **`hero_media_id?` / `cover_media_id?`→`nad_media_asset` (`ON DELETE SET NULL`, `V27`, EXISTING — P1)**, audit | **STUDENT-SAFE columns only.** Discovery indexes on `(publish_status,status,is_featured,is_hot,published_at)`, `country`, `funding_model`, `deadline`. `hero_media_id` / `cover_media_id` (V27) supersede the V21 `hero_image_url` / `cover_image_url` strings, kept as a deprecated read-only fallback for one release (§5.10). |
| `nad_scholarship_level` | `scholarship_id`→cascade, `level`(NON_DEGREE/DIPLOMA/BACHELOR/MASTER/PHD) | Unique `(scholarship_id, level)`. Student-safe. |
| `nad_scholarship_category` / `_category_link` | reference `(code unique, name, sort_order)` + M:N link | Extensible categories, seeded in V17. Link cascades from scholarship, `RESTRICT` on the category. |
| `nad_scholarship_intake` | `scholarship_id`→cascade, `term`, `application_open?`, `application_close?`, `sort_order` | Student-safe. |
| `nad_scholarship_eligibility` | `scholarship_id` PK=FK, `age_min?/max?`, `nationality_scope`(ANY/INCLUDE/EXCLUDE), `accepted_countries?`, `in_china?`, `gpa_min?`/`ielts_min?`/`toefl_min?`/`duolingo_min?`/`hsk_min?`/`csca_min?`, `notes?` | 1:1, student-safe, structured. |
| `nad_scholarship_fee` | `scholarship_id`→cascade, `kind`(TUITION_BEFORE/AFTER, REGISTRATION, APPLICATION, NADOUMI_APPLICATION, NADOUMI_SERVICE, INSURANCE, VISA, OTHER), `amount decimal(14,2)`, `currency char(3)`, `note?`, `sort_order` | Typed money line items. Student-safe. The DTO layer presents every amount in **both RMB and USD** (computed at a fixed display rate, `PublicScholarshipResponse.RMB_PER_USD`). ACCOMMODATION_* fee kinds are retired from the picker (see `nad_scholarship_accommodation`) but stay valid codes. |
| `nad_scholarship_level_stipend` | `scholarship_id`→cascade, `level`(NON_DEGREE/DIPLOMA/BACHELOR/MASTER/PHD), `amount decimal(14,2)`, `currency`, `frequency`(MONTHLY/YEARLY/ONE_OFF), `duration_months?`, `conditions?`, unique `(scholarship_id, level)` | **V22.** One stipend row per accepted level (a PhD stipend ≠ a Master's). Replaces the old 1:1 `nad_scholarship_stipend`. Student-safe. |
| `nad_scholarship_accommodation` | `scholarship_id`→cascade, `room_type`(SINGLE/DOUBLE/TRIPLE/QUAD/SHARED), `amount decimal(14,2)?`, `currency`, `note?`(AC / WiFi / private bathroom…), `sort_order` | **V22.** Room types with their own price and amenities. Student-safe. |
| `nad_scholarship.non_degree_duration` | column on the head — `HALF_YEAR` / `ONE_YEAR` | **V22.** Only meaningful when NON_DEGREE is an accepted level. In `v_scholarship_student`. |
| `nad_scholarship_coverage` | `scholarship_id`→cascade, `kind`(TUITION/ACCOMMODATION/STIPEND/MEDICAL_INSURANCE/SETTLEMENT_ALLOWANCE/TRAVEL/REGISTRATION_FEE/VISA_FEE/OTHER), `detail?`, `sort_order`, unique `(scholarship_id, kind)` | **V23.** Structured list of what the award pays for, alongside the prose `benefits`. Student-safe, edited whole. |
| `nad_scholarship` terms cols (V23) | `study_duration_months?`, `application_channel?`(DIRECT_UNIVERSITY/CSC_AGENCY/NADOUMI/OTHER), `agency_number?`, `renewal_conditions?`(prose), `requires_financial_proof`(bool), `requires_foundation_year`(bool) | **V23.** In `v_scholarship_student`. `renewal_conditions` is detail-only in the DTO; the rest are on card + detail. |
| `nad_scholarship_document_requirement` | `scholarship_id`→cascade, `doc_type`, `mandatory`, `note?`, `sort_order` | Unique `(scholarship_id, doc_type)`. The application reads this — never hard-codes documents. Student-safe. |
| `v_scholarship_student` *(DB VIEW)* | student-safe columns of `nad_scholarship` where `status='ACTIVE' AND publish_status='PUBLISHED'`, **recreated in `V27` to expose `hero_media_id` / `cover_media_id`** (resolved to `secure_url` in the service layer) | The **only** head-row object public/student MyBatis mappers read. `FlywayMigrationsIT` asserts it projects none of `university_id`/`partnership_id`/`operational_notes`/`confidential_terms`/`commission_model_json`/`internal_status` — the V27 recreation adds no confidential column and no join; `CatalogImageRetrofitTest` re-asserts this. |
| `nad_scholarship_internal` | `scholarship_id` **PK = FK** → `nad_scholarship` (cascade), `university_id?`→`nad_university` (**RESTRICT**), `partnership_id?`, `internal_status`, `operational_notes?`, `confidential_terms?`, `commission_model_json?`(json), audit | **CONFIDENTIAL.** 1:1. Touched only by `ScholarshipAdminService` behind `nad:scholarship:internal:*`. `StaffScholarshipTest` proves no public/student path can reach any of it. |
| `nad_scholarship_program` *(deferred)* | `id`, `scholarship_id`, `program_id` | PROGRAM_BOUND allowed-programmes list. Lands with `nadoumi-program` (V19); confidential-adjacent, staff-side only. |

### 5.5 Application

| Table | Key columns | Notes / invariants |
| --- | --- | --- |
| `nad_application` | `id`, `applicant_id`→`nad_applicant`, `application_type`(PROGRAM_ONLY/PROGRAM_WITH_SCHOLARSHIP/SCHOLARSHIP_LED), `program_id?`, `scholarship_id?`, `intake_id?`, `target_university_id?`, `workflow_instance_id?`, `current_stage_id?`, `current_status`, `assignee_user_id?`→`sys_user`, `version` | **INV6** `PROGRAM_ONLY` ⇒ `program_id NOT NULL AND scholarship_id IS NULL AND intake_id NOT NULL`. **INV7** `PROGRAM_WITH_SCHOLARSHIP` ⇒ `program_id NOT NULL AND scholarship_id NOT NULL AND intake_id NOT NULL`. **INV8** `SCHOLARSHIP_LED` ⇒ `scholarship_id NOT NULL`. **INV9** `current_status` is written **only** by the workflow engine. Indexes `(applicant_id)`, `(assignee_user_id, current_status)`, `(program_id)`, `(scholarship_id)`, `(current_status)`. |
| `nad_application_stage_history` | `id`, `application_id`, `from_stage_id?`, `to_stage_id`, `transition_code`, `changed_by`, `changed_at`, `reason?` | Append-only. Index `(application_id, changed_at)`. |
| `nad_application_event` | `id`, `application_id`, `event_type`, `payload_json?`, `created_by`, `created_at` | Append-only timeline. Index `(application_id, created_at)`. |
| `nad_application_task` | `id`, `application_id`, `wf_stage_task_template_id?`, `title`, `assignee_user_id?`, `due_at?`, `status`(OPEN/DONE/SKIPPED/CANCELLED), `mandatory`, `blocks_exit`, `completed_at?`, `completed_by?` | **Single** task table (engine-materialized when `wf_stage_task_template_id` set, else ad-hoc). Index `(application_id,status)`, `(assignee_user_id,status)`, `(due_at)`. |
| `nad_application_note` | `id`, `application_id`, `body`, `visibility`(INTERNAL/SHARED), audit | Index `(application_id)`. |
| `nad_application_decision` | `id`, `application_id`, `stage_id?`, `decision_type`(UNIVERSITY_OFFER/SCHOLARSHIP_AWARD/APPLICANT_RESPONSE/NADOUMI_INTERNAL), `outcome`, `drives_transition_code?`, `decided_by`, `decided_at`, `rationale?` | Append-only. |
| `nad_application_submission` | `id`, `application_id`, `channel`(UNIVERSITY_PORTAL/EMAIL/AGENT/OTHER), `external_reference?`, `submitted_by`, `submitted_at`, `notes?` | Append-only-ish (corrections add rows). |
| `nad_application_link` | `id`, `application_id`, `related_application_id`, `relation`(SIBLING/SUPERSEDES/SUPERSEDED_BY) | Symmetric pairs maintained by the service. |
| `nad_application_document` | `id`, `application_id`, `document_id`→`nad_document`, `requirement_id?`→`nad_document_requirement`, `added_by`, `added_at` | M:N attach of applicant-owned docs to an application. Unique `(application_id, document_id)`. |

### 5.6 Document

| Table | Key columns | Notes / invariants |
| --- | --- | --- |
| `nad_document` | `id`, `applicant_id`→`nad_applicant` (**always set**), `application_id?`, `doc_type`(dict), `status`(DRAFT/SUBMITTED/IN_REVIEW/VERIFIED/REJECTED/EXPIRED), `current_version_id?`→`nad_document_version`, `reviewer_user_id?`, `rejection_reason?`, `expires_on?` | **INV10** `status` is derived from `current_version_id`'s `verification_status` + `expires_on` (via service; optionally a generated column). Index `(applicant_id)`, `(application_id)`, `(status)`. |
| `nad_document_version` | `id`, `document_id`, `version_no`, ~~`storage_key`~~ **`media_asset_id`→`nad_media_asset`** (PLANNED — Step 7 not yet built; supersedes D5's `storage_key`, see §5.10), `content_type`, `size_bytes`, `checksum_sha256`, `uploaded_by`, `uploaded_at`, `verification_status`(PENDING/VERIFIED/REJECTED), `verified_by?`, `verified_at?`, `scan_status`(PENDING/CLEAN/INFECTED) | **Immutable.** Unique `(document_id, version_no)`. Bytes live in Cloudinary, referenced through the `nad_media_asset` registry — **not** in MySQL (DM1, supersedes D5). This table itself is **not yet built** (`nad_document` is Step 7). |
| `nad_document_event` | `id`, `document_id`, `event_type`, `actor_user_id`, `at`, `detail_json?` | Append-only. |
| `nad_document_requirement` | `id`, `scope`(PROGRAM/SCHOLARSHIP/WF_STAGE), `ref_id`, `doc_type`(dict), `mandatory`, `notes?` | Drives the per-application checklist. Index `(scope, ref_id)`. |

### 5.6a Media — BUILT (V26–V29, P1)

`nadoumi-media` foundational module (DM8). `Document ≠ File ≠ Cloudinary Asset`
(DM4): `nad_media_asset` is Nadoumi's metadata + provider-reference registry,
never the bytes and never Cloudinary's own row. Business tables hold a
`*_media_id bigint` FK and never a Cloudinary field. Full model:
`docs/DOCUMENT_MANAGEMENT.md`; security model: `docs/SECURITY.md` "File &
document storage".

| Table | Key columns | Notes / invariants |
| --- | --- | --- |
| `nad_media_asset` | `id`, `provider`(default `CLOUDINARY`), `access_class`(PUBLIC/PROTECTED/SENSITIVE), `category`(UNIVERSITY_LOGO/…/APPLICANT_PHOTO/…/JW202/OTHER_ATTACHMENT — 12 values), `resource_type`(image/raw/video), `delivery_type`(upload/authenticated), `public_id`, `asset_id?`, `cloud_version?`, `secure_url?` (**populated only when `access_class='PUBLIC'`**), `folder?`, `original_filename`, `content_type` (sniffed, not client-claimed), `byte_size`, `width?`, `height?`, `checksum_sha256?`, `uploaded_by`→`sys_user`, `owner_kind`(UNIVERSITY/SCHOLARSHIP/PROGRAM/APPLICANT/APPLICATION/DOCUMENT/USER), `owner_id`, `status`(ACTIVE/SUPERSEDED/DELETED), `superseded_by?`→self (**ON DELETE SET NULL**), `create_by/create_time/update_by/update_time` (`BaseEntity`), `deleted_at?`, `deleted_by?` | `V26`. Unique `(provider, public_id)`. Indexes `(owner_kind, owner_id)`, `(category)`, `(status)`. **No FK from `nad_media_asset` to owner tables** — the owner is polymorphic; referential integrity of `*_media_id` is enforced from the owning table side. |
| `nad_media_access_log` | `id`, `media_asset_id`→`nad_media_asset` (**ON DELETE RESTRICT**), `document_id?` (set once Step 7 exists), `application_id?`, `actor_user_id`, `actor_applicant_id?`, `access_kind`(SIGNED_URL_ISSUED/STREAM_PROXY/METADATA), `result`(GRANTED/DENIED), `deny_reason?`, `ttl_seconds?`, `ip?`, `user_agent?`, `created_at` | `V26`. **Append-only** — never `UPDATE`d/`DELETE`d. Indexes `(media_asset_id, created_at)`, `(application_id, created_at)`, `(actor_user_id, created_at)`. One row per PROTECTED/SENSITIVE access attempt (issue signed URL, stream proxy, or protected metadata read); the write is `REQUIRES_NEW` so a later denial can't roll it back. Public `GET /api/media/{id}` (PUBLIC assets) is **not** logged. Retention: `docs/DEPLOYMENT.md` (data-retention line — period **OPEN**). |

**Access classes → delivery:** PUBLIC = Cloudinary `type:upload`, `secure_url`
stored + served direct or via `GET /api/media/{id}` (302). PROTECTED =
`type:authenticated`, no stored URL, per-request short-TTL signed URL
(`nadoumi.media.signed-url-ttl-seconds`, default 180s, clamp [60,600]) after
Nadoumi authz. SENSITIVE = `type:authenticated`, Nadoumi backend stream-proxy
only — no URL ever reaches the client. Per-category default access class + MIME
allow-list + size cap: `docs/SECURITY.md` "File & document storage".

**Catalog retrofit (`*_media_id` FKs, all `ON DELETE SET NULL`):**
`nad_university.logo_media_id` / `.banner_media_id` (`V27`),
`nad_university_gallery.media_id` (`V27`), `nad_scholarship.hero_media_id` /
`.cover_media_id` (`V27`), `nad_program.image_media_id` (`V27`),
`nad_applicant.photo_media_id` (`V28`). `v_scholarship_student` recreated in
`V27` to expose `hero_media_id` / `cover_media_id` (§5.4). The legacy
`*_image_url` string columns from `V21` are kept as a **deprecated read-only
fallback for one release** — see the V21 row in §6 — then dropped in a tracked
follow-up migration.

**Reconciliation:** `V29` seeds a **paused** `sys_job` row
(`status='1'`) for `MediaReconciliationJob.run()` (Quartz) — sweeps
`status='DELETED'` rows past a grace period, issues the Cloudinary `destroy`,
hard-deletes the row (unless still referenced, in which case only the
Cloudinary object is destroyed and `status='PURGED'`), and flags orphaned
owners.

### 5.7 Communication

| Table | Key columns | Notes |
| --- | --- | --- |
| `nad_conversation` | `id`, `subject?`, `application_id?`→`nad_application`, `status`(OPEN/CLOSED), audit | **FK lives here.** 1 application → N conversations. Index `(application_id)`. |
| `nad_conversation_participant` | `id`, `conversation_id`, `user_id`→`sys_user`, `role`(STAFF/APPLICANT/AGENT/GUARDIAN), `added_at`, `removed_at?`, `last_read_message_id?`, `muted` | Unique `(conversation_id, user_id)` active. |
| `nad_message` | `id`, `conversation_id`, `sender_user_id`, `body`, `created_at`, `edited_at?`, `deleted_at?` | Authored messages only — **no** SYSTEM rows (timeline events stay in `nad_application_event`; UI merges). Index `(conversation_id, id)`. |
| `nad_message_attachment` | `id`, `message_id`, `promoted_document_id?`→`nad_document`, `storage_key`, `content_type`, `size_bytes` | Lightweight blob unless staff "promote to document". |

### 5.8 Notification

**Transactional outbox — BUILT (V30–V31, Step 5 slice 1).** Owned by
`nadoumi-notification`.

| Table | Key columns | Notes |
| --- | --- | --- |
| `nad_outbox_event` | `id`, `aggregate_type`, `aggregate_id`, `type` (domain event, see `docs/DOMAIN_EVENTS.md`), `payload_json` (`varchar(2000)`, safe scalars only — no PII/confidential), `status`(PENDING/PROCESSING/DONE/FAILED), `retry_count`, `last_error?`, `created_at`, `next_attempt_at`, `processed_at?` | `V30`. Producers `INSERT` here in the **same transaction** as the state change, via the `com.nadoumi.common.outbox.OutboxWriter` SPI (`Propagation.MANDATORY`). **No FK** — `aggregate_*` is polymorphic. Indexes `(status, next_attempt_at)` (drain), `(aggregate_type, aggregate_id)`, `(type)`. `OutboxPollerJob` drains PENDING rows whose `next_attempt_at <= now()`, marks `DONE`, or on a dispatcher exception increments `retry_count` with backoff and parks `FAILED` after 10 attempts. `V31` seeds an **active** `sys_job` (`outboxPollerJob.run()`, every 15s). v1 does not use the `PROCESSING` value — RuoYi's clustered Quartz store fires the poller on one instance at a time. |

**In-app + delivery model — PLANNED (Step 5 later slices).**

| Table | Key columns | Notes |
| --- | --- | --- |
| `nad_notification` | `id`, `recipient_user_id`→`sys_user`, `type`, `title`, `body`, `data_json?`, `application_id?`, `conversation_id?`, `message_id?`, `created_at`, `read_at?` | In-app store. Index `(recipient_user_id, read_at)`. |
| `nad_notification_delivery` | `id`, `notification_id`, `channel`(IN_APP/EMAIL/SMS/PUSH/WHATSAPP), `provider`, `provider_message_id?`, `status`(PENDING/SENT/DELIVERED/FAILED/BOUNCED), `attempts`, `last_error?`, `sent_at?` | Unique `(notification_id, channel)`. Retry sweep on `(status, attempts)`. |
| `nad_notification_preference` | `id`, `user_id`, `type`, `channel`, `enabled` | Unique `(user_id, type, channel)`. |
| `nad_notification_template` | `id`, `type`, `channel`, `locale`, `subject_tpl`, `body_tpl` | Unique `(type, channel, locale)`. |

### 5.8a Content — inbound (built)

| Table | Key columns | Notes |
| --- | --- | --- |
| `nad_contact_inquiry` | `id`, `first_name?`, `last_name?`, `name?`, `email`, `phone?`, `category?`, `subject?`, `message`, `status`(NEW/READ/RESPONDED/ARCHIVED), `source`(WEBSITE), `locale?`, `ip_address?`, `user_agent?`, `created_at`, `handled_by?`→`sys_user`, `handled_at?` | **Built (V11, split-name + phone + category in V14).** One row per public "Contact us" submission; the row is the source of truth, a failed support-inbox email is logged but not fatal. `name` is the composed display value (`first last`). Written only by the anonymous, IP-rate-limited `POST /api/public/contact`. Honeypot-filtered. Index `(status, created_at)` for the future triage screen. Lives in `nadoumi-identity`; moves to `nadoumi-content`. |

### 5.9 Forward references (not built now)

`nad_payment*` (fees/invoices/transactions/refunds, linked from `nad_application`),
`nad_content_*` (public CMS), `nad_commission_ledger` (Reporting/Finance),
reporting read-model tables/views.

## 6. Migrations (BASELINE · D14 APPROVED)

**Flyway** (`org.flywaydb:flyway-mysql`), locations `classpath:db/migration`,
`baseline-on-migrate=true`, `baseline-version=1`, `validate-on-migrate=true`,
`clean-disabled=true` in every environment.

| Version | Contents | Phase |
| --- | --- | --- |
| `V1__ruoyi_baseline.sql` | `sql/ry_20260417.sql` + `sql/quartz.sql` verbatim (74 KB). **DONE (Phase 2)** — present at `ruoyi-admin/src/main/resources/db/migration/`. Fresh DB → executes; populated DB → `baseline-on-migrate` records a `BASELINE` marker at v1 and does **not** run it (schema/data untouched). Both paths verified (app boot + `FlywayMigrationsIT` Testcontainers test). Needs the `spring-boot-flyway` module (Boot 4 module split). | Phase 2 ✅ |
| `V2__nad_reference_and_menu_seed.sql` | **DONE (Phase 3).** Scoped to the Identity + Applicant slice: `sys_config` flag `nad.student.register.enabled`; dicts `nad_user_type` / `nad_access_role` / `nad_degree_level` / `nad_test_type`; the top-level "Nadoumi" `sys_menu` group + `Applicants` screen + the `nad:applicant:*` permission buttons; roles `ops_manager` / `case_officer` + their `sys_role_menu` grants. Other contexts' dicts/menus land in their own migrations. | Phase 3 ✅ |
| `V3__nad_identity_access.sql` | **DONE (Phase 3).** `nad_user_applicant_access` + `owner_guard` / `active_guard` STORED generated columns (INV1 / INV2). FK to `nad_applicant` added in V4; FK to `nad_application` deferred to the Application slice. | Phase 3 ✅ |
| `V4__nad_applicant.sql` | **DONE (Phase 3).** `nad_applicant` + `nad_applicant_education` / `_test_score` / `_contact`, plus the deferred `fk_uaa_applicant`. | Phase 3 ✅ |
| `V5__nadoumi_baseline_seed.sql` | **DONE (Cleanup).** Removes RuoYi demo business data (`sys_dept` 101–109, `sys_post` 1–4, `sys_notice` 1–3, `sys_job` 1–3 + `RyTask`, user `ry`); disables `admin` (id 1) as a break-glass account (`status='1'`); creates super-admin **`almousleck`** (id 3, `user_type='00'`, `pwd_update_date` NULL so a change is forced) and role `nadoumi_super_admin` (id 3, all menus, `data_scope='1'`). Structural rows (menus, permissions, dict types, config keys) are kept. | Cleanup ✅ |
| `V6__nadoumi_english_labels.sql` | **DONE (Cleanup).** English `sys_config` names / dict labels. | Cleanup ✅ |
| `V7__nad_student_email_verified.sql` | **Revision 2.** `sys_user.email_verified` + `idx_sys_user_email` (email-first external identity, spec §15.3). | nadoumi-web build |
| `V8__drop_initial_password_nag.sql` | **Revision 2.** `sys.account.initPasswordModify → 0`, `sys.account.passwordValidateDays → 90` (spec §17). | nadoumi-web build |
| `V9__nad_university.sql` | **DONE.** `nad_university` + its `sys_menu` / role-grant seed (`nad:university:*`). Programmes deferred to a later migration. | Phase B |
| `V10__nad_university_profile.sql` | **DONE (Step 2).** Profile depth on `nad_university` (18 scalar cols: `name_cn`, `type`, `province`, `founded_year`, `total_students`, `international_students`, `faculty_count`, `introduction`, `history`, `campus_info`, `accommodation_info`, `nearby_info`, `admissions_email`, `office_phone`, `banner_document_id`, `is_recommended`, `is_featured`, `publish_status`) + `idx_university_publish` + `nad_university_ranking` + `nad_university_highlight` (both ON DELETE CASCADE). No new permissions (V9's `nad:university:*` cover it). | Step 2 ✅ |
| `V11__nad_contact_inquiry.sql` | **DONE (public site build-out).** `nad_contact_inquiry` (name, email, subject?, message, `status` NEW/READ/RESPONDED/ARCHIVED, source, locale?, ip_address?, user_agent?, created_at, handled_by?, handled_at?) + `idx_contact_inquiry_triage (status, created_at)`. No menu / permission seed yet — only the anonymous `POST /api/public/contact` writes it. Owned by `nadoumi-identity` for now; moves to `nadoumi-content`. | site build-out ✅ |
| `V14__nad_contact_inquiry_fields.sql` | **DONE (public site redesign PR-2).** `first_name` / `last_name` / `phone` / `category` on `nad_contact_inquiry`; `name` relaxed to nullable (service composes it). V12 / V13 were reserved for program / scholarship in earlier planning but never written — Flyway allows the gap; the next new migration is **V15** (Flyway does not permit adding a lower number once V14 is applied). | site redesign ✅ |
| `V15` | *(skipped — was briefly reserved for `nad_program`; V15 was never written, so the number is retired to keep migrations append-only. `nadoumi-program` is now V19.)* | — |
| `V16__nad_scholarship.sql` | **DONE (R3).** `nad_scholarship` (student-safe head) + `_level` / `_category` + `_category_link` / `_intake` / `_eligibility` / `_fee` (typed line items, `decimal(14,2)` + ISO-4217) / `_stipend` / `_document_requirement`; the CONFIDENTIAL `nad_scholarship_internal` (PK=FK, `university_id`→`nad_university`, operational/commission fields); the **`v_scholarship_student`** VIEW (PUBLISHED+ACTIVE, no confidential column). | R3 ✅ |
| `V17__nad_scholarship_seed.sql` | **DONE (R3).** `nad_scholarship_category` reference rows (12: CSC / CGS / GOVERNMENT / PROVINCIAL / UNIVERSITY / PRESIDENTIAL / LANGUAGE / TYPE_A–D / OTHER) + `Scholarships` admin menu + `nad:scholarship:list/view/create/edit/remove/publish/export` + `nad:scholarship:internal:view/edit` + role grants (super_admin all; ops_manager / partnerships_manager full; case_officer / read_only_analyst read + `internal:view`; content_editor read). | R3 ✅ |
| `V18__nad_university_gallery.sql` | **DONE (university depth).** `nad_university_gallery` (`image_url`, `caption?`, `sort_order`, `university_id`→`nad_university` ON DELETE CASCADE, `idx_university_gallery (university_id, sort_order)`). ≤ 6 images/university (campus life / dormitory / campus view). No new permissions — V9's `nad:university:*` cover it. | university depth ✅ |
| `V19__nad_program.sql` | **DONE (R2).** `nad_program` (`program_type` LANGUAGE/NON_DEGREE/DIPLOMA/BACHELOR/MASTER/PHD; `university_id`→`nad_university` ON DELETE RESTRICT; unique `(university_id, name)`) + `nad_program_major` + `nad_program_intake` (both ON DELETE CASCADE, edited whole). Programmes belong to a university; exposed only via the university experience. | R2 ✅ |
| `V20__nad_program_seed.sql` | **DONE (R2).** `Programmes` admin C-menu + `nad:program:list/view/create/edit/remove` + role grants (super_admin all; ops_manager + partnerships_manager full; case_officer / content_editor / read_only_analyst / support_agent read). | R2 ✅ |
| `V21__nad_catalog_images.sql` | **DONE. SUPERSEDED by `V27` media FKs (P1, DM1).** `nad_university` + `logo_image_url` / `cover_image_url`; `nad_scholarship` + `hero_image_url` / `cover_image_url` (all `varchar(500)`); `v_scholarship_student` recreated to expose the two scholarship image URLs. URL-based — the admin uploaded through RuoYi's `/common/upload` (files under `RUOYI_PROFILE`, served from `/profile/**`). These columns are now the **deprecated read-only fallback** behind the `V27` `*_media_id` FKs (§5.10), retained for one release, removal tracked as a follow-up migration. | catalog imagery ✅ |
| `V22__nad_scholarship_config.sql` | **DONE.** `nad_scholarship.non_degree_duration`; `nad_scholarship_level_stipend` (stipend per level, replaces the 1:1 `nad_scholarship_stipend`); `nad_scholarship_accommodation` (room types + price + amenities); `v_scholarship_student` recreated with `non_degree_duration`. | scholarship config ✅ |
| `V24__nad_catalog_slug.sql` | **DONE.** `slug varchar(160) not null` unique on `nad_university` and `nad_program` (backfilled from `name`, de-duplicated). Public URLs route by slug (`/universities/fudan-university`), never a sequential id. Internal PKs stay `bigint`. | catalog slug ✅ |
| `V25__nad_scholarship_reference.sql` | **DONE.** `reference_code varchar(20)` unique on `nad_scholarship` — the human-facing code `NAC-<year>-NNNN`, assigned on first publish (max-suffix + 1). `v_scholarship_student` recreated to expose it. | scholarship reference ✅ |
| `V26__nad_media_asset.sql` | **DONE (P1).** `nad_media_asset` + `nad_media_access_log` (§5.6a). Foundational `nadoumi-media` module — lands ahead of Step 5/6/7 because Step 6's document/JW202 attachments and the applicant-photo slice consume it. | P1 ✅ |
| `V27__nad_catalog_media_fk.sql` | **DONE (P1).** `*_media_id` FK columns on `nad_university` (`logo`, `banner`) / `nad_university_gallery` (`media_id`) / `nad_scholarship` (`hero`, `cover`) / `nad_program` (`image`, new capability); `v_scholarship_student` recreated to expose `hero_media_id` / `cover_media_id` (still no confidential column, no join). Legacy `V21` `*_image_url` columns retained as deprecated fallback. | P1 ✅ |
| `V28__nad_applicant_photo.sql` | **DONE (P1).** `nad_applicant.photo_media_id` FK → `nad_media_asset` (`ON DELETE SET NULL`) — `APPLICANT_PHOTO` category, PROTECTED access class; the first protected-media use, proving the signed-URL path end to end. | P1 ✅ |
| `V29__nad_media_reconciliation_job_seed.sql` | **DONE (P1).** `sys_job` seed for `MediaReconciliationJob.run()`, **paused** (`status='1'`) — enabled once the reconciliation sweep is verified in an environment. | P1 ✅ |
| `V30__nad_outbox_event.sql` | **DONE (Step 5 slice 1).** `nad_outbox_event` — the transactional outbox drained by `OutboxPollerJob` into the notification pipeline (§5.8, `docs/DOMAIN_EVENTS.md`). DDL only. | Step 5 ✅ |
| `V31__nad_outbox_job_seed.sql` | **DONE (Step 5 slice 1).** `sys_job` seed for `outboxPollerJob.run()`, **active** (`status='0'`), every 15s — draining the outbox is essential, and the job only reads PENDING rows. | Step 5 ✅ |
| `V32__nad_notification.sql` | **DONE (Step 5 slice 2).** `nad_notification` (+ `source_ref` idempotency key, `uk_notif_source`, `ON DELETE CASCADE` to `sys_user`) / `nad_notification_delivery` (`uk_notif_delivery_channel`) / `nad_notification_preference` / `nad_notification_template`. | Step 5 ✅ |
| `V33__nad_notification_seed.sql` | **DONE (Step 5 slice 2).** `Notifications` C-menu + `nad:notification:list/view/template:edit/preference:edit`; role-3 + `ops_manager` grants; four default `en` templates (`CONTACT_INQUIRY_RECEIVED` / `SCHOLARSHIP_PUBLISHED` × IN_APP/EMAIL) with `{{var}}` placeholders. | Step 5 ✅ |
| `V34__nad_notification_dispatch_job_seed.sql` | **DONE (Step 5 slice 3).** `sys_job` seed for `notificationDispatchJob.run()`, **active**, every 30s — drains PENDING `nad_notification_delivery` through the channel SPI (EMAIL) with backoff. | Step 5 ✅ |
| `V35__nad_employee.sql` | **DONE (HR, 2026-09-06 — Step 10 pulled forward).** `nad_employee` — one row per internal staff member, 1:1 with a `sys_user` (`ON DELETE CASCADE`); position (`sys_post` FK), dept, manager, employment type/status, start/probation/end dates, salary + currency + pay frequency, work location, emergency contact. Salary is only exposed to `nad:employee:compensation:view`. | HR ✅ |
| `V36__nad_task.sql` | **DONE (HR).** `nad_task` (title, description, priority LOW/MEDIUM/HIGH, status PENDING/IN_PROGRESS/COMPLETED/APPROVED/CANCELLED, assignee, creator, due date, started/completed/approved stamps) + append-only `nad_task_event` (CREATED/STATUS_CHANGED/ASSIGNED/PRIORITY_CHANGED, actor, note). Lifecycle + approval enforced in `TaskService`; every change emits `TaskProgressChanged`. | HR ✅ |
| `V37__nad_hr_seed.sql` | **DONE (HR).** `Employees` + `Tasks` C-menus + `nad:employee:*` (6) / `nad:task:*` (6) perms; role-3 + `ops_manager` grants (+ `case_officer` read/edit on tasks); two `TASK_PROGRESS` `en` notification templates. | HR ✅ |
| `V38__nad_finance.sql` | **DONE (Finance).** `nad_expense_category`, `nad_expense` (lifecycle DRAFT→SUBMITTED→APPROVED→PAID/REJECTED, `receipt_no` `NAD-EXP-<year>-NNNN` on first approval), `nad_revenue`. Money is `decimal` + ISO-4217 `currency` (default `CNY`); totals grouped by currency, never summed across. | Finance ✅ |
| `V39__nad_finance_seed.sql` | **DONE (Finance).** 9 expense categories; `Finance` (earnings) / `Expenses` / `Revenue` C-menus + `nad:finance:view` / `nad:expense:*` (6) / `nad:revenue:*` (5) perms; role-3 + `ops_manager` grants. | Finance ✅ |
| `V40__nad_fx_rate_seed.sql` | **DONE.** `sys_config` row `nadoumi.fx.cny_usd = 0.1381` (`config_type='Y'` — editable in System › Configuration, not deletable). Catalog money is entered in RMB; the USD figure shown beside it is `amount_cny × rate`, computed at read time (`com.nadoumi.identity.money.FxRates`), never stored. | Finance ✅ |
| `V41__nad_scholarship_program_link.sql` | **DONE.** `nad_scholarship_internal.program_id` (FK `nad_program` `ON DELETE SET NULL`). CONFIDENTIAL — a programme reveals its university, so the link lives in the internal table, never `nad_scholarship`; only `nad:scholarship:internal:*` holders read/write it. | Scholarship ✅ |
| `V42__nad_staff_role_seed.sql` | **DONE.** Seeds role `staff` (role_key `staff`) — the baseline for a rank-and-file employee: `nad:task:list/query/add/edit` (API scopes non-approvers to their own tasks), `system:role:list` (view), `system:dept:list`+`:add` (view + add sub), `system:post:*` (full CRUD). No Menus, no System group, no applicant/scholarship/finance. Row-level "own records only" scoping is a later change. | RBAC ✅ |
| `V43__nad_department.sql` | **DONE.** `nad_department` (per-university academic department / college; `uk_department_university_name`; FK `nad_university` `ON DELETE CASCADE`). `nad_program_major.department_id` (FK `nad_department` `ON DELETE SET NULL`). `nad_program.term_length` (`ONE_SEMESTER`/`ONE_YEAR`) — set for `LANGUAGE`/`NON_DEGREE` only; degree types (`DIPLOMA`/`BACHELOR`/`MASTER`/`PHD`) use the majors list. `nad_program.field` kept (deprecated; admin no longer writes it). | Program ✅ |
| `V44__nad_department_seed.sql` | **DONE.** `nad:department:list/add/edit/remove` — F-perms under the Universities C-menu (managed inline on University detail, no new nav item). Role 3 + `ops_manager` + `partnerships_manager` full; read-only roles get `:list`. | Program ✅ |
| `V45__nad_payroll_seed.sql` | **DONE.** `Payroll` C-menu (`nadoumi/finance/payroll`) + `nad:payroll:view` perm; role 3 + `ops_manager`. No table — the read-only payroll screen rolls up `nad_employee` compensation (`PayrollService` normalises each salary to a monthly-equivalent by `pay_frequency`). A pay-run ledger arrives with the Payment module. | HR ✅ |
| `V46__nad_university_reference_partner.sql` | **DONE.** `nad_university.reference_code` (`NAD-UNI-NNNN`, unique, assigned on create; backfilled) + `nad_university.partner_status` (`NONE`/`PROSPECT`/`PARTNER`, default `NONE`) — **INTERNAL**, staff-only, never in `PublicUniversityResponse`. A full Partnership module is a later step. | University ✅ |
| `V47__nad_employee_emergency_contact.sql` | **DONE.** `nad_employee` gains `emergency_contact_relationship` (varchar 60), `emergency_contact_phone` (varchar 32), `emergency_contact_email` (varchar 120), all nullable, beside the existing free-text `emergency_contact` (name). The admin employee drawer now captures name, relationship, phone and optional email as separate fields. | HR ✅ |
| `V48__nad_program_multilevel.sql` | **DONE.** `nad_program.program_type` collapses to the KIND (`DEGREE` / `LANGUAGE` / `NON_DEGREE`); the specific degree levels move to new table `nad_program_level` (`program_id` FK `nad_program` `ON DELETE CASCADE`, `uk_program_level (program_id, level)`, level in `DIPLOMA`/`BACHELOR`/`MASTER`/`PHD`, `sort_order`). `nad_program_major.level` (varchar 16, nullable) ties a major to one of the programme's levels. Existing degree rows migrate: one `nad_program_level` row per old type, `program_type` set to `DEGREE`. `term_length` stays a `LANGUAGE` / `NON_DEGREE` concept. | Program ✅ |
| `V49__nad_staff_role_scope.sql` | **DONE.** New permission `nad:task:progress` (F-menu under Tasks) backs `PUT /api/staff/tasks/{id}/status`; `TaskService.changeStatus` also refuses a move on a task the caller was not assigned / did not create (unless `nad:task:approve`). The baseline `staff` role is re-scoped: its `sys_role_menu` rows are cleared and re-seeded to **only** `nad:task:list` / `:query` / `:progress`. Removes `system:role:list`, `system:dept:list`/`:add`, `system:post:*`, `nad:task:add`/`:edit` from `staff` — an employee no longer sees the Roles / Departments / Posts admin screens (their own roles / posts / department still show on Profile). Any role that had `nad:task:edit` also gets `nad:task:progress`. | HR ✅ |
| `V50__nad_staff_role_catalog_read.sql` | **DONE.** Widens the `staff` role for catalog work with no destructive rights: `nad:applicant:list`/`:view` (read only); `nad:university`/`nad:program`/`nad:scholarship` `:list`/`:view`/`:create`/`:edit` (no `:remove`, no `:publish`, no `:internal`); `system:user:list` (Students, read only); `system:dept:list` + `nad:department:list` (read only). | RBAC ✅ |
| `V51__nad_catalog_published_notifications.sql` | **DONE.** `nad_notification_template` rows for `UNIVERSITY_PUBLISHED` + `PROGRAM_PUBLISHED` (IN_APP + EMAIL, `en`); the `SCHOLARSHIP_PUBLISHED` `en` copy is rewritten student-friendly. `UniversityService` / `ProgramService` now emit `UniversityPublished` / `ProgramPublished` on the transition to ACTIVE+PUBLISHED (both gained an `OutboxWriter`); `OutboxToNotificationDispatcher` maps the two types and fans all three "published" events to staff holding `nad:notification:list` **and** every active registered student (`user_type='10'`) — IN_APP always, EMAIL by preference. | Notification ✅ |
| `V52__nad_university_public_partner.sql` | **DONE.** `nad_university.public_partner tinyint(1) not null default 0` — a **curated editorial** flag: staff opt a university into the public "Our partners" showcase on the website. **Deliberately separate** from the confidential internal `partner_status` (NONE/PROSPECT/PARTNER), which is still staff-only and never drives public output. Threaded through `UniversityRequest`/`UniversityResponse`/`PublicUniversityResponse` + `UniversitySearch` (public filter `?publicPartner=true` on `/api/public/universities`). Admin drawer has a toggle with a hint that it is public + separate from partnership status. `nadoumi-web` home page renders a clean partner-logo grid from `publicGet('universities', { publicPartner: true })`. | University ✅ |
| `V53__nad_scholarship_deadline_reminder.sql` | **DONE.** New table `nad_scholarship_deadline_reminder (scholarship_id PK, reminded_at)` (FK `nad_scholarship` `ON DELETE CASCADE`) — one row per scholarship whose "deadline approaching" reminder has been sent (idempotency). 2 `en` templates for `SCHOLARSHIP_DEADLINE_REMINDER` (IN_APP + EMAIL). `sys_job` row `scholarshipDeadlineReminderJob.run()` seeded **active**, daily `0 0 8 * * ?`. The job (`ScholarshipDeadlineReminderJob` in `nadoumi-scholarship`, `@Component` bean) selects published+active scholarships with `deadline` within 10 days and no reminder row, writes one `ScholarshipDeadlineReminder` outbox event each + inserts the reminder row. `OutboxToNotificationDispatcher` maps the type and fans it to **every active registered student** (`user_type='10'`) — IN_APP always, EMAIL by preference. | Scholarship ✅ |
| `V54__nad_university_import_seed.sql` | **DONE.** DML-only catalog import: 22 China universities into `nad_university` (+ ~430 `nad_university_highlight`, ~66 `nad_university_ranking` child rows). All rows `status='ACTIVE'`, `publish_status='DRAFT'`, `partner_status='PROSPECT'` (INTERNAL), `create_by='import'`; `reference_code` continues the `NAD-UNI-NNNN` sequence; `slug` hard-coded. Figures and rankings are approximate (each ranking row carries a "verify" `note`). Left blank for the admin dashboard: images, `accommodation_info`, `nearby_info`, `admissions_email`, `office_phone`, `nad_department` rows, most rankings. `Hailing International School` is a placeholder (looks like a K-12 school — needs staff decision). Rollback: `delete from nad_university where create_by='import' and reference_code like 'NAD-UNI-%'` (children cascade). | University ✅ |
| `V55+__…` | `nadoumi-application` `nad_wf_*` + `nad_application*` (Step 6); `nadoumi-document` (Step 7); communication, **payment (Step 9, greenfield)**, content, reporting. Next new migration is **V55**. | Step 6+ |

**`FlywayMigrationsIT` counts (after `V54`):** **51 fresh / 50 baselined**
migrations (fresh-DB path runs `V1` through `V48`; the baseline-on-migrate path
starts at `V2` since `V1` is the baseline marker). `AbstractNadIntegrationTest`
clears the `rate_limit:*` Redis keys and `nad_outbox_event` / `nad_notification`
(the latter cascades deliveries) plus `nad_task` / `nad_employee` / `nad_expense`
/ `nad_revenue` per test alongside the existing table-clear order (which now also
deletes `nad_program` before `nad_department` before `nad_university`, so the new
`nad_program_level` rows cascade cleanly);
`OutboxPipelineTest` also clears `nad_outbox_event` itself.

- The **reviewed DDL draft** `docs/ddl/nad_core.draft.sql` is **not** under
  `db/migration` and Flyway never sees it. `V3`/`V4` were split out of it in Phase 3;
  later slices follow. **Migration numbers are assigned at creation time** — the
  Phase-4+ rows above are indicative, not reserved (the Revision 2 `V7`/`V8` took the
  next free slots after the `V6` English-labels migration).
- `sys_menu` / `sys_role_menu` / role seeding is always a migration, never a console
  action.
- No hand-run SQL in any shared environment. `flyway:clean` is disabled.

## 7. Decision status after Phase 1 (DB-facing)

| ID | Decision | Status |
| --- | --- | --- |
| **D8** | DB name + credential strategy | **APPROVED** — secrets via env vars + optional git-ignored `config/application-local.yml` (`spring.config.import=optional:file:./config/application-local.yml`). Committed `application-druid.yml` default stays `ry-vue`/`root`/`password`; local dev DB is renamed `ry_vue`→`ry-vue` (one-time, documented in DEVELOPMENT_GUIDELINES §1). |
| **D13** | PK generation — `bigint` auto-increment for v1 | **APPROVED** |
| **D14** | Migration tool — Flyway (§6) | **APPROVED** |
| **D9 / D11 / D12** | Table shapes (Application target, Document attachment, single task table) | **APPROVED** — §5 is the baseline; realized as `docs/ddl/nad_core.draft.sql` → `V3`–`V4` (Phase 3) then Phase-4+ migrations (§6). |
| sub | `passport_no` uniqueness | **APPROVED** — soft warn, no unique constraint; staff-driven merge later. |
| sub | Temporal type for new columns | **APPROVED** — match RuoYi (`datetime` + `GMT+8`) in v1. |
| sub | `nad_partnership_program` (per-programme commission) | **OPEN** — deferred; commission stays at partnership level until a real need. |
| sub | PII column encryption (`passport_no`, `national_id`) + data residency | **OPEN** — SECURITY §6; needs security/legal input; not a Phase 2 blocker. |
