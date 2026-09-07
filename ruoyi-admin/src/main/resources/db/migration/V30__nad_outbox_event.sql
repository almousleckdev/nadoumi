-- Transactional outbox for Nadoumi domain events (docs/DOMAIN_EVENTS.md, Step 5).
-- Producers insert a row here in the SAME transaction as the state change that
-- produced it; OutboxPollerJob drains PENDING rows into the notification
-- pipeline. payload_json carries IDs and safe scalars only -- never PII, never
-- confidential fields (docs/SECURITY.md section 6).
--
-- The aggregate reference (aggregate_type, aggregate_id) is polymorphic, so there
-- is deliberately no FK to the owning tables.
--
-- manual rollback: DROP TABLE nad_outbox_event;

create table nad_outbox_event (
  id              bigint         not null auto_increment,
  aggregate_type  varchar(40)    not null comment 'source aggregate, e.g. scholarship / contact_inquiry',
  aggregate_id    bigint         not null comment 'source aggregate primary key',
  type            varchar(60)    not null comment 'domain event type -- see docs/DOMAIN_EVENTS.md',
  payload_json    varchar(2000)  not null default '{}' comment 'safe scalars only, no PII / confidential',
  status          varchar(12)    not null default 'PENDING' comment 'PENDING | PROCESSING | DONE | FAILED',
  retry_count     int            not null default 0,
  last_error      varchar(500)       null,
  created_at      datetime       not null,
  next_attempt_at datetime       not null comment 'earliest next dispatch attempt (backoff)',
  processed_at    datetime           null comment 'set when status becomes DONE',
  primary key (id),
  key idx_outbox_ready (status, next_attempt_at),
  key idx_outbox_aggregate (aggregate_type, aggregate_id),
  key idx_outbox_type (type)
) engine=innodb default charset=utf8mb4;
