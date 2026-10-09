-- Removes student accounts that no longer have any applicant. They are what an applicant deletion left behind before
-- deleting an applicant also closed its student login: the applicant was gone from the admin, but the account could
-- still sign in and still held its username and email. Registration always gives a student an applicant, so a live
-- student with no access grant at all is an orphan. Their chats, tickets, notifications, comments and likes go
-- first; the account row is deleted unless something still references it, in which case it is anonymised.
--
-- manual rollback: not reversible (the rows are removed on purpose).

create temporary table tmp_orphan_students as
select u.user_id
  from sys_user u
 where u.user_type = '10' and u.del_flag = '0'
   and not exists (select 1 from nad_user_applicant_access g where g.user_id = u.user_id);

delete mt from nad_support_meeting mt
  join nad_support_ticket t on t.id = mt.ticket_id
  join tmp_orphan_students o on o.user_id = t.opened_by_user_id;

delete t from nad_support_ticket t
  join tmp_orphan_students o on o.user_id = t.opened_by_user_id;

delete c from nad_conversation c
 where c.id in (select p.conversation_id
                  from nad_conversation_participant p
                  join tmp_orphan_students o on o.user_id = p.user_id);

delete n from nad_notification n
  join tmp_orphan_students o on o.user_id = n.recipient_user_id;

update nad_article_comment cm
  join tmp_orphan_students o on o.user_id = cm.author_id
   set cm.status = 'DELETED', cm.deleted_time = now()
 where cm.status = 'VISIBLE';

delete l from nad_article_like l
  join tmp_orphan_students o on o.user_id = l.user_id;

delete l from nad_article_comment_like l
  join tmp_orphan_students o on o.user_id = l.user_id;

delete r from sys_user_role r
  join tmp_orphan_students o on o.user_id = r.user_id;

delete u from sys_user u
  join tmp_orphan_students o on o.user_id = u.user_id
 where not exists (select 1 from nad_message m where m.sender_user_id = u.user_id)
   and not exists (select 1 from nad_support_ticket t where t.opened_by_user_id = u.user_id)
   and not exists (select 1 from nad_support_ticket_event e where e.actor_user_id = u.user_id)
   and not exists (select 1 from nad_user_applicant_access a
                    where a.granted_by_user_id = u.user_id or a.revoked_by_user_id = u.user_id);

update sys_user u
  join tmp_orphan_students o on o.user_id = u.user_id
   set u.del_flag = '2', u.status = '1', u.user_name = concat('deleted_', u.user_id), u.nick_name = 'Deleted user',
       u.email = '', u.phonenumber = '', u.avatar = ''
 where u.del_flag = '0';

drop temporary table tmp_orphan_students;
