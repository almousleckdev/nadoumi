-- Widen the baseline "staff" role so a rank-and-file employee can do catalog
-- work but nothing destructive:
--   * Applicants     — view only (list + view). No create / edit / archive /
--                      status change / merge / export / PII.
--   * Universities   — view + create + edit. NO delete.
--   * Programmes     — view + create + edit. NO delete.
--   * Scholarships   — view + create + edit. NO delete, NO publish, NO internal.
--   * Students       — view only (system:user:list).
--   * Departments    — view only (system:dept:list + nad:department:list). No add
--                      / edit / delete, incl. no sub-department.
-- Tasks stay as V49 left them (list / query / progress).
--
-- manual rollback:
--   delete rm from sys_role_menu rm
--   join sys_role r on r.role_id = rm.role_id
--   join sys_menu m on m.menu_id = rm.menu_id
--   where r.role_key = 'staff'
--     and m.perms in (
--       'nad:applicant:list','nad:applicant:view',
--       'nad:university:list','nad:university:view','nad:university:create','nad:university:edit',
--       'nad:program:list','nad:program:view','nad:program:create','nad:program:edit',
--       'nad:scholarship:list','nad:scholarship:view','nad:scholarship:create','nad:scholarship:edit',
--       'system:user:list','system:dept:list','nad:department:list');

set @staff := (select role_id from sys_role where role_key = 'staff' limit 1);

insert into sys_role_menu (role_id, menu_id)
select @staff, m.menu_id
from sys_menu m
where @staff is not null
  and m.perms in (
    'nad:applicant:list', 'nad:applicant:view',
    'nad:university:list', 'nad:university:view', 'nad:university:create', 'nad:university:edit',
    'nad:program:list', 'nad:program:view', 'nad:program:create', 'nad:program:edit',
    'nad:scholarship:list', 'nad:scholarship:view', 'nad:scholarship:create', 'nad:scholarship:edit',
    'system:user:list',
    'system:dept:list',
    'nad:department:list'
  )
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = @staff and rm.menu_id = m.menu_id
  );

-- also grant the parent C-menus so RuoYi's own router build is consistent
insert into sys_role_menu (role_id, menu_id)
select @staff, p.menu_id
from sys_menu p
where @staff is not null
  and p.menu_type = 'C'
  and p.menu_id in (
    select distinct m.parent_id from sys_menu m
    where m.perms in (
      'nad:applicant:list', 'nad:university:list', 'nad:program:list',
      'nad:scholarship:list', 'system:user:list', 'system:dept:list'
    )
  )
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = @staff and rm.menu_id = p.menu_id
  );
