-- A required document can apply only to some applicants: a guarantee letter for a minor, a transfer certificate for a
-- student already in China, a study plan for master's and doctoral applicants. applies_when holds one RequirementCondition
-- code (null = always); a requirement that does not apply to a student is left off their checklist.
--
-- manual rollback:
--   alter table nad_document_requirement drop column applies_when;
--   alter table nad_scholarship_document_requirement drop column applies_when;

alter table nad_document_requirement add column applies_when varchar(32) null after waivable;
alter table nad_scholarship_document_requirement add column applies_when varchar(32) null after note;
