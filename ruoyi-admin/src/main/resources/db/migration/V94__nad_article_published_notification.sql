-- Notification templates for the new-article announcement. Producer: ArticleService emits
-- ArticlePublished on an article's first publish. OutboxToNotificationDispatcher fans it to
-- active registered students only (scope STUDENTS); staff are not notified.
--
-- Double-brace {{var}} placeholders only (Flyway placeholder replacement is on).
--
-- manual rollback:
--   delete from nad_notification_template where type = 'ARTICLE_PUBLISHED';

insert into nad_notification_template (type, channel, locale, subject_tpl, body_tpl, create_by, create_time) values
 ('ARTICLE_PUBLISHED', 'IN_APP', 'en', null,
  'New on Nadoumi News: {{articleTitle}}.', 'system', now()),
 ('ARTICLE_PUBLISHED', 'EMAIL', 'en', 'New article: {{articleTitle}}',
  '{{articleTitle}}\n\n{{articleSubtitle}}\n\nRead it on the Nadoumi News page.', 'system', now());
