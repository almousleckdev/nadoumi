-- Human-facing university reference code, assigned on create:
--   NAD-UNI-NNNN  (e.g. NAD-UNI-0007)
-- Shown in the admin universities list next to the logo. The slug stays the
-- public URL identifier; this is the code staff quote to each other.
--
-- partner_status is INTERNAL (staff-only) — whether Nadoumi has a working
-- relationship with the institution. It is never returned on a public / student
-- endpoint. A full Partnership module (contracts, commission, contacts) is a
-- later step; this flag is the stopgap.
--
-- manual rollback:
--   alter table nad_university drop column partner_status;
--   alter table nad_university drop column reference_code;

alter table nad_university
  add column reference_code varchar(20) null after slug,
  add column partner_status varchar(12) not null default 'NONE'
      comment 'INTERNAL: NONE | PROSPECT | PARTNER' after is_featured;

alter table nad_university
  add unique key uk_university_reference (reference_code);

-- backfill existing rows so the list never shows a blank code
set @seq := 0;
update nad_university
set reference_code = concat('NAD-UNI-', lpad((@seq := @seq + 1), 4, '0'))
where reference_code is null
order by id;
