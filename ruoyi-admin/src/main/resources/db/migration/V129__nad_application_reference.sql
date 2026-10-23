-- A readable reference for every application: NAD + the date it was started (yyyyMMdd, China time) + that day's running
-- number, for example NAD20261023001, NAD20261023002. Students, staff, emails and PDFs use it instead of the database id.
-- nad_application_reference_counter hands out the day's next number atomically (one row per day).
--
-- manual rollback: alter table nad_application drop key uk_application_reference, drop column reference;
--   drop table nad_application_reference_counter;

alter table nad_application
  add column reference varchar(24) null after id,
  add unique key uk_application_reference (reference);

create table nad_application_reference_counter (
  ref_date date not null,
  last_seq int  not null,
  primary key (ref_date)
) engine=innodb default charset=utf8mb4;
