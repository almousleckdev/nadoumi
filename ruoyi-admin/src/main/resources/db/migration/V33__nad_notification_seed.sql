-- Notification admin menu + permissions + default en templates (DML only; DDL is V32).
-- Mirrors the V17 / V20 seed pattern.

-- ---- admin menu + permissions ------------------------------------------------
set @nad_root := (select menu_id from sys_menu where path = 'nadoumi' and menu_type = 'M' limit 1);

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
 values ('Notifications', @nad_root, 8, 'notification', 'nadoumi/notification/index', 'C', '0', '0', 'nad:notification:list', 'message', 'admin', now(), 'Sent notifications + delivery status');
set @nad_notification := last_insert_id();

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark) values
 ('Notification query',       @nad_notification, 1, '', '', 'F', '0', '0', 'nad:notification:view',            '#', 'admin', now(), ''),
 ('Notification templates',   @nad_notification, 2, '', '', 'F', '0', '0', 'nad:notification:template:edit',   '#', 'admin', now(), ''),
 ('Notification preferences', @nad_notification, 3, '', '', 'F', '0', '0', 'nad:notification:preference:edit', '#', 'admin', now(), '');

-- nadoumi_super_admin (role 3): every menu.
insert into sys_role_menu (role_id, menu_id)
 select 3, m.menu_id from sys_menu m
 where m.menu_id = @nad_notification or m.perms like 'nad:notification:%';

-- ops_manager: full notification administration.
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id = @nad_notification or m.perms like 'nad:notification:%'
 where r.role_key in ('ops_manager');

-- support_agent / read_only_analyst: read only.
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id = @nad_notification
                 or m.perms in ('nad:notification:list', 'nad:notification:view')
 where r.role_key in ('support_agent', 'read_only_analyst');

-- ---- default en templates --------------------------------------------------
-- {{var}} placeholders are resolved against the notification context map by the
-- renderer (slice 3); IN_APP has no subject. Double braces (not the dollar-brace
-- form) so Flyway placeholder replacement leaves them untouched.
insert into nad_notification_template (type, channel, locale, subject_tpl, body_tpl, create_by, create_time) values
 ('CONTACT_INQUIRY_RECEIVED', 'IN_APP', 'en', null,
  'New contact inquiry #{{inquiryId}} from {{inquiryName}} ({{inquiryCategory}}).', 'system', now()),
 ('CONTACT_INQUIRY_RECEIVED', 'EMAIL', 'en', 'New contact inquiry #{{inquiryId}}',
  'A new inquiry was submitted via the public site.\n\nReference: #{{inquiryId}}\nFrom: {{inquiryName}}\nCategory: {{inquiryCategory}}\n\nOpen the admin console to review and respond.', 'system', now()),
 ('SCHOLARSHIP_PUBLISHED', 'IN_APP', 'en', null,
  'Scholarship "{{scholarshipTitle}}" ({{scholarshipReference}}) was published.', 'system', now()),
 ('SCHOLARSHIP_PUBLISHED', 'EMAIL', 'en', 'Scholarship published: {{scholarshipTitle}}',
  'The scholarship "{{scholarshipTitle}}" (reference {{scholarshipReference}}) is now published and visible on the public site.', 'system', now());
