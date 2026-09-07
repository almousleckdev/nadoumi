-- Finance: expenses (with receipts) and revenue records. Earnings are derived
-- (revenue - expenses) and never stored. Money is decimal + ISO-4217 currency;
-- multi-currency totals are grouped by currency, never summed across.
--
-- manual rollback: DROP TABLE nad_expense; DROP TABLE nad_revenue; DROP TABLE nad_expense_category;

create table nad_expense_category (
  id          bigint       not null auto_increment,
  code        varchar(32)  not null,
  name        varchar(120) not null,
  sort_order  int          not null default 0,
  active      tinyint(1)   not null default 1,
  primary key (id),
  unique key uk_expense_category_code (code)
) engine=innodb default charset=utf8mb4;

create table nad_expense (
  id               bigint         not null auto_increment,
  receipt_no       varchar(32)        null comment 'NAD-EXP-<year>-NNNN, assigned on first approval',
  category_id      bigint             null comment 'nad_expense_category.id',
  title            varchar(200)   not null,
  description      varchar(2000)      null,
  amount           decimal(14,2)  not null,
  currency         char(3)        not null default 'CNY',
  spent_on         date           not null,
  vendor           varchar(200)      null,
  payment_method   varchar(20)        null comment 'CASH | BANK_TRANSFER | CARD | WECHAT | ALIPAY | OTHER',
  status           varchar(12)    not null default 'DRAFT' comment 'DRAFT | SUBMITTED | APPROVED | PAID | REJECTED',
  submitted_by     bigint             null comment 'sys_user.user_id',
  approved_by      bigint             null,
  approved_at      datetime           null,
  paid_at          datetime           null,
  notes            varchar(1000)     null,
  create_by        varchar(64)    not null default '',
  create_time      datetime           null,
  update_by        varchar(64)    not null default '',
  update_time      datetime           null,
  primary key (id),
  unique key uk_expense_receipt (receipt_no),
  key idx_expense_status (status),
  key idx_expense_spent_on (spent_on),
  key idx_expense_category (category_id),
  constraint fk_expense_category foreign key (category_id)
    references nad_expense_category (id) on delete set null
) engine=innodb default charset=utf8mb4;

create table nad_revenue (
  id            bigint         not null auto_increment,
  source        varchar(20)    not null comment 'APPLICATION_FEE | SERVICE_FEE | COMMISSION | TUITION_SHARE | OTHER',
  title         varchar(200)   not null,
  description   varchar(2000)      null,
  amount        decimal(14,2)  not null,
  currency      char(3)        not null default 'CNY',
  received_on   date           not null,
  reference     varchar(120)      null comment 'external ref / invoice no',
  related_type  varchar(24)       null comment 'application | applicant | university | scholarship',
  related_id    bigint             null,
  recorded_by   bigint             null comment 'sys_user.user_id',
  notes         varchar(1000)     null,
  create_by     varchar(64)    not null default '',
  create_time   datetime           null,
  update_by     varchar(64)    not null default '',
  update_time   datetime           null,
  primary key (id),
  key idx_revenue_source (source),
  key idx_revenue_received_on (received_on)
) engine=innodb default charset=utf8mb4;
