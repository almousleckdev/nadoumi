-- V136: data fix that must run before V137 adds the unique index.
--
-- sys_user.user_name had no unique constraint, so a double submit or two concurrent creations could leave two ACTIVE
-- rows (del_flag = '0') with the same user name. selectUserByUserName then returned two rows and login failed with
-- "Expected one result (or null) to be returned by selectOne(), but found: 2".
--
-- For every user name that is active more than once, KEEP one row and soft-delete the others (del_flag = '2'; no row is
-- removed, so nothing referencing it breaks). The kept row is the one that logged in most recently; accounts that never
-- logged in rank last, and the lowest user_id wins a tie. Each soft-deleted row says which one was kept in its remark.
-- This is a no-op on a database without duplicates.
--
-- Manual rollback (does not recreate the failed logins, and must be done before V137 is applied or after dropping its index):
--   update sys_user set del_flag = '0', remark = null where update_by = 'flyway-V136';

update sys_user d
join (
    select user_name,
           cast(substring_index(group_concat(user_id order by (login_date is null), login_date desc, user_id asc), ',', 1) as unsigned) as keep_id
    from sys_user
    where del_flag = '0'
    group by user_name
    having count(*) > 1
) k on k.user_name = d.user_name
set d.del_flag = '2',
    d.update_by = 'flyway-V136',
    d.update_time = now(),
    d.remark = concat('duplicate user name, soft-deleted by V136; kept user_id ', k.keep_id)
where d.del_flag = '0' and d.user_id <> k.keep_id;
