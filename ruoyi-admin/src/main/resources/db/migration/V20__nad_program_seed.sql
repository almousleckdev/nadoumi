-- Programme admin menu + permissions (DML only; DDL is V19).
-- Mirrors the V9 university / V17 scholarship seed pattern and
-- docs/PERMISSION_CATALOGUE.md.

set @nad_root := (select menu_id from sys_menu where path = 'nadoumi' and menu_type = 'M' limit 1);

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
 values ('Programmes', @nad_root, 3, 'program', 'nadoumi/program/index', 'C', '0', '0', 'nad:program:list', 'documentation', 'admin', now(), 'Programmes offered by universities');
set @nad_program := last_insert_id();

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark) values
 ('Programme query',  @nad_program, 1, '', '', 'F', '0', '0', 'nad:program:view',   '#', 'admin', now(), ''),
 ('Programme create', @nad_program, 2, '', '', 'F', '0', '0', 'nad:program:create', '#', 'admin', now(), ''),
 ('Programme edit',   @nad_program, 3, '', '', 'F', '0', '0', 'nad:program:edit',   '#', 'admin', now(), ''),
 ('Programme remove', @nad_program, 4, '', '', 'F', '0', '0', 'nad:program:remove', '#', 'admin', now(), '');

-- nadoumi_super_admin (role 3): every menu.
insert into sys_role_menu (role_id, menu_id)
 select 3, m.menu_id from sys_menu m
 where m.menu_id = @nad_program or m.perms like 'nad:program:%';

-- ops_manager + partnerships_manager: full programme management.
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id = @nad_program or m.perms like 'nad:program:%'
 where r.role_key in ('ops_manager', 'partnerships_manager');

-- case_officer / content_editor / read_only_analyst / support_agent: read only.
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id = @nad_program
                 or m.perms in ('nad:program:view', 'nad:program:list')
 where r.role_key in ('case_officer', 'content_editor', 'read_only_analyst', 'support_agent');
