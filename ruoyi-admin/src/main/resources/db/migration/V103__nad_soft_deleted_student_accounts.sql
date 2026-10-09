-- Removes student accounts that were deleted the old way. Before deleting a person removed them completely, RuoYi's
-- soft delete left the row (del_flag = '2') behind, sometimes still carrying the email, and student chats and tickets
-- stayed in the staff inbox. This clears those rows and what hung off them.
--
-- Scope is deliberately narrow: only sys_user rows with user_type = '10' (students) AND del_flag = '2'. Staff, the
-- break-glass administrator, live students and the catalog (universities, programmes, scholarships, content) are never
-- selected: nothing here reads or writes them, and no catalog table references sys_user.
--
-- A row is deleted outright unless another record still references it, in which case it is anonymised instead.
--
-- manual rollback: not reversible (the rows are removed on purpose).

create temporary table tmp_deleted_students as
select u.user_id
  from sys_user u
 where u.user_type = '10' and u.del_flag = '2';

delete mt from nad_support_meeting mt
  join nad_support_ticket t on t.id = mt.ticket_id
  join tmp_deleted_students d on d.user_id = t.opened_by_user_id;

delete t from nad_support_ticket t
  join tmp_deleted_students d on d.user_id = t.opened_by_user_id;

delete c from nad_conversation c
 where c.id in (select p.conversation_id
                  from nad_conversation_participant p
                  join tmp_deleted_students d on d.user_id = p.user_id);

delete n from nad_notification n
  join tmp_deleted_students d on d.user_id = n.recipient_user_id;

update nad_article_comment cm
  join tmp_deleted_students d on d.user_id = cm.author_id
   set cm.status = 'DELETED', cm.deleted_time = now()
 where cm.status = 'VISIBLE';

delete l from nad_article_like l
  join tmp_deleted_students d on d.user_id = l.user_id;

delete l from nad_article_comment_like l
  join tmp_deleted_students d on d.user_id = l.user_id;

delete r from sys_user_role r
  join tmp_deleted_students d on d.user_id = r.user_id;

delete p from sys_user_post p
  join tmp_deleted_students d on d.user_id = p.user_id;

delete u from sys_user u
  join tmp_deleted_students d on d.user_id = u.user_id
 where u.user_type = '10' and u.del_flag = '2'
   and not exists (select 1 from nad_message m where m.sender_user_id = u.user_id)
   and not exists (select 1 from nad_support_ticket t where t.opened_by_user_id = u.user_id)
   and not exists (select 1 from nad_support_ticket_event e where e.actor_user_id = u.user_id)
   and not exists (select 1 from nad_user_applicant_access a
                    where a.user_id = u.user_id or a.granted_by_user_id = u.user_id or a.revoked_by_user_id = u.user_id);

-- whatever is still referenced keeps no personal data
update sys_user u
  join tmp_deleted_students d on d.user_id = u.user_id
   set u.user_name = concat('deleted_', u.user_id), u.nick_name = 'Deleted user',
       u.email = '', u.phonenumber = '', u.avatar = '', u.status = '1'
 where u.user_type = '10' and u.del_flag = '2';

drop temporary table tmp_deleted_students;
