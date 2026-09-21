-- Communication domain, slice 3: messages. Human-authored only -- workflow/timeline
-- events stay in nad_application_event; the UI merges them chronologically with
-- these rows (docs/COMMUNICATION_AND_NOTIFICATIONS.md §3.1, fix G-C2).
-- Soft-delete (deleted_at): history is kept, never hard-deleted.
-- manual rollback: DROP TABLE nad_message;

create table nad_message (
  id               bigint        not null auto_increment,
  conversation_id  bigint        not null,
  sender_user_id   bigint        not null comment 'sys_user.user_id',
  body             varchar(4000) not null,
  created_at       datetime      not null,
  edited_at        datetime          null,
  deleted_at       datetime          null,
  primary key (id),
  key idx_message_conversation (conversation_id, id) comment 'id is the pagination/unread cursor',
  constraint fk_message_conversation foreign key (conversation_id)
    references nad_conversation (id) on delete cascade,
  constraint fk_message_sender foreign key (sender_user_id)
    references sys_user (user_id) on delete restrict
) engine=innodb default charset=utf8mb4;
