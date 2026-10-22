-- What the Nadoumi application form asks about a person beyond identity, kept on the applicant so it is asked once:
--   nad_applicant_personal       marital status, religion, place of birth, official language, permanent address (1:1)
--   nad_applicant_family_member  father, mother, spouse, siblings and other relatives (many)
--   nad_applicant_person         the financial supporter and the emergency contact in China (one of each)
-- A family member who has died is kept with a note; the parents-or-two-relatives completeness rule is evaluated in
-- the service (FamilyRules), not in the database.
--
-- manual rollback:
--   drop table if exists nad_applicant_person;
--   drop table if exists nad_applicant_family_member;
--   drop table if exists nad_applicant_personal;

create table nad_applicant_personal (
  id                  bigint       not null auto_increment,
  applicant_id        bigint       not null,
  marital_status      varchar(16)      null,
  religion            varchar(60)      null,
  place_of_birth      varchar(120)     null,
  official_language   varchar(60)      null,
  permanent_address   varchar(300)     null,
  permanent_postcode  varchar(20)      null,
  create_by           varchar(64)  not null default '',
  create_time         datetime         null,
  update_by           varchar(64)  not null default '',
  update_time         datetime         null,
  primary key (id),
  unique key uk_personal_applicant (applicant_id),
  constraint fk_personal_applicant foreign key (applicant_id) references nad_applicant (id) on delete restrict
) engine=innodb default charset=utf8mb4;

create table nad_applicant_family_member (
  id              bigint       not null auto_increment,
  applicant_id    bigint       not null,
  relation        varchar(16)  not null,
  full_name       varchar(150) not null,
  date_of_birth   date             null,
  nationality     varchar(60)      null,
  deceased        tinyint(1)   not null default 0,
  note            varchar(300)     null,
  company         varchar(200)     null,
  position        varchar(150)     null,
  company_address varchar(300)     null,
  phone           varchar(32)      null,
  email           varchar(120)     null,
  create_by       varchar(64)  not null default '',
  create_time     datetime         null,
  update_by       varchar(64)  not null default '',
  update_time     datetime         null,
  primary key (id),
  key idx_family_applicant (applicant_id),
  constraint fk_family_applicant foreign key (applicant_id) references nad_applicant (id) on delete restrict
) engine=innodb default charset=utf8mb4;

create table nad_applicant_person (
  id              bigint       not null auto_increment,
  applicant_id    bigint       not null,
  role            varchar(20)  not null,
  full_name       varchar(150) not null,
  relationship    varchar(80)  not null,
  gender          varchar(12)      null,
  email           varchar(120)     null,
  phone           varchar(32)  not null,
  position        varchar(150)     null,
  company         varchar(200)     null,
  company_address varchar(300)     null,
  home_address    varchar(300)     null,
  create_by       varchar(64)  not null default '',
  create_time     datetime         null,
  update_by       varchar(64)  not null default '',
  update_time     datetime         null,
  primary key (id),
  unique key uk_person_applicant_role (applicant_id, role),
  constraint fk_person_applicant foreign key (applicant_id) references nad_applicant (id) on delete restrict
) engine=innodb default charset=utf8mb4;
