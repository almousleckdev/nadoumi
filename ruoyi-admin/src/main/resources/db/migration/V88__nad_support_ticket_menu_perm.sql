-- Support/Ticketing domain, slice 2: staff console menu + permissions (mirrors the
-- V33 / V83 pattern). DML only; DDL is V87.
--
-- manual rollback:
--   delete from sys_role_menu where menu_id in (select menu_id from sys_menu where perms like 'nad:support:%' or path = 'support');
--   delete from sys_menu where perms like 'nad:support:%' or path = 'support';

set @nad_root := (select menu_id from sys_menu where path = 'nadoumi' and menu_type = 'M' limit 1);

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
 values ('Support Tickets', @nad_root, 10, 'support', 'nadoumi/support/index', 'C', '0', '0', 'nad:support:ticket:view', 'service', 'admin', now(), 'Student support ticket queue');
set @nad_support := last_insert_id();

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark) values
 ('Manage tickets', @nad_support, 1, '', '', 'F', '0', '0', 'nad:support:ticket:manage', '#', 'admin', now(), 'Reply, change status / priority / category'),
 ('Assign tickets', @nad_support, 2, '', '', 'F', '0', '0', 'nad:support:ticket:assign', '#', 'admin', now(), 'Assign and reassign tickets to staff');

-- nadoumi_super_admin (role 3): every support menu.
insert into sys_role_menu (role_id, menu_id)
 select 3, m.menu_id from sys_menu m
 where m.menu_id = @nad_support or m.perms like 'nad:support:%';

-- ops_manager: full queue control including assignment.
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id = @nad_support or m.perms like 'nad:support:%'
 where r.role_key in ('ops_manager');

-- support_agent: work the queue (view + manage) but not (re)assign.
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id = @nad_support or m.perms in ('nad:support:ticket:view', 'nad:support:ticket:manage')
 where r.role_key in ('support_agent');

-- read_only_analyst: view only.
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id = @nad_support
 where r.role_key in ('read_only_analyst');
