-- Applicants get an unguessable public id. Staff screens and the staff API identify an applicant by this UUID
-- instead of the sequential row id, so ids 1, 2, 3 cannot be walked. The numeric id stays the internal key.
--
-- manual rollback:
--   alter table nad_applicant drop index uk_applicant_public_id, drop column public_id;

alter table nad_applicant add column public_id char(36) null after id;
update nad_applicant set public_id = uuid() where public_id is null;
alter table nad_applicant
  modify column public_id char(36) not null default (uuid()),
  add unique key uk_applicant_public_id (public_id);
