-- Seed workflow definition SCHOLARSHIP_ONLY_V1: applying for a standalone scholarship (no programme, no intake).
-- Same shape as PROGRAM_ONLY_V1 with the university stages replaced by the scholarship body's: the application is
-- submitted to the scholarship body, which decides; an awarded student then accepts or declines.
-- Status labels reuse the shared vocabulary (IN_REVIEW, DECISION, OFFER, CLOSED_*), so no front-end change is needed.
-- Like the other definitions it is seeded DRAFT and must be activated by an administrator.
--
-- Rollback (manual): delete from nad_wf_definition where code = 'SCHOLARSHIP_ONLY_V1' (cascades).

insert into nad_wf_definition (code, name, version, status, create_by, create_time)
values ('SCHOLARSHIP_ONLY_V1', 'Scholarship only (v1)', 1, 'DRAFT', 'system', now());
set @def := last_insert_id();

insert into nad_wf_stage (definition_id, code, name, order_no, stage_type, status_label, sla_hours) values
 (@def, 'DRAFT',                 'Draft',                          1,  'START',    'DRAFT',                    null),
 (@def, 'SUBMITTED',             'Submitted',                      2,  'NORMAL',   'IN_REVIEW',                16),
 (@def, 'ELIGIBILITY_REVIEW',    'Eligibility review',             3,  'NORMAL',   'IN_REVIEW',                24),
 (@def, 'DOCUMENT_COLLECTION',   'Document collection',            4,  'NORMAL',   'IN_REVIEW',                120),
 (@def, 'PACKAGE_PREPARATION',   'Package preparation',            5,  'NORMAL',   'IN_REVIEW',                40),
 (@def, 'SUBMITTED_TO_PROVIDER', 'Submitted to scholarship body',  6,  'NORMAL',   'IN_REVIEW',                null),
 (@def, 'SCHOLARSHIP_DECISION',  'Scholarship decision',           7,  'DECISION', 'DECISION',                 24),
 (@def, 'AWARD_RESPONSE',        'Award response',                 8,  'NORMAL',   'OFFER',                    80),
 (@def, 'AWARDED',               'Awarded',                        9,  'TERMINAL', 'CLOSED_SUCCESS',           null),
 (@def, 'UNSUCCESSFUL',          'Unsuccessful',                   10, 'TERMINAL', 'CLOSED_UNSUCCESSFUL',      null),
 (@def, 'WITHDRAWN',             'Withdrawn',                      11, 'TERMINAL', 'CLOSED_WITHDRAWN',         null);

set @st_draft     := (select id from nad_wf_stage where definition_id = @def and code = 'DRAFT');
set @st_submit    := (select id from nad_wf_stage where definition_id = @def and code = 'SUBMITTED');
set @st_elig      := (select id from nad_wf_stage where definition_id = @def and code = 'ELIGIBILITY_REVIEW');
set @st_docs      := (select id from nad_wf_stage where definition_id = @def and code = 'DOCUMENT_COLLECTION');
set @st_package   := (select id from nad_wf_stage where definition_id = @def and code = 'PACKAGE_PREPARATION');
set @st_toprov    := (select id from nad_wf_stage where definition_id = @def and code = 'SUBMITTED_TO_PROVIDER');
set @st_schdec    := (select id from nad_wf_stage where definition_id = @def and code = 'SCHOLARSHIP_DECISION');
set @st_award     := (select id from nad_wf_stage where definition_id = @def and code = 'AWARD_RESPONSE');
set @st_awarded   := (select id from nad_wf_stage where definition_id = @def and code = 'AWARDED');
set @st_unsucc    := (select id from nad_wf_stage where definition_id = @def and code = 'UNSUCCESSFUL');
set @st_withdrawn := (select id from nad_wf_stage where definition_id = @def and code = 'WITHDRAWN');

insert into nad_wf_transition (definition_id, code, from_stage_id, to_stage_id, guard_json, role_required, auto) values
 (@def, 'submit',                @st_draft,   @st_submit,   '{"all":["FIELD_SET:scholarship_id"]}', null, 0),
 (@def, 'claim',                 @st_submit,  @st_elig,     null, 'case_officer', 0),
 (@def, 'eligible',              @st_elig,    @st_docs,     '{"all":["DECISION_RECORDED:NADOUMI_INTERNAL:ELIGIBLE","ALL_MANDATORY_TASKS_DONE"]}', 'case_officer', 0),
 (@def, 'ineligible',            @st_elig,    @st_unsucc,   '{"all":["DECISION_RECORDED:NADOUMI_INTERNAL:INELIGIBLE"]}', 'case_officer', 0),
 (@def, 'docs_complete',         @st_docs,    @st_package,  '{"all":["DECISION_RECORDED:DOCUMENTS_COMPLETE:CONFIRMED","ALL_MANDATORY_TASKS_DONE"]}', 'case_officer', 0),
 (@def, 'package_ready',         @st_package, @st_toprov,   '{"all":["ALL_MANDATORY_TASKS_DONE","DECISION_RECORDED:FEE_SETTLED:CONFIRMED"]}', 'case_officer', 0),
 (@def, 'provider_responded',    @st_toprov,  @st_schdec,   null, 'case_officer', 0),
 (@def, 'sch_awarded',           @st_schdec,  @st_award,    '{"all":["DECISION_RECORDED:SCHOLARSHIP_AWARD:AWARDED"]}', 'case_officer', 0),
 (@def, 'sch_waitlist',          @st_schdec,  @st_toprov,   '{"all":["DECISION_RECORDED:SCHOLARSHIP_AWARD:WAITLIST"]}', 'case_officer', 0),
 (@def, 'sch_rejected',          @st_schdec,  @st_unsucc,   '{"all":["DECISION_RECORDED:SCHOLARSHIP_AWARD:REJECT"]}', 'case_officer', 0),
 (@def, 'accept_offer',          @st_award,   @st_awarded,  '{"all":["DECISION_RECORDED:APPLICANT_RESPONSE:ACCEPTED_BY_APPLICANT"]}', null, 0),
 (@def, 'decline_offer',         @st_award,   @st_unsucc,   '{"all":["DECISION_RECORDED:APPLICANT_RESPONSE:DECLINED_BY_APPLICANT"]}', null, 0),
 (@def, 'withdraw', @st_draft,   @st_withdrawn, null, null, 0),
 (@def, 'withdraw', @st_submit,  @st_withdrawn, null, null, 0),
 (@def, 'withdraw', @st_elig,    @st_withdrawn, null, null, 0),
 (@def, 'withdraw', @st_docs,    @st_withdrawn, null, null, 0),
 (@def, 'withdraw', @st_package, @st_withdrawn, null, null, 0),
 (@def, 'withdraw', @st_toprov,  @st_withdrawn, null, null, 0),
 (@def, 'withdraw', @st_schdec,  @st_withdrawn, null, null, 0),
 (@def, 'withdraw', @st_award,   @st_withdrawn, null, null, 0);

insert into nad_wf_stage_task_template (stage_id, title, role_required, mandatory, blocks_exit, order_no) values
 (@st_elig,    'Verify the applicant meets the scholarship eligibility',   null,               1, 1, 1),
 (@st_elig,    'Record eligibility decision',                              null,               1, 1, 2),
 (@st_docs,    'Send document checklist to applicant',                     null,               1, 1, 1),
 (@st_docs,    'Review & verify each submitted document',                  'document_reviewer',1, 1, 2),
 (@st_docs,    'Confirm language requirement satisfied',                   null,               1, 1, 3),
 (@st_package, 'Complete the scholarship application form',               null,               1, 1, 1),
 (@st_package, 'Compile transcripts / certificates package',               null,               1, 1, 2),
 (@st_package, 'Collect application fee',                                  'finance',          1, 1, 3),
 (@st_package, 'Internal QA of the package',                               'ops_manager',      1, 1, 4),
 (@st_toprov,  'Record submission reference / portal acknowledgement',     null,               1, 1, 1),
 (@st_toprov,  'Follow up if no response by SLA',                          null,               0, 0, 2),
 (@st_award,   'Explain the scholarship outcome to the applicant',         null,               1, 0, 1),
 (@st_award,   'Record the applicant''s accept/decline decision',          null,               1, 1, 2);
