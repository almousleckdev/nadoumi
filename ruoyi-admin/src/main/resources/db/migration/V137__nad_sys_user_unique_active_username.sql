-- V137: one ACTIVE account per user name, enforced by the database.
--
-- A plain unique index on user_name would also forbid re-using the name of a soft-deleted account (del_flag = '2'), which
-- RuoYi allows. The generated column is the user name while the row is active and NULL otherwise, and NULLs never collide
-- in a unique index, so only active rows are constrained. The comparison uses the column's own collation, the same one the
-- login lookup uses, so "Admin" and "admin" count as the same name here too.
--
-- Depends on V136 having removed the existing duplicates. A second active row now fails with a duplicate-key error.
--
-- Manual rollback:
--   alter table sys_user drop index uk_sys_user_active_name, drop column active_user_name;

alter table sys_user
    add column active_user_name varchar(30)
        generated always as (case when del_flag = '0' then user_name end) virtual comment 'user_name while active, else NULL',
    add unique index uk_sys_user_active_name (active_user_name);
