-- Study interests, one row per applicant, plus the multi-select answers (fields of study, preferred
-- cities) in one child table keyed by kind so staff can filter applicants by a chosen city or field.
-- Rollback (manual): drop table nad_applicant_interest_choice; drop table nad_applicant_interest;

create table nad_applicant_interest (
  id                   bigint      not null auto_increment,
  applicant_id         bigint      not null,
  desired_level        varchar(16) not null comment 'StudyLevel',
  scholarship_interest varchar(12)     null comment 'REQUIRED | PREFERRED | NOT_NEEDED',
  intake_year          smallint        null,
  intake_term          varchar(8)      null comment 'SPRING | SUMMER | FALL | WINTER',
  teaching_language    varchar(8)      null,
  notes                varchar(500)    null,
  create_by            varchar(64) not null default '',
  create_time          datetime        null,
  update_by            varchar(64) not null default '',
  update_time          datetime        null,
  primary key (id),
  unique key uk_interest_applicant (applicant_id),
  constraint fk_interest_applicant foreign key (applicant_id) references nad_applicant (id) on delete restrict
) engine=innodb default charset=utf8mb4;

create table nad_applicant_interest_choice (
  id           bigint      not null auto_increment,
  applicant_id bigint      not null,
  kind         varchar(8)  not null comment 'FIELD | CITY',
  value        varchar(80) not null,
  primary key (id),
  unique key uk_interest_choice (applicant_id, kind, value),
  key idx_interest_choice_lookup (kind, value),
  constraint fk_interest_choice_applicant foreign key (applicant_id) references nad_applicant (id) on delete restrict
) engine=innodb default charset=utf8mb4;
