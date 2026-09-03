-- Human-facing scholarship reference code, assigned on first publish:
--   NAC-<year>-NNNN  (e.g. NAC-2026-0007)
-- Shown in the public scholarship table and on the detail page. The slug stays
-- the URL identifier; this is the code staff and students quote to each other.
--
-- Rollback: alter table nad_scholarship drop column reference_code;

alter table nad_scholarship
  add column reference_code varchar(20) null after slug;
alter table nad_scholarship
  add unique key uk_scholarship_reference (reference_code);

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
  s.is_featured, s.is_recommended, s.is_hot, s.published_at
from nad_scholarship s
where s.status = 'ACTIVE' and s.publish_status = 'PUBLISHED';
