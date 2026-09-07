-- Baseline role for a rank-and-file employee. Assign it (People > Roles, or on
-- the employee record) so a new hire can actually work:
--   * Tasks  — list / view / create / edit; the API scopes non-approvers to the
--     tasks they were assigned or created (StaffTaskController).
--   * Roles  — view only (system:role:list).
--   * Departments — view + add sub-department (system:dept:list + :add), no edit/remove.
--   * Posts  — full CRUD (system:post:*).
-- It does NOT grant Menus & Permissions, the System group, or any applicant /
-- scholarship / finance data. Row-level "own records only" scoping for
-- roles / posts / departments is a later change; this is permission-level only.
--
-- data_scope '1' (all) — RuoYi's @DataScope self-filter does not apply cleanly to
-- posts/roles, so ownership is enforced in application code where it exists.
--
-- manual rollback:
--   delete from sys_role_menu where role_id = (select role_id from sys_role where role_key = 'staff');
--   delete from sys_role where role_key = 'staff';

insert into sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly,
                      dept_check_strictly, status, create_by, create_time, remark)
select 'Staff', 'staff', 20, '1', 1, 1, '0', 'admin', now(),
       'Rank-and-file employee: own tasks, view roles, view/add departments, manage posts'
where not exists (select 1 from sys_role where role_key = 'staff');

set @staff := (select role_id from sys_role where role_key = 'staff');

insert into sys_role_menu (role_id, menu_id)
select @staff, m.menu_id
from sys_menu m
where m.perms in (
  'nad:task:list', 'nad:task:query', 'nad:task:add', 'nad:task:edit',
  'system:role:list',
  'system:dept:list', 'system:dept:add',
  'system:post:list', 'system:post:query', 'system:post:add', 'system:post:edit', 'system:post:remove'
)
and not exists (select 1 from sys_role_menu rm where rm.role_id = @staff and rm.menu_id = m.menu_id);
