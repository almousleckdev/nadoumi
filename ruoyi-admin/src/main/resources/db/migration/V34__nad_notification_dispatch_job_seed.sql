-- Seed (DML only): register the notification channel-dispatch Quartz job.
--
-- Seeded ACTIVE (status = '0'), every 30s. Drains PENDING nad_notification_delivery
-- rows through their channel (EMAIL in v1); the same query also picks up
-- backed-off retries. Safe to run everywhere. Guarded insert -> re-runnable.
--
-- Manual rollback:
--   delete from sys_job where invoke_target = 'notificationDispatchJob.run()';

insert into sys_job
    (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent,
     status, create_by, create_time, remark)
select
    'Notification dispatch', 'DEFAULT', 'notificationDispatchJob.run()', '0/30 * * * * ?', '3', '1',
    '0', 'system', now(),
    'Delivers PENDING nad_notification_delivery rows (EMAIL) every 30s; retries with backoff.'
from dual
where not exists (
    select 1 from sys_job where invoke_target = 'notificationDispatchJob.run()'
);
