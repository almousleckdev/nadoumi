-- Applicant profile photo -> nad_media_asset. The photo is a PROTECTED asset,
-- served through a short-lived signed URL and never a public URL
-- (docs/superpowers/specs/2026-09-03-media-storage-and-application-engine-design.md
-- section I.9). ON DELETE SET NULL so a purged media row detaches cleanly.
--
-- manual rollback:
--   alter table nad_applicant drop foreign key fk_applicant_photo_media,
--                             drop column photo_media_id;

alter table nad_applicant
  add column photo_media_id bigint null after phone,
  add constraint fk_applicant_photo_media foreign key (photo_media_id)
    references nad_media_asset (id) on delete set null;
