-- Onboarding v2, slice 3: education history detail. Adds where the study took place, the qualification
-- title and whether the student is still studying (an open-ended record). Existing rows stay valid:
-- every new column is nullable or defaults to 0. `level` keeps its varchar(32) and is now validated
-- on write against the EducationLevel codes.
-- Rollback (manual): alter table nad_applicant_education drop column is_current,
--   drop column qualification, drop column city, drop column country;

alter table nad_applicant_education
  add column country       varchar(2)   null after institution,
  add column city          varchar(80)  null after country,
  add column qualification varchar(200) null after level,
  add column is_current    tinyint(1)   not null default 0 after end_date;
