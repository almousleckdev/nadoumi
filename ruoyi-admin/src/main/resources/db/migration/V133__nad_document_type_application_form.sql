-- The filled-in application form is a document the applicant uploads: they download the form (a template of the
-- programme or scholarship), fill it in and upload it back. It needs its own document type.
--
-- manual rollback: delete from sys_dict_data where dict_type = 'nad_document_type' and dict_value = 'APPLICATION_FORM';

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, remark)
select * from (
  select 15 as dict_sort, 'Application form (filled in)' as dict_label, 'APPLICATION_FORM' as dict_value, 'nad_document_type' as dict_type, 'N' as is_default, '0' as status, 'admin' as create_by, now() as create_time, '' as remark
) v
where not exists (select 1 from sys_dict_data d where d.dict_type = 'nad_document_type' and d.dict_value = v.dict_value);
