-- Notification templates for the new "published to the public catalog" events.
-- Producers: UniversityService / ProgramService emit UniversityPublished /
-- ProgramPublished on the DRAFT|INACTIVE -> ACTIVE+PUBLISHED transition;
-- ScholarshipPublished already existed. OutboxToNotificationDispatcher now fans
-- all three to staff holding nad:notification:list AND every active registered
-- student (user_type='10'). IN_APP is always produced; EMAIL follows preference.
--
-- Double-brace {{var}} placeholders only (Flyway placeholder replacement is on).
--
-- manual rollback:
--   delete from nad_notification_template where type in ('UNIVERSITY_PUBLISHED','PROGRAM_PUBLISHED');

insert into nad_notification_template (type, channel, locale, subject_tpl, body_tpl, create_by, create_time) values
 ('UNIVERSITY_PUBLISHED', 'IN_APP', 'en', null,
  'New university on Nadoumi: {{universityName}} ({{country}}).', 'system', now()),
 ('UNIVERSITY_PUBLISHED', 'EMAIL', 'en', 'New university: {{universityName}}',
  'A new university is now listed on Nadoumi: {{universityName}}.\n\nExplore it in your dashboard.', 'system', now()),
 ('PROGRAM_PUBLISHED', 'IN_APP', 'en', null,
  'New programme: {{programName}} at {{universityName}}.', 'system', now()),
 ('PROGRAM_PUBLISHED', 'EMAIL', 'en', 'New programme: {{programName}}',
  'A new programme is available: {{programName}} at {{universityName}}.\n\nExplore it in your dashboard.', 'system', now());

-- keep the existing ScholarshipPublished copy student-friendly (it is fanned to
-- students now, not only staff). Overwrite the en templates in place.
update nad_notification_template
   set body_tpl = 'A new scholarship is available: {{scholarshipTitle}} ({{scholarshipReference}}).'
 where type = 'SCHOLARSHIP_PUBLISHED' and channel = 'IN_APP' and locale = 'en';

update nad_notification_template
   set subject_tpl = 'New scholarship: {{scholarshipTitle}}',
       body_tpl = 'A new scholarship is available on Nadoumi: {{scholarshipTitle}} ({{scholarshipReference}}).\n\nCheck the eligibility and deadline in your dashboard.'
 where type = 'SCHOLARSHIP_PUBLISHED' and channel = 'EMAIL' and locale = 'en';
