-- Richer "Contact us" form: split name, optional phone, and a subject category.
-- `name` stays as the display value the triage screen shows (the service composes
-- it from first + last), so it becomes nullable rather than dropped.

alter table nad_contact_inquiry
  add column first_name varchar(80)  null after id,
  add column last_name  varchar(80)  null after first_name,
  add column phone      varchar(40)  null after email,
  add column category   varchar(40)  null after subject,
  modify column name    varchar(160) null;
