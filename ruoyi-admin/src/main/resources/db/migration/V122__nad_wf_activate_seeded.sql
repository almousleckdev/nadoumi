-- Activates the three seeded application workflows. They were seeded DRAFT so that an administrator would activate them, but
-- there is no admin screen or endpoint that does it, so no student could start an application on a fresh environment.
-- Only a DRAFT definition whose code has no ACTIVE version is touched, so an environment where someone already activated
-- or replaced a version is left exactly as it is, and a rerun changes nothing.
--
-- manual rollback: update nad_wf_definition set status = 'DRAFT' where code in ('PROGRAM_ONLY_V1',
--   'PROGRAM_WITH_SCHOLARSHIP_V1', 'SCHOLARSHIP_ONLY_V1') and version = 1 and update_by = 'flyway-V122';

update nad_wf_definition d
   set d.status = 'ACTIVE', d.update_by = 'flyway-V122', d.update_time = now()
 where d.id > 0
   and d.status = 'DRAFT'
   and d.code in ('PROGRAM_ONLY_V1', 'PROGRAM_WITH_SCHOLARSHIP_V1', 'SCHOLARSHIP_ONLY_V1')
   and not exists (select 1 from (select code from nad_wf_definition where status = 'ACTIVE') a where a.code = d.code);
