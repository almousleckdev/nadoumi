-- A curated, editorial "show this university in the public Partners showcase"
-- flag. This is DELIBERATELY SEPARATE from the confidential internal
-- nad_university.partner_status (NONE / PROSPECT / PARTNER, staff-only, never in
-- a public response). Staff opt a university into the public showcase on purpose;
-- partner_status is never exposed and never drives public output.
--
-- manual rollback:
--   alter table nad_university drop column public_partner;

alter table nad_university
  add column public_partner tinyint(1) not null default 0
    comment 'Curated: feature in the public Partners showcase. NOT the confidential partner_status.'
    after is_featured;
