-- Settings and the job for the payment documents (V112).
--
-- manual rollback:
--   delete from sys_config where config_key = 'nadoumi.application.notify-roles';
--   delete from sys_job where invoke_target = 'applicationPaymentDocumentsJob.run()';
--   re-insert the PAYMENT_CONFIRMED EMAIL template from V111.

insert into sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark) values
 ('Application form recipients (role keys)', 'nadoumi.application.notify-roles', 'nadoumi_super_admin', 'Y', 'admin', now(),
  'Comma-separated role keys. Active users holding one of these roles get the application form PDF by email when a student submits. Staff roles are not notified.');

insert into sys_job
    (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent,
     status, create_by, create_time, remark)
select
    'Application payment documents', 'DEFAULT', 'applicationPaymentDocumentsJob.run()', '0 * * * * ?', '3', '1',
    '0', 'system', now(),
    'Every minute. Renders the receipt and application form of confirmed payments, emails the receipt to the student and the submitted form to the administrators.'
from dual
where not exists (
    select 1 from sys_job where invoke_target = 'applicationPaymentDocumentsJob.run()'
);

-- The student now gets the receipt email (with the PDF) from the payment module, so the plain
-- "payment confirmed" email would be a duplicate. The in-app notification stays.
delete from nad_notification_template where type = 'PAYMENT_CONFIRMED' and channel = 'EMAIL';
