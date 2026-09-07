-- Academic-department permissions. Managed inline on the University detail
-- screen, so these are F-perms under the existing Universities C-menu (no new
-- top-level nav item).

set @nad_university := (select menu_id from sys_menu
                        where perms = 'nad:university:list' and menu_type = 'C' limit 1);

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status,
                      perms, icon, create_by, create_time, remark) values
 ('Department list',   @nad_university, 20, '', '', 'F', '0', '0', 'nad:department:list',   '#', 'admin', now(), 'Academic departments / colleges'),
 ('Department create', @nad_university, 21, '', '', 'F', '0', '0', 'nad:department:add',    '#', 'admin', now(), ''),
 ('Department edit',   @nad_university, 22, '', '', 'F', '0', '0', 'nad:department:edit',   '#', 'admin', now(), ''),
 ('Department remove', @nad_university, 23, '', '', 'F', '0', '0', 'nad:department:remove', '#', 'admin', now(), '');

-- nadoumi_super_admin (role 3): everything.
insert into sys_role_menu (role_id, menu_id)
 select 3, m.menu_id from sys_menu m where m.perms like 'nad:department:%';

-- catalog managers: full department management.
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id from sys_role r
 join sys_menu m on m.perms like 'nad:department:%'
 where r.role_key in ('ops_manager', 'partnerships_manager');

-- read-only roles: view.
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id from sys_role r
 join sys_menu m on m.perms = 'nad:department:list'
 where r.role_key in ('case_officer', 'content_editor', 'read_only_analyst', 'support_agent');
