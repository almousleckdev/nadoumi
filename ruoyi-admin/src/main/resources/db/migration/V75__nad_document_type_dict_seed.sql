-- nad_document_type dictionary, v1 set (docs/superpowers/specs/2026-09-20-document-
-- domain-design.md §4 / docs/DOCUMENT_MANAGEMENT.md §3.2a). The five SENSITIVE-
-- override types (PASSPORT, VISA, FINANCIAL_PROOF, TRANSCRIPT, POLICE_CLEARANCE)
-- must match §3.2a exactly — the service resolves the media access-class override
-- off these exact dict values. Seed data, kept separate from DDL per
-- .claude/rules/database.md.

insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark) values
 ('Nadoumi document type', 'nad_document_type', '0', 'admin', now(),
  'nad_document.doc_type — the five SENSITIVE-override types must match DOCUMENT_MANAGEMENT.md §3.2a');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time, remark) values
 (1, 'Passport',              'PASSPORT',              'nad_document_type', 'N', '0', 'admin', now(), 'SENSITIVE override'),
 (2, 'Visa',                  'VISA',                  'nad_document_type', 'N', '0', 'admin', now(), 'SENSITIVE override'),
 (3, 'Financial proof',       'FINANCIAL_PROOF',       'nad_document_type', 'N', '0', 'admin', now(), 'SENSITIVE override'),
 (4, 'Transcript',            'TRANSCRIPT',            'nad_document_type', 'N', '0', 'admin', now(), 'SENSITIVE override'),
 (5, 'Police clearance',      'POLICE_CLEARANCE',      'nad_document_type', 'N', '0', 'admin', now(), 'SENSITIVE override'),
 (6, 'Degree certificate',    'DEGREE_CERTIFICATE',    'nad_document_type', 'N', '0', 'admin', now(), ''),
 (7, 'Recommendation letter', 'RECOMMENDATION_LETTER', 'nad_document_type', 'N', '0', 'admin', now(), ''),
 (8, 'Personal statement',    'PERSONAL_STATEMENT',    'nad_document_type', 'N', '0', 'admin', now(), ''),
 (9, 'Language test report',  'LANGUAGE_TEST_REPORT',  'nad_document_type', 'N', '0', 'admin', now(), ''),
 (10, 'Other',                'OTHER',                 'nad_document_type', 'N', '0', 'admin', now(), '');
