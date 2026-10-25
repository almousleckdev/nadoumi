-- V138: permissions to delete an application or a payment (buttons under their menus).
--
-- Only the super administrator (role 3) receives them; other roles can be given them in Roles. A confirmed payment can never
-- be deleted whatever the permission, because it is booked revenue.
--
-- Manual rollback:
--   delete from sys_role_menu where menu_id in (select menu_id from sys_menu where perms in ('nad:application:remove', 'nad:payment:remove'));
--   delete from sys_menu where perms in ('nad:application:remove', 'nad:payment:remove');

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'Application delete', m.menu_id, 20, '', '', 'F', '0', '0', 'nad:application:remove', '#', 'flyway-V138', now(),
       'Permanently delete an application (refused while a confirmed payment exists)'
from sys_menu m
where m.perms = 'nad:application:list' and m.menu_type = 'C'
  and not exists (select 1 from sys_menu x where x.perms = 'nad:application:remove');

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'Payment delete', m.menu_id, 20, '', '', 'F', '0', '0', 'nad:payment:remove', '#', 'flyway-V138', now(),
       'Permanently delete a payment that was not confirmed'
from sys_menu m
where m.perms = 'nad:payment:list' and m.menu_type = 'C'
  and not exists (select 1 from sys_menu x where x.perms = 'nad:payment:remove');

insert into sys_role_menu (role_id, menu_id)
select 3, m.menu_id from sys_menu m
where m.perms in ('nad:application:remove', 'nad:payment:remove')
  and not exists (select 1 from sys_role_menu rm where rm.role_id = 3 and rm.menu_id = m.menu_id);
