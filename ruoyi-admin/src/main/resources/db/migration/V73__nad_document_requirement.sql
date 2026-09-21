-- nad_document_requirement: a document-type checklist definition scoped to a
-- program, scholarship, or workflow stage. Read-only from the Application
-- domain's (not yet built) workflow engine to resolve a per-application
-- checklist and drive its ALL_REQUIRED_DOCUMENTS_VERIFIED-style guard
-- (docs/DOCUMENT_MANAGEMENT.md §3.1). ref_id is polymorphic on scope, same
-- pattern as nad_media_asset.owner_kind/owner_id — no FK, enforced by the
-- resolving service.
--
-- manual rollback: DROP TABLE nad_document_requirement;

create table nad_document_requirement (
  id           bigint        not null auto_increment,
  scope        varchar(16)   not null comment 'PROGRAM|SCHOLARSHIP|WF_STAGE',
  ref_id       bigint        not null comment 'polymorphic on scope — the program/scholarship/wf_stage id',
  doc_type     varchar(40)   not null comment 'sys_dict nad_document_type',
  mandatory    tinyint(1)    not null default 1,
  waivable     tinyint(1)    not null default 0,
  notes        varchar(255)      null,
  create_by    varchar(64)   not null default '',
  create_time  datetime          null,
  update_by    varchar(64)   not null default '',
  update_time  datetime          null,
  primary key (id),
  unique key uk_document_requirement (scope, ref_id, doc_type),
  key idx_document_requirement_scope (scope, ref_id)
) engine=innodb default charset=utf8mb4;
