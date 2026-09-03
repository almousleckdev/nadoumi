-- Catalog imagery: logo + cover for a university, hero + cover for a scholarship.
-- URL-based for now (the admin uploads via RuoYi's /common/upload, which stores
-- the file under RUOYI_PROFILE and serves it from /profile/**). These migrate to
-- document_id references with the Document / object-storage slice.
--
-- Rollback:
--   alter table nad_university   drop column logo_image_url, drop column cover_image_url;
--   alter table nad_scholarship  drop column hero_image_url, drop column cover_image_url;
--   (then restore the previous v_scholarship_student definition from V16).

alter table nad_university
  add column logo_image_url  varchar(500) null after banner_document_id,
  add column cover_image_url varchar(500) null after logo_image_url;

alter table nad_scholarship
  add column hero_image_url  varchar(500) null after remark,
  add column cover_image_url varchar(500) null after hero_image_url;

-- the student view must expose the two public image URLs
drop view if exists v_scholarship_student;
create view v_scholarship_student as
select
  s.id, s.slug, s.title, s.summary, s.country, s.province, s.city, s.field,
  s.teaching_language, s.funding_model, s.has_stipend, s.deadline,
  s.benefits, s.requirements, s.policy,
  s.application_fee_amount, s.application_fee_currency,
  s.service_fee_amount, s.service_fee_currency, s.slots,
  s.hero_image_url, s.cover_image_url,
  s.is_featured, s.is_recommended, s.is_hot, s.published_at
from nad_scholarship s
where s.status = 'ACTIVE' and s.publish_status = 'PUBLISHED';
