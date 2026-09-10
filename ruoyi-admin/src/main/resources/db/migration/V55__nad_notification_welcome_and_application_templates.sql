-- Welcome + application-email notification templates, and a subject-line polish
-- across the existing EMAIL templates so every Nadoumi email subject ends
-- " - Nadoumi".
--
-- WELCOME: IN_APP is produced by NotificationService; the personalised email is
-- composed and sent by WelcomeContentComposer (its EMAIL body_tpl here is the
-- intro paragraph, editable without a redeploy). APPLICATION_SUBMITTED /
-- APPLICATION_STATUS_CHANGED have no producer yet — the Application module
-- (platform Step 6) emits ApplicationSubmitted / ApplicationStatusChanged with
-- {{applicationRef}}, {{opportunityTitle}}, {{status}} in the context.
--
-- Double-brace {{var}} placeholders only (Flyway placeholder replacement is on;
-- no dollar-brace).
--
-- manual rollback:
--   delete from nad_notification_template
--     where type in ('WELCOME','APPLICATION_SUBMITTED','APPLICATION_STATUS_CHANGED');
--   -- the subject UPDATE below is idempotent and safe to leave in place.

insert into nad_notification_template (type, channel, locale, subject_tpl, body_tpl, create_by, create_time) values
 ('WELCOME', 'IN_APP', 'en', null,
  'Welcome to Nadoumi - explore programmes and scholarships from your dashboard.', 'system', now()),
 ('WELCOME', 'EMAIL', 'en', 'Welcome to Nadoumi',
  'Your Nadoumi account is ready. Discover universities, programmes and scholarships, build your applicant profile, upload your documents, and track every application in one place.', 'system', now()),

 ('APPLICATION_SUBMITTED', 'IN_APP', 'en', null,
  'Application {{applicationRef}} for {{opportunityTitle}} was submitted.', 'system', now()),
 ('APPLICATION_SUBMITTED', 'EMAIL', 'en', 'We received your application - Nadoumi',
  'We have received your application {{applicationRef}} for {{opportunityTitle}}.\n\nOur team will review it and keep you updated here and by email. You can follow its progress from your Nadoumi dashboard.', 'system', now()),

 ('APPLICATION_STATUS_CHANGED', 'IN_APP', 'en', null,
  'Application {{applicationRef}} is now {{status}}.', 'system', now()),
 ('APPLICATION_STATUS_CHANGED', 'EMAIL', 'en', 'Your application status: {{status}} - Nadoumi',
  'The status of your application {{applicationRef}} for {{opportunityTitle}} changed to {{status}}.\n\nOpen your Nadoumi dashboard for the details and any next steps.', 'system', now());

-- Subject-line polish: append " - Nadoumi" to every en EMAIL subject that does
-- not already carry it. Idempotent (guarded by the NOT LIKE).
update nad_notification_template
   set subject_tpl = concat(subject_tpl, ' - Nadoumi'),
       update_by = 'system', update_time = now()
 where channel = 'EMAIL'
   and locale = 'en'
   and subject_tpl is not null
   and subject_tpl not like '% - Nadoumi';
