-- Replace RuoYi demo/sample data with the Nadoumi baseline. Structural system
-- rows (menus, permissions, dictionary types, config keys) are kept.

delete from QRTZ_CRON_TRIGGERS   where TRIGGER_NAME in ('TASK_CLASS_NAME1', 'TASK_CLASS_NAME2', 'TASK_CLASS_NAME3');
delete from QRTZ_SIMPLE_TRIGGERS where TRIGGER_NAME in ('TASK_CLASS_NAME1', 'TASK_CLASS_NAME2', 'TASK_CLASS_NAME3');
delete from QRTZ_TRIGGERS        where TRIGGER_NAME in ('TASK_CLASS_NAME1', 'TASK_CLASS_NAME2', 'TASK_CLASS_NAME3');
delete from QRTZ_JOB_DETAILS     where JOB_NAME     in ('TASK_CLASS_NAME1', 'TASK_CLASS_NAME2', 'TASK_CLASS_NAME3');
delete from sys_job    where job_id    in (1, 2, 3);
delete from sys_notice where notice_id in (1, 2, 3);
delete from sys_post   where post_id   in (1, 2, 3, 4);
delete from sys_dept   where dept_id between 101 and 109;

update sys_dept set dept_name = 'Nadoumi', leader = '', phone = '', email = '' where dept_id = 100;
update sys_role set role_name = 'Super administrator' where role_id = 1;
update sys_role set role_name = 'Common' where role_id = 2;

delete from sys_user_post where user_id = 2;
delete from sys_user_role where user_id = 2;
delete from sys_user      where user_id = 2;

update sys_user
   set status    = '1',
       nick_name = 'Break-glass administrator',
       remark    = 'Break-glass only: RuoYi grants *:*:* to user id 1. Day-to-day super admin is almousleck.'
 where user_id = 1;

-- Nadoumi super administrator. This BCrypt hash is an initialization seed only;
-- sys.account.initPasswordModify = 1 forces a change on first sign-in and every
-- real environment must reset it (plaintext documented in docs/SECURITY.md).
insert into sys_user
  (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar,
   password, status, del_flag, login_ip, login_date, pwd_update_date,
   create_by, create_time, update_by, update_time, remark)
values
  (3, 100, 'almousleck', 'Almousleck', '00', '', '', '0', '',
   '$2a$10$OnLCEX62quodp7K/pce8Wu4.73Mpu5cFts0VW6s57Se9k76Xux5qu', '0', '0', '', null, null,
   'system', now(), '', null, 'Nadoumi super administrator');

insert into sys_role
  (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly,
   status, del_flag, create_by, create_time, update_by, update_time, remark)
values
  (3, 'Nadoumi super administrator', 'nadoumi_super_admin', 1, '1', 1, 1,
   '0', '0', 'system', now(), '', null, 'Every menu; org-wide data scope');

insert into sys_user_role (user_id, role_id) values (3, 3);
insert into sys_role_menu (role_id, menu_id) select 3, menu_id from sys_menu;
