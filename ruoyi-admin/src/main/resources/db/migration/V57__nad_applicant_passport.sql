-- Applicant passport, onboarding v2 slice 2.
--   passport_no already exists on nad_applicant.
--   passport_given_name / passport_family_name / passport_dob: the holder data as read from (or typed
--     off) the passport; compared with the profile on save and again at onboarding completion.
--   passport_read_method: MRZ (read in the browser) or MANUAL; passport_data_edited: the student changed
--     what the reader produced. Both are shown to staff reviewing the document.
--   passport_media_id: the scanned passport, a PROTECTED media asset (never a public URL).
-- Rollback (manual): alter table nad_applicant drop foreign key fk_applicant_passport_media,
--   drop column passport_data_edited, drop column passport_read_method, drop column passport_expiry_date,
--   drop column passport_issue_date, drop column passport_dob, drop column passport_family_name,
--   drop column passport_given_name, drop column passport_media_id;

alter table nad_applicant
  add column passport_media_id    bigint       null after passport_no,
  add column passport_given_name  varchar(100) null after passport_media_id,
  add column passport_family_name varchar(100) null after passport_given_name,
  add column passport_dob         date         null after passport_family_name,
  add column passport_issue_date  date         null after passport_dob,
  add column passport_expiry_date date         null after passport_issue_date,
  add column passport_read_method varchar(12)  null after passport_expiry_date,
  add column passport_data_edited tinyint(1)   not null default 0 after passport_read_method,
  add constraint fk_applicant_passport_media foreign key (passport_media_id)
    references nad_media_asset (id) on delete set null;
