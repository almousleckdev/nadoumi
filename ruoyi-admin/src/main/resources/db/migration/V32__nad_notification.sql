-- Notification model (docs/COMMUNICATION_AND_NOTIFICATIONS.md §4, Step 5 slice 2).
-- Owned by nadoumi-notification. The transactional outbox (V30) feeds
-- NotificationService.create(...), which writes one nad_notification row and one
-- PENDING nad_notification_delivery per enabled channel. Channels: IN_APP + EMAIL
-- are dispatched in v1; SMS / PUSH / WHATSAPP are schema-ready only.
--
-- manual rollback:
--   DROP TABLE nad_notification_delivery;
--   DROP TABLE nad_notification_template;
--   DROP TABLE nad_notification_preference;
--   DROP TABLE nad_notification;

create table nad_notification (
  id                bigint        not null auto_increment,
  recipient_user_id bigint        not null comment 'sys_user.user_id (staff or student)',
  type              varchar(60)   not null comment 'NotificationType, e.g. CONTACT_INQUIRY_RECEIVED',
  title             varchar(200)  not null,
  body              varchar(2000) not null comment 'rendered in-app text -- IDs and safe fields only',
  data_json         varchar(2000)     null comment 'structured refs for the client, safe scalars only',
  source_ref        varchar(80)       null comment 'idempotency key for event-driven rows, e.g. outbox:<id>:<user>',
  application_id    bigint            null,
  conversation_id   bigint            null,
  message_id        bigint            null,
  created_at        datetime      not null,
  read_at           datetime          null,
  primary key (id),
  key idx_notif_recipient (recipient_user_id, read_at, id),
  key idx_notif_type (type),
  unique key uk_notif_source (recipient_user_id, source_ref),
  constraint fk_notif_recipient foreign key (recipient_user_id)
    references sys_user (user_id) on delete cascade
) engine=innodb default charset=utf8mb4;

create table nad_notification_delivery (
  id                  bigint       not null auto_increment,
  notification_id     bigint       not null,
  channel             varchar(12)  not null comment 'IN_APP | EMAIL | SMS | PUSH | WHATSAPP',
  provider            varchar(40)      null comment 'set by the channel impl',
  provider_message_id varchar(255)     null,
  status              varchar(12)  not null default 'PENDING' comment 'PENDING | SENT | DELIVERED | FAILED | BOUNCED',
  attempts            int          not null default 0,
  last_error          varchar(500)     null,
  next_attempt_at     datetime     not null comment 'earliest next send attempt (backoff)',
  sent_at             datetime         null,
  created_at          datetime     not null,
  primary key (id),
  unique key uk_notif_delivery_channel (notification_id, channel),
  key idx_notif_delivery_ready (status, next_attempt_at),
  constraint fk_notif_delivery_notif foreign key (notification_id)
    references nad_notification (id) on delete cascade
) engine=innodb default charset=utf8mb4;

create table nad_notification_preference (
  id      bigint      not null auto_increment,
  user_id bigint      not null comment 'sys_user.user_id',
  type    varchar(60) not null,
  channel varchar(12) not null,
  enabled tinyint(1)  not null default 1,
  primary key (id),
  unique key uk_notif_pref (user_id, type, channel)
) engine=innodb default charset=utf8mb4;

create table nad_notification_template (
  id          bigint        not null auto_increment,
  type        varchar(60)   not null,
  channel     varchar(12)   not null,
  locale      varchar(10)   not null default 'en',
  subject_tpl varchar(300)      null comment 'EMAIL subject; null for IN_APP',
  body_tpl    varchar(4000) not null comment 'double-brace placeholders resolved against the notification context',
  create_by   varchar(64)   not null default '',
  create_time datetime          null,
  update_by   varchar(64)   not null default '',
  update_time datetime          null,
  primary key (id),
  unique key uk_notif_tpl (type, channel, locale)
) engine=innodb default charset=utf8mb4;
