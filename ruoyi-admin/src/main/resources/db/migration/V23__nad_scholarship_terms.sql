-- Scholarship terms depth (user request 2026-09-03, from CSC application practice):
--   * structured coverage list (what the award actually pays for) alongside the prose
--   * explicit study duration, distinct from a programme's duration
--   * application channel + CSC / university agency number
--   * a dedicated renewal / annual-review field (was buried in policy text)
--   * a financial-proof / guarantor requirement flag (relevant for PARTIAL / SELF)
--   * a foundation / preparatory-year requirement flag
--
-- All student-safe. Rollback:
--   drop table nad_scholarship_coverage;
--   alter table nad_scholarship
--     drop column study_duration_months, drop column application_channel,
--     drop column agency_number, drop column renewal_conditions,
--     drop column requires_financial_proof, drop column requires_foundation_year;
--   (then restore the previous v_scholarship_student from V22).

alter table nad_scholarship
  add column study_duration_months    int           null comment 'total study length; distinct from a programme duration' after non_degree_duration,
  add column application_channel      varchar(16)   null comment 'DIRECT_UNIVERSITY | CSC_AGENCY | NADOUMI | OTHER' after study_duration_months,
  add column agency_number            varchar(64)   null comment 'CSC agency / university application code' after application_channel,
  add column renewal_conditions       varchar(2000) null comment 'student-safe prose: annual review / renewal terms' after policy,
  add column requires_financial_proof tinyint(1)    not null default 0 after renewal_conditions,
  add column requires_foundation_year tinyint(1)    not null default 0 after requires_financial_proof;

-- what the award covers (edited whole; one row per kind)
create table nad_scholarship_coverage (
  id             bigint       not null auto_increment,
  scholarship_id bigint       not null,
  kind           varchar(24)  not null comment 'TUITION | ACCOMMODATION | STIPEND | MEDICAL_INSURANCE | SETTLEMENT_ALLOWANCE | TRAVEL | REGISTRATION_FEE | VISA_FEE | OTHER',
  detail         varchar(300)     null,
  sort_order     int          not null default 0,
  primary key (id),
  unique key uk_scholarship_coverage (scholarship_id, kind),
  constraint fk_coverage_scholarship foreign key (scholarship_id)
    references nad_scholarship (id) on delete cascade
) engine=innodb default charset=utf8mb4;

-- the student view carries the new scalar terms
drop view if exists v_scholarship_student;
create view v_scholarship_student as
select
  s.id, s.slug, s.title, s.summary, s.country, s.province, s.city, s.field,
  s.teaching_language, s.funding_model, s.has_stipend, s.deadline,
  s.benefits, s.requirements, s.policy, s.renewal_conditions,
  s.non_degree_duration, s.study_duration_months,
  s.application_channel, s.agency_number,
  s.requires_financial_proof, s.requires_foundation_year,
  s.application_fee_amount, s.application_fee_currency,
  s.service_fee_amount, s.service_fee_currency, s.slots,
  s.hero_image_url, s.cover_image_url,
  s.is_featured, s.is_recommended, s.is_hot, s.published_at
from nad_scholarship s
where s.status = 'ACTIVE' and s.publish_status = 'PUBLISHED';
