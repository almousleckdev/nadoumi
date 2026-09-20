-- Applications staff menu + permissions. Docs: PERMISSION_CATALOGUE.md §3 (note:*
-- and submission:record permissions are NOT seeded here — no notes or external
-- submission-record feature is built in P2; only what's implemented is granted).
-- Rollback (manual): delete from sys_role_menu where menu_id in
--   (select menu_id from sys_menu where perms like 'nad:application:%' or menu_id = @nad_application);
--   delete from sys_menu where perms like 'nad:application:%' or menu_id = @nad_application;

set @nad_root := (select menu_id from sys_menu where path = 'nadoumi' and menu_type = 'M' limit 1);

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
 values ('Applications', @nad_root, 5, 'application', 'nadoumi/application/index', 'C', '0', '0', 'nad:application:list', 'form', 'admin', now(), 'Application case management');
set @nad_application := last_insert_id();

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark) values
 ('Application query',      @nad_application, 1, '', '', 'F', '0', '0', 'nad:application:view',       '#', 'admin', now(), ''),
 ('Application create',     @nad_application, 2, '', '', 'F', '0', '0', 'nad:application:create',     '#', 'admin', now(), ''),
 ('Application edit',       @nad_application, 3, '', '', 'F', '0', '0', 'nad:application:edit',       '#', 'admin', now(), ''),
 ('Application assign',     @nad_application, 4, '', '', 'F', '0', '0', 'nad:application:assign',     '#', 'admin', now(), ''),
 ('Application claim',      @nad_application, 5, '', '', 'F', '0', '0', 'nad:application:claim',      '#', 'admin', now(), ''),
 ('Application transition', @nad_application, 6, '', '', 'F', '0', '0', 'nad:application:transition', '#', 'admin', now(), ''),
 ('Application withdraw',   @nad_application, 7, '', '', 'F', '0', '0', 'nad:application:withdraw',   '#', 'admin', now(), ''),
 ('Application decide',     @nad_application, 8, '', '', 'F', '0', '0', 'nad:application:decide',     '#', 'admin', now(), ''),
 ('Application export',     @nad_application, 9, '', '', 'F', '0', '0', 'nad:application:export',     '#', 'admin', now(), '');

-- nadoumi_super_admin (role 3): every menu.
insert into sys_role_menu (role_id, menu_id)
 select 3, m.menu_id from sys_menu m
 where m.menu_id = @nad_application or m.perms like 'nad:application:%';

-- ops_manager: full application management, org-wide.
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id = @nad_application or m.perms like 'nad:application:%'
 where r.role_key = 'ops_manager';

-- case_officer: full case work except export; the API further scopes them to
-- assigned/in-queue applications (docs/PERMISSION_CATALOGUE.md §4).
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id = @nad_application
                 or m.perms in ('nad:application:view', 'nad:application:list', 'nad:application:create',
                                'nad:application:edit', 'nad:application:claim', 'nad:application:transition',
                                'nad:application:withdraw', 'nad:application:decide')
 where r.role_key = 'case_officer';
