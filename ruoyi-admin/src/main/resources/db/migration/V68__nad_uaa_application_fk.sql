-- nad_user_applicant_access.application_id has existed since V3 with no FK (there
-- was nothing to point at). nad_application now exists — close the gap per
-- database.md's table-design checklist (explicit ON DELETE behaviour). RESTRICT
-- matches fk_uaa_user / fk_uaa_grantor on the same table. No backfill needed: the
-- column has been NULL for every row until now.
-- Rollback (manual): alter table nad_user_applicant_access drop foreign key fk_uaa_application;

alter table nad_user_applicant_access
  add constraint fk_uaa_application foreign key (application_id) references nad_application (id) on delete restrict;
