-- A degree programme now spans one or more levels. nad_program.program_type
-- collapses to the KIND (DEGREE | LANGUAGE | NON_DEGREE); the specific degree
-- levels move to nad_program_level. A major can be tied to one of those levels.
--
-- manual rollback:
--   alter table nad_program_major drop column level;
--   update nad_program p join nad_program_level l on l.program_id = p.id set p.program_type = l.level;
--   drop table nad_program_level;

create table nad_program_level (
  id         bigint      not null auto_increment,
  program_id bigint      not null,
  level      varchar(16) not null comment 'DIPLOMA | BACHELOR | MASTER | PHD',
  sort_order int         not null default 0,
  primary key (id),
  unique key uk_program_level (program_id, level),
  key idx_program_level (program_id, sort_order),
  constraint fk_program_level_program foreign key (program_id)
    references nad_program (id) on delete cascade
) engine=innodb default charset=utf8mb4;

-- each existing degree programme keeps its level as one row
insert into nad_program_level (program_id, level, sort_order)
select id, program_type, 0 from nad_program
where program_type in ('DIPLOMA', 'BACHELOR', 'MASTER', 'PHD');

-- program_type is now the kind
update nad_program set program_type = 'DEGREE'
where program_type in ('DIPLOMA', 'BACHELOR', 'MASTER', 'PHD');

alter table nad_program_major
  add column level varchar(16) null
    comment 'DIPLOMA | BACHELOR | MASTER | PHD -- degree programmes only' after department_id;
