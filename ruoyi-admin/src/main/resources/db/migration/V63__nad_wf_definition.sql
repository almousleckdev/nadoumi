-- Application engine, definition layer (config, versioned). Docs: APPLICATION_WORKFLOW.md §3.1,
-- docs/superpowers/specs/2026-09-03-media-storage-and-application-engine-design.md Part II §II.2.
-- Renumbered from the original spec's V29 (see 2026-09-20 addendum §3).
-- Rollback (manual): drop table nad_wf_stage_task_template, nad_wf_transition, nad_wf_stage, nad_wf_definition;

create table nad_wf_definition (
  id            bigint       not null auto_increment,
  code          varchar(64)  not null,
  name          varchar(120) not null,
  version       int          not null,
  status        varchar(16)  not null default 'DRAFT', -- DRAFT | ACTIVE | RETIRED
  create_by     varchar(64)  default '',
  create_time   datetime     default null,
  update_by     varchar(64)  default '',
  update_time   datetime     default null,
  remark        varchar(500) default null,
  primary key (id),
  unique key uk_wf_definition_code_version (code, version),
  key idx_wf_definition_status (status)
) engine=InnoDB default charset=utf8mb4 comment='Workflow definition (versioned config, D4)';

create table nad_wf_stage (
  id            bigint       not null auto_increment,
  definition_id bigint       not null,
  code          varchar(64)  not null,
  name          varchar(120) not null,
  order_no      int          not null,
  stage_type    varchar(16)  not null, -- START | NORMAL | DECISION | TERMINAL
  status_label  varchar(40)  not null, -- written to nad_application.current_status on entry
  sla_hours     int          default null,
  primary key (id),
  unique key uk_wf_stage_definition_code (definition_id, code),
  key idx_wf_stage_definition (definition_id, order_no),
  constraint fk_wf_stage_definition foreign key (definition_id) references nad_wf_definition (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 comment='Workflow stage, owned by one definition';

create table nad_wf_transition (
  id            bigint       not null auto_increment,
  definition_id bigint       not null,
  code          varchar(64)  not null,
  from_stage_id bigint       default null,
  to_stage_id   bigint       not null,
  guard_json    varchar(2000) default null, -- {"all":["PRED", "PRED:ARG", ...]} — fixed predicate set only, no expression language
  role_required varchar(40)  default null,  -- staff role gate, e.g. 'case_officer'; null = any nad:application:transition holder
  auto          tinyint(1)   not null default 0,
  primary key (id),
  -- a transition "family" (e.g. withdraw) legitimately exists as several rows
  -- sharing one code, one per eligible from_stage_id — the engine resolves by
  -- (definition, code, current stage), not by code alone.
  unique key uk_wf_transition_definition_code_from (definition_id, code, from_stage_id),
  key idx_wf_transition_from (from_stage_id),
  key idx_wf_transition_to (to_stage_id),
  constraint fk_wf_transition_definition foreign key (definition_id) references nad_wf_definition (id) on delete cascade,
  constraint fk_wf_transition_from foreign key (from_stage_id) references nad_wf_stage (id) on delete restrict,
  constraint fk_wf_transition_to   foreign key (to_stage_id)   references nad_wf_stage (id) on delete restrict
) engine=InnoDB default charset=utf8mb4 comment='Workflow transition between two stages of the same definition';

create table nad_wf_stage_task_template (
  id             bigint       not null auto_increment,
  stage_id       bigint       not null,
  title          varchar(200) not null,
  role_required  varchar(40)  default null, -- advisory default assignee role; not an RBAC gate
  mandatory      tinyint(1)   not null default 1,
  blocks_exit    tinyint(1)   not null default 1,
  order_no       int          not null,
  primary key (id),
  key idx_wf_task_template_stage (stage_id, order_no),
  constraint fk_wf_task_template_stage foreign key (stage_id) references nad_wf_stage (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 comment='Task template materialised into nad_application_task on stage entry';
