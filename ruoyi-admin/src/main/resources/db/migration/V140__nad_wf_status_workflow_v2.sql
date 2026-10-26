-- Application workflow v2: the fifteen student-facing statuses ARE the stages (Submitted .. Enrolled).
-- One new version of PROGRAM_ONLY_V1 and SCHOLARSHIP_ONLY_V1; version 1 is retired for NEW applications only. A running
-- application is pinned to the definition it started on (nad_wf_instance.definition_id), so nothing in flight is migrated.
-- Statuses (stage code = status_label, except the three closed ones, which keep the CLOSED_* codes the dashboards use):
--   SUBMITTED, UNDER_REVIEW, DOCUMENTS_REQUIRED, READY_FOR_UNIVERSITY_SUBMISSION, SUBMITTED_TO_UNIVERSITY,
--   UNDER_UNIVERSITY_REVIEW, INTERVIEW_REQUIRED, CONDITIONAL_OFFER, OFFER_RECEIVED, OFFER_ACCEPTED, WAITLISTED,
--   ADMISSION_CONFIRMED, then ENROLLED (CLOSED_SUCCESS), REJECTED (CLOSED_UNSUCCESSFUL), WITHDRAWN (CLOSED_WITHDRAWN).
-- Documents Required is a loop, not a new application: the student fixes the documents on the SAME application and runs the
-- student step 'resubmit' back to Under review. Submit (and with it the fee gate) is never run again.
-- Scholarship applications never name the university: their stage names say "provider" (confidentiality, CLAUDE.md section 8).
-- Manual rollback: update nad_wf_definition set status = 'ACTIVE' where version = 1 and code in
--   ('PROGRAM_ONLY_V1','SCHOLARSHIP_ONLY_V1'); delete from nad_wf_definition where version = 2 and code in
--   ('PROGRAM_ONLY_V1','SCHOLARSHIP_ONLY_V1') (only while no application runs on it; stages cascade).

-- ---- PROGRAM_ONLY_V1 v2 ----
insert into nad_wf_definition (code, name, version, status, create_by, create_time)
values ('PROGRAM_ONLY_V1', 'Program only (v2)', 2, 'ACTIVE', 'flyway-V140', now());
set @def1 := last_insert_id();

insert into nad_wf_stage (definition_id, code, name, order_no, stage_type, status_label, sla_hours) values
 (@def1, 'DRAFT', 'Draft', 1, 'START', 'DRAFT', null),
 (@def1, 'SUBMITTED', 'Submitted', 2, 'NORMAL', 'SUBMITTED', 16),
 (@def1, 'UNDER_REVIEW', 'Under review', 3, 'NORMAL', 'UNDER_REVIEW', 48),
 (@def1, 'DOCUMENTS_REQUIRED', 'Documents required', 4, 'NORMAL', 'DOCUMENTS_REQUIRED', null),
 (@def1, 'READY_FOR_UNIVERSITY_SUBMISSION', 'Ready for university submission', 5, 'NORMAL', 'READY_FOR_UNIVERSITY_SUBMISSION', 40),
 (@def1, 'SUBMITTED_TO_UNIVERSITY', 'Submitted to university', 6, 'NORMAL', 'SUBMITTED_TO_UNIVERSITY', null),
 (@def1, 'UNDER_UNIVERSITY_REVIEW', 'Under university review', 7, 'NORMAL', 'UNDER_UNIVERSITY_REVIEW', null),
 (@def1, 'INTERVIEW_REQUIRED', 'Interview required', 8, 'NORMAL', 'INTERVIEW_REQUIRED', null),
 (@def1, 'CONDITIONAL_OFFER', 'Conditional offer', 9, 'NORMAL', 'CONDITIONAL_OFFER', null),
 (@def1, 'OFFER_RECEIVED', 'Offer received', 10, 'NORMAL', 'OFFER_RECEIVED', null),
 (@def1, 'OFFER_ACCEPTED', 'Offer accepted', 11, 'NORMAL', 'OFFER_ACCEPTED', null),
 (@def1, 'WAITLISTED', 'Waitlisted', 12, 'NORMAL', 'WAITLISTED', null),
 (@def1, 'ADMISSION_CONFIRMED', 'Admission confirmed', 13, 'NORMAL', 'ADMISSION_CONFIRMED', null),
 (@def1, 'ENROLLED', 'Enrolled', 14, 'TERMINAL', 'CLOSED_SUCCESS', null),
 (@def1, 'REJECTED', 'Rejected', 15, 'TERMINAL', 'CLOSED_UNSUCCESSFUL', null),
 (@def1, 'WITHDRAWN', 'Withdrawn', 16, 'TERMINAL', 'CLOSED_WITHDRAWN', null);

set @s1_DRAFT := (select id from nad_wf_stage where definition_id = @def1 and code = 'DRAFT');
set @s1_SUBMITTED := (select id from nad_wf_stage where definition_id = @def1 and code = 'SUBMITTED');
set @s1_UNDER_REVIEW := (select id from nad_wf_stage where definition_id = @def1 and code = 'UNDER_REVIEW');
set @s1_DOCUMENTS_REQUIRED := (select id from nad_wf_stage where definition_id = @def1 and code = 'DOCUMENTS_REQUIRED');
set @s1_READY_FOR_UNIVERSITY_SUBMISSION := (select id from nad_wf_stage where definition_id = @def1 and code = 'READY_FOR_UNIVERSITY_SUBMISSION');
set @s1_SUBMITTED_TO_UNIVERSITY := (select id from nad_wf_stage where definition_id = @def1 and code = 'SUBMITTED_TO_UNIVERSITY');
set @s1_UNDER_UNIVERSITY_REVIEW := (select id from nad_wf_stage where definition_id = @def1 and code = 'UNDER_UNIVERSITY_REVIEW');
set @s1_INTERVIEW_REQUIRED := (select id from nad_wf_stage where definition_id = @def1 and code = 'INTERVIEW_REQUIRED');
set @s1_CONDITIONAL_OFFER := (select id from nad_wf_stage where definition_id = @def1 and code = 'CONDITIONAL_OFFER');
set @s1_OFFER_RECEIVED := (select id from nad_wf_stage where definition_id = @def1 and code = 'OFFER_RECEIVED');
set @s1_OFFER_ACCEPTED := (select id from nad_wf_stage where definition_id = @def1 and code = 'OFFER_ACCEPTED');
set @s1_WAITLISTED := (select id from nad_wf_stage where definition_id = @def1 and code = 'WAITLISTED');
set @s1_ADMISSION_CONFIRMED := (select id from nad_wf_stage where definition_id = @def1 and code = 'ADMISSION_CONFIRMED');
set @s1_ENROLLED := (select id from nad_wf_stage where definition_id = @def1 and code = 'ENROLLED');
set @s1_REJECTED := (select id from nad_wf_stage where definition_id = @def1 and code = 'REJECTED');
set @s1_WITHDRAWN := (select id from nad_wf_stage where definition_id = @def1 and code = 'WITHDRAWN');

insert into nad_wf_transition (definition_id, code, from_stage_id, to_stage_id, guard_json, role_required, auto) values
 (@def1, 'submit', @s1_DRAFT, @s1_SUBMITTED, '{"all":["FIELD_SET:program_id","FIELD_SET:intake_id"]}', null, 0),
 (@def1, 'claim', @s1_SUBMITTED, @s1_UNDER_REVIEW, null, 'case_officer', 0),
 (@def1, 'request_documents', @s1_UNDER_REVIEW, @s1_DOCUMENTS_REQUIRED, '{"all":["DECISION_RECORDED:DOCUMENT_REVIEW:DOCUMENTS_REQUIRED"]}', 'case_officer', 0),
 (@def1, 'resubmit', @s1_DOCUMENTS_REQUIRED, @s1_UNDER_REVIEW, '{"all":["ALL_REQUIRED_DOCUMENTS_ATTACHED"]}', null, 0),
 (@def1, 'mark_ready', @s1_UNDER_REVIEW, @s1_READY_FOR_UNIVERSITY_SUBMISSION, '{"all":["DECISION_RECORDED:NADOUMI_INTERNAL:ELIGIBLE","ALL_REQUIRED_DOCUMENTS_ATTACHED","ALL_MANDATORY_TASKS_DONE"]}', 'case_officer', 0),
 (@def1, 'ineligible', @s1_UNDER_REVIEW, @s1_REJECTED, '{"all":["DECISION_RECORDED:NADOUMI_INTERNAL:INELIGIBLE"]}', 'case_officer', 0),
 (@def1, 'send_to_university', @s1_READY_FOR_UNIVERSITY_SUBMISSION, @s1_SUBMITTED_TO_UNIVERSITY, '{"all":["ALL_MANDATORY_TASKS_DONE"]}', 'case_officer', 0),
 (@def1, 'start_university_review', @s1_SUBMITTED_TO_UNIVERSITY, @s1_UNDER_UNIVERSITY_REVIEW, '{"all":["ALL_MANDATORY_TASKS_DONE"]}', 'case_officer', 0),
 (@def1, 'request_interview', @s1_UNDER_UNIVERSITY_REVIEW, @s1_INTERVIEW_REQUIRED, '{"all":["DECISION_RECORDED:UNIVERSITY_REVIEW:INTERVIEW_REQUIRED"]}', 'case_officer', 0),
 (@def1, 'interview_done', @s1_INTERVIEW_REQUIRED, @s1_UNDER_UNIVERSITY_REVIEW, '{"all":["ALL_MANDATORY_TASKS_DONE"]}', 'case_officer', 0),
 (@def1, 'conditions_met', @s1_CONDITIONAL_OFFER, @s1_OFFER_RECEIVED, '{"all":["DECISION_RECORDED:OFFER_CONDITIONS:MET"]}', 'case_officer', 0),
 (@def1, 'accept_offer', @s1_OFFER_RECEIVED, @s1_OFFER_ACCEPTED, '{"all":["DECISION_RECORDED:APPLICANT_RESPONSE:ACCEPTED_BY_APPLICANT"]}', null, 0),
 (@def1, 'accept_offer', @s1_CONDITIONAL_OFFER, @s1_OFFER_ACCEPTED, '{"all":["DECISION_RECORDED:APPLICANT_RESPONSE:ACCEPTED_BY_APPLICANT"]}', null, 0),
 (@def1, 'decline_offer', @s1_OFFER_RECEIVED, @s1_WITHDRAWN, '{"all":["DECISION_RECORDED:APPLICANT_RESPONSE:DECLINED_BY_APPLICANT"]}', null, 0),
 (@def1, 'decline_offer', @s1_CONDITIONAL_OFFER, @s1_WITHDRAWN, '{"all":["DECISION_RECORDED:APPLICANT_RESPONSE:DECLINED_BY_APPLICANT"]}', null, 0),
 (@def1, 'confirm_admission', @s1_OFFER_ACCEPTED, @s1_ADMISSION_CONFIRMED, '{"all":["DECISION_RECORDED:ADMISSION:CONFIRMED","ALL_MANDATORY_TASKS_DONE"]}', 'case_officer', 0),
 (@def1, 'enroll', @s1_ADMISSION_CONFIRMED, @s1_ENROLLED, '{"all":["PAYMENT_SETTLED:SERVICE_FEE","ALL_MANDATORY_TASKS_DONE"]}', 'case_officer', 0),
 (@def1, 'offer', @s1_UNDER_UNIVERSITY_REVIEW, @s1_OFFER_RECEIVED, '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:OFFER"]}', 'case_officer', 0),
 (@def1, 'conditional_offer', @s1_UNDER_UNIVERSITY_REVIEW, @s1_CONDITIONAL_OFFER, '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:CONDITIONAL_OFFER"]}', 'case_officer', 0),
 (@def1, 'waitlist', @s1_UNDER_UNIVERSITY_REVIEW, @s1_WAITLISTED, '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:WAITLIST"]}', 'case_officer', 0),
 (@def1, 'reject', @s1_UNDER_UNIVERSITY_REVIEW, @s1_REJECTED, '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:REJECT"]}', 'case_officer', 0),
 (@def1, 'offer', @s1_INTERVIEW_REQUIRED, @s1_OFFER_RECEIVED, '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:OFFER"]}', 'case_officer', 0),
 (@def1, 'conditional_offer', @s1_INTERVIEW_REQUIRED, @s1_CONDITIONAL_OFFER, '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:CONDITIONAL_OFFER"]}', 'case_officer', 0),
 (@def1, 'waitlist', @s1_INTERVIEW_REQUIRED, @s1_WAITLISTED, '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:WAITLIST"]}', 'case_officer', 0),
 (@def1, 'reject', @s1_INTERVIEW_REQUIRED, @s1_REJECTED, '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:REJECT"]}', 'case_officer', 0),
 (@def1, 'offer', @s1_WAITLISTED, @s1_OFFER_RECEIVED, '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:OFFER"]}', 'case_officer', 0),
 (@def1, 'conditional_offer', @s1_WAITLISTED, @s1_CONDITIONAL_OFFER, '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:CONDITIONAL_OFFER"]}', 'case_officer', 0),
 (@def1, 'reject', @s1_WAITLISTED, @s1_REJECTED, '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:REJECT"]}', 'case_officer', 0),
 (@def1, 'mark_withdrawn', @s1_SUBMITTED, @s1_WITHDRAWN, null, 'case_officer', 0),
 (@def1, 'mark_withdrawn', @s1_UNDER_REVIEW, @s1_WITHDRAWN, null, 'case_officer', 0),
 (@def1, 'mark_withdrawn', @s1_DOCUMENTS_REQUIRED, @s1_WITHDRAWN, null, 'case_officer', 0),
 (@def1, 'mark_withdrawn', @s1_READY_FOR_UNIVERSITY_SUBMISSION, @s1_WITHDRAWN, null, 'case_officer', 0),
 (@def1, 'mark_withdrawn', @s1_SUBMITTED_TO_UNIVERSITY, @s1_WITHDRAWN, null, 'case_officer', 0),
 (@def1, 'mark_withdrawn', @s1_UNDER_UNIVERSITY_REVIEW, @s1_WITHDRAWN, null, 'case_officer', 0),
 (@def1, 'mark_withdrawn', @s1_INTERVIEW_REQUIRED, @s1_WITHDRAWN, null, 'case_officer', 0),
 (@def1, 'mark_withdrawn', @s1_CONDITIONAL_OFFER, @s1_WITHDRAWN, null, 'case_officer', 0),
 (@def1, 'mark_withdrawn', @s1_OFFER_RECEIVED, @s1_WITHDRAWN, null, 'case_officer', 0),
 (@def1, 'mark_withdrawn', @s1_OFFER_ACCEPTED, @s1_WITHDRAWN, null, 'case_officer', 0),
 (@def1, 'mark_withdrawn', @s1_WAITLISTED, @s1_WITHDRAWN, null, 'case_officer', 0),
 (@def1, 'mark_withdrawn', @s1_ADMISSION_CONFIRMED, @s1_WITHDRAWN, null, 'case_officer', 0);

insert into nad_wf_stage_task_template (stage_id, title, role_required, mandatory, blocks_exit, order_no) values
 (@s1_UNDER_REVIEW, 'Verify the applicant meets the entry requirements', null, 1, 0, 1),
 (@s1_UNDER_REVIEW, 'Review each uploaded document', 'document_reviewer', 1, 0, 2),
 (@s1_DOCUMENTS_REQUIRED, 'Follow up with the student if the documents do not arrive', null, 0, 0, 1),
 (@s1_READY_FOR_UNIVERSITY_SUBMISSION, 'Complete the university application form', null, 1, 0, 1),
 (@s1_READY_FOR_UNIVERSITY_SUBMISSION, 'Compile the transcripts and certificates package', null, 1, 0, 2),
 (@s1_READY_FOR_UNIVERSITY_SUBMISSION, 'Internal quality check of the package', 'ops_manager', 1, 0, 3),
 (@s1_SUBMITTED_TO_UNIVERSITY, 'Record the submission reference or university portal acknowledgement', null, 1, 0, 1),
 (@s1_UNDER_UNIVERSITY_REVIEW, 'Follow up with the university if there is no answer by the deadline', null, 0, 0, 1),
 (@s1_INTERVIEW_REQUIRED, 'Schedule the interview and send the details to the student', null, 1, 0, 1),
 (@s1_CONDITIONAL_OFFER, 'Explain the conditions of the offer to the student', null, 1, 0, 1),
 (@s1_OFFER_RECEIVED, 'Explain the offer to the student', null, 1, 0, 1),
 (@s1_OFFER_ACCEPTED, 'Confirm the student''s acceptance with the university', null, 1, 0, 1),
 (@s1_ADMISSION_CONFIRMED, 'Upload the admission letter', null, 1, 0, 1),
 (@s1_ADMISSION_CONFIRMED, 'Upload the JW202 form', null, 1, 0, 2);

update nad_wf_definition set status = 'RETIRED', update_by = 'flyway-V140', update_time = now()
 where code = 'PROGRAM_ONLY_V1' and version = 1 and status = 'ACTIVE';

-- ---- SCHOLARSHIP_ONLY_V1 v2 ----
insert into nad_wf_definition (code, name, version, status, create_by, create_time)
values ('SCHOLARSHIP_ONLY_V1', 'Scholarship only (v2)', 2, 'ACTIVE', 'flyway-V140', now());
set @def2 := last_insert_id();

insert into nad_wf_stage (definition_id, code, name, order_no, stage_type, status_label, sla_hours) values
 (@def2, 'DRAFT', 'Draft', 1, 'START', 'DRAFT', null),
 (@def2, 'SUBMITTED', 'Submitted', 2, 'NORMAL', 'SUBMITTED', 16),
 (@def2, 'UNDER_REVIEW', 'Under review', 3, 'NORMAL', 'UNDER_REVIEW', 48),
 (@def2, 'DOCUMENTS_REQUIRED', 'Documents required', 4, 'NORMAL', 'DOCUMENTS_REQUIRED', null),
 (@def2, 'READY_FOR_UNIVERSITY_SUBMISSION', 'Ready for provider submission', 5, 'NORMAL', 'READY_FOR_UNIVERSITY_SUBMISSION', 40),
 (@def2, 'SUBMITTED_TO_UNIVERSITY', 'Submitted to provider', 6, 'NORMAL', 'SUBMITTED_TO_UNIVERSITY', null),
 (@def2, 'UNDER_UNIVERSITY_REVIEW', 'Under provider review', 7, 'NORMAL', 'UNDER_UNIVERSITY_REVIEW', null),
 (@def2, 'INTERVIEW_REQUIRED', 'Interview required', 8, 'NORMAL', 'INTERVIEW_REQUIRED', null),
 (@def2, 'CONDITIONAL_OFFER', 'Conditional offer', 9, 'NORMAL', 'CONDITIONAL_OFFER', null),
 (@def2, 'OFFER_RECEIVED', 'Offer received', 10, 'NORMAL', 'OFFER_RECEIVED', null),
 (@def2, 'OFFER_ACCEPTED', 'Offer accepted', 11, 'NORMAL', 'OFFER_ACCEPTED', null),
 (@def2, 'WAITLISTED', 'Waitlisted', 12, 'NORMAL', 'WAITLISTED', null),
 (@def2, 'ADMISSION_CONFIRMED', 'Admission confirmed', 13, 'NORMAL', 'ADMISSION_CONFIRMED', null),
 (@def2, 'ENROLLED', 'Enrolled', 14, 'TERMINAL', 'CLOSED_SUCCESS', null),
 (@def2, 'REJECTED', 'Rejected', 15, 'TERMINAL', 'CLOSED_UNSUCCESSFUL', null),
 (@def2, 'WITHDRAWN', 'Withdrawn', 16, 'TERMINAL', 'CLOSED_WITHDRAWN', null);

set @s2_DRAFT := (select id from nad_wf_stage where definition_id = @def2 and code = 'DRAFT');
set @s2_SUBMITTED := (select id from nad_wf_stage where definition_id = @def2 and code = 'SUBMITTED');
set @s2_UNDER_REVIEW := (select id from nad_wf_stage where definition_id = @def2 and code = 'UNDER_REVIEW');
set @s2_DOCUMENTS_REQUIRED := (select id from nad_wf_stage where definition_id = @def2 and code = 'DOCUMENTS_REQUIRED');
set @s2_READY_FOR_UNIVERSITY_SUBMISSION := (select id from nad_wf_stage where definition_id = @def2 and code = 'READY_FOR_UNIVERSITY_SUBMISSION');
set @s2_SUBMITTED_TO_UNIVERSITY := (select id from nad_wf_stage where definition_id = @def2 and code = 'SUBMITTED_TO_UNIVERSITY');
set @s2_UNDER_UNIVERSITY_REVIEW := (select id from nad_wf_stage where definition_id = @def2 and code = 'UNDER_UNIVERSITY_REVIEW');
set @s2_INTERVIEW_REQUIRED := (select id from nad_wf_stage where definition_id = @def2 and code = 'INTERVIEW_REQUIRED');
set @s2_CONDITIONAL_OFFER := (select id from nad_wf_stage where definition_id = @def2 and code = 'CONDITIONAL_OFFER');
set @s2_OFFER_RECEIVED := (select id from nad_wf_stage where definition_id = @def2 and code = 'OFFER_RECEIVED');
set @s2_OFFER_ACCEPTED := (select id from nad_wf_stage where definition_id = @def2 and code = 'OFFER_ACCEPTED');
set @s2_WAITLISTED := (select id from nad_wf_stage where definition_id = @def2 and code = 'WAITLISTED');
set @s2_ADMISSION_CONFIRMED := (select id from nad_wf_stage where definition_id = @def2 and code = 'ADMISSION_CONFIRMED');
set @s2_ENROLLED := (select id from nad_wf_stage where definition_id = @def2 and code = 'ENROLLED');
set @s2_REJECTED := (select id from nad_wf_stage where definition_id = @def2 and code = 'REJECTED');
set @s2_WITHDRAWN := (select id from nad_wf_stage where definition_id = @def2 and code = 'WITHDRAWN');

insert into nad_wf_transition (definition_id, code, from_stage_id, to_stage_id, guard_json, role_required, auto) values
 (@def2, 'submit', @s2_DRAFT, @s2_SUBMITTED, '{"all":["FIELD_SET:scholarship_id"]}', null, 0),
 (@def2, 'claim', @s2_SUBMITTED, @s2_UNDER_REVIEW, null, 'case_officer', 0),
 (@def2, 'request_documents', @s2_UNDER_REVIEW, @s2_DOCUMENTS_REQUIRED, '{"all":["DECISION_RECORDED:DOCUMENT_REVIEW:DOCUMENTS_REQUIRED"]}', 'case_officer', 0),
 (@def2, 'resubmit', @s2_DOCUMENTS_REQUIRED, @s2_UNDER_REVIEW, '{"all":["ALL_REQUIRED_DOCUMENTS_ATTACHED"]}', null, 0),
 (@def2, 'mark_ready', @s2_UNDER_REVIEW, @s2_READY_FOR_UNIVERSITY_SUBMISSION, '{"all":["DECISION_RECORDED:NADOUMI_INTERNAL:ELIGIBLE","ALL_REQUIRED_DOCUMENTS_ATTACHED","ALL_MANDATORY_TASKS_DONE"]}', 'case_officer', 0),
 (@def2, 'ineligible', @s2_UNDER_REVIEW, @s2_REJECTED, '{"all":["DECISION_RECORDED:NADOUMI_INTERNAL:INELIGIBLE"]}', 'case_officer', 0),
 (@def2, 'send_to_university', @s2_READY_FOR_UNIVERSITY_SUBMISSION, @s2_SUBMITTED_TO_UNIVERSITY, '{"all":["ALL_MANDATORY_TASKS_DONE"]}', 'case_officer', 0),
 (@def2, 'start_university_review', @s2_SUBMITTED_TO_UNIVERSITY, @s2_UNDER_UNIVERSITY_REVIEW, '{"all":["ALL_MANDATORY_TASKS_DONE"]}', 'case_officer', 0),
 (@def2, 'request_interview', @s2_UNDER_UNIVERSITY_REVIEW, @s2_INTERVIEW_REQUIRED, '{"all":["DECISION_RECORDED:UNIVERSITY_REVIEW:INTERVIEW_REQUIRED"]}', 'case_officer', 0),
 (@def2, 'interview_done', @s2_INTERVIEW_REQUIRED, @s2_UNDER_UNIVERSITY_REVIEW, '{"all":["ALL_MANDATORY_TASKS_DONE"]}', 'case_officer', 0),
 (@def2, 'conditions_met', @s2_CONDITIONAL_OFFER, @s2_OFFER_RECEIVED, '{"all":["DECISION_RECORDED:OFFER_CONDITIONS:MET"]}', 'case_officer', 0),
 (@def2, 'accept_offer', @s2_OFFER_RECEIVED, @s2_OFFER_ACCEPTED, '{"all":["DECISION_RECORDED:APPLICANT_RESPONSE:ACCEPTED_BY_APPLICANT"]}', null, 0),
 (@def2, 'accept_offer', @s2_CONDITIONAL_OFFER, @s2_OFFER_ACCEPTED, '{"all":["DECISION_RECORDED:APPLICANT_RESPONSE:ACCEPTED_BY_APPLICANT"]}', null, 0),
 (@def2, 'decline_offer', @s2_OFFER_RECEIVED, @s2_WITHDRAWN, '{"all":["DECISION_RECORDED:APPLICANT_RESPONSE:DECLINED_BY_APPLICANT"]}', null, 0),
 (@def2, 'decline_offer', @s2_CONDITIONAL_OFFER, @s2_WITHDRAWN, '{"all":["DECISION_RECORDED:APPLICANT_RESPONSE:DECLINED_BY_APPLICANT"]}', null, 0),
 (@def2, 'confirm_admission', @s2_OFFER_ACCEPTED, @s2_ADMISSION_CONFIRMED, '{"all":["DECISION_RECORDED:ADMISSION:CONFIRMED","ALL_MANDATORY_TASKS_DONE"]}', 'case_officer', 0),
 (@def2, 'enroll', @s2_ADMISSION_CONFIRMED, @s2_ENROLLED, '{"all":["PAYMENT_SETTLED:SERVICE_FEE","ALL_MANDATORY_TASKS_DONE"]}', 'case_officer', 0),
 (@def2, 'offer', @s2_UNDER_UNIVERSITY_REVIEW, @s2_OFFER_RECEIVED, '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:OFFER"]}', 'case_officer', 0),
 (@def2, 'conditional_offer', @s2_UNDER_UNIVERSITY_REVIEW, @s2_CONDITIONAL_OFFER, '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:CONDITIONAL_OFFER"]}', 'case_officer', 0),
 (@def2, 'waitlist', @s2_UNDER_UNIVERSITY_REVIEW, @s2_WAITLISTED, '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:WAITLIST"]}', 'case_officer', 0),
 (@def2, 'reject', @s2_UNDER_UNIVERSITY_REVIEW, @s2_REJECTED, '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:REJECT"]}', 'case_officer', 0),
 (@def2, 'offer', @s2_INTERVIEW_REQUIRED, @s2_OFFER_RECEIVED, '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:OFFER"]}', 'case_officer', 0),
 (@def2, 'conditional_offer', @s2_INTERVIEW_REQUIRED, @s2_CONDITIONAL_OFFER, '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:CONDITIONAL_OFFER"]}', 'case_officer', 0),
 (@def2, 'waitlist', @s2_INTERVIEW_REQUIRED, @s2_WAITLISTED, '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:WAITLIST"]}', 'case_officer', 0),
 (@def2, 'reject', @s2_INTERVIEW_REQUIRED, @s2_REJECTED, '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:REJECT"]}', 'case_officer', 0),
 (@def2, 'offer', @s2_WAITLISTED, @s2_OFFER_RECEIVED, '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:OFFER"]}', 'case_officer', 0),
 (@def2, 'conditional_offer', @s2_WAITLISTED, @s2_CONDITIONAL_OFFER, '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:CONDITIONAL_OFFER"]}', 'case_officer', 0),
 (@def2, 'reject', @s2_WAITLISTED, @s2_REJECTED, '{"all":["DECISION_RECORDED:UNIVERSITY_OFFER:REJECT"]}', 'case_officer', 0),
 (@def2, 'mark_withdrawn', @s2_SUBMITTED, @s2_WITHDRAWN, null, 'case_officer', 0),
 (@def2, 'mark_withdrawn', @s2_UNDER_REVIEW, @s2_WITHDRAWN, null, 'case_officer', 0),
 (@def2, 'mark_withdrawn', @s2_DOCUMENTS_REQUIRED, @s2_WITHDRAWN, null, 'case_officer', 0),
 (@def2, 'mark_withdrawn', @s2_READY_FOR_UNIVERSITY_SUBMISSION, @s2_WITHDRAWN, null, 'case_officer', 0),
 (@def2, 'mark_withdrawn', @s2_SUBMITTED_TO_UNIVERSITY, @s2_WITHDRAWN, null, 'case_officer', 0),
 (@def2, 'mark_withdrawn', @s2_UNDER_UNIVERSITY_REVIEW, @s2_WITHDRAWN, null, 'case_officer', 0),
 (@def2, 'mark_withdrawn', @s2_INTERVIEW_REQUIRED, @s2_WITHDRAWN, null, 'case_officer', 0),
 (@def2, 'mark_withdrawn', @s2_CONDITIONAL_OFFER, @s2_WITHDRAWN, null, 'case_officer', 0),
 (@def2, 'mark_withdrawn', @s2_OFFER_RECEIVED, @s2_WITHDRAWN, null, 'case_officer', 0),
 (@def2, 'mark_withdrawn', @s2_OFFER_ACCEPTED, @s2_WITHDRAWN, null, 'case_officer', 0),
 (@def2, 'mark_withdrawn', @s2_WAITLISTED, @s2_WITHDRAWN, null, 'case_officer', 0),
 (@def2, 'mark_withdrawn', @s2_ADMISSION_CONFIRMED, @s2_WITHDRAWN, null, 'case_officer', 0);

insert into nad_wf_stage_task_template (stage_id, title, role_required, mandatory, blocks_exit, order_no) values
 (@s2_UNDER_REVIEW, 'Verify the applicant meets the entry requirements', null, 1, 0, 1),
 (@s2_UNDER_REVIEW, 'Review each uploaded document', 'document_reviewer', 1, 0, 2),
 (@s2_DOCUMENTS_REQUIRED, 'Follow up with the student if the documents do not arrive', null, 0, 0, 1),
 (@s2_READY_FOR_UNIVERSITY_SUBMISSION, 'Complete the provider application form', null, 1, 0, 1),
 (@s2_READY_FOR_UNIVERSITY_SUBMISSION, 'Compile the transcripts and certificates package', null, 1, 0, 2),
 (@s2_READY_FOR_UNIVERSITY_SUBMISSION, 'Internal quality check of the package', 'ops_manager', 1, 0, 3),
 (@s2_SUBMITTED_TO_UNIVERSITY, 'Record the submission reference or provider portal acknowledgement', null, 1, 0, 1),
 (@s2_UNDER_UNIVERSITY_REVIEW, 'Follow up with the provider if there is no answer by the deadline', null, 0, 0, 1),
 (@s2_INTERVIEW_REQUIRED, 'Schedule the interview and send the details to the student', null, 1, 0, 1),
 (@s2_CONDITIONAL_OFFER, 'Explain the conditions of the offer to the student', null, 1, 0, 1),
 (@s2_OFFER_RECEIVED, 'Explain the offer to the student', null, 1, 0, 1),
 (@s2_OFFER_ACCEPTED, 'Confirm the student''s acceptance with the provider', null, 1, 0, 1),
 (@s2_ADMISSION_CONFIRMED, 'Upload the admission letter', null, 1, 0, 1),
 (@s2_ADMISSION_CONFIRMED, 'Upload the JW202 form', null, 1, 0, 2);

update nad_wf_definition set status = 'RETIRED', update_by = 'flyway-V140', update_time = now()
 where code = 'SCHOLARSHIP_ONLY_V1' and version = 1 and status = 'ACTIVE';
