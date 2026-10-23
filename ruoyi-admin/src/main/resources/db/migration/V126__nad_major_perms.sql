-- Permissions for the major catalogue screen. F-perms under the Programmes menu (no new top-level menu): the Majors
-- screen is reached from the Education navigation group by anyone holding nad:major:list.
--
-- manual rollback: delete from sys_role_menu where menu_id in (select menu_id from sys_menu where perms like 'nad:major:%');
--   delete from sys_menu where perms like 'nad:major:%';

set @nad_program := (select menu_id from sys_menu
                     where perms = 'nad:program:list' and menu_type = 'C' limit 1);

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status,
                      perms, icon, create_by, create_time, remark)
select * from (
 select 'Major list' as menu_name, @nad_program as parent_id, 30 as order_num, '' as path, '' as component, 'F' as menu_type, '0' as visible, '0' as status, 'nad:major:list' as perms, '#' as icon, 'admin' as create_by, now() as create_time, 'Shared major catalogue' as remark
 union all select 'Major create', @nad_program, 31, '', '', 'F', '0', '0', 'nad:major:add',    '#', 'admin', now(), ''
 union all select 'Major edit',   @nad_program, 32, '', '', 'F', '0', '0', 'nad:major:edit',   '#', 'admin', now(), ''
 union all select 'Major remove', @nad_program, 33, '', '', 'F', '0', '0', 'nad:major:remove', '#', 'admin', now(), ''
) new_menu
 where @nad_program is not null
   and not exists (select 1 from sys_menu x where x.perms = new_menu.perms);

-- nadoumi_super_admin (role 3): everything.
insert into sys_role_menu (role_id, menu_id)
 select 3, m.menu_id from sys_menu m
  where m.perms like 'nad:major:%'
    and not exists (select 1 from sys_role_menu rm where rm.role_id = 3 and rm.menu_id = m.menu_id);

-- catalog managers: full management.
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id from sys_role r
 join sys_menu m on m.perms like 'nad:major:%'
 where r.role_key in ('ops_manager', 'partnerships_manager')
   and not exists (select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = m.menu_id);

-- read-only and content roles: view the list (the programme form picks from it).
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id from sys_role r
 join sys_menu m on m.perms = 'nad:major:list'
 where r.role_key in ('case_officer', 'content_editor', 'read_only_analyst', 'support_agent')
   and not exists (select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = m.menu_id);
