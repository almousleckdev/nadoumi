-- University: public catalog entity (docs/DOMAIN_MODEL.md §5, DATABASE_DESIGN §5.3).
-- PUBLIC — no commercial column ever lives here. Partnership linkage is a
-- separate confidential table added in a later phase.

create table nad_university (
  id               bigint       not null auto_increment,
  name             varchar(200) not null,
  country          varchar(2)   not null,
  city             varchar(120)     null,
  website          varchar(255)     null,
  ranking_tier     varchar(24)      null,
  logo_document_id bigint           null,
  status           varchar(16)  not null default 'ACTIVE',
  create_by        varchar(64)  not null default '',
  create_time      datetime         null,
  update_by        varchar(64)  not null default '',
  update_time      datetime         null,
  remark           varchar(500)     null,
  primary key (id),
  unique key uk_university_name_country (name, country),
  key idx_university_country (country),
  key idx_university_status  (status)
) engine=innodb default charset=utf8mb4;

-- ---- admin menu + permissions -------------------------------------------------
set @nad_root := (select menu_id from sys_menu where path = 'nadoumi' and menu_type = 'M' limit 1);

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
 values ('Universities', @nad_root, 2, 'university', 'nadoumi/university/index', 'C', '0', '0', 'nad:university:list', 'school', 'admin', now(), 'University catalog');
set @nad_university := last_insert_id();

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark) values
 ('University query',  @nad_university, 1, '', '', 'F', '0', '0', 'nad:university:view',   '#', 'admin', now(), ''),
 ('University create', @nad_university, 2, '', '', 'F', '0', '0', 'nad:university:create', '#', 'admin', now(), ''),
 ('University edit',   @nad_university, 3, '', '', 'F', '0', '0', 'nad:university:edit',   '#', 'admin', now(), ''),
 ('University remove', @nad_university, 4, '', '', 'F', '0', '0', 'nad:university:remove', '#', 'admin', now(), ''),
 ('University export', @nad_university, 5, '', '', 'F', '0', '0', 'nad:university:export', '#', 'admin', now(), '');

-- nadoumi_super_admin (role 3): every menu.
insert into sys_role_menu (role_id, menu_id)
 select 3, m.menu_id from sys_menu m
 where m.menu_id = @nad_university or m.perms like 'nad:university:%';

-- ops_manager + partnerships_manager: full university management.
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id = @nad_university or m.perms like 'nad:university:%'
 where r.role_key in ('ops_manager', 'partnerships_manager');

-- case_officer / content_editor / read_only_analyst / support_agent: read only.
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id = @nad_university
                 or m.perms in ('nad:university:view', 'nad:university:list')
 where r.role_key in ('case_officer', 'content_editor', 'read_only_analyst', 'support_agent');
