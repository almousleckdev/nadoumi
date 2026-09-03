-- Media asset registry + access audit log for the Cloudinary-backed media layer
-- (docs/superpowers/specs/2026-09-03-media-storage-and-application-engine-design.md
-- sections I.3 / I.4). nad_media_asset is a metadata + provider-reference registry
-- owned by the nadoumi-media module; business tables hold a *_media_id FK and never
-- carry Cloudinary fields. nad_media_access_log is append-only: one row per
-- PROTECTED / SENSITIVE access attempt (GRANTED / DENIED), never mutated.
--
-- The owner (owner_kind, owner_id) is polymorphic, so there is deliberately no FK
-- from nad_media_asset to the owning tables; the *_media_id FKs added in V27 / V28
-- enforce referential integrity from the owning side.
--
-- manual rollback: DROP TABLE nad_media_access_log; DROP TABLE nad_media_asset;

create table nad_media_asset (
  id                 bigint        not null auto_increment,
  provider           varchar(24)   not null default 'CLOUDINARY' comment 'future-proofing only',
  access_class       varchar(16)   not null comment 'PUBLIC | PROTECTED | SENSITIVE',
  category           varchar(40)   not null comment 'MediaCategory',
  resource_type      varchar(12)   not null comment 'Cloudinary image | raw | video',
  delivery_type      varchar(16)   not null comment 'upload (PUBLIC) | authenticated (PROTECTED/SENSITIVE)',
  public_id          varchar(255)  not null comment 'Cloudinary public_id (includes folder)',
  asset_id           varchar(64)       null comment 'Cloudinary asset_id',
  cloud_version      bigint            null comment 'Cloudinary version',
  secure_url         varchar(1024)     null comment 'populated only when access_class = PUBLIC',
  folder             varchar(255)      null,
  original_filename  varchar(255)  not null comment 'sanitised',
  content_type       varchar(128)  not null comment 'sniffed value, not the client claim',
  byte_size          bigint        not null,
  width              int               null comment 'images',
  height             int               null comment 'images',
  checksum_sha256    char(64)          null comment 'set for documents; optional for images',
  uploaded_by        bigint        not null comment 'sys_user.user_id',
  owner_kind         varchar(16)   not null comment 'MediaOwnerKind',
  owner_id           bigint        not null,
  status             varchar(16)   not null default 'ACTIVE' comment 'ACTIVE | SUPERSEDED | DELETED',
  superseded_by      bigint            null comment 'set on replace',
  create_by          varchar(64)   not null default '',
  create_time        datetime          null,
  update_by          varchar(64)   not null default '',
  update_time        datetime          null,
  deleted_at         datetime          null,
  deleted_by         bigint            null,
  primary key (id),
  unique key uk_media_provider_public_id (provider, public_id),
  key idx_media_owner (owner_kind, owner_id),
  key idx_media_category (category),
  key idx_media_status (status),
  constraint fk_media_superseded_by foreign key (superseded_by)
    references nad_media_asset (id) on delete set null
) engine=innodb default charset=utf8mb4;

create table nad_media_access_log (
  id                  bigint       not null auto_increment,
  media_asset_id      bigint       not null,
  document_id         bigint           null comment 'set once the document slice exists',
  application_id      bigint           null comment 'context, when the access is application-scoped',
  actor_user_id       bigint       not null comment 'authenticated principal',
  actor_applicant_id  bigint           null comment 'which applicant identity the actor used',
  access_kind         varchar(20)  not null comment 'SIGNED_URL_ISSUED | STREAM_PROXY | METADATA',
  result              varchar(8)   not null comment 'GRANTED | DENIED',
  deny_reason         varchar(120)     null comment 'e.g. NO_APPLICANT_GRANT, ROLE_MISSING, APP_NOT_VISIBLE',
  ttl_seconds         int              null comment 'for SIGNED_URL_ISSUED',
  ip                  varchar(45)      null,
  user_agent          varchar(255)     null comment 'truncated',
  created_at          datetime     not null,
  primary key (id),
  key idx_mal_asset (media_asset_id, created_at),
  key idx_mal_app (application_id, created_at),
  key idx_mal_actor (actor_user_id, created_at),
  constraint fk_mal_asset foreign key (media_asset_id)
    references nad_media_asset (id) on delete restrict
) engine=innodb default charset=utf8mb4;
