-- Chat rework: one private chat per staff/student pair, a denormalised last-message summary (so the inbox is
-- one query, not eight per conversation), delivery receipts, attachment metadata on the row, last-seen
-- presence, and an index for searching students by given name.
--
-- Existing conversations are kept and keep working: they stay GENERAL/SUPPORT with a null direct_key, and
-- the summary / pointer columns are backfilled below.
--
-- manual rollback:
--   drop table nad_user_presence;
--   alter table nad_applicant drop index idx_applicant_given_name;
--   alter table nad_message drop index idx_message_sender_time;
--   alter table nad_message_attachment drop column original_filename, drop column content_type, drop column byte_size;
--   alter table nad_conversation_participant drop column last_delivered_message_id;
--   alter table nad_conversation drop index uk_conversation_direct_key, drop index idx_conversation_last_message,
--     drop column direct_key, drop column last_message_id, drop column last_message_at,
--     drop column last_message_preview, drop column last_sender_user_id;

alter table nad_conversation
  add column direct_key            varchar(40)  null comment 'DIRECT only: "<staff user id>:<student user id>"; at most one chat per pair' after conversation_type,
  add column last_message_id       bigint       null comment 'newest non-deleted message',
  add column last_message_at       datetime     null,
  add column last_message_preview  varchar(160) null,
  add column last_sender_user_id   bigint       null,
  add unique key uk_conversation_direct_key (direct_key),
  add key idx_conversation_last_message (last_message_at);

alter table nad_conversation_participant
  add column last_delivered_message_id bigint null comment 'newest message that reached this participant''s device';

alter table nad_message_attachment
  add column original_filename varchar(255) null,
  add column content_type      varchar(128) null,
  add column byte_size         bigint       null;

alter table nad_message
  add key idx_message_sender_time (sender_user_id, created_at);

create index idx_applicant_given_name on nad_applicant (given_name);

create table nad_user_presence (
  user_id      bigint   not null,
  last_seen_at datetime not null comment 'UTC; written when a user''s last live connection closes',
  primary key (user_id),
  constraint fk_presence_user foreign key (user_id) references sys_user (user_id) on delete cascade
) engine=InnoDB default charset=utf8mb4 comment='Last time a user was connected to the chat stream';

-- backfill: last-message summary for existing conversations
update nad_conversation c
  join (select conversation_id, max(id) as message_id
        from nad_message where deleted_at is null group by conversation_id) newest on newest.conversation_id = c.id
  join nad_message m on m.id = newest.message_id
set c.last_message_id      = m.id,
    c.last_message_at      = m.created_at,
    c.last_message_preview = left(m.body, 160),
    c.last_sender_user_id  = m.sender_user_id;

-- backfill: what was read has necessarily been delivered
update nad_conversation_participant set last_delivered_message_id = last_read_message_id
  where last_read_message_id is not null;

-- backfill: attachment display metadata, so listing a thread never has to look up media rows
update nad_message_attachment a
  join nad_media_asset s on s.id = a.media_asset_id
set a.original_filename = s.original_filename,
    a.content_type      = s.content_type,
    a.byte_size         = s.byte_size;
