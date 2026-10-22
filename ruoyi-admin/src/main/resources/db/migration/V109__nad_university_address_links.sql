-- Street address and official / social channels shown on the public university page.
--
-- manual rollback:
--   drop table nad_university_link;
--   alter table nad_university drop column address;

alter table nad_university
  add column address varchar(300) null comment 'street address of the main campus' after nearby_info;

create table nad_university_link (
  id            bigint        not null auto_increment,
  university_id bigint        not null,
  kind          varchar(16)   not null comment 'ADMISSIONS | VIRTUAL_TOUR | WIKIPEDIA | FACEBOOK | X | INSTAGRAM | YOUTUBE | LINKEDIN | TIKTOK | WECHAT | WEIBO',
  link_value    varchar(500)  not null comment 'web URL, or the account id for WECHAT',
  sort_order    int           not null default 0,
  primary key (id),
  unique key uk_university_link (university_id, kind),
  constraint fk_university_link_university foreign key (university_id)
    references nad_university (id) on delete cascade
) engine=innodb default charset=utf8mb4;
