-- Interviews on an application, and the generic "application update" notification.
-- An interview is scheduled by staff while the case is in "Interview required". The time is an instant stored in UTC; staff
-- enter it in Beijing time (China has no daylight saving, so +08:00 is fixed) and the student sees it in their own zone.
-- Rescheduling cancels the open interview and adds a new one, so the history of what the student was told is kept.
-- At most one interview per application is SCHEDULED at a time (virtual generated key + unique index; virtual because a
-- stored column over application_id could not keep the cascading foreign key).
-- Manual rollback: drop table nad_application_interview;
--   delete from nad_notification_template where type = 'APPLICATION_UPDATE';

create table nad_application_interview (
  id               bigint        not null auto_increment,
  application_id   bigint        not null,
  starts_at        datetime      not null comment 'UTC',
  duration_minutes int           not null,
  platform         varchar(60)       null,
  meeting_url      varchar(500)  not null,
  instructions     varchar(2000)     null,
  status           varchar(16)   not null default 'SCHEDULED' comment 'SCHEDULED | CANCELLED',
  scheduled_key    bigint generated always as (case when status = 'SCHEDULED' then application_id else null end) virtual,
  create_by        varchar(64)   default '',
  create_time      datetime      default null,
  update_by        varchar(64)   default '',
  update_time      datetime      default null,
  primary key (id),
  unique key uk_interview_one_scheduled (scheduled_key),
  key idx_interview_application (application_id, starts_at),
  constraint fk_interview_application foreign key (application_id) references nad_application (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 comment='Interview of an application, scheduled by staff';

-- One reusable message for "something needs your attention on this application": the sender supplies the headline and text.
insert into nad_notification_template (type, channel, locale, subject_tpl, body_tpl, create_by, create_time) values
 ('APPLICATION_UPDATE', 'IN_APP', 'en', null, '{{headline}}: {{message}}', 'system', now()),
 ('APPLICATION_UPDATE', 'EMAIL', 'en', '{{headline}}',
  '{{message}}\n\nApplication {{applicationRef}}.\n\nSign in to your Nadoumi dashboard for the details.', 'system', now());
