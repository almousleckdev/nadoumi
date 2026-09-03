-- Scholarship reference data + admin menu + permissions (DML only; DDL is V16).
-- Mirrors the V9 university seed pattern and docs/PERMISSION_CATALOGUE.md §2.7–2.8.

-- ---- category reference -----------------------------------------------------
insert into nad_scholarship_category (code, name, sort_order) values
  ('CSC',          'China Scholarship Council (CSC)', 1),
  ('CGS',          'Chinese Government Scholarship',   2),
  ('GOVERNMENT',   'Government scholarship',           3),
  ('PROVINCIAL',   'Provincial scholarship',          4),
  ('UNIVERSITY',   'University scholarship',           5),
  ('PRESIDENTIAL', 'Presidential scholarship',         6),
  ('LANGUAGE',     'Language scholarship',             7),
  ('TYPE_A',       'Type A',                           8),
  ('TYPE_B',       'Type B',                           9),
  ('TYPE_C',       'Type C',                          10),
  ('TYPE_D',       'Type D',                          11),
  ('OTHER',        'Other',                           99);

-- ---- admin menu + permissions --------------------------------------------------
set @nad_root := (select menu_id from sys_menu where path = 'nadoumi' and menu_type = 'M' limit 1);

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
 values ('Scholarships', @nad_root, 4, 'scholarship', 'nadoumi/scholarship/index', 'C', '0', '0', 'nad:scholarship:list', 'star', 'admin', now(), 'Scholarship catalogue');
set @nad_scholarship := last_insert_id();

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark) values
 ('Scholarship query',    @nad_scholarship, 1, '', '', 'F', '0', '0', 'nad:scholarship:view',          '#', 'admin', now(), ''),
 ('Scholarship create',   @nad_scholarship, 2, '', '', 'F', '0', '0', 'nad:scholarship:create',        '#', 'admin', now(), ''),
 ('Scholarship edit',     @nad_scholarship, 3, '', '', 'F', '0', '0', 'nad:scholarship:edit',          '#', 'admin', now(), ''),
 ('Scholarship remove',   @nad_scholarship, 4, '', '', 'F', '0', '0', 'nad:scholarship:remove',        '#', 'admin', now(), ''),
 ('Scholarship publish',  @nad_scholarship, 5, '', '', 'F', '0', '0', 'nad:scholarship:publish',       '#', 'admin', now(), ''),
 ('Scholarship export',   @nad_scholarship, 6, '', '', 'F', '0', '0', 'nad:scholarship:export',        '#', 'admin', now(), ''),
 ('Scholarship linkage view', @nad_scholarship, 7, '', '', 'F', '0', '0', 'nad:scholarship:internal:view', '#', 'admin', now(), 'Confidential: partner university / partnership linkage'),
 ('Scholarship linkage edit', @nad_scholarship, 8, '', '', 'F', '0', '0', 'nad:scholarship:internal:edit', '#', 'admin', now(), 'Confidential');

-- nadoumi_super_admin (role 3): every menu.
insert into sys_role_menu (role_id, menu_id)
 select 3, m.menu_id from sys_menu m
 where m.menu_id = @nad_scholarship or m.perms like 'nad:scholarship:%';

-- ops_manager + partnerships_manager: full scholarship management incl. internal linkage.
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id = @nad_scholarship or m.perms like 'nad:scholarship:%'
 where r.role_key in ('ops_manager', 'partnerships_manager');

-- case_officer / read_only_analyst: read + internal linkage view (not edit).
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id = @nad_scholarship
                 or m.perms in ('nad:scholarship:view', 'nad:scholarship:list', 'nad:scholarship:internal:view')
 where r.role_key in ('case_officer', 'read_only_analyst');

-- content_editor: student-safe read only.
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id = @nad_scholarship
                 or m.perms in ('nad:scholarship:view', 'nad:scholarship:list')
 where r.role_key in ('content_editor');
