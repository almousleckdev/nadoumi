-- Seed workflow definition PROGRAM_ONLY_V1 — the scholarship-free sibling (DA2).
-- No SCHOLARSHIP_DECISION stage; uni_offer/uni_offer_conditional route straight to
-- OFFER_RESPONSE. scholarship_id must be NULL for this type — enforced in
-- WorkflowService.startDraft(), not a DB constraint (keeps one FIELD_SET(intake_id)
-- submit guard shared by both definitions, per the 2026-09-20 addendum §6 item 4).
-- Renumbered from the original spec's V33.
-- Rollback (manual): delete from nad_wf_definition where code = 'PROGRAM_ONLY_V1' (cascades).

insert into nad_wf_definition (code, name, version, status, create_by, create_time)
values ('PROGRAM_ONLY_V1', 'Program only (v1)', 1, 'DRAFT', 'system', now());
set @def := last_insert_id();

insert into nad_wf_stage (definition_id, code, name, order_no, stage_type, status_label, sla_hours) values
 (@def, 'DRAFT',                    'Draft',                       1,  'START',    'DRAFT',                    null),
 (@def, 'SUBMITTED',                'Submitted',                   2,  'NORMAL',   'IN_REVIEW',                16),
 (@def, 'ELIGIBILITY_REVIEW',       'Eligibility review',          3,  'NORMAL',   'IN_REVIEW',                24),
 (@def, 'DOCUMENT_COLLECTION',      'Document collection',         4,  'NORMAL',   'IN_REVIEW',                120),
 (@def, 'PACKAGE_PREPARATION',      'Package preparation',         5,  'NORMAL',   'IN_REVIEW',                40),
 (@def, 'SUBMITTED_TO_UNIVERSITY',  'Submitted to university',     6,  'NORMAL',   'SUBMITTED_TO_UNIVERSITY',  null),
 (@def, 'UNIVERSITY_DECISION',      'University decision',         7,  'DECISION', 'DECISION',                 24),
 (@def, 'OFFER_RESPONSE',           'Offer response',              8,  'NORMAL',   'OFFER',                    80),
 (@def, 'PRE_DEPARTURE',            'Pre-departure',               9,  'NORMAL',   'PRE_DEPARTURE',            240),
 (@def, 'ENROLLED',                 'Enrolled',                    10, 'TERMINAL', 'CLOSED_SUCCESS',           null),
 (@def, 'UNSUCCESSFUL',             'Unsuccessful',                11, 'TERMINAL', 'CLOSED_UNSUCCESSFUL',      null),
 (@def, 'WITHDRAWN',                'Withdrawn',                   12, 'TERMINAL', 'CLOSED_WITHDRAWN',         null);

set @st_draft    := (select id from nad_wf_stage where definition_id = @def and code = 'DRAFT');
set @st_submit   := (select id from nad_wf_stage where definition_id = @def and code = 'SUBMITTED');
set @st_elig     := (select id from nad_wf_stage where definition_id = @def and code = 'ELIGIBILITY_REVIEW');
set @st_docs     := (select id from nad_wf_stage where definition_id = @def and code = 'DOCUMENT_COLLECTION');
set @st_package  := (select id from nad_wf_stage where definition_id = @def and code = 'PACKAGE_PREPARATION');
set @st_touni    := (select id from nad_wf_stage where definition_id = @def and code = 'SUBMITTED_TO_UNIVERSITY');
set @st_unidec   := (select id from nad_wf_stage where definition_id = @def and code = 'UNIVERSITY_DECISION');
set @st_offer    := (select id from nad_wf_stage where definition_id = @def and code = 'OFFER_RESPONSE');
set @st_predep   := (select id from nad_wf_stage where definition_id = @def and code = 'PRE_DEPARTURE');
set @st_enrolled := (select id from nad_wf_stage where definition_id = @def and code = 'ENROLLED');
set @st_unsucc   := (select id from nad_wf_stage where definition_id = @def and code = 'UNSUCCESSFUL');
set @st_withdrawn:= (select id from nad_wf_stage where definition_id = @def and code = 'WITHDRAWN');

insert into nad_wf_transition (definition_id, code, from_stage_id, to_stage_id, guard_json, role_required, auto) values
 (@def, 'submit',              @st_draft,   @st_submit,   '{"all":["FIELD_SET:program_id","FIELD_SET:intake_id"]}', null, 0),
 (@def, 'claim',                @st_submit,  @st_elig,     null, 'case_officer', 0),
 (@def, 'eligible',             @st_elig,    @st_docs,     '{"all":["DECISION_RECORDED:NADOUMI_INTERNAL:ELIGIBLE","ALL_MANDATORY_TASKS_DONE"]}', 'case_officer', 0),
 (@def, 'ineligible',           @st_elig,    @st_unsucc,   '{"all":["DECISION_RECORDED:NADOUMI_INTERNAL:INELIGIBLE"]}', 'case_officer', 0),
 (@def, 'docs_complete',        @st_docs,    @st_package,  '{"all":["DECISION_RECORDED:DOCUMENTS_COMPLETE:CONFIRMED","ALL_MANDATORY_TASKS_DONE"]}', 'case_officer', 0),
 (@def, 'package_ready',        @st_package, @st_touni,    '{"all":["ALL_MANDATORY_TASKS_DONE","DECISION_RECORDED:FEE_SETTLED:CONFIRMED"]}', 'case_officer', 0),
 (@def, 'university_responded', @st_touni,   @st_unidec,   null, 'case_officer', 0),
 (@def, 'uni_offer',            @st_unidec,  @st_offer,    '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:OFFER"]}', 'case_officer', 0),
 (@def, 'uni_offer_conditional', @st_unidec, @st_offer,    '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:CONDITIONAL_OFFER"]}', 'case_officer', 0),
 (@def, 'uni_waitlist',         @st_unidec,  @st_touni,    '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:WAITLIST"]}', 'case_officer', 0),
 (@def, 'uni_reject',           @st_unidec,  @st_unsucc,   '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:REJECT"]}', 'case_officer', 0),
 (@def, 'accept_offer',         @st_offer,   @st_predep,   '{"all":["DECISION_RECORDED:APPLICANT_RESPONSE:ACCEPTED_BY_APPLICANT"]}', null, 0),
 (@def, 'decline_offer',        @st_offer,   @st_unsucc,   '{"all":["DECISION_RECORDED:APPLICANT_RESPONSE:DECLINED_BY_APPLICANT"]}', null, 0),
 (@def, 'enrolled',             @st_predep,  @st_enrolled, '{"all":["ALL_MANDATORY_TASKS_DONE"]}', 'case_officer', 0),
 (@def, 'withdraw', @st_draft,   @st_withdrawn, null, null, 0),
 (@def, 'withdraw', @st_submit,  @st_withdrawn, null, null, 0),
 (@def, 'withdraw', @st_elig,    @st_withdrawn, null, null, 0),
 (@def, 'withdraw', @st_docs,    @st_withdrawn, null, null, 0),
 (@def, 'withdraw', @st_package, @st_withdrawn, null, null, 0),
 (@def, 'withdraw', @st_touni,   @st_withdrawn, null, null, 0),
 (@def, 'withdraw', @st_unidec,  @st_withdrawn, null, null, 0),
 (@def, 'withdraw', @st_offer,   @st_withdrawn, null, null, 0),
 (@def, 'withdraw', @st_predep,  @st_withdrawn, null, null, 0);

insert into nad_wf_stage_task_template (stage_id, title, role_required, mandatory, blocks_exit, order_no) values
 (@st_elig,    'Verify applicant meets programme entry requirements', null,               1, 1, 1),
 (@st_elig,    'Record eligibility decision',                        null,               1, 1, 2),
 (@st_docs,    'Send document checklist to applicant',                null,               1, 1, 1),
 (@st_docs,    'Review & verify each submitted document',             'document_reviewer',1, 1, 2),
 (@st_docs,    'Confirm language requirement satisfied',              null,               1, 1, 3),
 (@st_package, 'Complete university application form',                null,               1, 1, 1),
 (@st_package, 'Compile transcripts / certificates package',          null,               1, 1, 2),
 (@st_package, 'Collect application fee',                             'finance',          1, 1, 3),
 (@st_package, 'Internal QA of the package',                          'ops_manager',      1, 1, 4),
 (@st_touni,   'Record submission reference / portal acknowledgement', null,              1, 1, 1),
 (@st_touni,   'Follow up if no response by SLA',                     null,               0, 0, 2),
 (@st_offer,   'Explain offer outcome to applicant',                  null,               1, 0, 1),
 (@st_offer,   'Record applicant''s accept/decline decision',         null,               1, 1, 2),
 (@st_predep,  'Confirm enrolment / acceptance with university',      null,               1, 1, 1),
 (@st_predep,  'Support visa application',                            null,               1, 1, 2),
 (@st_predep,  'Confirm tuition deposit paid',                        'finance',          1, 1, 3),
 (@st_predep,  'Pre-departure briefing',                               null,              0, 0, 4);
