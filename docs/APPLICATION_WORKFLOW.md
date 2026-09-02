# Nadoumi — Application Workflow

Status: **BASELINE** (approved) · **EXISTING** (in the repo today) · **PLANNED**
(approved, not built) · **OPEN** (needs a decision).

> Reconciled with `DOMAIN_MODEL.md` / `DATABASE_DESIGN.md` Rev 3 and
> `ARCHITECTURE.md` §7 (D4, D12 approved). Concrete first process:
> `docs/WORKFLOW_PROGRAM_WITH_SCHOLARSHIP.md`. Draft DDL: `docs/ddl/nad_core.draft.sql`
> §6, §8.

---

## 1. What exists today (EXISTING)

**Nothing application-specific.** No application entity, no workflow engine.

Reusable primitives:
- `sys_dict_type` / `sys_dict_data` (Redis-cached) — controlled vocabularies only,
  **not** a process engine.
- `ruoyi-quartz` (`sys_job`, `sys_job_log`) for SLA sweeps / deadline reminders.
  **Phase 2 done:** JDBC clustered job store (`LocalDataSourceJobStore`) is active and
  restart-persistence is verified — timers can now be relied on.
- `AsyncManager` / `AsyncFactory` / `ThreadPoolConfig` for post-transition side
  effects.
- `@Log` → `sys_oper_log` (technical audit); `BusinessType` enum.
- No Flowable / Camunda / Spring Statemachine on the classpath — and none is added
  (D4).

## 2. Requirements (CLAUDE.md §9–§10)

Application = a business case: applicant + opportunity + current stage + current
status + assignment + tasks + documents + notes + events + decisions + messages +
**history**. Auditable; history never silently overwritten. Different programmes /
scholarships may run different processes — no single global status enum.

## 3. Model (BASELINE)

### 3.1 Definition layer (versioned config) — DDL §6

```
nad_wf_definition          id, code, name, version, status(DRAFT|ACTIVE|RETIRED)
nad_wf_stage               id, definition_id, code, name, order_no,
                           stage_type(START|NORMAL|DECISION|TERMINAL),
                           status_label, sla_hours?
nad_wf_transition          id, definition_id, code, from_stage_id?, to_stage_id,
                           guard_json?, auto(bool)
nad_wf_stage_task_template  id, stage_id, title, role_required?, mandatory, blocks_exit, order_no
```

- `nad_program.workflow_definition_code` / `nad_scholarship.workflow_definition_code`
  route an application to a definition; the engine resolves the **`ACTIVE`** version at
  instance creation. `NULL` → the default definition for the `application_type`.
- **Guards are a fixed predicate set (D4)** — `guard_json` is only
  `{"all":[<predicate>, …]}` where each predicate is one of:
  `ALL_MANDATORY_TASKS_DONE`, `TASKS_BLOCKING_EXIT_DONE`,
  `ALL_REQUIRED_DOCUMENTS_ATTACHED`, `ALL_REQUIRED_DOCUMENTS_VERIFIED`,
  `DECISION_RECORDED(type[,outcome∈{…}])`, `PAYMENT_SETTLED(kind)`,
  `FIELD_SET(name)`. **No expression language, no SpEL.** OR is modelled as two
  transitions.

### 3.2 Instance layer (per application) — DDL §8, §8b

```
nad_application            ..., workflow_instance_id?, current_stage_id?,
                           current_status  (DENORMALISED; engine-only writer, INV9),
                           version (optimistic lock)
nad_wf_instance            id, definition_id, definition_version (PINNED),
                           application_id (1:1), current_stage_id?, status, started_at, closed_at
nad_application_task       SINGLE table (D12): engine-materialised rows carry
                           wf_stage_task_template_id; ad-hoc staff tasks have it NULL.
                           mandatory, blocks_exit, status(OPEN|DONE|SKIPPED|CANCELLED),
                           due_at, skip_reason
nad_application_stage_history   append-only  (from/to stage, transition_code, changed_by/at, reason)
nad_application_event          append-only timeline
nad_application_decision      append-only  (decision_type, outcome, drives_transition_code?)
```

There is **no** separate `nad_wf_instance_task` table (removed — D12).

### 3.3 Transition rules

A transition is allowed only when **all** of:
1. it is defined `from` the instance's current stage;
2. the caller passes the transition's role/capability gate (staff:
   `nad:application:transition` + any `role_required`; student: the relevant
   `nad_user_applicant_access` capability);
3. every current-stage task with `blocks_exit=1` is `DONE`/`SKIPPED` (always enforced);
4. `guard_json` predicates all evaluate true.

Executing a transition, in one DB transaction (optimistic-locked on
`nad_application.version`):
- append `nad_application_stage_history`;
- set `nad_application.current_stage_id` and **`current_status`** (from the target
  stage's `status_label` — the engine is the **only** writer of `current_status`);
- set `nad_wf_instance.current_stage_id`; close the instance if the target is `TERMINAL`;
- materialise the target stage's task templates into `nad_application_task`;
- append `nad_application_event`;
- publish a Spring `ApplicationEvent` → async notification fan-out + reporting
  projection update.

Auto-transitions (`auto=1`, guard already true, no blocking tasks) are chained by the
engine; each still writes history. A concurrent conflicting transition → `409`.

### 3.4 Engine — D4 APPROVED

**Data-driven config tables + a `WorkflowService`** (the tables above). No BPMN
engine. Rationale: directly satisfies the CLAUDE.md primitives, stays in
MySQL/MyBatis, keeps the audit trail first-class, and the fixed guard set removes the
security/complexity risk of an embedded expression evaluator. Flowable is revisited
**only** if parallel gateways / sub-processes / message correlation become real
requirements (documented change per CLAUDE.md §19).

### 3.5 Definition versioning

An instance **pins** `definition_version`. Activating a newer version does **not**
migrate running instances. A manual, opt-in migration tool is a later enhancement.
`activate` runs the validity checks: exactly one `START`; ≥1 `TERMINAL`; every
non-terminal stage has an outgoing transition; no unreachable stage; every `DECISION`
stage has ≥2 outgoing transitions distinguished by `DECISION_RECORDED(...)` guards.

## 4. Timers, SLA, escalation (PLANNED)

- A Quartz **JDBC-store** job sweeps `nad_application_task` for
  `due_at < now AND status='OPEN'` → `nad_application_event` + notification, optional
  auto-escalation to `ops_manager`.
- Stage `sla_hours` (business hours) drives "stage entered too long ago" dashboards and
  the `SLA_BREACH` notification.
- The same infra runs the D7 grant-expiry sweep and owner-invite escalation
  (7/14/30 days → applicant `UNLINKED`).
- **Dependency satisfied:** Quartz JDBC clustered job store is enabled and verified
  (Phase 2, `docs/DEPLOYMENT.md` §5).

## 5. Assignment (BASELINE)

- `nad_application.assignee_user_id` = owning staff member.
- v1: manual `assign` (`nad:application:assign`) + self-service `claim`
  (`nad:application:claim`) from a dept queue. Assignment changes are
  `nad_application_event`s. Load-based auto-assignment is a later enhancement.
- `case_officer` / `document_reviewer` see only assigned + in-queue rows
  (`docs/PERMISSION_CATALOGUE.md` §4).

## 6. Testing (PLANNED — Phase 3)

- `WorkflowDefinitionValidationTest` — the §3.5 checks.
- Guard enforcement — blocked while mandatory tasks open / documents unverified /
  payment unsettled / required decision missing; wrong-role → 403; optimistic-lock
  conflict → 409.
- History integrity — every stage/status/decision change appends exactly one row; no
  in-place mutation of `*_history` / `*_event` / `*_decision`.
- Per-opportunity routing — different `workflow_definition_code`s produce different
  instance stages.
- Version pinning — activating `_V2` leaves running `_V1` instances untouched.
- The concrete `PROGRAM_WITH_SCHOLARSHIP_V1` scenario tests in
  `docs/WORKFLOW_PROGRAM_WITH_SCHOLARSHIP.md` §9.
