-- Communication domain, slice 4: message attachments. A plain nad_media_asset
-- (category MESSAGE_ATTACHMENT), never auto-promoted to a governed nad_document --
-- promoted_document_id is set only by an explicit staff "promote to document"
-- action (docs/DOCUMENT_MANAGEMENT.md §3.5, a Step 7 follow-up). Soft ref, no FK:
-- nad_document does not exist yet.
-- manual rollback: DROP TABLE nad_message_attachment;

create table nad_message_attachment (
  id                    bigint    not null auto_increment,
  message_id            bigint    not null,
  media_asset_id        bigint    not null,
  promoted_document_id  bigint        null comment 'soft ref -- nad_document does not exist yet',
  create_time           datetime  not null,
  primary key (id),
  key idx_attachment_message (message_id),
  constraint fk_attachment_message foreign key (message_id)
    references nad_message (id) on delete cascade,
  constraint fk_attachment_media foreign key (media_asset_id)
    references nad_media_asset (id) on delete restrict
) engine=innodb default charset=utf8mb4;
