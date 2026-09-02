-- University profile depth (docs/PLATFORM_ARCHITECTURE.md §3 C5).
-- Additive: nad_university (V9) gains the full public profile; two child tables
-- carry the list-shaped data. Still no commercial / partnership column here.

alter table nad_university
  add column name_cn               varchar(200)  null after name,
  add column type                  varchar(16)   null  comment 'PUBLIC | PRIVATE'    after country,
  add column province              varchar(120)  null  after city,
  add column founded_year          smallint      null  after province,
  add column total_students        int           null,
  add column international_students int           null,
  add column faculty_count         int           null,
  add column introduction          text          null,
  add column history               text          null,
  add column campus_info           text          null,
  add column accommodation_info    text          null,
  add column nearby_info           text          null,
  add column admissions_email      varchar(120)  null,
  add column office_phone          varchar(32)   null,
  add column banner_document_id    bigint        null  comment 'FK nad_document (Document slice)',
  add column is_recommended        tinyint(1)    not null default 0,
  add column is_featured           tinyint(1)    not null default 0,
  add column publish_status        varchar(16)   not null default 'DRAFT' comment 'DRAFT | PUBLISHED';

create index idx_university_publish on nad_university (publish_status, status);

create table nad_university_ranking (
  id            bigint       not null auto_increment,
  university_id bigint       not null,
  source        varchar(40)  not null                comment 'e.g. QS | THE | ARWU | US_NEWS | NATIONAL',
  rank_position int          not null,
  rank_year     smallint         null,
  note          varchar(200)     null,
  primary key (id),
  key idx_uni_ranking (university_id),
  constraint fk_uni_ranking_university
    foreign key (university_id) references nad_university (id) on delete cascade
) engine=innodb default charset=utf8mb4;

create table nad_university_highlight (
  id            bigint       not null auto_increment,
  university_id bigint       not null,
  kind          varchar(16)  not null                comment 'HIGHLIGHT | ADVANTAGE',
  sort_order    int          not null default 0,
  text          varchar(400) not null,
  primary key (id),
  key idx_uni_highlight (university_id, kind, sort_order),
  constraint fk_uni_highlight_university
    foreign key (university_id) references nad_university (id) on delete cascade
) engine=innodb default charset=utf8mb4;
