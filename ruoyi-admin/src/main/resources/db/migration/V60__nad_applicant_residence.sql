-- Where the applicant is now. One row per applicant. `in_china` selects the branch: the China columns
-- (current education level, school, visa type and expiry) apply only when it is 1.
-- Rollback (manual): drop table nad_applicant_residence;

create table nad_applicant_residence (
  id                   bigint       not null auto_increment,
  applicant_id         bigint       not null,
  in_china             tinyint(1)   not null,
  country              varchar(2)   not null,
  city                 varchar(80)  not null,
  address              varchar(255)     null,
  china_education_level varchar(16)     null comment 'EducationLevel currently studied in China',
  china_school         varchar(200)     null,
  visa_type            varchar(8)       null comment 'ChinaVisaType',
  visa_expiry_date     date             null,
  create_by            varchar(64)  not null default '',
  create_time          datetime         null,
  update_by            varchar(64)  not null default '',
  update_time          datetime         null,
  primary key (id),
  unique key uk_residence_applicant (applicant_id),
  key idx_residence_visa_expiry (visa_expiry_date),
  constraint fk_residence_applicant foreign key (applicant_id) references nad_applicant (id) on delete restrict
) engine=innodb default charset=utf8mb4;
