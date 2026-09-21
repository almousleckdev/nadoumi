-- Communication domain, slice 2: conversation participants.
-- manual rollback: DROP TABLE nad_conversation_participant;

create table nad_conversation_participant (
  id                    bigint       not null auto_increment,
  conversation_id       bigint       not null,
  user_id               bigint       not null comment 'sys_user.user_id',
  role                  varchar(12)  not null comment 'STAFF | APPLICANT | AGENT | GUARDIAN',
  added_at              datetime     not null,
  removed_at            datetime         null,
  last_read_message_id  bigint           null,
  muted                 tinyint(1)   not null default 0,
  primary key (id),
  unique key uk_conversation_participant (conversation_id, user_id),
  key idx_participant_user (user_id, removed_at),
  constraint fk_participant_conversation foreign key (conversation_id)
    references nad_conversation (id) on delete cascade,
  constraint fk_participant_user foreign key (user_id)
    references sys_user (user_id) on delete cascade
) engine=innodb default charset=utf8mb4;
