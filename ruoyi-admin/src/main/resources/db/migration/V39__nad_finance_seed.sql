-- Finance reference data + admin menu + permissions (DML only; DDL is V38).

insert into nad_expense_category (code, name, sort_order) values
  ('OFFICE',        'Office & supplies',        1),
  ('TRAVEL',        'Travel & transport',       2),
  ('MARKETING',     'Marketing & advertising',  3),
  ('SOFTWARE',      'Software & subscriptions',  4),
  ('PROFESSIONAL',  'Professional services',    5),
  ('PARTNER',       'Partner / university fees', 6),
  ('SALARY',        'Payroll & benefits',       7),
  ('UTILITIES',     'Rent & utilities',         8),
  ('OTHER',         'Other',                    99);

set @nad_root := (select menu_id from sys_menu where path = 'nadoumi' and menu_type = 'M' limit 1);

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
 values ('Finance', @nad_root, 20, 'finance', 'nadoumi/finance/summary', 'C', '0', '0', 'nad:finance:view', 'money', 'admin', now(), 'Revenue, expenses and net earnings');
set @nad_finance := last_insert_id();

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
 values ('Expenses', @nad_root, 21, 'expenses', 'nadoumi/finance/expenses', 'C', '0', '0', 'nad:expense:list', 'wallet', 'admin', now(), 'Expense records + receipts');
set @nad_expense := last_insert_id();

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
 values ('Revenue', @nad_root, 22, 'revenue', 'nadoumi/finance/revenue', 'C', '0', '0', 'nad:revenue:list', 'coin', 'admin', now(), 'Revenue records');
set @nad_revenue := last_insert_id();

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark) values
 ('Expense query',   @nad_expense, 1, '', '', 'F', '0', '0', 'nad:expense:query',   '#', 'admin', now(), ''),
 ('Expense create',  @nad_expense, 2, '', '', 'F', '0', '0', 'nad:expense:add',     '#', 'admin', now(), ''),
 ('Expense edit',    @nad_expense, 3, '', '', 'F', '0', '0', 'nad:expense:edit',    '#', 'admin', now(), ''),
 ('Expense remove',  @nad_expense, 4, '', '', 'F', '0', '0', 'nad:expense:remove',  '#', 'admin', now(), ''),
 ('Expense approve', @nad_expense, 5, '', '', 'F', '0', '0', 'nad:expense:approve', '#', 'admin', now(), 'Approve / mark paid; assigns the receipt number'),
 ('Revenue query',   @nad_revenue, 1, '', '', 'F', '0', '0', 'nad:revenue:query',   '#', 'admin', now(), ''),
 ('Revenue create',  @nad_revenue, 2, '', '', 'F', '0', '0', 'nad:revenue:add',     '#', 'admin', now(), ''),
 ('Revenue edit',    @nad_revenue, 3, '', '', 'F', '0', '0', 'nad:revenue:edit',    '#', 'admin', now(), ''),
 ('Revenue remove',  @nad_revenue, 4, '', '', 'F', '0', '0', 'nad:revenue:remove',  '#', 'admin', now(), '');

-- nadoumi_super_admin (role 3): everything.
insert into sys_role_menu (role_id, menu_id)
 select 3, m.menu_id from sys_menu m
 where m.menu_id in (@nad_finance, @nad_expense, @nad_revenue)
    or m.perms like 'nad:finance:%' or m.perms like 'nad:expense:%' or m.perms like 'nad:revenue:%';

-- ops_manager: full finance.
insert into sys_role_menu (role_id, menu_id)
 select r.role_id, m.menu_id
 from sys_role r
 join sys_menu m on m.menu_id in (@nad_finance, @nad_expense, @nad_revenue)
                 or m.perms like 'nad:finance:%' or m.perms like 'nad:expense:%' or m.perms like 'nad:revenue:%'
 where r.role_key in ('ops_manager');
