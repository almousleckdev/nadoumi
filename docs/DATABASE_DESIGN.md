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
| `sys_user` (EXISTING) | + populate `user_type`; **+ `email_verified` (Revision 2, `V7`)** | `00` staff · `10` student · `20` agent · `30` guardian. No RuoYi role/menu rows for `10/20/30`. **Revision 2:** `email_verified tinyint(1) NOT NULL DEFAULT 0` + non-unique `idx_sys_user_email`. For externals the verified `email` is the login key (unique among `user_type='10'`, enforced in `StudentAuthService`); `user_name` is a generated internal handle. OTP codes/tickets live in **Redis**, not MySQL (spec §15.2). |
| `nad_user_applicant_access` | `id`, `user_id`→`sys_user`, `applicant_id`→`nad_applicant`, `application_id?`→`nad_application`, `access_role`(OWNER/AGENT/GUARDIAN/VIEWER), `status`(PENDING/ACTIVE/REVOKED/EXPIRED), `invited_email?`, `capability_overrides_json?`, `is_interim`(bool, default 0), `granted_by_user_id`, `granted_at`, `revoked_by_user_id?`, `revoked_at?`, `revoke_reason?`, `expires_at?` | **INV1** partial-unique `(applicant_id)` where `access_role='OWNER' AND status='ACTIVE'` → exactly one active owner. **INV2** partial-unique `(user_id, applicant_id, application_id)` where `status='ACTIVE'`. **INV3** a user cannot have OWNER + another active role on one applicant. `is_interim=1` only on the staff-held transitional OWNER grant (`expires_at` required, ≈ now+30d). Adding a privileged capability via `capability_overrides_json` requires a staff actor with `nad:applicant:access:manage`. Index `(user_id,status)`, `(applicant_id,status)`, `(status,expires_at)` for the expiry + invite-escalation sweep. Rows never hard-deleted. (MySQL has no partial indexes — INV1/INV2 are enforced by a generated helper column, e.g. `owner_guard = CASE WHEN access_role='OWNER' AND status='ACTIVE' THEN applicant_id END` with a plain `UNIQUE(owner_guard)`, plus a service-layer check.) |

### 5.2 Applicant

| Table | Key columns | Notes |
| --- | --- | --- |
| `nad_applicant` | `id`, `given_name`, `family_name`, `dob`, `nationality`, `passport_no?`, `email?`, `phone?`, `status`(DRAFT/ACTIVE/UNLINKED/ARCHIVED) | PII. `UNLINKED` = staff-created applicant whose owner invite was never accepted (DOMAIN_MODEL §4.3) — readable by staff, blocked from student-side submission. `passport_no` duplicate detection is **soft (warn)**, not a unique constraint; staff can merge records later. |
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
| `nad_university` | `id`, `name`, `name_cn?`, `country`, `type?`(PUBLIC/PRIVATE), `city?`, `province?`, `founded_year?`, `total_students?`, `international_students?`, `faculty_count?`, `website?`, `ranking_tier?`, `introduction?`, `history?`, `campus_info?`, `accommodation_info?`, `nearby_info?`, `admissions_email?`, `office_phone?`, `logo_document_id?`, `banner_document_id?`, `is_recommended`, `is_featured`, `publish_status`(DRAFT/PUBLISHED), `status`(ACTIVE/INACTIVE) | **PUBLIC.** No commercial column — `is_partner` is derived from an ACTIVE `nad_partnership`, never stored. Unique `(name, country)`. `V10` added the profile columns. Public API (`/api/public/universities`) serves only `publish_status='PUBLISHED' AND status='ACTIVE'` and never `status` / `publish_status` / `remark` / audit. |
| `nad_university_ranking` | `id`, `university_id`→`nad_university` (**ON DELETE CASCADE**), `source`, `rank_position`, `rank_year?`, `note?` | Owned child. One row per source (QS / THE / ARWU / national…). `rank_position` (not `rank`, MySQL reserved). |
| `nad_university_highlight` | `id`, `university_id`→`nad_university` (**ON DELETE CASCADE**), `kind`(HIGHLIGHT/ADVANTAGE), `sort_order`, `text` | Owned child. Edited whole: a save replaces the set inside the scalar-update transaction. |
| `nad_program` | `id`, `university_id`, `name`, `degree_level`(dict), `field`(dict), `language`(dict), `tuition_amount?`, `tuition_currency?`, `duration_months?`, `workflow_definition_code?`, `status` | **PUBLIC.** Index `(university_id)`, `(country via join)`, `(degree_level, field)`. `workflow_definition_code` null → default workflow. |
| `nad_program_intake` | `id`, `program_id`, `term`, `application_open?`, `application_close?`, `status` | Index `(program_id, term)`. |
| `nad_partnership` | `id`, `university_id`, `status`(DRAFT/ACTIVE/SUSPENDED/TERMINATED), `tier?`, `commission_model_json?`, `contract_start?`, `contract_end?`, `agreement_document_id?`, `internal_notes?` | **CONFIDENTIAL.** **INV4** partial-unique `(university_id)` where `status='ACTIVE'` → ≤ 1 active partnership/university. Never hard-deleted (history via `TERMINATED` + `nad_partnership_event`). |
| `nad_partnership_contact` | `id`, `partnership_id`, `name`, `role?`, `email?`, `phone?` | Confidential. |
| `nad_partnership_event` | `id`, `partnership_id`, `event_type`, `detail_json?`, `actor_user_id`, `at` | Append-only. |
| `nad_partnership_program` *(optional, D-sub G-P2)* | `id`, `partnership_id`, `program_id`, `commission_override_json?` | Add **only** if per-programme commission terms are actually needed; else commission stays at partnership level. |

### 5.4 Scholarship (confidentiality split) — BUILT (V16 / V17)

| Table | Key columns | Notes / invariants |
| --- | --- | --- |
| `nad_scholarship` | `id`, `slug`(unique), `title`, `summary?`, `country`, `province?`, `city?`, `field?`, `teaching_language?`(ENGLISH/CHINESE/BOTH), `funding_model`(FULLY/PARTIAL/SELF), `has_stipend`, `deadline?`, `benefits?`/`requirements?`/`policy?` (prose), `application_fee_*`/`service_fee_*` (`decimal(14,2)`+`char(3)`), `slots?`, `is_featured`/`is_recommended`/`is_hot`, `publish_status`(DRAFT/PUBLISHED), `published_at?`, `status`(ACTIVE/INACTIVE), audit | **STUDENT-SAFE columns only.** Discovery indexes on `(publish_status,status,is_featured,is_hot,published_at)`, `country`, `funding_model`, `deadline`. |
| `nad_scholarship_level` | `scholarship_id`→cascade, `level`(NON_DEGREE/DIPLOMA/BACHELOR/MASTER/PHD) | Unique `(scholarship_id, level)`. Student-safe. |
| `nad_scholarship_category` / `_category_link` | reference `(code unique, name, sort_order)` + M:N link | Extensible categories, seeded in V17. Link cascades from scholarship, `RESTRICT` on the category. |
| `nad_scholarship_intake` | `scholarship_id`→cascade, `term`, `application_open?`, `application_close?`, `sort_order` | Student-safe. |
| `nad_scholarship_eligibility` | `scholarship_id` PK=FK, `age_min?/max?`, `nationality_scope`(ANY/INCLUDE/EXCLUDE), `accepted_countries?`, `in_china?`, `gpa_min?`/`ielts_min?`/`toefl_min?`/`duolingo_min?`/`hsk_min?`/`csca_min?`, `notes?` | 1:1, student-safe, structured. |
| `nad_scholarship_fee` | `scholarship_id`→cascade, `kind`(TUITION_BEFORE/AFTER, ACCOMMODATION_BEFORE/AFTER, REGISTRATION, APPLICATION, NADOUMI_APPLICATION, NADOUMI_SERVICE, INSURANCE, VISA, OTHER), `amount decimal(14,2)`, `currency char(3)`, `note?`, `sort_order` | Typed money line items. Student-safe. |
| `nad_scholarship_stipend` | `scholarship_id` PK=FK, `amount decimal(14,2)`, `currency`, `frequency`(MONTHLY/YEARLY/ONE_OFF), `duration_months?`, `conditions?` | Nullable 1:1 — no row ⇒ no stipend. Student-safe. |
| `nad_scholarship_document_requirement` | `scholarship_id`→cascade, `doc_type`, `mandatory`, `note?`, `sort_order` | Unique `(scholarship_id, doc_type)`. The application reads this — never hard-codes documents. Student-safe. |
| `v_scholarship_student` *(DB VIEW)* | student-safe columns of `nad_scholarship` where `status='ACTIVE' AND publish_status='PUBLISHED'` | The **only** head-row object public/student MyBatis mappers read. `FlywayMigrationsIT` asserts it projects none of `university_id`/`partnership_id`/`operational_notes`/`confidential_terms`/`commission_model_json`/`internal_status`. |
| `nad_scholarship_internal` | `scholarship_id` **PK = FK** → `nad_scholarship` (cascade), `university_id?`→`nad_university` (**RESTRICT**), `partnership_id?`, `internal_status`, `operational_notes?`, `confidential_terms?`, `commission_model_json?`(json), audit | **CONFIDENTIAL.** 1:1. Touched only by `ScholarshipAdminService` behind `nad:scholarship:internal:*`. `StaffScholarshipTest` proves no public/student path can reach any of it. |
| `nad_scholarship_program` *(deferred)* | `id`, `scholarship_id`, `program_id` | PROGRAM_BOUND allowed-programmes list. Lands with `nadoumi-program` (V18); confidential-adjacent, staff-side only. |

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
| `nad_document_version` | `id`, `document_id`, `version_no`, `storage_key`, `content_type`, `size_bytes`, `checksum_sha256`, `uploaded_by`, `uploaded_at`, `verification_status`(PENDING/VERIFIED/REJECTED), `verified_by?`, `verified_at?`, `scan_status`(PENDING/CLEAN/INFECTED) | **Immutable.** Unique `(document_id, version_no)`. Bytes live in object storage under `storage_key` — **not** in MySQL (D5). |
| `nad_document_event` | `id`, `document_id`, `event_type`, `actor_user_id`, `at`, `detail_json?` | Append-only. |
| `nad_document_requirement` | `id`, `scope`(PROGRAM/SCHOLARSHIP/WF_STAGE), `ref_id`, `doc_type`(dict), `mandatory`, `notes?` | Drives the per-application checklist. Index `(scope, ref_id)`. |

### 5.7 Communication

| Table | Key columns | Notes |
| --- | --- | --- |
| `nad_conversation` | `id`, `subject?`, `application_id?`→`nad_application`, `status`(OPEN/CLOSED), audit | **FK lives here.** 1 application → N conversations. Index `(application_id)`. |
| `nad_conversation_participant` | `id`, `conversation_id`, `user_id`→`sys_user`, `role`(STAFF/APPLICANT/AGENT/GUARDIAN), `added_at`, `removed_at?`, `last_read_message_id?`, `muted` | Unique `(conversation_id, user_id)` active. |
| `nad_message` | `id`, `conversation_id`, `sender_user_id`, `body`, `created_at`, `edited_at?`, `deleted_at?` | Authored messages only — **no** SYSTEM rows (timeline events stay in `nad_application_event`; UI merges). Index `(conversation_id, id)`. |
| `nad_message_attachment` | `id`, `message_id`, `promoted_document_id?`→`nad_document`, `storage_key`, `content_type`, `size_bytes` | Lightweight blob unless staff "promote to document". |

### 5.8 Notification

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
| `V15` | *(skipped — was briefly reserved for `nad_program`; V15 was never written, so the number is retired to keep migrations append-only. `nadoumi-program` is now V18.)* | — |
| `V16__nad_scholarship.sql` | **DONE (R3).** `nad_scholarship` (student-safe head) + `_level` / `_category` + `_category_link` / `_intake` / `_eligibility` / `_fee` (typed line items, `decimal(14,2)` + ISO-4217) / `_stipend` / `_document_requirement`; the CONFIDENTIAL `nad_scholarship_internal` (PK=FK, `university_id`→`nad_university`, operational/commission fields); the **`v_scholarship_student`** VIEW (PUBLISHED+ACTIVE, no confidential column). | R3 ✅ |
| `V17__nad_scholarship_seed.sql` | **DONE (R3).** `nad_scholarship_category` reference rows (12: CSC / CGS / GOVERNMENT / PROVINCIAL / UNIVERSITY / PRESIDENTIAL / LANGUAGE / TYPE_A–D / OTHER) + `Scholarships` admin menu + `nad:scholarship:list/view/create/edit/remove/publish/export` + `nad:scholarship:internal:view/edit` + role grants (super_admin all; ops_manager / partnerships_manager full; case_officer / read_only_analyst read + `internal:view`; content_editor read). | R3 ✅ |
| `V18__nad_program.sql` | `nad_program` (`program_type` LANGUAGE/NON_DEGREE/DIPLOMA/BACHELOR/MASTER/PHD), `nad_program_major`, `nad_program_intake`. Programmes belong to a university; exposed only via the university experience. | R2 |
| `V19__nad_outbox_and_notification.sql` | `nad_outbox_event` + `nad_notification*` + templates. First event: `ScholarshipPublished`. | Step 5 |
| `V20–V21__nad_application*.sql` | `nad_application` (+ `@Version`) + profile/requirement snapshots + append-only children + `nad_wf_*` (one default definition). | Step 6 |
| `V20+__…` | document (+ object storage), communication, payment + finance, employee ops, content, reporting. | Step 7+ |

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
