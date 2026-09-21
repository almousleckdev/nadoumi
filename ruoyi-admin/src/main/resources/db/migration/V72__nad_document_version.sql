-- nad_document_version: immutable. Each upload/replace creates a new row and
-- repoints nad_document.current_version_id — prior versions are never mutated
-- or deleted (DOCUMENT_MANAGEMENT.md §3.1). Bytes live behind nad_media_asset
-- (media_asset_id), not a storage_key — this supersedes the originally-drafted
-- shape per docs/DATABASE_DESIGN.md:183 and the design addendum's §2 (the raw
-- docs/ddl/nad_core.draft.sql draft still has the pre-P1 storage_key shape and
-- is stale; this migration follows the reconciled DATABASE_DESIGN.md instead).
-- content_type/size_bytes/checksum_sha256 are a denormalized, immutable-at-upload
-- copy so "list my documents" needs no join; nad_media_asset stays the single
-- source of truth for the actual bytes/access-control path.
--
-- manual rollback:
--   alter table nad_document drop foreign key fk_document_current_version;
--   DROP TABLE nad_document_version;

create table nad_document_version (
  id                  bigint        not null auto_increment,
  document_id         bigint        not null,
  version_no          int           not null,
  media_asset_id      bigint        not null comment 'fk nad_media_asset; the actual bytes/access-control authority',
  content_type        varchar(128)  not null comment 'denormalized copy, immutable at upload',
  size_bytes          bigint        not null,
  checksum_sha256     char(64)          null,
  uploaded_by         bigint        not null comment 'sys_user.user_id',
  uploaded_at         datetime      not null,
  verification_status varchar(16)   not null default 'PENDING' comment 'PENDING|VERIFIED|REJECTED',
  verified_by         bigint            null comment 'sys_user.user_id',
  verified_at         datetime          null,
  scan_status         varchar(16)   not null default 'PENDING' comment 'PENDING|CLEAN|INFECTED — malware scanning not yet built, column reserved',
  primary key (id),
  unique key uk_document_version (document_id, version_no),
  key idx_document_version_media (media_asset_id),
  constraint fk_document_version_document foreign key (document_id)
    references nad_document (id) on delete restrict,
  constraint fk_document_version_media foreign key (media_asset_id)
    references nad_media_asset (id) on delete restrict
) engine=innodb default charset=utf8mb4;

alter table nad_document
  add constraint fk_document_current_version foreign key (current_version_id)
    references nad_document_version (id) on delete set null;
