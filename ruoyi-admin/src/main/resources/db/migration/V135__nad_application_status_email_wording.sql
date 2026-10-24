-- The status-change notification now names the stage the application moved to ("Eligibility review", "Offer", ...)
-- instead of the coarse status code, and says what the student can do next.
--
-- manual rollback: restore the V55 wording
--   update nad_notification_template set subject_tpl = 'Your application status: {{status}}',
--     body_tpl = 'The status of your application {{applicationRef}} for {{opportunityTitle}} changed to {{status}}.\n\nOpen your Nadoumi dashboard for the details and any next steps.'
--   where type = 'APPLICATION_STATUS_CHANGED' and channel = 'EMAIL' and locale = 'en';
--   update nad_notification_template set body_tpl = 'Application {{applicationRef}} is now {{status}}.'
--   where type = 'APPLICATION_STATUS_CHANGED' and channel = 'IN_APP' and locale = 'en';

update nad_notification_template
set subject_tpl = 'Your application {{applicationRef}}: {{status}}',
    body_tpl = 'Your application {{applicationRef}} for {{opportunityTitle}} has moved to: {{status}}.\n\nSign in to your Nadoumi dashboard to follow its progress and see any next steps.'
where type = 'APPLICATION_STATUS_CHANGED' and channel = 'EMAIL' and locale = 'en';

update nad_notification_template
set body_tpl = 'Application {{applicationRef}} moved to: {{status}}.'
where type = 'APPLICATION_STATUS_CHANGED' and channel = 'IN_APP' and locale = 'en';
