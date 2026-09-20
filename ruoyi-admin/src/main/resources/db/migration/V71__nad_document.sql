-- Document domain, Step 7 (docs/DOCUMENT_MANAGEMENT.md, docs/superpowers/specs/
-- 2026-09-20-document-domain-design.md). nad_document is the governed-document
-- head row: always owned by an applicant; application_id is set only for a
-- document specific to one application (a reusable applicant-level doc, e.g.
-- passport, carries application_id = null and is attached to applications via
-- nad_application_document instead — that attach table is deferred until the
-- Application domain's nad_application table exists, see the design doc's
-- DECISION REQUIRED #1).
--
-- status is derived by the service from current_version_id's verification_status
-- + expires_on (DOCUMENT_MANAGEMENT.md INV10) and written here as a plain column,
-- not a generated column, so it stays readable/filterable without recomputation
-- on every query.
--
-- manual rollback: DROP TABLE nad_document;

create table nad_document (
  id                 bigint        not null auto_increment,
  applicant_id       bigint        not null comment 'owner; always set',
  application_id     bigint            null comment 'set only for an application-specific document',
  doc_type           varchar(40)   not null comment 'sys_dict nad_document_type',
  status             varchar(16)   not null default 'DRAFT' comment 'DRAFT|SUBMITTED|IN_REVIEW|VERIFIED|REJECTED|EXPIRED',
  current_version_id bigint            null comment 'authoritative version; fk added in V72 after nad_document_version exists',
  reviewer_user_id   bigint            null comment 'sys_user.user_id of the staff reviewer, once assigned/acted',
  rejection_reason   varchar(500)      null comment 'SHARED visibility — the applicant may read this',
  expires_on         date              null,
  create_by          varchar(64)   not null default '',
  create_time        datetime          null,
  update_by          varchar(64)   not null default '',
  update_time        datetime          null,
  primary key (id),
  key idx_document_applicant (applicant_id),
  key idx_document_application (application_id),
  key idx_document_status (status)
) engine=innodb default charset=utf8mb4;
