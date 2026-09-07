-- Employee (HR) records for internal Nadoumi staff (docs/ADMIN_ARCHITECTURE.md §7.1,
-- Step 10 pulled forward). One row per person, 1:1 with a sys_user staff account
-- (user_type = '00'). The account holds identity + auth; this table holds the
-- employment relationship: position, compensation, dates, status.
--
-- manual rollback: DROP TABLE nad_employee;

create table nad_employee (
  id                  bigint       not null auto_increment,
  user_id             bigint       not null comment 'sys_user.user_id (staff account)',
  employee_no         varchar(32)      null comment 'human reference, e.g. NAD-EMP-0001',
  position_id         bigint           null comment 'sys_post.post_id',
  position_title      varchar(120)     null comment 'free-text title snapshot / override',
  dept_id             bigint           null comment 'sys_dept.dept_id (denormalised for reporting)',
  manager_user_id     bigint           null comment 'reports-to sys_user.user_id',
  employment_type     varchar(20)  not null default 'FULL_TIME' comment 'FULL_TIME | PART_TIME | CONTRACT | INTERN | TEMPORARY',
  employment_status   varchar(20)  not null default 'ACTIVE' comment 'PROBATION | ACTIVE | ON_LEAVE | SUSPENDED | TERMINATED',
  start_date          date         not null comment 'employment start',
  probation_end_date  date             null,
  end_date            date             null comment 'termination / contract end',
  work_location       varchar(120)     null,
  salary_amount       decimal(14,2)    null comment 'gross per pay period; NULL = not recorded',
  salary_currency     char(3)          null comment 'ISO 4217, e.g. CNY',
  pay_frequency       varchar(12)  not null default 'MONTHLY' comment 'MONTHLY | ANNUAL | WEEKLY | HOURLY',
  emergency_contact   varchar(200)     null,
  notes               varchar(1000)    null,
  create_by           varchar(64)  not null default '',
  create_time         datetime         null,
  update_by           varchar(64)  not null default '',
  update_time         datetime         null,
  primary key (id),
  unique key uk_employee_user (user_id),
  unique key uk_employee_no (employee_no),
  key idx_employee_dept (dept_id),
  key idx_employee_status (employment_status),
  key idx_employee_manager (manager_user_id),
  constraint fk_employee_user foreign key (user_id) references sys_user (user_id) on delete cascade,
  constraint fk_employee_position foreign key (position_id) references sys_post (post_id) on delete set null
) engine=innodb default charset=utf8mb4;
