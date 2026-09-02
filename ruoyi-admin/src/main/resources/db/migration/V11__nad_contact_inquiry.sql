-- Inbound "Contact us" messages from the public website
-- (docs/PLATFORM_ARCHITECTURE.md §7 Content). One row per submission; an admin
-- triage screen arrives with the Content/CRM slice. No menu / permission seed
-- yet — nothing reads this table server-side except the public write path.

create table nad_contact_inquiry (
  id          bigint       not null auto_increment,
  name        varchar(120) not null,
  email       varchar(190) not null,
  subject     varchar(160)     null,
  message     varchar(4000) not null,
  status      varchar(16)  not null default 'NEW' comment 'NEW | READ | RESPONDED | ARCHIVED',
  source      varchar(24)  not null default 'WEBSITE',
  locale      varchar(12)      null,
  ip_address  varchar(64)      null,
  user_agent  varchar(400)     null,
  created_at  datetime     not null default current_timestamp,
  handled_by  bigint           null comment 'FK sys_user (nullable; set on triage)',
  handled_at  datetime         null,
  primary key (id),
  key idx_contact_inquiry_triage (status, created_at)
) engine=innodb default charset=utf8mb4 comment='Public website contact submissions';
