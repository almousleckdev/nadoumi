-- Application engine, instance layer. CLAUDE.md §9-10: applicant + opportunity +
-- stage + status + assignment + tasks + notes + events + decisions + history,
-- auditable, history never overwritten. Renumbered from the original spec's V30.
-- Circular FK (nad_application <-> nad_wf_instance) resolved with a deferred
-- ALTER at the end of this file.
-- Rollback (manual): drop the tables below in reverse order, then
--   alter table nad_application drop foreign key fk_app_wf_instance;

create table nad_application (
  id                    bigint       not null auto_increment,
  applicant_id          bigint       not null,
  application_type      varchar(32)  not null, -- PROGRAM_WITH_SCHOLARSHIP | PROGRAM_ONLY (DA6; SCHOLARSHIP_ONLY deferred)
  program_id            bigint       not null,
  scholarship_id        bigint           null,
  intake_id             bigint           null,
  workflow_instance_id  bigint           null, -- FK added below, after nad_wf_instance exists
  current_stage_id      bigint           null,
  current_status        varchar(40)  not null default 'DRAFT', -- denormalised; engine is the only writer (INV9)
  assignee_user_id      bigint           null,
  submitted_at          datetime         null,
  version               int          not null default 0, -- optimistic lock
  create_by             varchar(64)  not null default '',
  create_time           datetime         null,
  update_by             varchar(64)  not null default '',
  update_time           datetime         null,
  remark                varchar(500)     null,
  primary key (id),
  key idx_app_applicant  (applicant_id),
  key idx_app_program    (program_id),
  key idx_app_scholarship(scholarship_id),
  key idx_app_intake     (intake_id),
  key idx_app_stage      (current_stage_id),
  key idx_app_assignee   (assignee_user_id, current_status),
  key idx_app_status     (current_status),
  constraint fk_app_applicant   foreign key (applicant_id)   references nad_applicant (id)       on delete restrict,
  constraint fk_app_program     foreign key (program_id)     references nad_program (id)          on delete restrict,
  constraint fk_app_scholarship foreign key (scholarship_id) references nad_scholarship (id)       on delete restrict,
  constraint fk_app_intake      foreign key (intake_id)      references nad_program_intake (id)    on delete restrict,
  constraint fk_app_stage       foreign key (current_stage_id) references nad_wf_stage (id)        on delete restrict
) engine=InnoDB default charset=utf8mb4 comment='Application: one business case (an applicant pursuing one opportunity)';

create table nad_wf_instance (
  id                  bigint      not null auto_increment,
  definition_id       bigint      not null,
  definition_version  int         not null, -- pinned at creation; a later ACTIVE version never migrates a running instance
  application_id      bigint      not null,
  current_stage_id    bigint          null,
  status              varchar(16) not null default 'RUNNING', -- RUNNING | CLOSED
  started_at          datetime    not null,
  closed_at           datetime        null,
  primary key (id),
  unique key uk_wf_instance_application (application_id),
  key idx_wf_instance_definition (definition_id),
  constraint fk_wf_instance_definition foreign key (definition_id)  references nad_wf_definition (id) on delete restrict,
  constraint fk_wf_instance_application foreign key (application_id) references nad_application (id)  on delete cascade,
  constraint fk_wf_instance_stage foreign key (current_stage_id) references nad_wf_stage (id) on delete restrict
) engine=InnoDB default charset=utf8mb4 comment='One running (or closed) instance of a workflow definition, 1:1 with an application';

create table nad_application_stage_history (
  id               bigint      not null auto_increment,
  application_id   bigint      not null,
  from_stage_id    bigint          null,
  to_stage_id      bigint      not null,
  transition_code  varchar(64) not null,
  changed_by       bigint      not null,
  changed_at       datetime    not null,
  reason           varchar(500)    null,
  primary key (id),
  key idx_app_stage_history_app (application_id, changed_at),
  constraint fk_ash_application foreign key (application_id) references nad_application (id) on delete cascade,
  constraint fk_ash_from_stage  foreign key (from_stage_id)  references nad_wf_stage (id)     on delete restrict,
  constraint fk_ash_to_stage    foreign key (to_stage_id)    references nad_wf_stage (id)     on delete restrict
) engine=InnoDB default charset=utf8mb4 comment='Append-only stage transition history — never updated or deleted';

create table nad_application_event (
  id              bigint       not null auto_increment,
  application_id  bigint       not null,
  event_type      varchar(64)  not null,
  actor_user_id   bigint           null,
  at              datetime     not null,
  detail_json     varchar(2000)    null,
  primary key (id),
  key idx_app_event_app (application_id, at),
  constraint fk_ae_application foreign key (application_id) references nad_application (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 comment='Append-only application timeline — never updated or deleted';

create table nad_application_decision (
  id                     bigint       not null auto_increment,
  application_id         bigint       not null,
  decision_type          varchar(64)  not null, -- e.g. NADOUMI_INTERNAL, UNIVERSITY_OFFER, SCHOLARSHIP_AWARD, APPLICANT_RESPONSE
  outcome                varchar(40)  not null,
  rationale              varchar(1000) not null,
  drives_transition_code varchar(64)      null,
  decided_by             bigint       not null,
  decided_at             datetime     not null,
  primary key (id),
  key idx_app_decision_app (application_id, decided_at),
  key idx_app_decision_type (application_id, decision_type),
  constraint fk_ad_application foreign key (application_id) references nad_application (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 comment='Append-only recorded decisions — drives DECISION-stage guards; never updated or deleted';

create table nad_application_task (
  id                        bigint       not null auto_increment,
  application_id            bigint       not null,
  wf_stage_task_template_id bigint           null, -- null = ad-hoc staff-added task (D12, same table)
  title                     varchar(200) not null,
  role_required             varchar(40)      null,
  mandatory                 tinyint(1)   not null default 1,
  blocks_exit               tinyint(1)   not null default 1,
  status                    varchar(16)  not null default 'OPEN', -- OPEN | DONE | SKIPPED | CANCELLED
  assignee_user_id          bigint           null,
  due_at                    datetime         null,
  skip_reason               varchar(500)     null,
  create_by                 varchar(64)  not null default '',
  create_time                datetime        null,
  update_by                 varchar(64)  not null default '',
  update_time                datetime        null,
  primary key (id),
  key idx_app_task_app (application_id, status),
  key idx_app_task_assignee (assignee_user_id, status),
  constraint fk_at_application foreign key (application_id) references nad_application (id) on delete cascade,
  constraint fk_at_template foreign key (wf_stage_task_template_id) references nad_wf_stage_task_template (id) on delete set null
) engine=InnoDB default charset=utf8mb4 comment='Materialised task, engine-generated (template) or ad-hoc (D12, single table)';

create table nad_application_snapshot (
  id              bigint       not null auto_increment,
  application_id  bigint       not null,
  kind            varchar(20)  not null, -- PROFILE | REQUIREMENTS
  payload_json    longtext     not null,
  created_at      datetime     not null,
  primary key (id),
  key idx_app_snapshot_app (application_id, kind),
  constraint fk_asn_application foreign key (application_id) references nad_application (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 comment='Immutable snapshot taken on submit — never updated or deleted (DA4)';

alter table nad_application
  add constraint fk_app_wf_instance foreign key (workflow_instance_id) references nad_wf_instance (id) on delete set null;
