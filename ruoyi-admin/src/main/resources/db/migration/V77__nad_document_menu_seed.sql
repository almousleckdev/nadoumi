-- Document staff menu + permissions. Docs: PERMISSION_CATALOGUE.md §4/§4-role-table.
-- Only what StaffDocumentController actually enforces is seeded (nad:document:view/
-- download/verify/reject) — requirement management (nad:document:requirement:*) has
-- no endpoint yet, so it is not granted here, matching V65's own convention ("only
-- what's implemented is granted").
--
-- Rollback (manual): delete from sys_role_menu where menu_id in
--   (select menu_id from sys_menu where perms like 'nad:document:%' or menu_id = @nad_document);
--   delete from sys_menu where perms like 'nad:document:%' or menu_id = @nad_document;
--   delete from sys_role where role_key = 'document_reviewer';

set @nad_root := (select menu_id from sys_menu where path = 'nadoumi' and menu_type = 'M' limit 1);

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
 values ('Documents', @nad_root, 6, 'document', 'nadoumi/document/index', 'C', '0', '0', 'nad:document:view', 'file', 'admin', now(), 'Document review queue');
set @nad_document := last_insert_id();

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark) values
 ('Document view',     @nad_document, 1, '', '', 'F', '0', '0', 'nad:document:view',     '#', 'admin', now(), ''),
 ('Document download', @nad_document, 2, '', '', 'F', '0', '0', 'nad:document:download', '#', 'admin', now(), 'sensitive'),
 ('Document verify',   @nad_document, 3, '', '', 'F', '0', '0', 'nad:document:verify',   '#', 'admin', now(), 'sensitive'),
 ('Document reject',   @nad_document, 4, '', '', 'F', '0', '0', 'nad:document:reject',   '#', 'admin', now(), '');

insert into sys_role (role_name, role_key, role_sort, data_scope, status, create_by, create_time, remark) values
 ('Document reviewer', 'document_reviewer', 12, '4', '0', 'admin', now(), 'reviews and verifies submitted documents; dept + children scope');

-- nadoumi_super_admin (role 3): every document menu.
insert into sys_role_menu (role_id, menu_id)
 select 3, m.menu_id from sys_menu m
 where m.menu_id = @nad_document or m.perms like 'nad:document:%';

-- document_reviewer: full set (PERMISSION_CATALOGUE.md §4 — view/download/verify/reject).
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id = @nad_document or m.perms like 'nad:document:%'
 where r.role_key = 'document_reviewer';

-- ops_manager, case_officer: view + download only, not verify/reject
-- (PERMISSION_CATALOGUE.md §4-role-table — verify is document_reviewer/super_admin only).
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id = @nad_document or m.perms in ('nad:document:view', 'nad:document:download')
 where r.role_key in ('ops_manager', 'case_officer');
