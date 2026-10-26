-- The admission letter and the JW202 form the university issues once admission is confirmed. Staff upload them on the
-- application; the student can open them only after the Nadoumi service fee is confirmed (enforced on the server, not by
-- hiding a button). A re-upload adds a row; the newest row of a kind is the current one, older ones stay as history.
-- The file lives in media storage (ADMISSION_DOCUMENT / JW202 categories); this row only points at it.
-- Manual rollback: drop table nad_application_letter;

create table nad_application_letter (
  id                 bigint        not null auto_increment,
  application_id     bigint        not null,
  kind               varchar(24)   not null comment 'ADMISSION_LETTER | JW202',
  media_id           bigint        not null,
  original_filename  varchar(255)  not null,
  uploaded_by        bigint        not null,
  create_by          varchar(64)   default '',
  create_time        datetime      default null,
  primary key (id),
  key idx_letter_application (application_id, kind, id),
  constraint fk_letter_application foreign key (application_id) references nad_application (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 comment='Admission letter / JW202 of an application, released after the service fee';
