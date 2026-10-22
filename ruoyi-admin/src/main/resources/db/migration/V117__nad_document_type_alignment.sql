-- One vocabulary of document types. A scholarship's required documents and a student's uploaded documents are
-- matched by doc_type, so they must use the same codes (the sys_dict nad_document_type values). Scholarships used
-- their own short codes; they are renamed here, and the four types the dictionary lacked are added.
--
--   DEGREE         -> DEGREE_CERTIFICATE
--   LANGUAGE_CERT  -> LANGUAGE_TEST_REPORT
--   BANK_STATEMENT -> FINANCIAL_PROOF
--   added to the dictionary: STUDY_PLAN, PHYSICAL_EXAM, PHOTO, CV
--
-- manual rollback: reverse the three renames below and delete the four dictionary rows.

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, remark)
select * from (
  select 11 as dict_sort, 'Study plan' as dict_label, 'STUDY_PLAN' as dict_value, 'nad_document_type' as dict_type, 'N' as is_default, '0' as status, 'admin' as create_by, now() as create_time, '' as remark
  union all select 12, 'Physical examination form', 'PHYSICAL_EXAM', 'nad_document_type', 'N', '0', 'admin', now(), ''
  union all select 13, 'Passport photo', 'PHOTO', 'nad_document_type', 'N', '0', 'admin', now(), ''
  union all select 14, 'CV', 'CV', 'nad_document_type', 'N', '0', 'admin', now(), ''
) v
where not exists (select 1 from sys_dict_data d where d.dict_type = 'nad_document_type' and d.dict_value = v.dict_value);

update nad_scholarship_document_requirement set doc_type = 'DEGREE_CERTIFICATE' where doc_type = 'DEGREE';
update nad_scholarship_document_requirement set doc_type = 'LANGUAGE_TEST_REPORT' where doc_type = 'LANGUAGE_CERT';
update nad_scholarship_document_requirement set doc_type = 'FINANCIAL_PROOF' where doc_type = 'BANK_STATEMENT';
