-- One-time cleanup for students deleted before deleting also removed their chats and tickets: those chats stayed in
-- the staff inbox as "Deleted student". From now on retiring a student removes this data itself; this clears what
-- earlier deletions left behind. Only accounts already soft-deleted students (user_type 10, del_flag 2) are touched.
--
-- manual rollback: not reversible (the rows are removed on purpose).

delete mt from nad_support_meeting mt
  join nad_support_ticket t on t.id = mt.ticket_id
  join sys_user u on u.user_id = t.opened_by_user_id
 where u.user_type = '10' and u.del_flag = '2';

delete t from nad_support_ticket t
  join sys_user u on u.user_id = t.opened_by_user_id
 where u.user_type = '10' and u.del_flag = '2';

delete c from nad_conversation c
 where c.id in (select p.conversation_id
                  from nad_conversation_participant p
                  join sys_user u on u.user_id = p.user_id
                 where u.user_type = '10' and u.del_flag = '2');

delete n from nad_notification n
  join sys_user u on u.user_id = n.recipient_user_id
 where u.user_type = '10' and u.del_flag = '2';

update nad_article_comment cm
  join sys_user u on u.user_id = cm.author_id
   set cm.status = 'DELETED', cm.deleted_time = now()
 where u.user_type = '10' and u.del_flag = '2' and cm.status = 'VISIBLE';

delete l from nad_article_like l
  join sys_user u on u.user_id = l.user_id
 where u.user_type = '10' and u.del_flag = '2';

delete l from nad_article_comment_like l
  join sys_user u on u.user_id = l.user_id
 where u.user_type = '10' and u.del_flag = '2';
