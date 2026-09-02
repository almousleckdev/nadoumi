-- =============================================================================
--  Nadoumi — CORE SCHEMA DRAFT  (Identity / Applicant / University / Program /
--                                Scholarship / Application)
--
--  STATUS: DRAFT — Phase 1 design artifact.  *** DO NOT EXECUTE ***
--  - This file is NOT a Flyway migration and is NOT under classpath:db/migration.
--  - No Nadoumi tables are created in Phase 1.
--  - In Phase 3 this content is split into V3__nad_identity_access.sql ..
--    V8__nad_application.sql after final review (see docs/DATABASE_DESIGN.md §6).
--
--  Conventions (docs/DATABASE_DESIGN.md §4):
--  - MySQL 8/9, InnoDB, utf8mb4.  bigint auto-increment PKs (D13).
--  - RuoYi-style audit columns on mutable entities:
--      create_by, create_time, update_by, update_time, remark
--  - Append-only tables (*_event, *_history, *_decision, *_version) carry their own
--    actor/time columns and are never UPDATEd/DELETEd.
--  - Explicit NAMED foreign keys (a deliberate improvement over RuoYi's FK-less
--    style).  ON DELETE RESTRICT everywhere — business rows are archived, not deleted.
--  - Timestamps: datetime + app/JDBC zone GMT+8 (match RuoYi in v1).
--  - MySQL has no partial indexes; the "exactly one ACTIVE OWNER" invariant (INV1)
--    is enforced by a generated guard column + a plain UNIQUE, plus a service check.
-- =============================================================================

SET NAMES utf8mb4;
-- FK checks disabled only so the DROP/CREATE ordering in this DRAFT is re-runnable
-- for syntax validation. The real per-slice Flyway migrations are forward-only and
-- contain no DROP statements, and run with FK checks ON.
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 0.  Dictionary / config seeds  (delivered separately in V2; listed for context)
-- -----------------------------------------------------------------------------
--   sys_dict_type / sys_dict_data:
--     nad_user_type            00 staff | 10 student | 20 agent | 30 guardian
--     nad_access_role          OWNER | AGENT | GUARDIAN | VIEWER
--     nad_document_type        PASSPORT | PHOTO | ACADEMIC_TRANSCRIPT | DEGREE_CERTIFICATE
--                              LANGUAGE_CERTIFICATE | CV | PERSONAL_STATEMENT
--                              RECOMMENDATION_LETTER | STUDY_PLAN | FINANCIAL_PROOF
--                              MEDICAL_FORM | POLICE_CLEARANCE | CORRESPONDENCE_ATTACHMENT
--     nad_test_type            IELTS | TOEFL | DUOLINGO | GAOKAO | SAT | GRE | GMAT | HSK | OTHER
--     nad_degree_level         FOUNDATION | DIPLOMA | BACHELOR | MASTER | PHD
--     nad_application_type     PROGRAM_ONLY | PROGRAM_WITH_SCHOLARSHIP | SCHOLARSHIP_LED
--   sys_menu: a top-level 'Nadoumi' (M) group + C/F children carrying the tokens
--             in docs/PERMISSION_CATALOGUE.md §2.
-- -----------------------------------------------------------------------------


-- =============================================================================
-- 1.  IDENTITY & ACCESS
--     sys_user is EXISTING and NOT recreated. user_type is populated on create
--     paths: '00' staff, '10' student, '20' agent, '30' guardian (D10).
-- =============================================================================

-- 1.1  User <-> Applicant access grants  (DOMAIN_MODEL §4, D7)
DROP TABLE IF EXISTS nad_user_applicant_access;
CREATE TABLE nad_user_applicant_access (
  id                        bigint          NOT NULL AUTO_INCREMENT       COMMENT 'PK',
  user_id                   bigint              NULL                      COMMENT 'FK sys_user; NULL while status=PENDING (email invite)',
  applicant_id              bigint          NOT NULL                      COMMENT 'FK nad_applicant',
  application_id            bigint              NULL                      COMMENT 'FK nad_application; when set, grant is scoped to one application',
  access_role               varchar(16)     NOT NULL                      COMMENT 'OWNER | AGENT | GUARDIAN | VIEWER',
  status                    varchar(16)     NOT NULL DEFAULT 'PENDING'    COMMENT 'PENDING | ACTIVE | REVOKED | EXPIRED',
  invited_email             varchar(120)        NULL                      COMMENT 'target address for a PENDING invite',
  capability_overrides_json json                NULL                      COMMENT 'per-grant +/- to the role default capability set',
  is_interim                tinyint(1)      NOT NULL DEFAULT 0            COMMENT '1 = transitional staff-held OWNER for a staff-created applicant',
  owner_guard               bigint AS (CASE WHEN access_role = 'OWNER' AND status = 'ACTIVE' THEN applicant_id END) STORED
                                                                          COMMENT 'INV1 helper: unique => at most one ACTIVE OWNER per applicant',
  active_guard              varchar(96) AS (CASE WHEN status = 'ACTIVE'
                                             THEN CONCAT_WS(':', user_id, applicant_id, COALESCE(application_id, 0)) END) STORED
                                                                          COMMENT 'INV2 helper: unique => one ACTIVE grant per (user,applicant,application)',
  granted_by_user_id        bigint              NULL                      COMMENT 'FK sys_user (staff or OWNER who issued the grant)',
  granted_at                datetime            NULL,
  revoked_by_user_id        bigint              NULL                      COMMENT 'FK sys_user',
  revoked_at                datetime            NULL,
  revoke_reason             varchar(255)        NULL,
  expires_at                datetime            NULL                      COMMENT 'time-bounded grants; nightly sweep -> EXPIRED',
  create_by                 varchar(64)     NOT NULL DEFAULT '',
  create_time               datetime            NULL,
  update_by                 varchar(64)     NOT NULL DEFAULT '',
  update_time               datetime            NULL,
  remark                    varchar(500)        NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_uaa_owner_guard  (owner_guard),
  UNIQUE KEY uk_uaa_active_guard (active_guard),
  KEY idx_uaa_user      (user_id, status),
  KEY idx_uaa_applicant (applicant_id, status),
  KEY idx_uaa_sweep     (status, expires_at),
  KEY idx_uaa_app       (application_id),
  CONSTRAINT fk_uaa_user       FOREIGN KEY (user_id)            REFERENCES sys_user (user_id) ON DELETE RESTRICT,
  CONSTRAINT fk_uaa_grantor    FOREIGN KEY (granted_by_user_id) REFERENCES sys_user (user_id) ON DELETE RESTRICT,
  CONSTRAINT fk_uaa_revoker    FOREIGN KEY (revoked_by_user_id) REFERENCES sys_user (user_id) ON DELETE RESTRICT
  -- fk_uaa_applicant   added by ALTER after nad_applicant exists   (see §2)
  -- fk_uaa_application added by ALTER after nad_application exists  (see §8)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User<->Applicant access grants (external authz)';


-- =============================================================================
-- 2.  APPLICANT
-- =============================================================================

-- 2.1  Applicant  (the person an application is about; PII)
DROP TABLE IF EXISTS nad_applicant;
CREATE TABLE nad_applicant (
  id             bigint       NOT NULL AUTO_INCREMENT           COMMENT 'PK',
  given_name     varchar(100) NOT NULL,
  family_name    varchar(100) NOT NULL,
  dob            date             NULL                          COMMENT 'PII',
  nationality    varchar(2)       NULL                          COMMENT 'ISO 3166-1 alpha-2; PII',
  passport_no    varchar(64)      NULL                          COMMENT 'PII; NOT unique - soft duplicate detection only',
  email          varchar(120)     NULL,
  phone          varchar(32)      NULL,
  status         varchar(16)  NOT NULL DEFAULT 'DRAFT'          COMMENT 'DRAFT | ACTIVE | UNLINKED | ARCHIVED',
  create_by      varchar(64)  NOT NULL DEFAULT '',
  create_time    datetime         NULL,
  update_by      varchar(64)  NOT NULL DEFAULT '',
  update_time    datetime         NULL,
  remark         varchar(500)     NULL,
  PRIMARY KEY (id),
  KEY idx_applicant_name        (family_name, given_name),
  KEY idx_applicant_status      (status),
  KEY idx_applicant_nat_passport(nationality, passport_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Applicant (subject of an application)';

-- deferred FK from §1 now that nad_applicant exists
ALTER TABLE nad_user_applicant_access
  ADD CONSTRAINT fk_uaa_applicant FOREIGN KEY (applicant_id) REFERENCES nad_applicant (id) ON DELETE RESTRICT;

-- 2.2  Education history
DROP TABLE IF EXISTS nad_applicant_education;
CREATE TABLE nad_applicant_education (
  id            bigint       NOT NULL AUTO_INCREMENT,
  applicant_id  bigint       NOT NULL,
  institution   varchar(200) NOT NULL,
  level         varchar(32)      NULL                            COMMENT 'dict nad_degree_level',
  field         varchar(120)     NULL,
  gpa           decimal(6,3)     NULL,
  gpa_scale     decimal(6,3)     NULL,
  start_date    date             NULL,
  end_date      date             NULL,
  create_by     varchar(64)  NOT NULL DEFAULT '',
  create_time   datetime         NULL,
  update_by     varchar(64)  NOT NULL DEFAULT '',
  update_time   datetime         NULL,
  PRIMARY KEY (id),
  KEY idx_edu_applicant (applicant_id),
  CONSTRAINT fk_edu_applicant FOREIGN KEY (applicant_id) REFERENCES nad_applicant (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Applicant education history';

-- 2.3  Test scores
DROP TABLE IF EXISTS nad_applicant_test_score;
CREATE TABLE nad_applicant_test_score (
  id             bigint       NOT NULL AUTO_INCREMENT,
  applicant_id   bigint       NOT NULL,
  test_type      varchar(24)  NOT NULL                           COMMENT 'dict nad_test_type',
  score          varchar(32)  NOT NULL                           COMMENT 'overall band/score as reported',
  sub_scores_json json            NULL,
  taken_on       date             NULL,
  expires_on     date             NULL,
  create_by      varchar(64)  NOT NULL DEFAULT '',
  create_time    datetime         NULL,
  update_by      varchar(64)  NOT NULL DEFAULT '',
  update_time    datetime         NULL,
  PRIMARY KEY (id),
  KEY idx_score_applicant (applicant_id, test_type),
  CONSTRAINT fk_score_applicant FOREIGN KEY (applicant_id) REFERENCES nad_applicant (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Applicant test scores';

-- 2.4  Contacts (guardian / emergency; NOT a login identity)
DROP TABLE IF EXISTS nad_applicant_contact;
CREATE TABLE nad_applicant_contact (
  id            bigint       NOT NULL AUTO_INCREMENT,
  applicant_id  bigint       NOT NULL,
  relation      varchar(16)  NOT NULL                            COMMENT 'GUARDIAN | EMERGENCY | OTHER',
  name          varchar(150) NOT NULL,
  email         varchar(120)     NULL,
  phone         varchar(32)      NULL,
  create_by     varchar(64)  NOT NULL DEFAULT '',
  create_time   datetime         NULL,
  update_by     varchar(64)  NOT NULL DEFAULT '',
  update_time   datetime         NULL,
  PRIMARY KEY (id),
  KEY idx_contact_applicant (applicant_id),
  CONSTRAINT fk_contact_applicant FOREIGN KEY (applicant_id) REFERENCES nad_applicant (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Applicant contacts (non-login)';


-- =============================================================================
-- 3.  UNIVERSITY / PROGRAM  (PUBLIC catalog — no commercial fields)
-- =============================================================================

DROP TABLE IF EXISTS nad_university;
CREATE TABLE nad_university (
  id                bigint       NOT NULL AUTO_INCREMENT,
  name              varchar(200) NOT NULL,
  country           varchar(2)   NOT NULL                        COMMENT 'ISO 3166-1 alpha-2',
  city              varchar(120)     NULL,
  website           varchar(255)     NULL,
  ranking_tier      varchar(32)      NULL,
  logo_document_id  bigint           NULL                        COMMENT 'FK nad_document (Document slice)',
  status            varchar(16)  NOT NULL DEFAULT 'ACTIVE'       COMMENT 'ACTIVE | INACTIVE',
  create_by         varchar(64)  NOT NULL DEFAULT '',
  create_time       datetime         NULL,
  update_by         varchar(64)  NOT NULL DEFAULT '',
  update_time       datetime         NULL,
  remark            varchar(500)     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_university_name_country (name, country),
  KEY idx_university_country (country, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='University (public catalog)';

DROP TABLE IF EXISTS nad_program;
CREATE TABLE nad_program (
  id                        bigint        NOT NULL AUTO_INCREMENT,
  university_id             bigint        NOT NULL,
  name                      varchar(200)  NOT NULL,
  degree_level              varchar(32)   NOT NULL                COMMENT 'dict nad_degree_level',
  field                     varchar(120)      NULL,
  language                  varchar(60)       NULL                COMMENT 'language of instruction',
  tuition_amount            decimal(14,2)     NULL,
  tuition_currency          varchar(3)        NULL                COMMENT 'ISO 4217',
  duration_months           int               NULL,
  workflow_definition_code  varchar(64)       NULL                COMMENT 'route matching applications to this nad_wf_definition; NULL -> default',
  status                    varchar(16)   NOT NULL DEFAULT 'ACTIVE',
  create_by                 varchar(64)   NOT NULL DEFAULT '',
  create_time               datetime          NULL,
  update_by                 varchar(64)   NOT NULL DEFAULT '',
  update_time               datetime          NULL,
  remark                    varchar(500)      NULL,
  PRIMARY KEY (id),
  KEY idx_program_university (university_id, status),
  KEY idx_program_taxonomy   (degree_level, field),
  CONSTRAINT fk_program_university FOREIGN KEY (university_id) REFERENCES nad_university (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Degree programme offered by a university (public)';

DROP TABLE IF EXISTS nad_program_intake;
CREATE TABLE nad_program_intake (
  id                bigint       NOT NULL AUTO_INCREMENT,
  program_id        bigint       NOT NULL,
  term              varchar(40)  NOT NULL                         COMMENT 'e.g. 2026-FALL',
  application_open  date             NULL,
  application_close date             NULL,
  status            varchar(16)  NOT NULL DEFAULT 'OPEN'          COMMENT 'OPEN | CLOSED',
  create_by         varchar(64)  NOT NULL DEFAULT '',
  create_time       datetime         NULL,
  update_by         varchar(64)  NOT NULL DEFAULT '',
  update_time       datetime         NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_intake_program_term (program_id, term),
  CONSTRAINT fk_intake_program FOREIGN KEY (program_id) REFERENCES nad_program (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Programme intake / term';


-- =============================================================================
-- 4.  PARTNERSHIP   *** DEPENDENCY STUB — finalized in the Partnership slice ***
--     Included here so scholarship-internal FKs resolve. Confidential.
-- =============================================================================

DROP TABLE IF EXISTS nad_partnership;
CREATE TABLE nad_partnership (
  id                    bigint        NOT NULL AUTO_INCREMENT,
  university_id         bigint        NOT NULL,
  status                varchar(16)   NOT NULL DEFAULT 'DRAFT'    COMMENT 'DRAFT | ACTIVE | SUSPENDED | TERMINATED',
  tier                  varchar(24)       NULL,
  commission_model_json json              NULL                    COMMENT 'CONFIDENTIAL',
  contract_start        date              NULL,
  contract_end          date              NULL,
  agreement_document_id bigint            NULL                    COMMENT 'FK nad_document',
  internal_notes        text              NULL                    COMMENT 'CONFIDENTIAL',
  active_guard          bigint AS (CASE WHEN status = 'ACTIVE' THEN university_id END) STORED
                                                                  COMMENT 'INV4: <=1 ACTIVE partnership per university',
  create_by             varchar(64)   NOT NULL DEFAULT '',
  create_time           datetime          NULL,
  update_by             varchar(64)   NOT NULL DEFAULT '',
  update_time           datetime          NULL,
  remark                varchar(500)      NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_partnership_active_university (active_guard),
  KEY idx_partnership_university (university_id, status),
  CONSTRAINT fk_partnership_university FOREIGN KEY (university_id) REFERENCES nad_university (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Nadoumi<->university commercial relationship (CONFIDENTIAL)';

DROP TABLE IF EXISTS nad_partnership_contact;
CREATE TABLE nad_partnership_contact (
  id             bigint       NOT NULL AUTO_INCREMENT,
  partnership_id bigint       NOT NULL,
  name           varchar(150) NOT NULL,
  role           varchar(80)      NULL,
  email          varchar(120)     NULL,
  phone          varchar(32)      NULL,
  create_by      varchar(64)  NOT NULL DEFAULT '',
  create_time    datetime         NULL,
  PRIMARY KEY (id),
  KEY idx_pcontact_partnership (partnership_id),
  CONSTRAINT fk_pcontact_partnership FOREIGN KEY (partnership_id) REFERENCES nad_partnership (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Partnership contact (CONFIDENTIAL)';

DROP TABLE IF EXISTS nad_partnership_event;
CREATE TABLE nad_partnership_event (
  id             bigint       NOT NULL AUTO_INCREMENT,
  partnership_id bigint       NOT NULL,
  event_type     varchar(48)  NOT NULL,
  detail_json    json             NULL,
  actor_user_id  bigint           NULL,
  at             datetime     NOT NULL,
  PRIMARY KEY (id),
  KEY idx_pevent_partnership (partnership_id, at),
  CONSTRAINT fk_pevent_partnership FOREIGN KEY (partnership_id) REFERENCES nad_partnership (id) ON DELETE RESTRICT,
  CONSTRAINT fk_pevent_actor       FOREIGN KEY (actor_user_id)  REFERENCES sys_user (user_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Partnership audit trail (append-only)';


-- =============================================================================
-- 5.  SCHOLARSHIP  (confidentiality split — DOMAIN_MODEL §6)
-- =============================================================================

-- 5.1  Student-safe scholarship
DROP TABLE IF EXISTS nad_scholarship;
CREATE TABLE nad_scholarship (
  id                        bigint        NOT NULL AUTO_INCREMENT,
  title                     varchar(200)  NOT NULL,
  country                   varchar(2)    NOT NULL                COMMENT 'ISO 3166-1 alpha-2',
  degree_level              varchar(32)       NULL                COMMENT 'dict nad_degree_level',
  field                     varchar(120)      NULL,
  benefits                  text              NULL,
  eligibility               text              NULL,
  requirements              text              NULL,
  deadline                  date              NULL,
  binding                   varchar(16)   NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN | PROGRAM_BOUND',
  workflow_definition_code  varchar(64)       NULL,
  status                    varchar(16)   NOT NULL DEFAULT 'DRAFT'COMMENT 'DRAFT | PUBLISHED | CLOSED | ARCHIVED',
  create_by                 varchar(64)   NOT NULL DEFAULT '',
  create_time               datetime          NULL,
  update_by                 varchar(64)   NOT NULL DEFAULT '',
  update_time               datetime          NULL,
  remark                    varchar(500)      NULL,
  PRIMARY KEY (id),
  KEY idx_scholarship_discovery (status, country, degree_level, field),
  KEY idx_scholarship_deadline  (deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Scholarship - STUDENT-SAFE columns only';

-- 5.2  Confidential linkage (1:1 with nad_scholarship)
DROP TABLE IF EXISTS nad_scholarship_internal;
CREATE TABLE nad_scholarship_internal (
  scholarship_id     bigint       NOT NULL                       COMMENT 'PK = FK nad_scholarship',
  university_id       bigint          NULL,
  partnership_id      bigint          NULL,
  internal_status     varchar(32)     NULL,
  operational_notes   text            NULL,
  confidential_terms  text            NULL                        COMMENT 'CONFIDENTIAL',
  update_by           varchar(64) NOT NULL DEFAULT '',
  update_time         datetime        NULL,
  PRIMARY KEY (scholarship_id),
  KEY idx_schint_university  (university_id),
  KEY idx_schint_partnership (partnership_id),
  CONSTRAINT fk_schint_scholarship FOREIGN KEY (scholarship_id) REFERENCES nad_scholarship (id)  ON DELETE RESTRICT,
  CONSTRAINT fk_schint_university  FOREIGN KEY (university_id)   REFERENCES nad_university (id)   ON DELETE RESTRICT,
  CONSTRAINT fk_schint_partnership FOREIGN KEY (partnership_id)  REFERENCES nad_partnership (id)  ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Scholarship confidential linkage - staff-only';

-- 5.3  Allowed programmes for a PROGRAM_BOUND scholarship (staff-side; confidential-adjacent)
DROP TABLE IF EXISTS nad_scholarship_program;
CREATE TABLE nad_scholarship_program (
  id             bigint NOT NULL AUTO_INCREMENT,
  scholarship_id bigint NOT NULL,
  program_id     bigint NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_schprog (scholarship_id, program_id),
  KEY idx_schprog_program (program_id),
  CONSTRAINT fk_schprog_scholarship FOREIGN KEY (scholarship_id) REFERENCES nad_scholarship (id) ON DELETE RESTRICT,
  CONSTRAINT fk_schprog_program     FOREIGN KEY (program_id)     REFERENCES nad_program (id)     ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Allowed programmes for a PROGRAM_BOUND scholarship (staff-only)';

-- 5.4  The ONLY object student / public MyBatis mappers may read (DOMAIN_MODEL §6.2 layer 2)
DROP VIEW IF EXISTS v_scholarship_student;
CREATE VIEW v_scholarship_student AS
  SELECT id, title, country, degree_level, field, benefits, eligibility,
         requirements, deadline, binding, status
  FROM   nad_scholarship
  WHERE  status = 'PUBLISHED';


-- =============================================================================
-- 6.  WORKFLOW (definition layer)   *** partial: instance table is in §8b ***
--     See docs/WORKFLOW_PROGRAM_WITH_SCHOLARSHIP.md for a concrete definition.
-- =============================================================================

DROP TABLE IF EXISTS nad_wf_definition;
CREATE TABLE nad_wf_definition (
  id          bigint       NOT NULL AUTO_INCREMENT,
  code        varchar(64)  NOT NULL,
  name        varchar(160) NOT NULL,
  version     int          NOT NULL DEFAULT 1,
  status      varchar(16)  NOT NULL DEFAULT 'DRAFT'               COMMENT 'DRAFT | ACTIVE | RETIRED',
  create_by   varchar(64)  NOT NULL DEFAULT '',
  create_time datetime         NULL,
  update_by   varchar(64)  NOT NULL DEFAULT '',
  update_time datetime         NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_wfdef_code_version (code, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Workflow definition (versioned)';

DROP TABLE IF EXISTS nad_wf_stage;
CREATE TABLE nad_wf_stage (
  id            bigint       NOT NULL AUTO_INCREMENT,
  definition_id bigint       NOT NULL,
  code          varchar(48)  NOT NULL,
  name          varchar(120) NOT NULL,
  order_no      int          NOT NULL,
  stage_type    varchar(16)  NOT NULL                             COMMENT 'START | NORMAL | DECISION | TERMINAL',
  status_label  varchar(40)      NULL                             COMMENT 'coarse current_status the engine stamps on entry',
  sla_hours     int              NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_wfstage_def_code (definition_id, code),
  KEY idx_wfstage_def_order (definition_id, order_no),
  CONSTRAINT fk_wfstage_def FOREIGN KEY (definition_id) REFERENCES nad_wf_definition (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Workflow stage';

DROP TABLE IF EXISTS nad_wf_transition;
CREATE TABLE nad_wf_transition (
  id            bigint       NOT NULL AUTO_INCREMENT,
  definition_id bigint       NOT NULL,
  code          varchar(48)  NOT NULL,
  from_stage_id bigint           NULL                             COMMENT 'NULL allowed only for the START-entry pseudo transition',
  to_stage_id   bigint       NOT NULL,
  guard_json    json             NULL                             COMMENT 'fixed predicate set only (D4): {"all":[...]}',
  auto          tinyint(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_wftrans_def_code (definition_id, code),
  KEY idx_wftrans_from (from_stage_id),
  KEY idx_wftrans_to   (to_stage_id),
  CONSTRAINT fk_wftrans_def  FOREIGN KEY (definition_id) REFERENCES nad_wf_definition (id) ON DELETE RESTRICT,
  CONSTRAINT fk_wftrans_from FOREIGN KEY (from_stage_id) REFERENCES nad_wf_stage (id)      ON DELETE RESTRICT,
  CONSTRAINT fk_wftrans_to   FOREIGN KEY (to_stage_id)   REFERENCES nad_wf_stage (id)      ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Workflow transition';

DROP TABLE IF EXISTS nad_wf_stage_task_template;
CREATE TABLE nad_wf_stage_task_template (
  id            bigint       NOT NULL AUTO_INCREMENT,
  stage_id      bigint       NOT NULL,
  title         varchar(200) NOT NULL,
  role_required varchar(64)      NULL                             COMMENT 'sys_role.role_key expected to complete it',
  mandatory     tinyint(1)   NOT NULL DEFAULT 1,
  blocks_exit   tinyint(1)   NOT NULL DEFAULT 1,
  order_no      int          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_wftasktpl_stage (stage_id, order_no),
  CONSTRAINT fk_wftasktpl_stage FOREIGN KEY (stage_id) REFERENCES nad_wf_stage (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Task template materialised on stage entry';


-- =============================================================================
-- 7.  DOCUMENT   *** DEPENDENCY STUB — finalized in the Document slice ***
--     Minimal shape so nad_application_document / requirement FKs resolve.
-- =============================================================================

DROP TABLE IF EXISTS nad_document;
CREATE TABLE nad_document (
  id                 bigint       NOT NULL AUTO_INCREMENT,
  applicant_id        bigint      NOT NULL                        COMMENT 'owner - always set',
  application_id      bigint          NULL                        COMMENT 'set only for application-specific documents',
  doc_type            varchar(48) NOT NULL                        COMMENT 'dict nad_document_type',
  status              varchar(16) NOT NULL DEFAULT 'DRAFT'        COMMENT 'derived from current_version + expires_on',
  current_version_id  bigint          NULL,
  reviewer_user_id    bigint          NULL,
  rejection_reason    varchar(500)    NULL,
  expires_on          date            NULL,
  create_by           varchar(64) NOT NULL DEFAULT '',
  create_time         datetime        NULL,
  update_by           varchar(64) NOT NULL DEFAULT '',
  update_time         datetime        NULL,
  PRIMARY KEY (id),
  KEY idx_document_applicant   (applicant_id),
  KEY idx_document_application (application_id),
  KEY idx_document_status      (status),
  CONSTRAINT fk_document_applicant FOREIGN KEY (applicant_id)     REFERENCES nad_applicant (id) ON DELETE RESTRICT,
  CONSTRAINT fk_document_reviewer  FOREIGN KEY (reviewer_user_id) REFERENCES sys_user (user_id) ON DELETE RESTRICT
  -- fk_document_application added by ALTER after nad_application exists (see §8)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Business document (STUB - Document slice finalizes)';

DROP TABLE IF EXISTS nad_document_version;
CREATE TABLE nad_document_version (
  id                  bigint       NOT NULL AUTO_INCREMENT,
  document_id          bigint      NOT NULL,
  version_no           int         NOT NULL,
  storage_key          varchar(255) NOT NULL                      COMMENT 'object-storage key; bytes NOT in MySQL (D5)',
  content_type         varchar(120) NOT NULL,
  size_bytes           bigint       NOT NULL,
  checksum_sha256      char(64)         NULL,
  uploaded_by          bigint           NULL,
  uploaded_at          datetime     NOT NULL,
  verification_status  varchar(16)  NOT NULL DEFAULT 'PENDING'    COMMENT 'PENDING | VERIFIED | REJECTED',
  verified_by          bigint           NULL,
  verified_at          datetime         NULL,
  scan_status          varchar(16)  NOT NULL DEFAULT 'PENDING'    COMMENT 'PENDING | CLEAN | INFECTED',
  PRIMARY KEY (id),
  UNIQUE KEY uk_docver (document_id, version_no),
  CONSTRAINT fk_docver_document FOREIGN KEY (document_id) REFERENCES nad_document (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Immutable document version (append-only)';

ALTER TABLE nad_document
  ADD CONSTRAINT fk_document_current_version
  FOREIGN KEY (current_version_id) REFERENCES nad_document_version (id) ON DELETE RESTRICT;

DROP TABLE IF EXISTS nad_document_requirement;
CREATE TABLE nad_document_requirement (
  id         bigint       NOT NULL AUTO_INCREMENT,
  scope      varchar(16)  NOT NULL                                COMMENT 'PROGRAM | SCHOLARSHIP | WF_STAGE',
  ref_id     bigint       NOT NULL                                COMMENT 'program_id | scholarship_id | wf_stage_id per scope',
  doc_type   varchar(48)  NOT NULL                                COMMENT 'dict nad_document_type',
  mandatory  tinyint(1)   NOT NULL DEFAULT 1,
  waivable   tinyint(1)   NOT NULL DEFAULT 0,
  notes      varchar(500)     NULL,
  PRIMARY KEY (id),
  KEY idx_docreq_scope (scope, ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Document requirement -> per-application checklist';

DROP TABLE IF EXISTS nad_document_event;
CREATE TABLE nad_document_event (
  id            bigint      NOT NULL AUTO_INCREMENT,
  document_id    bigint     NOT NULL,
  event_type     varchar(48) NOT NULL,
  actor_user_id  bigint         NULL,
  at             datetime    NOT NULL,
  detail_json    json           NULL,
  PRIMARY KEY (id),
  KEY idx_docevent_document (document_id, at),
  CONSTRAINT fk_docevent_document FOREIGN KEY (document_id) REFERENCES nad_document (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Document audit trail (append-only)';


-- =============================================================================
-- 8.  APPLICATION  (the business case — DOMAIN_MODEL §5, §7)
-- =============================================================================

-- 8.1  Application root
DROP TABLE IF EXISTS nad_application;
CREATE TABLE nad_application (
  id                     bigint       NOT NULL AUTO_INCREMENT,
  applicant_id            bigint      NOT NULL,
  application_type        varchar(32) NOT NULL                    COMMENT 'PROGRAM_ONLY | PROGRAM_WITH_SCHOLARSHIP | SCHOLARSHIP_LED',
  program_id              bigint          NULL,
  scholarship_id          bigint          NULL,
  intake_id               bigint          NULL,
  target_university_id    bigint          NULL                    COMMENT 'for SCHOLARSHIP_LED before a programme is chosen',
  workflow_instance_id    bigint          NULL,
  current_stage_id        bigint          NULL,
  current_status          varchar(40)     NULL                    COMMENT 'DENORMALISED; written ONLY by the workflow engine (INV9)',
  assignee_user_id        bigint          NULL                    COMMENT 'owning staff member',
  version                 int         NOT NULL DEFAULT 0          COMMENT 'optimistic lock (@Version)',
  create_by               varchar(64) NOT NULL DEFAULT '',
  create_time             datetime        NULL,
  update_by               varchar(64) NOT NULL DEFAULT '',
  update_time             datetime        NULL,
  remark                  varchar(500)    NULL,
  PRIMARY KEY (id),
  KEY idx_app_applicant  (applicant_id),
  KEY idx_app_assignee   (assignee_user_id, current_status),
  KEY idx_app_program    (program_id),
  KEY idx_app_scholarship(scholarship_id),
  KEY idx_app_status     (current_status),
  -- INV6/INV7/INV8: enforced by CHECK + service layer
  CONSTRAINT ck_app_type_invariants CHECK (
      (application_type = 'PROGRAM_ONLY'
          AND program_id IS NOT NULL AND scholarship_id IS NULL AND intake_id IS NOT NULL)
   OR (application_type = 'PROGRAM_WITH_SCHOLARSHIP'
          AND program_id IS NOT NULL AND scholarship_id IS NOT NULL AND intake_id IS NOT NULL)
   OR (application_type = 'SCHOLARSHIP_LED'
          AND scholarship_id IS NOT NULL)
  ),
  CONSTRAINT fk_app_applicant   FOREIGN KEY (applicant_id)         REFERENCES nad_applicant (id)     ON DELETE RESTRICT,
  CONSTRAINT fk_app_program     FOREIGN KEY (program_id)           REFERENCES nad_program (id)       ON DELETE RESTRICT,
  CONSTRAINT fk_app_scholarship FOREIGN KEY (scholarship_id)       REFERENCES nad_scholarship (id)   ON DELETE RESTRICT,
  CONSTRAINT fk_app_intake      FOREIGN KEY (intake_id)            REFERENCES nad_program_intake (id) ON DELETE RESTRICT,
  CONSTRAINT fk_app_target_uni  FOREIGN KEY (target_university_id) REFERENCES nad_university (id)    ON DELETE RESTRICT,
  CONSTRAINT fk_app_stage       FOREIGN KEY (current_stage_id)     REFERENCES nad_wf_stage (id)      ON DELETE RESTRICT,
  CONSTRAINT fk_app_assignee    FOREIGN KEY (assignee_user_id)     REFERENCES sys_user (user_id)     ON DELETE RESTRICT
  -- fk_app_wf_instance added in §8b (circular with nad_wf_instance)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Application (business case)';

-- deferred FKs now that nad_application exists
ALTER TABLE nad_user_applicant_access
  ADD CONSTRAINT fk_uaa_application FOREIGN KEY (application_id) REFERENCES nad_application (id) ON DELETE RESTRICT;
ALTER TABLE nad_document
  ADD CONSTRAINT fk_document_application FOREIGN KEY (application_id) REFERENCES nad_application (id) ON DELETE RESTRICT;

-- 8b.  Workflow instance (circular FK with nad_application)
DROP TABLE IF EXISTS nad_wf_instance;
CREATE TABLE nad_wf_instance (
  id                 bigint      NOT NULL AUTO_INCREMENT,
  definition_id       bigint     NOT NULL,
  definition_version  int        NOT NULL                         COMMENT 'pinned; no auto-migration (D4)',
  application_id       bigint     NOT NULL,
  current_stage_id     bigint         NULL,
  status              varchar(16) NOT NULL DEFAULT 'RUNNING'      COMMENT 'RUNNING | CLOSED',
  started_at          datetime    NOT NULL,
  closed_at           datetime        NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_wfinst_application (application_id),
  KEY idx_wfinst_def (definition_id),
  CONSTRAINT fk_wfinst_def   FOREIGN KEY (definition_id)   REFERENCES nad_wf_definition (id) ON DELETE RESTRICT,
  CONSTRAINT fk_wfinst_app   FOREIGN KEY (application_id)   REFERENCES nad_application (id)   ON DELETE RESTRICT,
  CONSTRAINT fk_wfinst_stage FOREIGN KEY (current_stage_id) REFERENCES nad_wf_stage (id)     ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Workflow instance (1:1 with application)';

ALTER TABLE nad_application
  ADD CONSTRAINT fk_app_wf_instance FOREIGN KEY (workflow_instance_id) REFERENCES nad_wf_instance (id) ON DELETE RESTRICT;

-- 8.2  Stage history (append-only)
DROP TABLE IF EXISTS nad_application_stage_history;
CREATE TABLE nad_application_stage_history (
  id              bigint      NOT NULL AUTO_INCREMENT,
  application_id   bigint     NOT NULL,
  from_stage_id    bigint         NULL,
  to_stage_id      bigint     NOT NULL,
  transition_code  varchar(48)    NULL,
  changed_by       bigint         NULL,
  changed_at       datetime   NOT NULL,
  reason           varchar(500)   NULL,
  PRIMARY KEY (id),
  KEY idx_ash_app (application_id, changed_at),
  CONSTRAINT fk_ash_app  FOREIGN KEY (application_id) REFERENCES nad_application (id) ON DELETE RESTRICT,
  CONSTRAINT fk_ash_from FOREIGN KEY (from_stage_id)  REFERENCES nad_wf_stage (id)    ON DELETE RESTRICT,
  CONSTRAINT fk_ash_to   FOREIGN KEY (to_stage_id)    REFERENCES nad_wf_stage (id)    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Application stage history (append-only)';

-- 8.3  Event timeline (append-only)
DROP TABLE IF EXISTS nad_application_event;
CREATE TABLE nad_application_event (
  id             bigint      NOT NULL AUTO_INCREMENT,
  application_id  bigint     NOT NULL,
  event_type      varchar(48) NOT NULL,
  payload_json    json           NULL,
  created_by      bigint         NULL,
  created_at      datetime    NOT NULL,
  PRIMARY KEY (id),
  KEY idx_aevent_app (application_id, created_at),
  CONSTRAINT fk_aevent_app FOREIGN KEY (application_id) REFERENCES nad_application (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Application event timeline (append-only)';

-- 8.4  Tasks (SINGLE table: engine-materialised + ad-hoc, D12)
DROP TABLE IF EXISTS nad_application_task;
CREATE TABLE nad_application_task (
  id                        bigint       NOT NULL AUTO_INCREMENT,
  application_id             bigint      NOT NULL,
  wf_stage_task_template_id  bigint          NULL                 COMMENT 'NULL = ad-hoc staff task',
  stage_id                  bigint          NULL                  COMMENT 'stage the task belongs to',
  title                     varchar(200) NOT NULL,
  assignee_user_id          bigint          NULL,
  mandatory                 tinyint(1)   NOT NULL DEFAULT 0,
  blocks_exit               tinyint(1)   NOT NULL DEFAULT 0,
  status                    varchar(16)  NOT NULL DEFAULT 'OPEN'  COMMENT 'OPEN | DONE | SKIPPED | CANCELLED',
  due_at                    datetime        NULL,
  completed_by              bigint          NULL,
  completed_at              datetime        NULL,
  skip_reason               varchar(500)    NULL,
  create_by                 varchar(64)  NOT NULL DEFAULT '',
  create_time               datetime        NULL,
  update_by                 varchar(64)  NOT NULL DEFAULT '',
  update_time               datetime        NULL,
  PRIMARY KEY (id),
  KEY idx_atask_app      (application_id, status),
  KEY idx_atask_assignee (assignee_user_id, status),
  KEY idx_atask_due      (due_at),
  CONSTRAINT fk_atask_app      FOREIGN KEY (application_id)            REFERENCES nad_application (id)           ON DELETE RESTRICT,
  CONSTRAINT fk_atask_template FOREIGN KEY (wf_stage_task_template_id) REFERENCES nad_wf_stage_task_template (id) ON DELETE RESTRICT,
  CONSTRAINT fk_atask_stage    FOREIGN KEY (stage_id)                  REFERENCES nad_wf_stage (id)              ON DELETE RESTRICT,
  CONSTRAINT fk_atask_assignee FOREIGN KEY (assignee_user_id)         REFERENCES sys_user (user_id)             ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Application task (engine-materialised + ad-hoc)';

-- 8.5  Notes (INTERNAL vs SHARED visibility)
DROP TABLE IF EXISTS nad_application_note;
CREATE TABLE nad_application_note (
  id             bigint      NOT NULL AUTO_INCREMENT,
  application_id  bigint     NOT NULL,
  body            text       NOT NULL,
  visibility      varchar(16) NOT NULL DEFAULT 'INTERNAL'         COMMENT 'INTERNAL | SHARED',
  create_by       varchar(64) NOT NULL DEFAULT '',
  create_time     datetime       NULL,
  update_by       varchar(64) NOT NULL DEFAULT '',
  update_time     datetime       NULL,
  PRIMARY KEY (id),
  KEY idx_anote_app (application_id, visibility),
  CONSTRAINT fk_anote_app FOREIGN KEY (application_id) REFERENCES nad_application (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Application note';

-- 8.6  Decisions (append-only)
DROP TABLE IF EXISTS nad_application_decision;
CREATE TABLE nad_application_decision (
  id                     bigint      NOT NULL AUTO_INCREMENT,
  application_id          bigint     NOT NULL,
  stage_id               bigint          NULL,
  decision_type          varchar(32) NOT NULL                     COMMENT 'UNIVERSITY_OFFER | SCHOLARSHIP_AWARD | APPLICANT_RESPONSE | NADOUMI_INTERNAL',
  outcome                varchar(32) NOT NULL                     COMMENT 'OFFER | CONDITIONAL_OFFER | REJECT | WAITLIST | AWARDED | DECLINED_BY_APPLICANT | ACCEPTED_BY_APPLICANT | ELIGIBLE | INELIGIBLE',
  drives_transition_code varchar(48)     NULL,
  decided_by             bigint          NULL,
  decided_at             datetime    NOT NULL,
  rationale              varchar(1000)   NULL,
  PRIMARY KEY (id),
  KEY idx_adec_app (application_id, decided_at),
  CONSTRAINT fk_adec_app   FOREIGN KEY (application_id) REFERENCES nad_application (id) ON DELETE RESTRICT,
  CONSTRAINT fk_adec_stage FOREIGN KEY (stage_id)       REFERENCES nad_wf_stage (id)    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Application decision (append-only)';

-- 8.7  Submissions to a university / scholarship body
DROP TABLE IF EXISTS nad_application_submission;
CREATE TABLE nad_application_submission (
  id                 bigint      NOT NULL AUTO_INCREMENT,
  application_id      bigint     NOT NULL,
  channel            varchar(24) NOT NULL                         COMMENT 'UNIVERSITY_PORTAL | EMAIL | AGENT | OTHER',
  external_reference  varchar(160)   NULL,
  submitted_by        bigint         NULL,
  submitted_at        datetime    NOT NULL,
  notes              varchar(500)    NULL,
  PRIMARY KEY (id),
  KEY idx_asub_app (application_id, submitted_at),
  CONSTRAINT fk_asub_app FOREIGN KEY (application_id) REFERENCES nad_application (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Application submission record';

-- 8.8  Sibling / supersede links
DROP TABLE IF EXISTS nad_application_link;
CREATE TABLE nad_application_link (
  id                     bigint      NOT NULL AUTO_INCREMENT,
  application_id          bigint     NOT NULL,
  related_application_id  bigint     NOT NULL,
  relation               varchar(24) NOT NULL                     COMMENT 'SIBLING | SUPERSEDES | SUPERSEDED_BY',
  create_by              varchar(64) NOT NULL DEFAULT '',
  create_time            datetime        NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_alink (application_id, related_application_id, relation),
  CONSTRAINT fk_alink_app     FOREIGN KEY (application_id)         REFERENCES nad_application (id) ON DELETE RESTRICT,
  CONSTRAINT fk_alink_related FOREIGN KEY (related_application_id) REFERENCES nad_application (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Application relationship links';

-- 8.9  Attach applicant-owned documents to an application (M:N, D11)
DROP TABLE IF EXISTS nad_application_document;
CREATE TABLE nad_application_document (
  id             bigint      NOT NULL AUTO_INCREMENT,
  application_id  bigint     NOT NULL,
  document_id     bigint     NOT NULL,
  requirement_id  bigint         NULL                             COMMENT 'FK nad_document_requirement - which checklist item this satisfies',
  added_by        bigint         NULL,
  added_at        datetime   NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_appdoc (application_id, document_id),
  KEY idx_appdoc_document    (document_id),
  KEY idx_appdoc_requirement (requirement_id),
  CONSTRAINT fk_appdoc_app         FOREIGN KEY (application_id) REFERENCES nad_application (id)          ON DELETE RESTRICT,
  CONSTRAINT fk_appdoc_document    FOREIGN KEY (document_id)    REFERENCES nad_document (id)            ON DELETE RESTRICT,
  CONSTRAINT fk_appdoc_requirement FOREIGN KEY (requirement_id) REFERENCES nad_document_requirement (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Attach applicant-owned documents to an application (M:N)';

-- =============================================================================
-- END OF DRAFT.  Review checklist before this becomes V3..V8:
--   [ ] CHECK constraint support confirmed on the target MySQL (8.0.16+; MySQL 9 OK).
--   [ ] Stored generated columns for owner_guard/active_guard behave under the
--       concurrent grant/transfer flows (add SELECT ... FOR UPDATE in the service).
--   [ ] Decide ON DELETE for child tables if hard-delete of an application is ever
--       allowed (currently RESTRICT everywhere; model is archive-only).
--   [ ] Confirm json column type acceptable (MySQL native JSON) vs longtext.
--   [ ] Align varchar lengths with i18n needs (names, titles).
--   [ ] Index review against the real query patterns from the mappers.
--   [ ] Split into per-slice migration files; move dictionary/menu seeds to V2.
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 1;
