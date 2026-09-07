-- Scholarship deadline reminders: a daily Quartz job (scholarshipDeadlineReminderJob)
-- finds published + active scholarships whose deadline is 10 days out or nearer
-- and has not been reminded yet, emits one ScholarshipDeadlineReminder outbox
-- event per scholarship, and records it here so it fires exactly once. The outbox
-- dispatcher fans the event to every active registered student (user_type='10'),
-- IN_APP always + EMAIL by preference.
--
-- {{var}} placeholders only (Flyway placeholder replacement is on; no dollar-brace).
--
-- manual rollback:
--   delete from sys_job where invoke_target = 'scholarshipDeadlineReminderJob.run()';
--   delete from nad_notification_template where type = 'SCHOLARSHIP_DEADLINE_REMINDER';
--   drop table nad_scholarship_deadline_reminder;

create table nad_scholarship_deadline_reminder (
  scholarship_id bigint   not null,
  reminded_at    datetime not null,
  primary key (scholarship_id),
  constraint fk_deadline_reminder_scholarship foreign key (scholarship_id)
    references nad_scholarship (id) on delete cascade
) engine=innodb default charset=utf8mb4
  comment 'One row per scholarship whose "deadline approaching" reminder has been sent';

insert into nad_notification_template (type, channel, locale, subject_tpl, body_tpl, create_by, create_time) values
 ('SCHOLARSHIP_DEADLINE_REMINDER', 'IN_APP', 'en', null,
  'Deadline soon: {{scholarshipTitle}} closes on {{deadline}} ({{daysLeft}} days left).', 'system', now()),
 ('SCHOLARSHIP_DEADLINE_REMINDER', 'EMAIL', 'en',
  'Deadline in {{daysLeft}} days: {{scholarshipTitle}}',
  'The application deadline for {{scholarshipTitle}} ({{scholarshipReference}}) is {{deadline}}, {{daysLeft}} days from now.\n\nOpen your Nadoumi dashboard to check the requirements and apply before it closes.', 'system', now());

insert into sys_job
    (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent,
     status, create_by, create_time, remark)
select
    'Scholarship deadline reminders', 'DEFAULT', 'scholarshipDeadlineReminderJob.run()', '0 0 8 * * ?', '3', '1',
    '0', 'system', now(),
    'Daily 08:00. Emails + in-app reminders to all students for scholarships closing within 10 days; fires once per scholarship.'
from dual
where not exists (
    select 1 from sys_job where invoke_target = 'scholarshipDeadlineReminderJob.run()'
);
