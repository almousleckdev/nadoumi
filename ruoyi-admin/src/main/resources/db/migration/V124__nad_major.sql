-- A shared catalogue of majors. Until now a major was free text typed again on every programme, so the same subject
-- existed many times under different spellings and nothing could be reused, filtered or merged. A programme now links
-- each of its majors (nad_program_major.major_id) to one catalogue row; the level and the department stay on the link,
-- because different programmes and universities offer the same major at different levels.
--
-- nad_program_major.name / name_cn stay: they hold the title shown for that programme (the catalogue title unless the
-- programme overrides it), so every existing read keeps working. major_id is nullable only so the data migration
-- (V125) can fill it; the service always sets it from now on. RESTRICT: a major in use cannot be deleted, it is merged.
--
-- manual rollback:
--   alter table nad_program_major drop foreign key fk_program_major_catalogue;
--   alter table nad_program_major drop key idx_program_major_catalogue, drop column major_id;
--   drop table nad_major;

create table nad_major (
  id          bigint       not null auto_increment,
  name        varchar(200) not null,
  name_cn     varchar(200)     null,
  discipline  varchar(32)      null comment 'broad subject group, a string code such as MEDICINE or ENGINEERING',
  create_by   varchar(64)  not null default '',
  create_time datetime         null,
  update_by   varchar(64)  not null default '',
  update_time datetime         null,
  primary key (id),
  unique key uk_major_name (name),
  key idx_major_discipline (discipline)
) engine=innodb default charset=utf8mb4;

alter table nad_program_major
  add column major_id bigint null after program_id,
  add key idx_program_major_catalogue (major_id),
  add constraint fk_program_major_catalogue foreign key (major_id) references nad_major (id) on delete restrict;
