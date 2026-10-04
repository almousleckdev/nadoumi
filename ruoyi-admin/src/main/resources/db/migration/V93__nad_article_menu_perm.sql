-- Content domain: staff News menu + permissions (mirrors the V88 pattern). DML only; DDL is V92.
--
-- manual rollback:
--   delete from sys_role_menu where menu_id in (select menu_id from sys_menu where perms like 'nad:article:%' or path = 'news');
--   delete from sys_menu where perms like 'nad:article:%' or path = 'news';

set @nad_root := (select menu_id from sys_menu where path = 'nadoumi' and menu_type = 'M' limit 1);

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
 values ('News', @nad_root, 11, 'news', 'nadoumi/news/index', 'C', '0', '0', 'nad:article:list', 'documentation', 'admin', now(), 'News articles and comment moderation');
set @nad_news := last_insert_id();

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark) values
 ('Article query',   @nad_news, 1, '', '', 'F', '0', '0', 'nad:article:view',           '#', 'admin', now(), ''),
 ('Article create',  @nad_news, 2, '', '', 'F', '0', '0', 'nad:article:create',         '#', 'admin', now(), ''),
 ('Article edit',    @nad_news, 3, '', '', 'F', '0', '0', 'nad:article:edit',           '#', 'admin', now(), ''),
 ('Article publish', @nad_news, 4, '', '', 'F', '0', '0', 'nad:article:publish',        '#', 'admin', now(), 'Publish and unpublish'),
 ('Article remove',  @nad_news, 5, '', '', 'F', '0', '0', 'nad:article:remove',         '#', 'admin', now(), ''),
 ('Comment remove',  @nad_news, 6, '', '', 'F', '0', '0', 'nad:article:comment:remove', '#', 'admin', now(), 'Delete student comments');

-- nadoumi_super_admin (role 3): every News menu.
insert into sys_role_menu (role_id, menu_id)
 select 3, m.menu_id from sys_menu m
 where m.menu_id = @nad_news or m.perms like 'nad:article:%';

-- ops_manager: full control.
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id = @nad_news or m.perms like 'nad:article:%'
 where r.role_key in ('ops_manager');

-- read_only_analyst: view only.
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id = @nad_news or m.perms in ('nad:article:view')
 where r.role_key in ('read_only_analyst');
