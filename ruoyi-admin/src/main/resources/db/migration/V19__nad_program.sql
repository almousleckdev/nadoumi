-- Programme: a course of study offered by exactly one university
-- (docs/PUBLIC_WEBSITE_REDESIGN.md §5a). A programme belongs to a university and
-- is exposed only through the university experience -- there is no top-level
-- Programmes product.
--
-- No commercial / partnership data lives here. Public reads are student-safe.
--
-- DDL only. The admin menu + permissions are seeded in V20__nad_program_seed.sql.
--
-- Rollback: drop table nad_program_intake, nad_program_major, nad_program.

create table nad_program (
  id                bigint        not null auto_increment,
  university_id     bigint        not null,
  name              varchar(200)  not null,
  name_cn           varchar(200)      null,
  program_type      varchar(16)   not null comment 'LANGUAGE | NON_DEGREE | DIPLOMA | BACHELOR | MASTER | PHD',
  field             varchar(120)      null comment 'discipline / field of study',
  teaching_language varchar(16)       null comment 'ENGLISH | CHINESE | BILINGUAL',
  duration_months   int               null,
  tuition_amount    decimal(12, 2)    null,
  tuition_currency  char(3)           null comment 'ISO 4217',
  summary           varchar(4000)     null comment 'student-safe prose',
  is_featured       tinyint(1)    not null default 0,
  is_hot            tinyint(1)    not null default 0,
  publish_status    varchar(16)   not null default 'DRAFT' comment 'DRAFT | PUBLISHED',
  status            varchar(16)   not null default 'ACTIVE' comment 'ACTIVE | INACTIVE',
  create_by         varchar(64)   not null default '',
  create_time       datetime          null,
  update_by         varchar(64)   not null default '',
  update_time       datetime          null,
  remark            varchar(500)      null,
  primary key (id),
  unique key uk_program_university_name (university_id, name),
  key idx_program_university (university_id),
  key idx_program_publish (publish_status, status),
  key idx_program_type (program_type),
  key idx_program_discovery (is_featured, is_hot),
  constraint fk_program_university foreign key (university_id)
    references nad_university (id) on delete restrict
) engine=innodb default charset=utf8mb4;

-- majors / specialisations under a programme (edited whole)
create table nad_program_major (
  id         bigint       not null auto_increment,
  program_id bigint       not null,
  name       varchar(200) not null,
  name_cn    varchar(200)     null,
  sort_order int          not null default 0,
  primary key (id),
  key idx_program_major (program_id, sort_order),
  constraint fk_program_major_program foreign key (program_id)
    references nad_program (id) on delete cascade
) engine=innodb default charset=utf8mb4;

-- intake terms for a programme (edited whole)
create table nad_program_intake (
  id               bigint      not null auto_increment,
  program_id       bigint      not null,
  term             varchar(32) not null comment 'SPRING_MARCH | AUTUMN_SEPTEMBER | ...',
  application_open  date            null,
  application_close date            null,
  sort_order       int         not null default 0,
  primary key (id),
  key idx_program_intake (program_id, sort_order),
  constraint fk_program_intake_program foreign key (program_id)
    references nad_program (id) on delete cascade
) engine=innodb default charset=utf8mb4;
