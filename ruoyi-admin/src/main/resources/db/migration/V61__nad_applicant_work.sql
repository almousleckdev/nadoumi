-- Work experience: repeatable, optional. Work done in China also records the work visa.
-- Rollback (manual): drop table nad_applicant_work;

create table nad_applicant_work (
  id               bigint       not null auto_increment,
  applicant_id     bigint       not null,
  employer         varchar(200) not null,
  job_title        varchar(150) not null,
  employment_type  varchar(12)      null comment 'FULL_TIME | PART_TIME | INTERNSHIP | FREELANCE',
  country          varchar(2)   not null,
  city             varchar(80)      null,
  start_date       date         not null,
  end_date         date             null,
  is_current       tinyint(1)   not null default 0,
  description      varchar(1000)    null,
  work_visa_type   varchar(8)       null comment 'ChinaVisaType; only for work done in China',
  work_visa_expiry date             null,
  create_by        varchar(64)  not null default '',
  create_time      datetime         null,
  update_by        varchar(64)  not null default '',
  update_time      datetime         null,
  primary key (id),
  key idx_work_applicant (applicant_id),
  constraint fk_work_applicant foreign key (applicant_id) references nad_applicant (id) on delete restrict
) engine=innodb default charset=utf8mb4;
