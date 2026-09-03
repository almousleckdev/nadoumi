-- Seed (DML only): register the media reconciliation Quartz job.
--
-- The job is seeded PAUSED (status = '1'). It destroys Cloudinary objects and
-- hard-deletes rows, so an operator enables it per environment rather than
-- letting it run everywhere by default. Guarded insert -> safe to re-run.
--
-- Manual rollback:
--   delete from sys_job where invoke_target = 'mediaReconciliationJob.run()';

insert into sys_job
    (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent,
     status, create_by, create_time, remark)
select
    'Media reconciliation', 'DEFAULT', 'mediaReconciliationJob.run()', '0 30 3 * * ?', '3', '1',
    '1', 'system', now(),
    'Destroys Cloudinary objects for soft-deleted media past the grace period; hard-deletes unreferenced rows.'
from dual
where not exists (
    select 1 from sys_job where invoke_target = 'mediaReconciliationJob.run()'
);
