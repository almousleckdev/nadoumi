-- Applicant: the person an application is about (docs/DOMAIN_MODEL.md §4).
-- dob / nationality / passport_no are PII (docs/SECURITY.md §6); passport_no is not unique.

create table nad_applicant (
  id          bigint       not null auto_increment,
  given_name  varchar(100) not null,
  family_name varchar(100) not null,
  dob         date             null,
  nationality varchar(2)       null,
  passport_no varchar(64)      null,
  email       varchar(120)     null,
  phone       varchar(32)      null,
  status      varchar(16)  not null default 'DRAFT',
  create_by   varchar(64)  not null default '',
  create_time datetime         null,
  update_by   varchar(64)  not null default '',
  update_time datetime         null,
  remark      varchar(500)     null,
  primary key (id),
  key idx_applicant_name         (family_name, given_name),
  key idx_applicant_status       (status),
  key idx_applicant_nat_passport (nationality, passport_no)
) engine=innodb default charset=utf8mb4;

alter table nad_user_applicant_access
  add constraint fk_uaa_applicant foreign key (applicant_id) references nad_applicant (id) on delete restrict;

create table nad_applicant_education (
  id           bigint       not null auto_increment,
  applicant_id bigint       not null,
  institution  varchar(200) not null,
  level        varchar(32)      null,
  field        varchar(120)     null,
  gpa          decimal(6,3)     null,
  gpa_scale    decimal(6,3)     null,
  start_date   date             null,
  end_date     date             null,
  create_by    varchar(64)  not null default '',
  create_time  datetime         null,
  update_by    varchar(64)  not null default '',
  update_time  datetime         null,
  primary key (id),
  key idx_edu_applicant (applicant_id),
  constraint fk_edu_applicant foreign key (applicant_id) references nad_applicant (id) on delete restrict
) engine=innodb default charset=utf8mb4;

create table nad_applicant_test_score (
  id              bigint       not null auto_increment,
  applicant_id    bigint       not null,
  test_type       varchar(24)  not null,
  score           varchar(32)  not null,
  sub_scores_json json             null,
  taken_on        date             null,
  expires_on      date             null,
  create_by       varchar(64)  not null default '',
  create_time     datetime         null,
  update_by       varchar(64)  not null default '',
  update_time     datetime         null,
  primary key (id),
  key idx_score_applicant (applicant_id, test_type),
  constraint fk_score_applicant foreign key (applicant_id) references nad_applicant (id) on delete restrict
) engine=innodb default charset=utf8mb4;

create table nad_applicant_contact (
  id           bigint       not null auto_increment,
  applicant_id bigint       not null,
  relation     varchar(16)  not null,
  name         varchar(150) not null,
  email        varchar(120)     null,
  phone        varchar(32)      null,
  create_by    varchar(64)  not null default '',
  create_time  datetime         null,
  update_by    varchar(64)  not null default '',
  update_time  datetime         null,
  primary key (id),
  key idx_contact_applicant (applicant_id),
  constraint fk_contact_applicant foreign key (applicant_id) references nad_applicant (id) on delete restrict
) engine=innodb default charset=utf8mb4;
