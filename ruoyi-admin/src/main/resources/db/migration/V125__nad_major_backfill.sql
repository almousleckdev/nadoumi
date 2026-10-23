-- Builds the major catalogue from the majors already typed on programmes and links them. Names are matched
-- case-insensitively (the table collation), so "Computer science" and "Computer Science" become one major. Safe to run
-- again, and written so it also runs where sql_safe_updates is on.
--
-- manual rollback: update nad_program_major set major_id = null where id > 0; delete from nad_major where create_by = 'flyway-V125';

insert into nad_major (name, name_cn, create_by, create_time)
select src.name, src.name_cn, 'flyway-V125', now()
  from (select min(pm.name) as name, max(pm.name_cn) as name_cn
          from nad_program_major pm
         where pm.name <> ''
         group by pm.name) src
 where not exists (select 1 from nad_major m where m.name = src.name);

update nad_program_major pm
  join nad_major m on m.name = pm.name
   set pm.major_id = m.id
 where pm.id > 0 and pm.major_id is null;
