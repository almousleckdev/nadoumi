-- Support/Ticketing domain, slice 1: the ticket work item + its audit log
-- (docs/superpowers/specs/2026-09-20-support-ticketing-domain-design.md §4).
-- A ticket is a thin wrapper (status/priority/category/assignment) that owns a 1:1
-- SUPPORT conversation in nad_conversation; all message machinery lives there.
--
-- manual rollback: DROP TABLE nad_support_ticket_event; DROP TABLE nad_support_ticket;

create table nad_support_ticket (
  id                 bigint       not null auto_increment,
  conversation_id    bigint       not null,
  applicant_id       bigint           null comment 'optional: a student may need help before having an applicant profile',
  opened_by_user_id  bigint       not null,
  subject            varchar(200) not null,
  category           varchar(40)  not null comment 'ACCOUNT | APPLICATION | DOCUMENT | PAYMENT | TECHNICAL | OTHER',
  priority           varchar(10)  not null default 'NORMAL' comment 'LOW | NORMAL | HIGH | URGENT',
  status             varchar(20)  not null default 'OPEN' comment 'OPEN | IN_PROGRESS | WAITING_ON_STUDENT | RESOLVED | CLOSED',
  assigned_staff_id  bigint           null,
  resolved_at        datetime         null,
  closed_at          datetime         null,
  create_by          varchar(64)  not null default '',
  create_time        datetime         null,
  update_by          varchar(64)  not null default '',
  update_time        datetime         null,
  primary key (id),
  unique key uq_ticket_conversation (conversation_id),
  key idx_ticket_status (status),
  key idx_ticket_assignee (assigned_staff_id),
  key idx_ticket_opened_by (opened_by_user_id),
  key idx_ticket_applicant (applicant_id),
  constraint fk_ticket_conversation foreign key (conversation_id) references nad_conversation (id) on delete restrict,
  constraint fk_ticket_applicant    foreign key (applicant_id)    references nad_applicant (id)    on delete set null,
  constraint fk_ticket_opened_by    foreign key (opened_by_user_id) references sys_user (user_id)  on delete restrict,
  constraint fk_ticket_assignee     foreign key (assigned_staff_id) references sys_user (user_id)  on delete set null
) engine=innodb default charset=utf8mb4;

-- Append-only audit log of work-item state changes (never chat messages).
create table nad_support_ticket_event (
  id              bigint       not null auto_increment,
  ticket_id       bigint       not null,
  event_type      varchar(30)  not null comment 'OPENED | STATUS_CHANGED | ASSIGNED | REASSIGNED | PRIORITY_CHANGED | CATEGORY_CHANGED',
  old_value       varchar(100)     null,
  new_value       varchar(100)     null,
  actor_user_id   bigint       not null,
  created_at      datetime     not null,
  primary key (id),
  key idx_ticket_event_ticket (ticket_id),
  constraint fk_ticket_event_ticket foreign key (ticket_id)     references nad_support_ticket (id) on delete cascade,
  constraint fk_ticket_event_actor  foreign key (actor_user_id) references sys_user (user_id)      on delete restrict
) engine=innodb default charset=utf8mb4;
