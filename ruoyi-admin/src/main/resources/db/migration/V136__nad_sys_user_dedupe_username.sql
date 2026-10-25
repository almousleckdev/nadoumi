-- V136: data fix that must run before V137 adds the unique index.
--
-- sys_user.user_name had no unique constraint, so the new-hire form (which created a staff account without checking the
-- name) could leave two ACTIVE rows (del_flag = '0') with the same user name. selectUserByUserName then returned two rows and
-- login failed with "Expected one result (or null) to be returned by selectOne(), but found: 2".
--
-- Both accounts may belong to real people, so nothing is deleted. For every user name that is active more than once, ONE row
-- keeps the name (the one that logged in most recently; accounts that never logged in rank last; the lowest user_id wins a
-- tie) and each other row is RENAMED to <name>_<user_id> (cut to fit the 30 character column). Their password, roles and
-- employee record are untouched; the person signs in with the new name until an administrator gives them a better one. The
-- remark holds the old name. This is a no-op on a database without duplicates.
--
-- Manual rollback (only possible before V137 is applied, or after dropping its index): for each row with
-- update_by = 'flyway-V136', set user_name back to the name written at the end of its remark.

update sys_user d
join (
    select user_name,
           cast(substring_index(group_concat(user_id order by (login_date is null), login_date desc, user_id asc), ',', 1) as unsigned) as keep_id
    from sys_user
    where del_flag = '0'
    group by user_name
    having count(*) > 1
) k on k.user_name = d.user_name
set d.remark = concat('duplicate user name renamed by V136, original name: ', d.user_name),
    d.user_name = concat(left(d.user_name, 30 - char_length(cast(d.user_id as char)) - 1), '_', d.user_id),
    d.update_by = 'flyway-V136',
    d.update_time = now()
where d.del_flag = '0' and d.user_id <> k.keep_id;
