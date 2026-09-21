-- nad_document_event: append-only audit trail for a document (CLAUDE.md §9/§11 —
-- never silently overwrite verification state without a history row). Internal
-- review commentary lives here (detail_json) or on a nad_application_note with
-- visibility=INTERNAL — never surfaced to the student, unlike nad_document.
-- rejection_reason which is SHARED.
--
-- manual rollback: DROP TABLE nad_document_event;

create table nad_document_event (
  id             bigint        not null auto_increment,
  document_id    bigint        not null,
  event_type     varchar(32)   not null comment 'CREATED|VERSION_UPLOADED|SUBMITTED|VERIFIED|REJECTED|DOWNLOADED|DELETED',
  actor_user_id  bigint        not null comment 'sys_user.user_id',
  at             datetime      not null,
  detail_json    text              null,
  primary key (id),
  key idx_document_event_document (document_id, at),
  constraint fk_document_event_document foreign key (document_id)
    references nad_document (id) on delete restrict
) engine=innodb default charset=utf8mb4;
