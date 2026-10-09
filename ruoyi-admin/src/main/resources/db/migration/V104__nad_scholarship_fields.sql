-- A scholarship can cover several fields of study, and they can differ by degree level (a Bachelor's in Engineering,
-- a Master's in Engineering and Medicine, a PhD in Medicine...). Until now it had one free-text discipline.
--
-- nad_scholarship_field holds one row per (level, field name). level NULL means "every level the scholarship offers".
-- The old nad_scholarship.field column stays as a searchable summary (the names joined with ", ") that the writer
-- keeps up to date, so the student views and the public text search keep working unchanged.
--
-- manual rollback:
--   drop table nad_scholarship_field;

create table nad_scholarship_field (
  id              bigint       not null auto_increment,
  scholarship_id  bigint       not null,
  level           varchar(16)      null comment 'EducationLevel code; NULL = every level',
  level_key       varchar(16) as (coalesce(level, 'ALL')) stored,
  name            varchar(120) not null comment 'field of study, e.g. Engineering',
  sort_order      int          not null default 0,
  primary key (id),
  unique key uk_scholarship_field (scholarship_id, level_key, name),
  key idx_scholarship_field_name (name),
  constraint fk_scholarship_field_scholarship foreign key (scholarship_id)
    references nad_scholarship (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 comment='Fields of study of a scholarship, optionally per degree level';

-- carry the existing single value over as an every-level field
insert into nad_scholarship_field (scholarship_id, level, name, sort_order)
select id, null, field, 0 from nad_scholarship where field is not null and trim(field) <> '';
