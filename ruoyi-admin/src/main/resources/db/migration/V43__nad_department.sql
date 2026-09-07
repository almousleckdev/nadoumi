-- Academic department / college inside a university. A degree programme's majors
-- (a.k.a. fields of study) each belong to one department; LANGUAGE / NON_DEGREE
-- programmes have no majors and carry a term length instead.
--
-- nad_program.field is kept (deprecated) — the admin drawer no longer writes it,
-- the public catalog filter still reads it for legacy rows. A later migration
-- drops it once the public site moves to department / major facets.
--
-- manual rollback:
--   alter table nad_program_major drop foreign key fk_program_major_department;
--   alter table nad_program_major drop column department_id;
--   alter table nad_program drop column term_length;
--   drop table nad_department;

create table nad_department (
  id            bigint       not null auto_increment,
  university_id bigint       not null,
  name          varchar(160) not null,
  name_cn       varchar(160)     null,
  sort_order    int          not null default 0,
  create_by     varchar(64)  not null default '',
  create_time   datetime         null,
  update_by     varchar(64)  not null default '',
  update_time   datetime         null,
  primary key (id),
  unique key uk_department_university_name (university_id, name),
  key idx_department_university (university_id, sort_order),
  constraint fk_department_university foreign key (university_id)
    references nad_university (id) on delete cascade
) engine=innodb default charset=utf8mb4;

alter table nad_program_major
  add column department_id bigint null comment 'nad_department.id -- the college this major sits in' after program_id,
  add key idx_program_major_department (department_id),
  add constraint fk_program_major_department foreign key (department_id)
    references nad_department (id) on delete set null;

alter table nad_program
  add column term_length varchar(16) null comment 'ONE_SEMESTER | ONE_YEAR -- LANGUAGE / NON_DEGREE only' after field;
