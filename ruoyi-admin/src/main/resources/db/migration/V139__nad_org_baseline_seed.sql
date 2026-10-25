-- V139: a working organisation to start from: the departments and posts a study-abroad company runs on.
--
-- Departments go under the root "Nadoumi" (dept_id 100); posts get the generated code form NA + date + running number.
-- Nothing is overwritten: a department or post whose name already exists is left as it is, so this is safe on a database
-- where staff already created their own.
--
-- Manual rollback:
--   delete from sys_dept where create_by = 'flyway-V139' and dept_id not in (select dept_id from sys_user where dept_id is not null)
--     and dept_id not in (select dept_id from nad_employee where dept_id is not null);
--   delete from sys_post where create_by = 'flyway-V139' and post_id not in (select position_id from nad_employee where position_id is not null);

insert into sys_dept (parent_id, ancestors, dept_name, order_num, status, del_flag, create_by, create_time)
select 100, '0,100', v.name, v.sort, '0', '0', 'flyway-V139', now()
from (
    select 'Admissions' as name, 1 as sort
    union all select 'Operations', 2
    union all select 'Finance', 3
    union all select 'Human Resources', 4
    union all select 'Marketing', 5
    union all select 'Student Support', 6
) v
where exists (select 1 from sys_dept r where r.dept_id = 100)
  and not exists (select 1 from sys_dept d where d.dept_name = v.name and d.parent_id = 100 and d.del_flag = '0');

set @day := date_format(convert_tz(now(), '+00:00', '+08:00'), '%Y%m%d');
set @n := (select count(*) from sys_post where post_code like concat('NA', @day, '%'));

insert into sys_post (post_code, post_name, post_sort, status, create_by, create_time, remark)
select concat('NA', @day, lpad(@n := @n + 1, 2, '0')), v.name, v.sort, '0', 'flyway-V139', now(), v.remark
from (
    select 'Managing director' as name, 1 as sort, 'Leads the company' as remark
    union all select 'Operations manager', 2, 'Runs day to day operations and the application pipeline'
    union all select 'Case officer', 3, 'Owns applications from review to enrolment'
    union all select 'Document reviewer', 4, 'Checks student documents'
    union all select 'Admissions officer', 5, 'Works with partner universities on admissions'
    union all select 'Finance officer', 6, 'Confirms payments and keeps the books'
    union all select 'HR officer', 7, 'Hiring, contracts and payroll'
    union all select 'Marketing officer', 8, 'Campaigns, content and events'
    union all select 'Student support agent', 9, 'Answers students and follows up'
) v
where not exists (select 1 from sys_post p where p.post_name = v.name);
