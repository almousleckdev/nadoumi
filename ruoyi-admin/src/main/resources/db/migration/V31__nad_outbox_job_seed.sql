-- Seed (DML only): register the outbox poller Quartz job.
--
-- Seeded ACTIVE (status = '0') and frequent -- draining the outbox is essential,
-- not optional, and the job is safe (it only reads PENDING rows and hands them to
-- the dispatcher). RuoYi's clustered Quartz JDBC store fires it on one instance
-- at a time. Guarded insert -> safe to re-run.
--
-- Manual rollback:
--   delete from sys_job where invoke_target = 'outboxPollerJob.run()';

insert into sys_job
    (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent,
     status, create_by, create_time, remark)
select
    'Outbox poller', 'DEFAULT', 'outboxPollerJob.run()', '0/15 * * * * ?', '3', '1',
    '0', 'system', now(),
    'Drains nad_outbox_event into the notification pipeline every 15s.'
from dual
where not exists (
    select 1 from sys_job where invoke_target = 'outboxPollerJob.run()'
);
