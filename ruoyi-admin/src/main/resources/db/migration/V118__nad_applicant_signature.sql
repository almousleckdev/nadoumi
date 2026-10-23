-- The applicant's handwritten signature, drawn or uploaded once and printed on the application form. The image is a
-- SENSITIVE media asset (APPLICANT_SIGNATURE); the applicant row only points at it. It lives on nad_applicant_personal
-- (one row per applicant), together with the moment it was given.
--
-- manual rollback:
--   alter table nad_applicant_personal drop foreign key fk_personal_signature_media,
--       drop column signature_media_id, drop column signature_at;

alter table nad_applicant_personal
  add column signature_media_id bigint   null after permanent_postcode,
  add column signature_at       datetime null after signature_media_id,
  add constraint fk_personal_signature_media foreign key (signature_media_id) references nad_media_asset (id) on delete set null;
