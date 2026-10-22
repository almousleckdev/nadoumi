-- Application payments: the Nadoumi application fee a student pays (by QR, for now) before an
-- application can be submitted. One row per payment attempt (session); history is never overwritten.
--
--   PENDING          the student opened the QR; the payment window is open until expires_at
--   PROOF_SUBMITTED  the student reported the payment (transaction number + screenshot); waiting for staff
--   CONFIRMED        staff verified the money arrived; the application may now be submitted
--   REJECTED         staff refused the proof (reason kept); the student may start again
--   EXPIRED          the window ran out before any proof was submitted
--   CANCELLED        the student closed the session
--
-- Integrity (database-enforced, not only in code):
--   uk_payment_open  at most one open (PENDING / PROOF_SUBMITTED) payment per application and kind
--   uk_payment_txn   one bank transaction number can back one live payment only
--   uk_payment_reference  the reference the student writes in the transfer note is unique
--
-- applicant_id is a soft reference (no foreign key) so the erasure participant can remove a person's
-- payment rows without a join into another module's table.
--
-- manual rollback: drop table nad_application_payment_event; drop table nad_application_payment;

create table nad_application_payment (
  id               bigint         not null auto_increment,
  application_id   bigint         not null,
  applicant_id     bigint         not null comment 'soft reference to nad_applicant',
  applicant_name   varchar(200)   not null comment 'name at the time of payment, for the staff queue',
  kind             varchar(24)    not null default 'APPLICATION_FEE',
  amount           decimal(12, 2) not null comment 'snapshot of the fee when the session was opened',
  currency         char(3)        not null default 'CNY',
  method           varchar(12)    not null default 'QR',
  reference        varchar(32)    not null comment 'written in the transfer note by the student',
  status           varchar(20)    not null,
  expires_at       datetime       not null comment 'end of the payment window (UTC)',
  student_txn_no   varchar(80)        null comment 'bank / wallet transaction number typed by the student',
  proof_media_id   bigint             null,
  submitted_at     datetime           null,
  verified_by      bigint             null comment 'sys_user.user_id of the staff member who confirmed or rejected',
  verified_at      datetime           null,
  rejection_reason varchar(300)       null,
  version          int            not null default 0,
  create_by        varchar(64)    not null default '' comment 'sys_user.user_id of the student account that opened it',
  create_time      datetime           null,
  update_by        varchar(64)    not null default '',
  update_time      datetime           null,
  open_flag        tinyint generated always as
                     (case when status in ('PENDING', 'PROOF_SUBMITTED') then 1 else null end) virtual,
  txn_key          varchar(80) generated always as
                     (case when status in ('PROOF_SUBMITTED', 'CONFIRMED') then student_txn_no else null end) virtual,
  primary key (id),
  unique key uk_payment_reference (reference),
  unique key uk_payment_open (application_id, kind, open_flag),
  unique key uk_payment_txn (txn_key),
  key idx_payment_queue (status, submitted_at),
  key idx_payment_expiry (status, expires_at),
  key idx_payment_applicant (applicant_id),
  constraint fk_payment_application foreign key (application_id)
    references nad_application (id) on delete restrict,
  constraint fk_payment_proof foreign key (proof_media_id)
    references nad_media_asset (id) on delete set null
) engine=innodb default charset=utf8mb4;

create table nad_application_payment_event (
  id          bigint       not null auto_increment,
  payment_id  bigint       not null,
  from_status varchar(20)      null,
  to_status   varchar(20)  not null,
  actor_kind  varchar(10)  not null comment 'STUDENT | STAFF | SYSTEM',
  actor_id    bigint           null comment 'sys_user.user_id, null for SYSTEM',
  note        varchar(300)     null,
  created_at  datetime     not null,
  primary key (id),
  key idx_payment_event (payment_id, created_at),
  constraint fk_payment_event_payment foreign key (payment_id)
    references nad_application_payment (id) on delete cascade
) engine=innodb default charset=utf8mb4;
