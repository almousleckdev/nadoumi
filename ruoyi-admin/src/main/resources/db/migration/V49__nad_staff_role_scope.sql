-- Tighten the baseline "staff" role to a true rank-and-file employee:
--   * Tasks  — view + change progress only. A new `nad:task:progress` permission
--     backs PUT /api/staff/tasks/{id}/status; the service also refuses a status
--     change on a task the actor was not assigned / did not create.
--   * NO create / edit / delete on tasks.
--   * NO Roles, Departments or Posts admin screens. An employee sees their own
--     roles / posts / department on the Profile page, which needs no permission.
--   * NO Menus & Permissions, Dictionaries, Configuration, Scheduled Jobs,
--     Operation Logs or Sign-in Logs (the System group) — already ungranted.
--
-- Row-level ownership on RuoYi's own system tables (roles/posts/departments) is
-- still out of scope; this migration removes the permissions instead.
--
-- manual rollback:
--   delete from sys_menu where perms = 'nad:task:progress';
--   -- then re-run V42's grant block to restore the old staff permissions.

-- ---- new permission: change task status (progress) only -------------------
set @nad_task_menu := (select menu_id from sys_menu
                       where perms = 'nad:task:list' and menu_type = 'C' limit 1);

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type,
                      visible, status, perms, icon, create_by, create_time, remark)
select 'Task progress', @nad_task_menu, 6, '', '', 'F', '0', '0',
       'nad:task:progress', '#', 'admin', now(),
       'Change a task''s status without full edit rights'
where @nad_task_menu is not null
  and not exists (select 1 from sys_menu where perms = 'nad:task:progress');

set @nad_task_progress := (select menu_id from sys_menu where perms = 'nad:task:progress' limit 1);

-- anyone who could already edit a task keeps progress rights
insert into sys_role_menu (role_id, menu_id)
select rm.role_id, @nad_task_progress
from sys_role_menu rm
join sys_menu m on m.menu_id = rm.menu_id and m.perms = 'nad:task:edit'
where @nad_task_progress is not null
  and not exists (select 1 from sys_role_menu x
                  where x.role_id = rm.role_id and x.menu_id = @nad_task_progress);

-- ---- re-scope the "staff" role -------------------------------------------
set @staff := (select role_id from sys_role where role_key = 'staff' limit 1);

delete from sys_role_menu where role_id = @staff;

insert into sys_role_menu (role_id, menu_id)
select @staff, m.menu_id
from sys_menu m
where @staff is not null
  and (m.menu_id = @nad_task_menu
       or m.perms in ('nad:task:list', 'nad:task:query', 'nad:task:progress'));
