-- 1. A suspended or blocked account must say why: sys_user.status_reason (cleared on activation).
-- 2. Permanent applicant deletion replaces "archive" and the student-only delete from V98:
--    nad:applicant:delete (super admin only) replaces nad:student:delete.
--
-- manual rollback:
--   alter table sys_user drop column status_reason;
--   delete from sys_role_menu where menu_id in (select menu_id from sys_menu where perms = 'nad:applicant:delete');
--   delete from sys_menu where perms = 'nad:applicant:delete';

alter table sys_user add column status_reason varchar(255) null comment 'why the account is suspended or blocked; cleared on activation';

delete from sys_role_menu where menu_id in (select menu_id from sys_menu where perms = 'nad:student:delete');
delete from sys_menu where perms = 'nad:student:delete';

set @nad_applicant := (select menu_id from sys_menu where perms = 'nad:applicant:list' limit 1);
insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values ('Delete applicants', @nad_applicant, 20, '', '', 'F', '0', '0', 'nad:applicant:delete', '#', 'admin', now(),
        'Permanently delete an applicant with its applications, documents and student login');
insert into sys_role_menu (role_id, menu_id)
select 3, m.menu_id from sys_menu m where m.perms = 'nad:applicant:delete';
