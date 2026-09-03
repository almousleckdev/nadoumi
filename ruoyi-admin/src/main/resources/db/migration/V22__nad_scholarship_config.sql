-- Scholarship configuration depth (docs/PLATFORM_ARCHITECTURE.md C4, user request 2026-09-03):
--   * stipend is per accepted education level (a PhD stipend differs from a Master's)
--   * accommodation is offered as room types, each with its own price + amenities
--   * a NON_DEGREE programme runs for a half year or a full year
--   * fees are shown in both RMB and USD -- computed in the DTO layer from the
--     single stored (amount, currency), so no fee column changes here.
--
-- Rollback:
--   drop table nad_scholarship_accommodation;
--   drop table nad_scholarship_level_stipend;
--   create table nad_scholarship_stipend (... V16 definition ...);
--   alter table nad_scholarship drop column non_degree_duration;
--   (then restore the previous v_scholarship_student from V21).

alter table nad_scholarship
  add column non_degree_duration varchar(16) null comment 'HALF_YEAR | ONE_YEAR' after policy;

-- one stipend row per accepted level
create table nad_scholarship_level_stipend (
  id              bigint        not null auto_increment,
  scholarship_id  bigint        not null,
  level           varchar(16)   not null comment 'NON_DEGREE | DIPLOMA | BACHELOR | MASTER | PHD',
  amount          decimal(14, 2) not null,
  currency        char(3)       not null default 'CNY',
  frequency       varchar(16)   not null comment 'MONTHLY | YEARLY | ONE_OFF',
  duration_months int               null,
  conditions      varchar(1000)     null,
  primary key (id),
  unique key uk_level_stipend (scholarship_id, level),
  constraint fk_level_stipend_scholarship foreign key (scholarship_id)
    references nad_scholarship (id) on delete cascade
) engine=innodb default charset=utf8mb4;

-- accommodation options: room types with their own price + amenity notes
create table nad_scholarship_accommodation (
  id              bigint        not null auto_increment,
  scholarship_id  bigint        not null,
  room_type       varchar(16)   not null comment 'SINGLE | DOUBLE | TRIPLE | QUAD | SHARED',
  amount          decimal(14, 2)    null,
  currency        char(3)       not null default 'CNY',
  note            varchar(200)      null comment 'e.g. AC, private bathroom, WiFi',
  sort_order      int           not null default 0,
  primary key (id),
  key idx_accommodation (scholarship_id, sort_order),
  constraint fk_accommodation_scholarship foreign key (scholarship_id)
    references nad_scholarship (id) on delete cascade
) engine=innodb default charset=utf8mb4;

-- the single-row stipend table is replaced by nad_scholarship_level_stipend
drop table nad_scholarship_stipend;

-- the student view must carry the non-degree duration
drop view if exists v_scholarship_student;
create view v_scholarship_student as
select
  s.id, s.slug, s.title, s.summary, s.country, s.province, s.city, s.field,
  s.teaching_language, s.funding_model, s.has_stipend, s.deadline,
  s.benefits, s.requirements, s.policy, s.non_degree_duration,
  s.application_fee_amount, s.application_fee_currency,
  s.service_fee_amount, s.service_fee_currency, s.slots,
  s.hero_image_url, s.cover_image_url,
  s.is_featured, s.is_recommended, s.is_hot, s.published_at
from nad_scholarship s
where s.status = 'ACTIVE' and s.publish_status = 'PUBLISHED';
