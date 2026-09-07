-- Internal task management: create tasks, assign to staff, track progress with an
-- append-only event log. Lifecycle: PENDING -> IN_PROGRESS -> COMPLETED -> APPROVED
-- (approval is admin-only); CANCELLED from any non-terminal state. Every state
-- change writes a nad_task_event and emits a TaskProgressChanged domain event so
-- the creator, the assignee and admins are notified.
--
-- manual rollback: DROP TABLE nad_task_event; DROP TABLE nad_task;

create table nad_task (
  id                bigint        not null auto_increment,
  title             varchar(200)  not null,
  description       varchar(4000)     null,
  priority          varchar(10)   not null default 'MEDIUM' comment 'LOW | MEDIUM | HIGH',
  status            varchar(16)   not null default 'PENDING' comment 'PENDING | IN_PROGRESS | COMPLETED | APPROVED | CANCELLED',
  assignee_user_id  bigint            null comment 'sys_user.user_id of the assigned staff member',
  created_by_user_id bigint       not null comment 'sys_user.user_id of the creator',
  due_date          date              null,
  started_at        datetime          null,
  completed_at      datetime          null,
  approved_by_user_id bigint          null,
  approved_at       datetime          null,
  related_type      varchar(24)       null comment 'optional link, e.g. application / applicant / university',
  related_id        bigint            null,
  create_by         varchar(64)   not null default '',
  create_time       datetime          null,
  update_by         varchar(64)   not null default '',
  update_time       datetime          null,
  primary key (id),
  key idx_task_assignee (assignee_user_id, status),
  key idx_task_status (status),
  key idx_task_priority (priority),
  key idx_task_due (due_date),
  constraint fk_task_assignee foreign key (assignee_user_id) references sys_user (user_id) on delete set null,
  constraint fk_task_creator foreign key (created_by_user_id) references sys_user (user_id) on delete cascade
) engine=innodb default charset=utf8mb4;

create table nad_task_event (
  id            bigint       not null auto_increment,
  task_id       bigint       not null,
  event_type    varchar(20)  not null comment 'CREATED | STATUS_CHANGED | ASSIGNED | COMMENT | PRIORITY_CHANGED',
  from_status   varchar(16)      null,
  to_status     varchar(16)      null,
  actor_user_id bigint       not null,
  note          varchar(2000)    null,
  created_at    datetime     not null,
  primary key (id),
  key idx_task_event_task (task_id, created_at),
  constraint fk_task_event_task foreign key (task_id) references nad_task (id) on delete cascade
) engine=innodb default charset=utf8mb4;
