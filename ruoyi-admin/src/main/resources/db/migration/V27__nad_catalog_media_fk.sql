-- Catalog media FKs: point universities, the university gallery, scholarships and
-- programmes at nad_media_asset (docs/superpowers/specs/2026-09-03-media-storage-
-- and-application-engine-design.md section I.9). The legacy *_image_url columns
-- from V21 stay as a fallback and are removed in a later release. Every FK is
-- ON DELETE SET NULL so a purged media row simply detaches from its owner.
--
-- The student-safe v_scholarship_student view is recreated to also project
-- hero_media_id / cover_media_id. It still exposes no confidential column
-- (university, partnership, commission) -- SECURITY.md scholarship confidentiality.
--
-- manual rollback:
--   alter table nad_university        drop foreign key fk_university_logo_media,
--                                     drop foreign key fk_university_banner_media,
--                                     drop column logo_media_id, drop column banner_media_id;
--   alter table nad_university_gallery drop foreign key fk_gallery_media,
--                                     drop column media_id;
--   alter table nad_scholarship       drop foreign key fk_scholarship_hero_media,
--                                     drop foreign key fk_scholarship_cover_media,
--                                     drop column hero_media_id, drop column cover_media_id;
--   alter table nad_program           drop foreign key fk_program_image_media,
--                                     drop column image_media_id;
--   then recreate v_scholarship_student from V25__nad_scholarship_reference.sql.

alter table nad_university
  add column logo_media_id   bigint null after cover_image_url,
  add column banner_media_id bigint null after logo_media_id,
  add constraint fk_university_logo_media foreign key (logo_media_id)
    references nad_media_asset (id) on delete set null,
  add constraint fk_university_banner_media foreign key (banner_media_id)
    references nad_media_asset (id) on delete set null;

alter table nad_university_gallery
  add column media_id bigint null after image_url,
  add constraint fk_gallery_media foreign key (media_id)
    references nad_media_asset (id) on delete set null;

alter table nad_scholarship
  add column hero_media_id  bigint null after cover_image_url,
  add column cover_media_id bigint null after hero_media_id,
  add constraint fk_scholarship_hero_media foreign key (hero_media_id)
    references nad_media_asset (id) on delete set null,
  add constraint fk_scholarship_cover_media foreign key (cover_media_id)
    references nad_media_asset (id) on delete set null;

alter table nad_program
  add column image_media_id bigint null after summary,
  add constraint fk_program_image_media foreign key (image_media_id)
    references nad_media_asset (id) on delete set null;

drop view if exists v_scholarship_student;
create view v_scholarship_student as
select
  s.id, s.slug, s.reference_code, s.title, s.summary, s.country, s.province, s.city, s.field,
  s.teaching_language, s.funding_model, s.has_stipend, s.deadline,
  s.benefits, s.requirements, s.policy, s.renewal_conditions,
  s.non_degree_duration, s.study_duration_months,
  s.application_channel, s.agency_number,
  s.requires_financial_proof, s.requires_foundation_year,
  s.application_fee_amount, s.application_fee_currency,
  s.service_fee_amount, s.service_fee_currency, s.slots,
  s.hero_image_url, s.cover_image_url,
  s.hero_media_id, s.cover_media_id,
  s.is_featured, s.is_recommended, s.is_hot, s.published_at
from nad_scholarship s
where s.status = 'ACTIVE' and s.publish_status = 'PUBLISHED';
