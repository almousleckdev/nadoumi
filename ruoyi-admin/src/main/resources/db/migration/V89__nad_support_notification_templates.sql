-- Support/Ticketing domain, slice 3: default en templates for the three ticket
-- notification types (NotificationType.TICKET_OPENED / TICKET_ASSIGNED /
-- TICKET_STATUS_CHANGED). IN_APP only in v1 -- no secondary channel is declared.
-- The ticket subject is student-authored free text and is the only user text
-- included; message bodies are never put in a notification (same rule as
-- MESSAGE_POSTED, V83). Double-brace placeholders only (Flyway placeholder
-- replacement is on).
--
-- manual rollback:
--   delete from nad_notification_template
--     where type in ('TICKET_OPENED','TICKET_ASSIGNED','TICKET_STATUS_CHANGED');

insert into nad_notification_template (type, channel, locale, subject_tpl, body_tpl, create_by, create_time) values
 ('TICKET_OPENED', 'IN_APP', 'en', null,
  'New support ticket: {{subject}} ({{category}}), opened by {{openedByName}}.', 'system', now()),
 ('TICKET_ASSIGNED', 'IN_APP', 'en', null,
  '{{assignedByName}} assigned you the support ticket: {{subject}}.', 'system', now()),
 ('TICKET_STATUS_CHANGED', 'IN_APP', 'en', null,
  'Your support ticket {{subject}} is now {{status}}.', 'system', now());
