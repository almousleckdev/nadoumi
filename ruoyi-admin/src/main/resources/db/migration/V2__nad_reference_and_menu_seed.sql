-- Nadoumi reference data + staff menu/permission seed (docs/PERMISSION_CATALOGUE.md).

insert into sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark) values
 ('Student self-registration', 'nad.student.register.enabled', 'true', 'Y', 'admin', now(),
  'gate for POST /api/student/register (separate from sys.account.registerUser)');

insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark) values
 ('Nadoumi user type',   'nad_user_type',    '0', 'admin', now(), 'sys_user.user_type: 00 staff / 10 student / 20 agent / 30 guardian'),
 ('Applicant access role','nad_access_role', '0', 'admin', now(), 'nad_user_applicant_access.access_role'),
 ('Degree level',         'nad_degree_level','0', 'admin', now(), 'applicant education + program level'),
 ('Test type',            'nad_test_type',   '0', 'admin', now(), 'applicant standardized test scores');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, remark) values
 (1, 'Staff',    '00', 'nad_user_type', 'Y', '0', 'admin', now(), 'internal operator - RuoYi RBAC'),
 (2, 'Student',  '10', 'nad_user_type', 'N', '0', 'admin', now(), 'applicant-side self-service user'),
 (3, 'Agent',    '20', 'nad_user_type', 'N', '0', 'admin', now(), 'submits on behalf of applicants'),
 (4, 'Guardian', '30', 'nad_user_type', 'N', '0', 'admin', now(), 'guardian / sponsor'),

 (1, 'Owner',    'OWNER',    'nad_access_role', 'N', '0', 'admin', now(), 'exactly one active owner per applicant'),
 (2, 'Agent',    'AGENT',    'nad_access_role', 'N', '0', 'admin', now(), ''),
 (3, 'Guardian', 'GUARDIAN', 'nad_access_role', 'N', '0', 'admin', now(), ''),
 (4, 'Viewer',   'VIEWER',   'nad_access_role', 'N', '0', 'admin', now(), 'read-only'),

 (1, 'Foundation', 'FOUNDATION', 'nad_degree_level', 'N', '0', 'admin', now(), ''),
 (2, 'Diploma',    'DIPLOMA',    'nad_degree_level', 'N', '0', 'admin', now(), ''),
 (3, 'Bachelor',   'BACHELOR',   'nad_degree_level', 'N', '0', 'admin', now(), ''),
 (4, 'Master',     'MASTER',     'nad_degree_level', 'N', '0', 'admin', now(), ''),
 (5, 'PhD',        'PHD',        'nad_degree_level', 'N', '0', 'admin', now(), ''),

 (1, 'IELTS',    'IELTS',    'nad_test_type', 'N', '0', 'admin', now(), ''),
 (2, 'TOEFL',    'TOEFL',    'nad_test_type', 'N', '0', 'admin', now(), ''),
 (3, 'Duolingo', 'DUOLINGO', 'nad_test_type', 'N', '0', 'admin', now(), ''),
 (4, 'Gaokao',   'GAOKAO',   'nad_test_type', 'N', '0', 'admin', now(), ''),
 (5, 'SAT',      'SAT',      'nad_test_type', 'N', '0', 'admin', now(), ''),
 (6, 'GRE',      'GRE',      'nad_test_type', 'N', '0', 'admin', now(), ''),
 (7, 'GMAT',     'GMAT',     'nad_test_type', 'N', '0', 'admin', now(), ''),
 (8, 'HSK',      'HSK',      'nad_test_type', 'N', '0', 'admin', now(), ''),
 (9, 'Other',    'OTHER',    'nad_test_type', 'N', '0', 'admin', now(), '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
 values ('Nadoumi', 0, 5, 'nadoumi', null, 'M', '0', '0', '', 'international', 'admin', now(), 'Nadoumi business modules');
set @nad_root := last_insert_id();

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
 values ('Applicants', @nad_root, 1, 'applicant', 'nadoumi/applicant/index', 'C', '0', '0', 'nad:applicant:list', 'peoples', 'admin', now(), 'Applicant profiles');
set @nad_applicant := last_insert_id();

insert into sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark) values
 ('Applicant query',        @nad_applicant, 1, '', '', 'F', '0', '0', 'nad:applicant:view',           '#', 'admin', now(), ''),
 ('Applicant create',       @nad_applicant, 2, '', '', 'F', '0', '0', 'nad:applicant:create',         '#', 'admin', now(), ''),
 ('Applicant edit',         @nad_applicant, 3, '', '', 'F', '0', '0', 'nad:applicant:edit',           '#', 'admin', now(), ''),
 ('Applicant archive',      @nad_applicant, 4, '', '', 'F', '0', '0', 'nad:applicant:archive',        '#', 'admin', now(), ''),
 ('Applicant export',       @nad_applicant, 5, '', '', 'F', '0', '0', 'nad:applicant:export',         '#', 'admin', now(), ''),
 ('Applicant PII view',     @nad_applicant, 6, '', '', 'F', '0', '0', 'nad:applicant:pii:view',       '#', 'admin', now(), ''),
 ('Applicant merge',        @nad_applicant, 7, '', '', 'F', '0', '0', 'nad:applicant:merge',          '#', 'admin', now(), ''),
 ('Applicant access view',  @nad_applicant, 8, '', '', 'F', '0', '0', 'nad:applicant:access:view',    '#', 'admin', now(), ''),
 ('Applicant access manage',@nad_applicant, 9, '', '', 'F', '0', '0', 'nad:applicant:access:manage',  '#', 'admin', now(), '');

insert into sys_role (role_name, role_key, role_sort, data_scope, status, create_by, create_time, remark) values
 ('Operations manager', 'ops_manager',  10, '1', '0', 'admin', now(), 'org-wide applicant + application operations'),
 ('Case officer',       'case_officer', 11, '4', '0', 'admin', now(), 'owns assigned cases; dept + children scope');

insert into sys_role_menu (role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (@nad_root, @nad_applicant)
                or m.perms like 'nad:applicant:%'
where r.role_key = 'ops_manager';

insert into sys_role_menu (role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (@nad_root, @nad_applicant)
                or m.perms in ('nad:applicant:view', 'nad:applicant:edit',
                               'nad:applicant:pii:view', 'nad:applicant:access:view')
where r.role_key = 'case_officer';
