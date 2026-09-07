-- Admin menu + permissions for the Employees (HR) and Tasks screens (DML only;
-- DDL is V35 / V36). Mirrors the V17 / V33 seed pattern + notification templates
-- for the TASK_PROGRESS notification type.

set @nad_root := (select menu_id from sys_menu where path = 'nadoumi' and menu_type = 'M' limit 1);

-- ---- Employees (HR) ---------------------------------------------------------
insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
 values ('Employees', @nad_root, 12, 'employees', 'nadoumi/employees/index', 'C', '0', '0', 'nad:employee:list', 'user-filled', 'admin', now(), 'Internal staff HR records');
set @nad_employee := last_insert_id();

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark) values
 ('Employee query',      @nad_employee, 1, '', '', 'F', '0', '0', 'nad:employee:query',       '#', 'admin', now(), ''),
 ('Employee create',     @nad_employee, 2, '', '', 'F', '0', '0', 'nad:employee:add',         '#', 'admin', now(), ''),
 ('Employee edit',       @nad_employee, 3, '', '', 'F', '0', '0', 'nad:employee:edit',        '#', 'admin', now(), ''),
 ('Employee remove',     @nad_employee, 4, '', '', 'F', '0', '0', 'nad:employee:remove',      '#', 'admin', now(), ''),
 ('Employee compensation view', @nad_employee, 5, '', '', 'F', '0', '0', 'nad:employee:compensation:view', '#', 'admin', now(), 'Salary is only visible with this permission');

-- ---- Tasks ---------------------------------------------------------------
insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
 values ('Tasks', @nad_root, 13, 'tasks', 'nadoumi/tasks/index', 'C', '0', '0', 'nad:task:list', 'tickets', 'admin', now(), 'Task assignment + progress tracking');
set @nad_task := last_insert_id();

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark) values
 ('Task query',   @nad_task, 1, '', '', 'F', '0', '0', 'nad:task:query',   '#', 'admin', now(), ''),
 ('Task create',  @nad_task, 2, '', '', 'F', '0', '0', 'nad:task:add',     '#', 'admin', now(), ''),
 ('Task edit',    @nad_task, 3, '', '', 'F', '0', '0', 'nad:task:edit',    '#', 'admin', now(), ''),
 ('Task remove',  @nad_task, 4, '', '', 'F', '0', '0', 'nad:task:remove',  '#', 'admin', now(), ''),
 ('Task approve', @nad_task, 5, '', '', 'F', '0', '0', 'nad:task:approve', '#', 'admin', now(), 'Approve a completed task (admin)');

-- nadoumi_super_admin (role 3): every new menu.
insert into sys_role_menu (role_id, menu_id)
 select 3, m.menu_id from sys_menu m
 where m.menu_id in (@nad_employee, @nad_task)
    or m.perms like 'nad:employee:%' or m.perms like 'nad:task:%';

-- ops_manager: full HR + tasks.
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id in (@nad_employee, @nad_task)
                 or m.perms like 'nad:employee:%' or m.perms like 'nad:task:%'
 where r.role_key in ('ops_manager');

-- case_officer: see tasks + own assignments, no HR compensation.
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id in (@nad_task)
                 or m.perms in ('nad:task:list', 'nad:task:query', 'nad:task:edit',
                                'nad:employee:list', 'nad:employee:query')
 where r.role_key in ('case_officer');

-- ---- notification templates for TASK_PROGRESS ---------------------------
insert into nad_notification_template (type, channel, locale, subject_tpl, body_tpl, create_by, create_time) values
 ('TASK_PROGRESS', 'IN_APP', 'en', null,
  'Task #{{taskId}} "{{taskTitle}}" is now {{taskStatus}} (was {{fromStatus}}).', 'system', now()),
 ('TASK_PROGRESS', 'EMAIL', 'en', 'Task update: {{taskTitle}} -> {{taskStatus}}',
  'Task #{{taskId}} "{{taskTitle}}" changed from {{fromStatus}} to {{taskStatus}} by {{actor}}.\n\nOpen the admin console to review.', 'system', now());
