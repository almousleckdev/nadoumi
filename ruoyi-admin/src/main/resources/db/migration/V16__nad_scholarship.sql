-- Scholarship: a configurable, independent opportunity
-- (docs/PUBLIC_WEBSITE_REDESIGN.md §5, docs/SECURITY.md "Scholarship confidentiality").
--
-- CONFIDENTIALITY: nad_scholarship + its child tables are STUDENT-SAFE. The
-- university / partnership / commission linkage lives ONLY in
-- nad_scholarship_internal and is never joined by a public or student query.
-- Public and student MyBatis mappers read ONLY the v_scholarship_student view
-- and the student-safe child tables.
--
-- DDL only. Reference data (categories) + admin menu + permissions are seeded in
-- V17__nad_scholarship_seed.sql.

create table nad_scholarship (
  id                       bigint        not null auto_increment,
  slug                     varchar(160)  not null,
  title                    varchar(200)  not null,
  summary                  varchar(2000)     null,
  country                  varchar(2)    not null comment 'ISO 3166-1 alpha-2',
  province                 varchar(120)      null,
  city                     varchar(120)      null,
  field                    varchar(120)      null comment 'discipline / field of study',
  teaching_language        varchar(16)       null comment 'ENGLISH | CHINESE | BOTH',
  funding_model            varchar(16)   not null comment 'FULLY | PARTIAL | SELF',
  has_stipend              tinyint(1)    not null default 0,
  deadline                 date              null comment 'null = rolling',
  benefits                 text              null comment 'student-safe prose',
  requirements             text              null comment 'student-safe prose',
  policy                   text              null comment 'student-safe prose',
  application_fee_amount    decimal(14, 2)    null,
  application_fee_currency  char(3)           null,
  service_fee_amount        decimal(14, 2)    null,
  service_fee_currency      char(3)           null,
  slots                    int               null comment 'available places, null = unspecified',
  is_featured              tinyint(1)    not null default 0,
  is_recommended           tinyint(1)    not null default 0,
  is_hot                   tinyint(1)    not null default 0,
  publish_status           varchar(16)   not null default 'DRAFT' comment 'DRAFT | PUBLISHED',
  published_at             datetime          null comment 'UTC; set when first PUBLISHED',
  status                   varchar(16)   not null default 'ACTIVE' comment 'ACTIVE | INACTIVE',
  create_by                varchar(64)   not null default '',
  create_time              datetime          null,
  update_by                varchar(64)   not null default '',
  update_time              datetime          null,
  remark                   varchar(500)      null,
  primary key (id),
  unique key uk_scholarship_slug (slug),
  key idx_scholarship_publish (publish_status, status),
  key idx_scholarship_discovery (publish_status, status, is_featured, is_hot, published_at),
  key idx_scholarship_country (country),
  key idx_scholarship_funding (funding_model),
  key idx_scholarship_deadline (deadline)
) engine=innodb default charset=utf8mb4;

-- accepted education levels (a scholarship can accept several)
create table nad_scholarship_level (
  id             bigint      not null auto_increment,
  scholarship_id bigint      not null,
  level          varchar(16) not null comment 'NON_DEGREE | DIPLOMA | BACHELOR | MASTER | PHD',
  primary key (id),
  unique key uk_scholarship_level (scholarship_id, level),
  constraint fk_sch_level_scholarship foreign key (scholarship_id)
    references nad_scholarship (id) on delete cascade
) engine=innodb default charset=utf8mb4;

-- extensible category reference (seeded in V17)
create table nad_scholarship_category (
  id        bigint       not null auto_increment,
  code      varchar(32)  not null,
  name      varchar(120) not null,
  sort_order int         not null default 0,
  primary key (id),
  unique key uk_scholarship_category_code (code)
) engine=innodb default charset=utf8mb4;

create table nad_scholarship_category_link (
  scholarship_id bigint not null,
  category_id    bigint not null,
  primary key (scholarship_id, category_id),
  constraint fk_sch_catlink_scholarship foreign key (scholarship_id)
    references nad_scholarship (id) on delete cascade,
  constraint fk_sch_catlink_category foreign key (category_id)
    references nad_scholarship_category (id) on delete restrict
) engine=innodb default charset=utf8mb4;

create table nad_scholarship_intake (
  id               bigint      not null auto_increment,
  scholarship_id   bigint      not null,
  term             varchar(24) not null comment 'SPRING_MARCH | AUTUMN_SEPTEMBER | ...',
  application_open  date            null,
  application_close date            null,
  sort_order       int         not null default 0,
  primary key (id),
  key idx_scholarship_intake (scholarship_id, sort_order),
  constraint fk_sch_intake_scholarship foreign key (scholarship_id)
    references nad_scholarship (id) on delete cascade
) engine=innodb default charset=utf8mb4;

-- structured eligibility (1:1, student-safe)
create table nad_scholarship_eligibility (
  scholarship_id     bigint       not null,
  age_min            tinyint          null,
  age_max            tinyint          null,
  nationality_scope  varchar(16)  not null default 'ANY' comment 'ANY | INCLUDE | EXCLUDE',
  accepted_countries varchar(400)     null comment 'comma-separated ISO codes when scope != ANY',
  in_china           tinyint(1)       null comment 'null = no requirement',
  gpa_min            decimal(4, 2)    null,
  ielts_min          decimal(3, 1)    null,
  toefl_min          smallint         null,
  duolingo_min       smallint         null,
  hsk_min            tinyint          null,
  csca_min           smallint         null,
  notes              varchar(2000)    null,
  primary key (scholarship_id),
  constraint fk_sch_elig_scholarship foreign key (scholarship_id)
    references nad_scholarship (id) on delete cascade
) engine=innodb default charset=utf8mb4;

-- typed money line items (student-safe)
create table nad_scholarship_fee (
  id             bigint         not null auto_increment,
  scholarship_id bigint         not null,
  kind           varchar(24)    not null comment 'TUITION_BEFORE | TUITION_AFTER | ACCOMMODATION_BEFORE | ACCOMMODATION_AFTER | REGISTRATION | APPLICATION | NADOUMI_APPLICATION | NADOUMI_SERVICE | INSURANCE | VISA | OTHER',
  amount         decimal(14, 2) not null,
  currency       char(3)        not null comment 'ISO 4217, e.g. RMB is CNY',
  note           varchar(200)       null,
  sort_order     int            not null default 0,
  primary key (id),
  key idx_scholarship_fee (scholarship_id, sort_order),
  constraint fk_sch_fee_scholarship foreign key (scholarship_id)
    references nad_scholarship (id) on delete cascade
) engine=innodb default charset=utf8mb4;

-- stipend (nullable 1:1 — no row means no stipend)
create table nad_scholarship_stipend (
  scholarship_id  bigint         not null,
  amount          decimal(14, 2) not null,
  currency        char(3)        not null,
  frequency       varchar(16)    not null comment 'MONTHLY | YEARLY | ONE_OFF',
  duration_months smallint           null,
  conditions      varchar(1000)      null,
  primary key (scholarship_id),
  constraint fk_sch_stipend_scholarship foreign key (scholarship_id)
    references nad_scholarship (id) on delete cascade
) engine=innodb default charset=utf8mb4;

-- configurable document requirements — the frontend reads this, never hard-codes
create table nad_scholarship_document_requirement (
  id             bigint       not null auto_increment,
  scholarship_id bigint       not null,
  doc_type       varchar(48)  not null comment 'e.g. PASSPORT | DEGREE | TRANSCRIPT | LANGUAGE_CERT | VISA | PHYSICAL_EXAM | STUDY_PLAN | RECOMMENDATION_LETTER | OTHER',
  mandatory      tinyint(1)   not null default 1,
  note           varchar(400)     null,
  sort_order     int          not null default 0,
  primary key (id),
  unique key uk_scholarship_doc (scholarship_id, doc_type),
  key idx_scholarship_doc (scholarship_id, sort_order),
  constraint fk_sch_doc_scholarship foreign key (scholarship_id)
    references nad_scholarship (id) on delete cascade
) engine=innodb default charset=utf8mb4;

-- CONFIDENTIAL: never joined by a public / student query. Staff-only, behind
-- nad:scholarship:internal:* permissions.
create table nad_scholarship_internal (
  scholarship_id       bigint        not null comment 'PK = FK to nad_scholarship',
  university_id        bigint             null comment 'FK nad_university — the partner institution',
  partnership_id       bigint             null comment 'FK nad_partnership (added with the Partnership module)',
  internal_status      varchar(24)   not null default 'DRAFT',
  operational_notes    text               null,
  confidential_terms   text               null,
  commission_model_json json              null,
  create_by            varchar(64)   not null default '',
  create_time          datetime           null,
  update_by            varchar(64)   not null default '',
  update_time          datetime           null,
  primary key (scholarship_id),
  key idx_scholarship_internal_university (university_id),
  constraint fk_sch_internal_scholarship foreign key (scholarship_id)
    references nad_scholarship (id) on delete cascade,
  constraint fk_sch_internal_university foreign key (university_id)
    references nad_university (id) on delete restrict
) engine=innodb default charset=utf8mb4;

-- The ONLY object public / student mappers may read for the scholarship head row.
-- No confidential column is projected. A leak test asserts this.
create view v_scholarship_student as
select
  s.id, s.slug, s.title, s.summary, s.country, s.province, s.city, s.field,
  s.teaching_language, s.funding_model, s.has_stipend, s.deadline,
  s.benefits, s.requirements, s.policy,
  s.application_fee_amount, s.application_fee_currency,
  s.service_fee_amount, s.service_fee_currency, s.slots,
  s.is_featured, s.is_recommended, s.is_hot, s.published_at
from nad_scholarship s
where s.status = 'ACTIVE' and s.publish_status = 'PUBLISHED';
