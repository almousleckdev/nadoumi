-- Application payment configuration, expiry job, admin menu + permissions and notification templates
-- (DML only; DDL is V110).
--
-- nadoumi.application.fee is the Nadoumi application fee in RMB. 0 means "no fee, no payment step". It is
-- seeded as 0 because the amount is a business decision: set it in System > Configuration before launch.
--
-- {{var}} placeholders only (Flyway placeholder replacement is on; no dollar-brace).
--
-- manual rollback:
--   delete from sys_config where config_key in ('nadoumi.application.fee', 'nadoumi.payment.payee');
--   delete from sys_job where invoke_target = 'applicationPaymentExpiryJob.run()';
--   delete from nad_notification_template where type in ('PAYMENT_PROOF_SUBMITTED', 'PAYMENT_CONFIRMED', 'PAYMENT_REJECTED');
--   delete from sys_role_menu where menu_id in (select menu_id from sys_menu where perms like 'nad:payment:%');
--   delete from sys_menu where perms like 'nad:payment:%';

insert into sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark) values
 ('Nadoumi application fee (RMB)', 'nadoumi.application.fee', '0', 'Y', 'admin', now(),
  'The Nadoumi application fee a student pays before submitting an application, in RMB. 0 disables the payment step.'),
 ('Payment payee label', 'nadoumi.payment.payee', 'Sichuan Nadoumi Education Consulting Co., Ltd. (四川纳豆米教育咨询有限公司)', 'Y', 'admin', now(),
  'The payee name shown to the student next to the payment QR code.');

insert into sys_job
    (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent,
     status, create_by, create_time, remark)
select
    'Application payment expiry', 'DEFAULT', 'applicationPaymentExpiryJob.run()', '0 0/5 * * * ?', '3', '1',
    '0', 'system', now(),
    'Every 5 minutes. Marks payment sessions whose 15-minute window ran out without a proof as EXPIRED.'
from dual
where not exists (
    select 1 from sys_job where invoke_target = 'applicationPaymentExpiryJob.run()'
);

set @nad_root := (select menu_id from sys_menu where path = 'nadoumi' and menu_type = 'M' limit 1);

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
 values ('Payments', @nad_root, 19, 'payments', 'nadoumi/payment/index', 'C', '0', '0', 'nad:payment:list', 'wallet', 'admin', now(), 'Application fee payments waiting for verification');
set @nad_payment := last_insert_id();

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark) values
 ('Payment query',  @nad_payment, 1, '', '', 'F', '0', '0', 'nad:payment:query',  '#', 'admin', now(), 'View a payment and its proof'),
 ('Payment verify', @nad_payment, 2, '', '', 'F', '0', '0', 'nad:payment:verify', '#', 'admin', now(), 'Confirm or reject a payment');

-- nadoumi_super_admin (role 3): everything.
insert into sys_role_menu (role_id, menu_id)
 select 3, m.menu_id from sys_menu m
 where m.menu_id = @nad_payment or m.perms like 'nad:payment:%';

-- ops_manager: verifies payments.
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id = @nad_payment or m.perms like 'nad:payment:%'
 where r.role_key in ('ops_manager');

insert into nad_notification_template (type, channel, locale, subject_tpl, body_tpl, create_by, create_time) values
 ('PAYMENT_PROOF_SUBMITTED', 'IN_APP', 'en', null,
  'Payment proof for {{applicationRef}} ({{amount}}) is waiting for verification. Reference {{reference}}.', 'system', now()),
 ('PAYMENT_CONFIRMED', 'IN_APP', 'en', null,
  'Your payment for {{applicationRef}} was confirmed. You can now submit your application.', 'system', now()),
 ('PAYMENT_CONFIRMED', 'EMAIL', 'en', 'Payment confirmed for {{applicationRef}}',
  'We confirmed your payment of {{amount}} (reference {{reference}}) for {{applicationRef}}.\n\nOpen your Nadoumi dashboard to submit your application.', 'system', now()),
 ('PAYMENT_REJECTED', 'IN_APP', 'en', null,
  'Your payment proof for {{applicationRef}} was not accepted: {{reason}}. You can start a new payment.', 'system', now()),
 ('PAYMENT_REJECTED', 'EMAIL', 'en', 'Payment not accepted for {{applicationRef}}',
  'We could not confirm your payment for {{applicationRef}} (reference {{reference}}).\n\nReason: {{reason}}\n\nOpen your Nadoumi dashboard to start a new payment.', 'system', now());
