-- A programme can span several levels and each level has its own duration(s), tuition
-- and accommodation. nad_program_offering holds one row per (level, duration); the
-- duration / tuition columns on nad_program stay as the "from" summary (shortest
-- duration, cheapest tuition) so cards, filters and sorting keep working.
-- Also adds the admission content shown on the public programme page.
--
-- manual rollback:
--   alter table nad_program drop column admission_requirements, drop column application_documents,
--     drop column career_prospects, drop column scholarship_info, drop column min_hsk,
--     drop column min_ielts, drop column min_toefl, drop column application_fee_amount,
--     drop column application_fee_currency;
--   drop table nad_program_offering;

create table nad_program_offering (
  id                    bigint         not null auto_increment,
  program_id            bigint         not null,
  level                 varchar(16)        null comment 'DIPLOMA | BACHELOR | MASTER | PHD for degree programmes; null for language / non-degree',
  duration_months       int            not null,
  tuition_amount        decimal(12, 2)     null,
  tuition_currency      char(3)            null comment 'ISO 4217',
  tuition_period        varchar(16)    not null default 'PER_YEAR' comment 'PER_YEAR | PER_SEMESTER | TOTAL',
  accommodation_amount  decimal(12, 2)     null,
  accommodation_currency char(3)           null comment 'ISO 4217',
  accommodation_period  varchar(16)    not null default 'PER_MONTH' comment 'PER_MONTH | PER_YEAR | TOTAL',
  note                  varchar(300)       null,
  sort_order            int            not null default 0,
  primary key (id),
  key idx_program_offering (program_id, sort_order),
  constraint fk_program_offering_program foreign key (program_id)
    references nad_program (id) on delete cascade
) engine=innodb default charset=utf8mb4;

-- existing degree programmes: one offering per level carrying the old duration / tuition
insert into nad_program_offering (program_id, level, duration_months, tuition_amount, tuition_currency, sort_order)
select p.id, l.level, p.duration_months, p.tuition_amount, p.tuition_currency, l.sort_order
from nad_program p
join nad_program_level l on l.program_id = p.id
where p.duration_months is not null;

-- existing language / non-degree programmes: a single level-less offering
insert into nad_program_offering (program_id, level, duration_months, tuition_amount, tuition_currency, sort_order)
select p.id, null, p.duration_months, p.tuition_amount, p.tuition_currency, 0
from nad_program p
where p.program_type <> 'DEGREE' and p.duration_months is not null;

alter table nad_program
  add column admission_requirements  text            null comment 'student-safe prose' after summary,
  add column application_documents   text            null comment 'student-safe prose, one document per line' after admission_requirements,
  add column career_prospects        text            null after application_documents,
  add column scholarship_info        text            null comment 'which scholarships typically fund this programme' after career_prospects,
  add column min_hsk                 tinyint unsigned null comment 'minimum HSK level (1-6)' after scholarship_info,
  add column min_ielts               decimal(2, 1)   null after min_hsk,
  add column min_toefl               smallint unsigned null after min_ielts,
  add column application_fee_amount  decimal(12, 2)  null after min_toefl,
  add column application_fee_currency char(3)        null comment 'ISO 4217' after application_fee_amount;
