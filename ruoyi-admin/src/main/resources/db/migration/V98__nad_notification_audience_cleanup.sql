-- Notification audience cleanup.
--   * Catalog and news announcements (scholarship, university, programme, article) are student-only and
--     in-app only now: staff are not told about what they published, and no email goes out per publish.
--     Their EMAIL templates are removed and any still-queued EMAIL deliveries for them are cancelled so a
--     backlog is not sent after deploy.
--   * New staff work item APPLICATION_RECEIVED (in-app) for the application queue.
--   * Staff notification rows that the old broadcast already created for the catalog types are removed.
--   * nad:support:ticket:delete (ticket + its chat) and nad:student:delete (student account), super admin only.
--
-- manual rollback:
--   delete from nad_notification_template where type = 'APPLICATION_RECEIVED';
--   delete from sys_role_menu where menu_id in (select menu_id from sys_menu where perms in ('nad:support:ticket:delete','nad:student:delete'));
--   delete from sys_menu where perms in ('nad:support:ticket:delete','nad:student:delete');
--   (removed EMAIL templates and cancelled deliveries are not restored; re-run V51/V94 inserts if needed)

update nad_notification_delivery d
  join nad_notification n on n.id = d.notification_id
   set d.status = 'FAILED', d.last_error = 'cancelled: type no longer sends email'
 where d.channel = 'EMAIL' and d.status = 'PENDING'
   and n.type in ('SCHOLARSHIP_PUBLISHED', 'UNIVERSITY_PUBLISHED', 'PROGRAM_PUBLISHED', 'ARTICLE_PUBLISHED');

delete from nad_notification_template
 where channel = 'EMAIL'
   and type in ('SCHOLARSHIP_PUBLISHED', 'UNIVERSITY_PUBLISHED', 'PROGRAM_PUBLISHED', 'ARTICLE_PUBLISHED');

delete n from nad_notification n
  join sys_user u on u.user_id = n.recipient_user_id
 where u.user_type = '00'
   and n.type in ('SCHOLARSHIP_PUBLISHED', 'SCHOLARSHIP_DEADLINE_REMINDER', 'UNIVERSITY_PUBLISHED', 'PROGRAM_PUBLISHED');

insert into nad_notification_template (type, channel, locale, subject_tpl, body_tpl, create_by, create_time) values
 ('APPLICATION_RECEIVED', 'IN_APP', 'en', null,
  'New application {{applicationRef}} for {{opportunityTitle}} is waiting for review.', 'system', now());

-- Destructive permissions go to the super admin role (3) only; other roles can be granted them in Roles.
set @nad_support := (select menu_id from sys_menu where perms = 'nad:support:ticket:view' limit 1);
insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values ('Delete tickets', @nad_support, 3, '', '', 'F', '0', '0', 'nad:support:ticket:delete', '#', 'admin', now(), 'Delete a ticket and its chat');

set @nad_applicant := (select menu_id from sys_menu where perms = 'nad:applicant:list' limit 1);
insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values ('Delete students', @nad_applicant, 20, '', '', 'F', '0', '0', 'nad:student:delete', '#', 'admin', now(), 'Delete a student account that has no applications');

insert into sys_role_menu (role_id, menu_id)
select 3, m.menu_id from sys_menu m where m.perms in ('nad:support:ticket:delete', 'nad:student:delete');
