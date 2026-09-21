-- Communication domain, slice 1: conversations (docs/COMMUNICATION_AND_NOTIFICATIONS.md
-- §3, docs/superpowers/specs/2026-09-20-messaging-domain-design.md §4).
-- application_id is a soft reference -- nad_application does not exist yet (Step 6);
-- no FK, same treatment nad_notification.application_id already has.
--
-- manual rollback: DROP TABLE nad_conversation;

create table nad_conversation (
  id                 bigint       not null auto_increment,
  subject            varchar(200)     null,
  application_id     bigint           null comment 'soft ref -- nad_application does not exist yet',
  conversation_type  varchar(20)  not null default 'GENERAL' comment 'GENERAL | SUPPORT',
  status             varchar(10)  not null default 'OPEN' comment 'OPEN | CLOSED',
  create_by          varchar(64)  not null default '',
  create_time        datetime         null,
  update_by          varchar(64)  not null default '',
  update_time        datetime         null,
  primary key (id),
  key idx_conversation_application (application_id),
  key idx_conversation_type_status (conversation_type, status)
) engine=innodb default charset=utf8mb4;
