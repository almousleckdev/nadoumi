-- Payroll: a read-only view over nad_employee compensation. No new tables -- a
-- pay-run ledger comes with the Payment module (Step 9). Just the menu + perm.
--
-- manual rollback:
--   delete from sys_role_menu where menu_id in (select menu_id from sys_menu where perms like 'nad:payroll:%');
--   delete from sys_menu where perms like 'nad:payroll:%';

set @nad_root := (select menu_id from sys_menu where path = 'nadoumi' and menu_type = 'M' limit 1);

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status,
                      perms, icon, create_by, create_time, remark)
 values ('Payroll', @nad_root, 24, 'payroll', 'nadoumi/finance/payroll', 'C', '0', '0',
         'nad:payroll:view', 'coin', 'admin', now(), 'Staff compensation summary (read-only)');

-- nadoumi_super_admin (role 3) + ops_manager.
insert into sys_role_menu (role_id, menu_id)
 select 3, m.menu_id from sys_menu m where m.perms like 'nad:payroll:%';

insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id from sys_role r
 join sys_menu m on m.perms like 'nad:payroll:%'
 where r.role_key in ('ops_manager');
