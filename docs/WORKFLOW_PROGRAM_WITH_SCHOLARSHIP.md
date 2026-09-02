# Nadoumi — Workflow Definition: `PROGRAM_WITH_SCHOLARSHIP_V1`

Status: **BASELINE** (Phase 1, approved). The first concrete
`nad_wf_definition` (D4, D9). Realises `docs/APPLICATION_WORKFLOW.md` for
`nad_application.application_type = 'PROGRAM_WITH_SCHOLARSHIP'`.

> This is a **design artifact**. The seed rows in §8 are illustrative and are applied
> as a Phase 3 Flyway migration (`V7`/`V8` region) — **not executed now**.

---

## 1. Scope & assumptions

- Applies to applications where the student pursues **admission to a programme AND a
  funding request decided alongside it** (`program_id`, `scholarship_id`, `intake_id`
  all set — INV7).
- The target university is known from `program.university_id` (public). The
  scholarship→university linkage (`nad_scholarship_internal`) is only relevant to
  staff holding `nad:scholarship:internal:view`.
- Engine: data-driven (`WorkflowService`), structured guard predicates only (D4). No
  expression language.
- Instance pins `definition_version = 1`. A later `PROGRAM_WITH_SCHOLARSHIP_V2` does
  not migrate live instances.
- Timers/SLA depend on the Quartz **JDBC** job store (Phase 2, risk R2).

## 2. Guard predicate vocabulary (fixed set — D4)

| Predicate | True when |
| --- | --- |
| `ALL_MANDATORY_TASKS_DONE` | every `nad_application_task` on the current stage with `mandatory=1` is `DONE` or `SKIPPED`. |
| `TASKS_BLOCKING_EXIT_DONE` | every current-stage task with `blocks_exit=1` is `DONE`/`SKIPPED` (subset of the above; the engine always enforces this). |
| `ALL_REQUIRED_DOCUMENTS_ATTACHED` | for every `mandatory` `nad_document_requirement` in scope, a `nad_application_document` row exists. |
| `ALL_REQUIRED_DOCUMENTS_VERIFIED` | …and each linked document's `current_version` has `verification_status='VERIFIED'` and is not expired. |
| `DECISION_RECORDED(type[,outcome∈{…}])` | a `nad_application_decision` row exists with that `decision_type` (and, if given, `outcome` in the set). |
| `PAYMENT_SETTLED(kind)` | a `nad_payment` of that kind for this application is `SETTLED`. |
| `FIELD_SET(name)` | the named `nad_application` column is non-null. |

Guards compose with AND only. If a transition needs OR, model it as two transitions.

## 3. Stages

| # | `code` | `stage_type` | `current_status` (denormalised) | Owner | SLA (business days) |
| --- | --- | --- | --- | --- | --- |
| 1 | `DRAFT` | `START` | `DRAFT` | applicant / agent | — |
| 2 | `SUBMITTED` | `NORMAL` | `IN_REVIEW` | triage desk | 2 (to triage) |
| 3 | `ELIGIBILITY_REVIEW` | `NORMAL` | `IN_REVIEW` | `case_officer` | 3 |
| 4 | `DOCUMENT_COLLECTION` | `NORMAL` | `IN_REVIEW` | `case_officer` + applicant | 15 (applicant-dependent) |
| 5 | `PACKAGE_PREPARATION` | `NORMAL` | `IN_REVIEW` | `case_officer` | 5 |
| 6 | `SUBMITTED_TO_UNIVERSITY` | `NORMAL` | `SUBMITTED_TO_UNIVERSITY` | external (university) | 30 (monitor only) |
| 7 | `UNIVERSITY_DECISION` | `DECISION` | `DECISION` | `case_officer` | 3 (to record) |
| 8 | `SCHOLARSHIP_DECISION` | `DECISION` | `DECISION` | `case_officer` | 3 (to record) |
| 9 | `OFFER_RESPONSE` | `NORMAL` | `OFFER` | applicant | 10 |
| 10 | `PRE_DEPARTURE` | `NORMAL` | `PRE_DEPARTURE` | `case_officer` + applicant | 30 |
| 11 | `ENROLLED` | `TERMINAL` | `CLOSED_SUCCESS` | — | — |
| 12 | `UNSUCCESSFUL` | `TERMINAL` | `CLOSED_UNSUCCESSFUL` | — | — |
| 13 | `WITHDRAWN` | `TERMINAL` | `CLOSED_WITHDRAWN` | — | — |

`current_status` is written **only** by the engine on stage entry (INV9).

## 4. Transitions

`auto` = engine fires it automatically when `from` stage is entered and the guard is
already true. Actor "student" = a `nad_user_applicant_access` holder with the named
capability; actor "staff:`role`" = `@PreAuthorize` on `nad:application:transition`
plus the role.

| `code` | from → to | Guard | Actor | On-enter side effects |
| --- | --- | --- | --- | --- |
| `submit` | `DRAFT → SUBMITTED` | `FIELD_SET(program_id)` ∧ `FIELD_SET(scholarship_id)` ∧ `FIELD_SET(intake_id)` ∧ `ALL_REQUIRED_DOCUMENTS_ATTACHED` | student `SUBMIT_APPLICATION` | stamp `submitted_at`; notify triage desk; `event: APPLICATION_SUBMITTED` |
| `claim` | `SUBMITTED → ELIGIBILITY_REVIEW` | — | staff:`case_officer` (`application:claim`) | set `assignee_user_id`; materialise stage-3 tasks; `event: CASE_ASSIGNED` |
| `eligible` | `ELIGIBILITY_REVIEW → DOCUMENT_COLLECTION` | `DECISION_RECORDED(NADOUMI_INTERNAL, outcome∈{ELIGIBLE})` ∧ `ALL_MANDATORY_TASKS_DONE` | staff:`case_officer` | materialise document requirements → checklist; notify applicant of required docs |
| `ineligible` | `ELIGIBILITY_REVIEW → UNSUCCESSFUL` | `DECISION_RECORDED(NADOUMI_INTERNAL, outcome∈{INELIGIBLE})` | staff:`case_officer` | notify applicant (templated, reason from decision rationale) |
| `docs_complete` | `DOCUMENT_COLLECTION → PACKAGE_PREPARATION` | `ALL_REQUIRED_DOCUMENTS_VERIFIED` ∧ `ALL_MANDATORY_TASKS_DONE` | staff:`case_officer` | materialise packaging tasks |
| `package_ready` | `PACKAGE_PREPARATION → SUBMITTED_TO_UNIVERSITY` | `ALL_MANDATORY_TASKS_DONE` ∧ `PAYMENT_SETTLED(APPLICATION_FEE)` | staff:`case_officer` | create `nad_application_submission` (channel, external_reference); `event: SUBMITTED_TO_UNIVERSITY`; notify applicant |
| `university_responded` | `SUBMITTED_TO_UNIVERSITY → UNIVERSITY_DECISION` | — | staff:`case_officer` | — |
| `uni_offer` | `UNIVERSITY_DECISION → SCHOLARSHIP_DECISION` | `DECISION_RECORDED(UNIVERSITY_OFFER, outcome∈{OFFER,CONDITIONAL_OFFER})` | staff:`case_officer` | notify applicant (offer received, scholarship pending) |
| `uni_waitlist` | `UNIVERSITY_DECISION → SUBMITTED_TO_UNIVERSITY` | `DECISION_RECORDED(UNIVERSITY_OFFER, outcome∈{WAITLIST})` | staff:`case_officer` | set a follow-up task with due date |
| `uni_reject` | `UNIVERSITY_DECISION → UNSUCCESSFUL` | `DECISION_RECORDED(UNIVERSITY_OFFER, outcome∈{REJECT})` | staff:`case_officer` | notify applicant |
| `sch_awarded` | `SCHOLARSHIP_DECISION → OFFER_RESPONSE` | `DECISION_RECORDED(SCHOLARSHIP_AWARD, outcome∈{AWARDED})` | staff:`case_officer` | notify applicant (full outcome); materialise response tasks |
| `sch_declined_body` | `SCHOLARSHIP_DECISION → OFFER_RESPONSE` | `DECISION_RECORDED(SCHOLARSHIP_AWARD, outcome∈{REJECT})` | staff:`case_officer` | notify applicant (offer stands, scholarship not awarded — self-funded decision) |
| `accept_offer` | `OFFER_RESPONSE → PRE_DEPARTURE` | `DECISION_RECORDED(APPLICANT_RESPONSE, outcome∈{ACCEPTED_BY_APPLICANT})` | student `SUBMIT_APPLICATION` **or** staff:`case_officer` recording it | materialise pre-departure tasks; `event: OFFER_ACCEPTED` |
| `decline_offer` | `OFFER_RESPONSE → UNSUCCESSFUL` | `DECISION_RECORDED(APPLICANT_RESPONSE, outcome∈{DECLINED_BY_APPLICANT})` | student / staff | notify case officer |
| `enrolled` | `PRE_DEPARTURE → ENROLLED` | `ALL_MANDATORY_TASKS_DONE` | staff:`case_officer` | `event: ENROLLED`; close instance; trigger commission-accrual hook (Reporting/Finance, later) |
| `withdraw` | `{DRAFT, SUBMITTED, ELIGIBILITY_REVIEW, DOCUMENT_COLLECTION, PACKAGE_PREPARATION, SUBMITTED_TO_UNIVERSITY, UNIVERSITY_DECISION, SCHOLARSHIP_DECISION, OFFER_RESPONSE, PRE_DEPARTURE} → WITHDRAWN` | reason required (free text on the transition, stored in `stage_history.reason`) | student `SUBMIT_APPLICATION` **or** staff:`ops_manager`/`case_officer` | `event: APPLICATION_WITHDRAWN`; close instance |

Definition-validity checks (enforced at `activate`): exactly one `START`; ≥1
`TERMINAL`; every non-terminal stage has ≥1 outgoing transition; no unreachable stage;
every `DECISION` stage has ≥2 outgoing transitions distinguished by
`DECISION_RECORDED(...)` guards.

## 5. Stage task templates (`nad_wf_stage_task_template`)

| Stage | Task title | `mandatory` | `blocks_exit` | Default assignee |
| --- | --- | :---: | :---: | --- |
| `ELIGIBILITY_REVIEW` | Verify applicant meets programme entry requirements | ✅ | ✅ | assignee |
| `ELIGIBILITY_REVIEW` | Verify applicant meets scholarship eligibility | ✅ | ✅ | assignee |
| `ELIGIBILITY_REVIEW` | Record eligibility decision (`NADOUMI_INTERNAL`) | ✅ | ✅ | assignee |
| `DOCUMENT_COLLECTION` | Send document checklist to applicant | ✅ | ✅ | assignee |
| `DOCUMENT_COLLECTION` | Review & verify each submitted document | ✅ | ✅ | `document_reviewer` |
| `DOCUMENT_COLLECTION` | Confirm language requirement satisfied | ✅ | ✅ | assignee |
| `PACKAGE_PREPARATION` | Complete university application form | ✅ | ✅ | assignee |
| `PACKAGE_PREPARATION` | Complete scholarship application form | ✅ | ✅ | assignee |
| `PACKAGE_PREPARATION` | Compile transcripts / certificates package | ✅ | ✅ | assignee |
| `PACKAGE_PREPARATION` | Collect application fee | ✅ | ✅ | `finance` |
| `PACKAGE_PREPARATION` | Internal QA of the package | ✅ | ✅ | `ops_manager` |
| `SUBMITTED_TO_UNIVERSITY` | Record submission reference / portal ack | ✅ | ✅ | assignee |
| `SUBMITTED_TO_UNIVERSITY` | Follow up if no response by SLA | ❌ | ❌ | assignee |
| `OFFER_RESPONSE` | Explain offer + scholarship outcome to applicant | ✅ | ❌ | assignee |
| `OFFER_RESPONSE` | Record applicant's accept/decline decision | ✅ | ✅ | assignee |
| `PRE_DEPARTURE` | Confirm enrolment / acceptance with university | ✅ | ✅ | assignee |
| `PRE_DEPARTURE` | Support visa application | ✅ | ✅ | assignee |
| `PRE_DEPARTURE` | Confirm tuition deposit paid | ✅ | ✅ | `finance` |
| `PRE_DEPARTURE` | Pre-departure briefing | ❌ | ❌ | assignee |

Ad-hoc tasks (staff-added, `wf_stage_task_template_id = NULL`) live in the **same**
`nad_application_task` table (D12) and can also be marked `blocks_exit`.

## 6. Document requirements

Seed `nad_document_requirement` rows (scope varies). "Waivable" = the checklist item
can be `SKIPPED` by a `case_officer` with a recorded reason.

| `doc_type` | Scope | `mandatory` | Waivable | Notes |
| --- | --- | :---: | :---: | --- |
| `PASSPORT` | `WF_STAGE:DOCUMENT_COLLECTION` | ✅ | ❌ | expiry ≥ 6 months beyond intake. |
| `PHOTO` | `WF_STAGE:DOCUMENT_COLLECTION` | ✅ | ❌ | |
| `ACADEMIC_TRANSCRIPT` | `WF_STAGE:DOCUMENT_COLLECTION` | ✅ | ❌ | most recent level. |
| `DEGREE_CERTIFICATE` | `PROGRAM` (graduate programmes) | ✅ | ✅ | provisional accepted with a task. |
| `LANGUAGE_CERTIFICATE` | `PROGRAM` | ✅ | ✅ | waived if instruction language = applicant's first language, decision recorded. |
| `CV` | `SCHOLARSHIP` | ✅ | ❌ | |
| `PERSONAL_STATEMENT` | `SCHOLARSHIP` | ✅ | ❌ | |
| `RECOMMENDATION_LETTER` | `SCHOLARSHIP` | ✅ | ❌ | ×2 (two `nad_application_document` links). |
| `STUDY_PLAN` | `SCHOLARSHIP` | ✅ | ❌ | scholarship-specific. |
| `FINANCIAL_PROOF` | `SCHOLARSHIP` | ✅ | ✅ | waived for full scholarships, decision recorded. |
| `MEDICAL_FORM` | `WF_STAGE:PRE_DEPARTURE` | ✅ | ❌ | |
| `POLICE_CLEARANCE` | `PROGRAM` (country-dependent) | ❌ | ✅ | required for some destinations. |

## 7. Notifications (per transition / event)

| Trigger | Recipients | Channels (v1) | Template `type` |
| --- | --- | --- | --- |
| `submit` | triage desk | IN_APP | `APPLICATION_SUBMITTED_STAFF` |
| `eligible` | applicant (OWNER/AGENT/GUARDIAN) | IN_APP + EMAIL | `DOCUMENTS_REQUESTED` |
| `ineligible` / `uni_reject` / `decline_offer` | applicant | IN_APP + EMAIL | `APPLICATION_CLOSED_UNSUCCESSFUL` |
| document `REJECTED` | applicant | IN_APP + EMAIL | `DOCUMENT_REJECTED` (carries `rejection_reason`) |
| `package_ready` | applicant | IN_APP + EMAIL | `SUBMITTED_TO_UNIVERSITY` |
| `uni_offer` | applicant + assignee | IN_APP + EMAIL | `UNIVERSITY_OFFER` |
| `sch_awarded` | applicant + assignee | IN_APP + EMAIL | `SCHOLARSHIP_AWARDED` |
| SLA breach (any stage) | assignee + `ops_manager` | IN_APP | `SLA_BREACH` |
| `enrolled` | applicant + assignee + `ops_manager` | IN_APP + EMAIL | `ENROLLED` |

All templates reference IDs + safe fields only — no PII in email/SMS bodies
(`docs/SECURITY.md` §6).

## 8. Seed data (illustrative — Phase 3 migration, NOT executed now)

```text
nad_wf_definition
  code='PROGRAM_WITH_SCHOLARSHIP_V1'  name='Program + Scholarship (v1)'  version=1  status='ACTIVE'

nad_wf_stage         (definition_id → above; order_no ascending)
  DRAFT(START) SUBMITTED ELIGIBILITY_REVIEW DOCUMENT_COLLECTION PACKAGE_PREPARATION
  SUBMITTED_TO_UNIVERSITY UNIVERSITY_DECISION(DECISION) SCHOLARSHIP_DECISION(DECISION)
  OFFER_RESPONSE PRE_DEPARTURE ENROLLED(TERMINAL) UNSUCCESSFUL(TERMINAL) WITHDRAWN(TERMINAL)
  sla_hours: SUBMITTED=16 ELIGIBILITY_REVIEW=24 DOCUMENT_COLLECTION=120
             PACKAGE_PREPARATION=40 UNIVERSITY_DECISION=24 SCHOLARSHIP_DECISION=24
             OFFER_RESPONSE=80 PRE_DEPARTURE=240        (8h business day)

nad_wf_transition    → the 15 rows in §4 (code, from_stage_id, to_stage_id, guard_json, auto=0)
nad_wf_stage_task_template → the 19 rows in §5
nad_document_requirement  → the 12 rows in §6

# nad_program.workflow_definition_code / nad_scholarship.workflow_definition_code
#   may be set to 'PROGRAM_WITH_SCHOLARSHIP_V1' to route matching applications here;
#   otherwise the engine falls back to the default definition for the application_type.
```

`guard_json` example (transition `docs_complete`):
`{"all":["ALL_REQUIRED_DOCUMENTS_VERIFIED","ALL_MANDATORY_TASKS_DONE"]}`

## 9. Tests (Phase 3)

- Definition validity checks (§4) run in `WorkflowDefinitionValidationTest`.
- Happy path: `DRAFT → … → ENROLLED` with all guards satisfied in order.
- Guard blocks: `docs_complete` refused while a mandatory document is `PENDING`;
  `package_ready` refused while `APPLICATION_FEE` unsettled; `eligible` refused with no
  `NADOUMI_INTERNAL` decision.
- `DECISION` fan-out: `uni_reject` vs `uni_offer` chosen by the recorded outcome.
- `withdraw` from every non-terminal stage lands in `WITHDRAWN`, writes history, closes
  the instance.
- Version pinning: activating `…_V2` does not alter a running `…_V1` instance.
- Optimistic-lock conflict on concurrent transition → 409.
