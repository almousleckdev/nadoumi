-- Communication domain, slice 5: admin menu + permissions (mirrors the V33 pattern)
-- and the MESSAGE_POSTED default en template (DML only; DDL is V79-V82).

-- ---- admin menu + permissions ------------------------------------------------
set @nad_root := (select menu_id from sys_menu where path = 'nadoumi' and menu_type = 'M' limit 1);

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
 values ('Conversations', @nad_root, 9, 'conversation', 'nadoumi/conversation/index', 'C', '0', '0', 'nad:conversation:participate', 'message', 'admin', now(), 'Staff/student conversations');
set @nad_conversation := last_insert_id();

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark) values
 ('Manage participants', @nad_conversation, 1, '', '', 'F', '0', '0', 'nad:conversation:participant:manage', '#', 'admin', now(), '');

-- nadoumi_super_admin (role 3): every menu.
insert into sys_role_menu (role_id, menu_id)
 select 3, m.menu_id from sys_menu m
 where m.menu_id = @nad_conversation or m.perms like 'nad:conversation:%';

-- ops_manager / support_agent: full conversation participation + moderation.
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id = @nad_conversation or m.perms like 'nad:conversation:%'
 where r.role_key in ('ops_manager', 'support_agent');

-- read_only_analyst: read only (participate, but not manage other participants).
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id = @nad_conversation
 where r.role_key in ('read_only_analyst');

-- ---- default en template -----------------------------------------------------
-- Deliberately excludes the message body itself (docs/superpowers/specs/
-- 2026-09-20-messaging-domain-design.md §3): only the sender name and a
-- conversation label, matching the "never the message body itself" rule already
-- applied to the SSE ping. IN_APP only -- MESSAGE_POSTED declares no secondary
-- channel (NotificationType.java).
insert into nad_notification_template (type, channel, locale, subject_tpl, body_tpl, create_by, create_time) values
 ('MESSAGE_POSTED', 'IN_APP', 'en', null,
  '{{senderName}} sent a new message in {{conversationSubject}}.', 'system', now());
