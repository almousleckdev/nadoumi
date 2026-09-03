# P2 — Application Engine (backend) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans. Steps use `- [ ]`.
> **Depends on P1** (`nadoumi-media` merged; migrations through V29). This plan adds **V30–V34**.
> Task steps here are at task+interface+test granularity. Each task's executor writes the bite-sized RED/GREEN/commit steps following the exact pattern established in `2026-09-03-p1-media-storage-cloudinary.md` (write failing test → run → implement → run → install → commit, standard trailers, `git reset -q .claude .gitignore ry.sh`).

**Goal:** The `nadoumi-application` module — data-driven workflow engine (`WorkflowService`), `nad_wf_*` definition tables, `nad_application*` instance tables, two seeded definitions (`PROGRAM_WITH_SCHOLARSHIP_V1`, `PROGRAM_ONLY_V1`), definition-validity checks, and the staff API. No public/student endpoints, no UI (P3/P4).

**Architecture:** Config tables + a `WorkflowService`; no BPMN engine (D4). Guard evaluator implements every predicate name; `ALL_REQUIRED_DOCUMENTS_*` / `PAYMENT_SETTLED` dispatch to `DocumentGuardProvider` / `PaymentGuardProvider` — **no bean in this build** → `activate()` rejects any definition referencing them. Seeded V1 definitions gate documents/fee on `DECISION_RECORDED(DOCUMENTS_COMPLETE|FEE_SETTLED,{CONFIRMED})`. Optimistic lock on `nad_application.version`. Append-only history / events / decisions. Snapshot frozen on submit.

**Tech Stack:** Java 21 target, Spring Boot 4.1, MyBatis, MySQL 8.4, Flyway, `com.nadoumi.common.workflow.GuardPredicate` (already exists), JUnit 5 + Testcontainers.

**Spec:** `docs/superpowers/specs/2026-09-03-media-storage-and-application-engine-design.md` (Part II).

## Global Constraints

Same as P1's "Global Constraints" section, plus:
- Cross-module: `nadoumi-application` calls `ApplicantService` / `ProgramService` / `ScholarshipService` and `NadoumiAccessService` **through their interfaces** only. No cross-module mapper/entity access. DB FKs `nad_application → nad_applicant / nad_program / nad_scholarship` are `ON DELETE RESTRICT`.
- `current_status` on `nad_application` is written **only** by `WorkflowService` on stage entry. No other code path sets it.
- `nad_application_stage_history` / `_event` / `_decision` mappers expose **no** update or delete statement.
- Guard grammar: `guard_json` = `{"all":["PRED", "PRED:ARG", "PRED:ARG:OUTCOME", …]}`, AND-only. OR = two transitions. Argument encoding matches `GuardPredicate`'s Javadoc ("DECISION_RECORDED:UNIVERSITY_OFFER", "FIELD_SET:program_id").
- Next migration after P1 is **V30**.
- Roles referenced by seeds must exist: `ops_manager`, `case_officer` are seeded; `document_reviewer` / `finance` are **not** — a seed `insert … select` that matches nothing is fine (grants simply don't materialise), but note it in the migration comment. Do **not** invent role rows here.

## File Structure

**New — `nadoumi-modules/nadoumi-application/`**
- `pom.xml` (deps: `nadoumi-common`, `nadoumi-identity`, `nadoumi-applicant`, `nadoumi-university`, `nadoumi-program`, `nadoumi-scholarship`, web, validation, mybatis, `fastjson2` for `guard_json` parsing — check what the other modules use for JSON; `com.alibaba.fastjson2` is on the RuoYi classpath).
- `src/main/java/com/nadoumi/application/`
  - `domain/WorkflowDefinition.java`, `WorkflowStage.java`, `WorkflowTransition.java`, `WorkflowStageTaskTemplate.java`
  - `domain/Application.java`, `WorkflowInstance.java`, `ApplicationStageHistory.java`, `ApplicationEvent.java`, `ApplicationDecision.java`, `ApplicationTask.java`, `ApplicationSnapshot.java`
  - `domain/enums/` — `ApplicationType {PROGRAM_WITH_SCHOLARSHIP, PROGRAM_ONLY}`, `StageType {START, NORMAL, DECISION, TERMINAL}`, `DefinitionStatus {DRAFT, ACTIVE, RETIRED}`, `InstanceStatus {OPEN, CLOSED}`, `TaskStatus {OPEN, DONE, SKIPPED, CANCELLED}`, `SnapshotKind {PROFILE, REQUIREMENTS}`
  - `mapper/WorkflowDefinitionMapper.java`, `ApplicationMapper.java`, `ApplicationTaskMapper.java`, `ApplicationHistoryMapper.java` (append-only), `ApplicationEventMapper.java` (append-only), `ApplicationDecisionMapper.java` (append-only), `ApplicationSnapshotMapper.java` (append-only)
  - `mapper/ApplicationSearch.java` (record: `q, type, status, stageCode, assigneeUserId` + `staff()` factory)
  - `guard/GuardEvaluator.java`, `guard/GuardContext.java`, `guard/DocumentGuardProvider.java` (interface, no impl), `guard/PaymentGuardProvider.java` (interface, no impl), `guard/UnbackedGuardException.java`
  - `service/WorkflowService.java`, `service/DefinitionValidator.java`, `service/SnapshotAssembler.java`
  - `service/spi/` — (none exported yet; P3 adds the public/student callers)
  - `event/ApplicationTransitioned.java` (Spring `ApplicationEvent`), `event/LoggingTransitionListener.java` (stub until Step 5)
  - `web/StaffApplicationController.java`, `web/StaffWorkflowDefinitionController.java`
  - `web/request/` — `StartApplicationRequest`, `TransitionRequest`, `RecordDecisionRequest`, `SkipTaskRequest`, `AssignRequest`, `StaffCreateApplicationRequest`
  - `web/response/` — `ApplicationResponse`, `ApplicationListItem`, `ApplicationTimelineResponse`, `WorkflowDefinitionResponse`, `TransitionOptionResponse` (code + enabled + blockedReason)
- `src/main/resources/mapper/application/*.xml`
- `src/test/java/com/nadoumi/application/service/WorkflowServiceTest.java` (unit; mocked mappers) + `DefinitionValidatorTest.java` + `GuardEvaluatorTest.java`

**New — migrations**
- `ruoyi-admin/src/main/resources/db/migration/V30__nad_wf_definition.sql`
- `V31__nad_application.sql`
- `V32__nad_application_menu_seed.sql`
- `V33__nad_wf_program_scholarship_v1_seed.sql`
- `V34__nad_wf_program_only_v1_seed.sql`

**Modified — build wiring**
- `nadoumi-modules/pom.xml` (add `nadoumi-application` after `nadoumi-scholarship`), root `pom.xml` `dependencyManagement`, `ruoyi-admin/pom.xml` (dep).

**Modified — `ruoyi-admin` tests**
- `AbstractNadIntegrationTest.baseSetup()` — clear the new tables in FK order.
- `FlywayMigrationsIT` — counts 26 → **31** fresh / 25 → **30** baselined; assertions for V30–V34.
- New: `ruoyi-admin/src/test/java/com/ruoyi/nadoumi/StaffApplicationEngineTest.java`, `WorkflowDefinitionSeedTest.java`, `ApplicationAuthorizationTest.java`.

**Modified — docs**
- `docs/APPLICATION_WORKFLOW.md`, `docs/WORKFLOW_PROGRAM_WITH_SCHOLARSHIP.md`, `docs/DATABASE_DESIGN.md`, `docs/API_DESIGN.md`, `docs/PERMISSION_CATALOGUE.md`, `docs/DOMAIN_MODEL.md`, `docs/PLATFORM_ARCHITECTURE.md §8`.

---

## Task 1: Module skeleton + build wiring

**Files:** `nadoumi-modules/nadoumi-application/pom.xml`, `package-info.java`; `nadoumi-modules/pom.xml`; root `pom.xml`; `ruoyi-admin/pom.xml`.
**Interfaces:** produces an installable `com.nadoumi:nadoumi-application` on the `ruoyi-admin` classpath.
**Steps:** mirror P1 Task 2. Build order: after all catalog modules (it depends on them). Verify `mvn -o -pl nadoumi-modules/nadoumi-application -am install -DskipTests` + `mvn -o -pl ruoyi-admin -am -DskipTests compile`. Commit `build(application): scaffold nadoumi-application module`.

---

## Task 2: V30 — `nad_wf_definition` / `_stage` / `_transition` / `_stage_task_template`

**Files:** `V30__nad_wf_definition.sql`; `FlywayMigrationsIT` (27/26).
**Interfaces:** columns exactly per `APPLICATION_WORKFLOW.md §3.1`:
- `nad_wf_definition(id, code varchar(64), name, version int, status varchar(12), audit)` — unique `(code, version)`.
- `nad_wf_stage(id, definition_id FK, code varchar(48), name, order_no int, stage_type varchar(12), status_label varchar(48), sla_hours int null)` — unique `(definition_id, code)`, index `(definition_id, order_no)`.
- `nad_wf_transition(id, definition_id FK, code varchar(48), from_stage_id bigint null FK, to_stage_id bigint FK, guard_json varchar(1000) null, auto tinyint default 0)` — unique `(definition_id, code)`.
- `nad_wf_stage_task_template(id, stage_id FK, title varchar(200), role_required varchar(64) null, mandatory tinyint, blocks_exit tinyint, order_no int)`.
All FKs `ON DELETE CASCADE` within a definition. Header rollback comment.
**Test:** `tableExists` × 4, `indexExists` for the uniques, count 27/26.
**Commit:** `feat(application): V30 nad_wf_* definition tables`.

---

## Task 3: V31 — `nad_application` + instance/history/event/decision/task/snapshot

**Files:** `V31__nad_application.sql`; `FlywayMigrationsIT` (28/27).
**Interfaces:** per spec §II.2:
- `nad_application(id, applicant_id FK→nad_applicant RESTRICT, application_type varchar(32), program_id FK→nad_program RESTRICT, scholarship_id bigint null FK→nad_scholarship RESTRICT, intake_id bigint null, workflow_instance_id bigint null, current_stage_id bigint null, current_status varchar(32) null, assignee_user_id bigint null, submitted_at datetime null, version int not null default 0, audit)`. Indexes: `(applicant_id)`, `(assignee_user_id)`, `(application_type, current_status)`, `(current_stage_id)`.
- `nad_wf_instance(id, definition_id FK, definition_version int, application_id bigint not null unique, current_stage_id bigint null, status varchar(12), started_at datetime, closed_at datetime null)`.
- `nad_application_stage_history(id, application_id FK CASCADE, from_stage_id bigint null, to_stage_id bigint, transition_code varchar(48), changed_by bigint, changed_at datetime, reason varchar(1000) null)` — index `(application_id, changed_at)`. **Append-only.**
- `nad_application_event(id, application_id FK CASCADE, event_type varchar(48), actor_user_id bigint null, at datetime, detail_json varchar(2000) null)` — index `(application_id, at)`. **Append-only.**
- `nad_application_decision(id, application_id FK CASCADE, decision_type varchar(48), outcome varchar(48), rationale varchar(2000), drives_transition_code varchar(48) null, decided_by bigint, decided_at datetime)` — index `(application_id, decision_type)`. **Append-only.**
- `nad_application_task(id, application_id FK CASCADE, wf_stage_task_template_id bigint null, stage_code varchar(48), title varchar(200), role_required varchar(64) null, mandatory tinyint, blocks_exit tinyint, status varchar(12), assignee_user_id bigint null, due_at datetime null, skip_reason varchar(1000) null, audit)` — index `(application_id, status)`.
- `nad_application_snapshot(id, application_id FK CASCADE, kind varchar(16), payload_json longtext, created_at datetime)` — unique `(application_id, kind)`. **Append-only** (one row per kind, written once).
**Test:** `tableExists` × 7, `columnExists("nad_application","version")`, unique on `nad_wf_instance.application_id`, count 28/27.
**Commit:** `feat(application): V31 nad_application + instance/history/event/decision/task/snapshot`.

---

## Task 4: V32 — menu + permission seed

**Files:** `V32__nad_application_menu_seed.sql`; `FlywayMigrationsIT` (29/28).
**Interfaces:** `Applications` C-menu (`path='application'`, `perms='nad:application:list'`, next free `order_num` under the Nadoumi parent — check V20's pattern) + F-menus for the `nad:application:*` perms in `PERMISSION_CATALOGUE.md §3` (`view, list, create, edit, assign, claim, transition, withdraw, decide, note:view, note:internal:view, note:add, submission:record, export`). Grants: `insert into sys_role_menu … select r.role_id, m.menu_id from sys_role r join sys_menu m … where r.role_key in ('ops_manager','case_officer','document_reviewer','nadoumi_super_admin') and m.perms like 'nad:application:%'` — with the `document_reviewer` no-op noted in a comment. Idempotent (`on duplicate key`/guarded, like V17/V20).
**Test:** `nad:application:transition` menu row exists; `ops_manager` has it.
**Commit:** `feat(application): V32 Applications menu + nad:application:* perms`.

---

## Task 5: Definition-side domain + mapper (`nad_wf_*`)

**Files:** `domain/Workflow{Definition,Stage,Transition,StageTaskTemplate}.java`, `domain/enums/*`, `mapper/WorkflowDefinitionMapper.java` + XML.
**Interfaces:** `WorkflowDefinitionMapper` — `findActiveByCode(String)`, `findById(long)`, `findStages(long defId)`, `findTransitions(long defId)`, `findTransitionsFrom(long defId, Long fromStageId)`, `findTaskTemplates(long stageId)`, `insertDefinition/Stage/Transition/TaskTemplate` (used by seeds via SQL, by tests via mapper), `updateDefinitionStatus(long id, String status)`, `findByCodeAnyVersion(String)`.
Explicit `<resultMap>` for every table (underscore mapping off). `guard_json` maps to a `String` field; parsing is `GuardEvaluator`'s job.
**Test:** `WorkflowDefinitionMapperTest` is covered by the ITs; a light module unit test only if it needs no DB.
**Commit:** `feat(application): workflow definition domain + mapper`.

---

## Task 6: Instance-side domain + mappers (append-only discipline)

**Files:** `domain/{Application,WorkflowInstance,ApplicationStageHistory,ApplicationEvent,ApplicationDecision,ApplicationTask,ApplicationSnapshot}.java`, `mapper/{ApplicationMapper,ApplicationTaskMapper,ApplicationHistoryMapper,ApplicationEventMapper,ApplicationDecisionMapper,ApplicationSnapshotMapper}.java` + XML, `mapper/ApplicationSearch.java`.
**Interfaces:**
- `ApplicationMapper` — `insert`, `findById`, `search(ApplicationSearch)` (PageHelper), `findByApplicant(long)`, `compareAndBumpVersion(@Param("id") long, @Param("expected") int) -> int` (returns rows updated; `UPDATE nad_application SET version = version + 1 WHERE id = ? AND version = ?`), `updateStageAndStatus(@Param("id") long, @Param("stageId") long, @Param("status") String)`, `updateAssignee(long id, Long userId)`, `updateSubmittedAt(long id)`, `updateOpportunity(long id, Long programId, Long scholarshipId, Long intakeId)`, `updateWorkflowInstanceId(long id, long instId)`.
- `WorkflowInstanceMapper` (or on `ApplicationMapper`) — `insert`, `findByApplicationId(long)`, `updateCurrentStage(long id, Long stageId)`, `close(long id)`.
- `ApplicationTaskMapper` — `insertMany(List)`, `findOpenByStage(long appId, String stageCode)`, `findBlockingOpen(long appId, String stageCode)`, `findById(long)`, `markDone(long id, String by)`, `markSkipped(long id, String reason, String by)`, `findByApplication(long)`.
- History / Event / Decision / Snapshot mappers — **`insert` + `findByApplication` only**. No update/delete.
**Test:** `HistoryImmutabilityTest` (ruoyi-admin IT, Task 15) greps the compiled mapper XML / asserts no `<update>`/`<delete>` in those four namespaces.
**Commit:** `feat(application): instance/history/event/decision/task/snapshot domain + append-only mappers`.

---

## Task 7: `GuardEvaluator` + provider interfaces

**Files:** `guard/GuardEvaluator.java`, `guard/GuardContext.java`, `guard/DocumentGuardProvider.java`, `guard/PaymentGuardProvider.java`, `guard/UnbackedGuardException.java`; `service/GuardEvaluatorTest.java`.
**Interfaces:**
- `GuardContext(long applicationId, String currentStageCode, java.util.function.Supplier<List<ApplicationTask>> tasks, java.util.function.Supplier<List<ApplicationDecision>> decisions, java.util.function.Function<String,Object> field)` — lazy suppliers so guards only query what they need.
- `GuardEvaluator.parse(String guardJson) -> List<GuardTerm>` where `GuardTerm(GuardPredicate predicate, String arg, Set<String> outcomes)`.
- `GuardEvaluator.referencedPredicates(String guardJson) -> Set<GuardPredicate>` (for `DefinitionValidator`).
- `GuardEvaluator.evaluate(String guardJson, GuardContext ctx) -> boolean` — AND over terms.
- `ALL_MANDATORY_TASKS_DONE`, `TASKS_BLOCKING_EXIT_DONE` — over `ctx.tasks()`.
- `DECISION_RECORDED` — arg = `decision_type`; optional `outcomes` — a matching `ApplicationDecision` exists.
- `FIELD_SET` — `ctx.field(arg) != null`. Whitelist: `program_id, scholarship_id, intake_id, submitted_at` (reject others at `parse` time → `NadBadRequestException`).
- `ALL_REQUIRED_DOCUMENTS_ATTACHED` / `_VERIFIED` → `documentGuardProvider` (nullable-injected: `@Autowired(required=false)`). Null → `evaluate` throws `UnbackedGuardException` (should never happen at runtime because `activate` blocks such definitions).
- `PAYMENT_SETTLED` → `paymentGuardProvider` (same).
**Tests:** `parse` round-trips the four seeded guard strings; `FIELD_SET:foo` (not whitelisted) → exception; `ALL_MANDATORY_TASKS_DONE` true iff every mandatory task DONE/SKIPPED; `DECISION_RECORDED:UNIVERSITY_OFFER:OFFER` true only with a matching decision; `ALL_REQUIRED_DOCUMENTS_ATTACHED` with no provider → `UnbackedGuardException`.
**Commit:** `feat(application): fixed-set guard evaluator + document/payment provider SPIs`.

---

## Task 8: `DefinitionValidator` + `WorkflowService.activate`

**Files:** `service/DefinitionValidator.java`, `service/WorkflowService.java` (partial — `activate` only), `service/DefinitionValidatorTest.java`.
**Interfaces:** `DefinitionValidator.validate(WorkflowDefinition, List<WorkflowStage>, List<WorkflowTransition>) -> ValidationReport(boolean ok, List<String> problems)`. Checks (spec §II.4): exactly one START; ≥1 TERMINAL; every non-terminal stage has ≥1 outgoing transition; no unreachable stage (BFS from START over `to_stage_id`); every DECISION stage has ≥2 outgoing transitions each with a `DECISION_RECORDED(...)` term; every predicate in every `guard_json` has a registered provider (`GuardEvaluator.referencedPredicates` ∩ {`ALL_REQUIRED_DOCUMENTS_*`,`PAYMENT_SETTLED`} must be empty **unless** the matching provider bean is present) → problem `UNBACKED_GUARD_PREDICATE:<name>`.
`WorkflowService.activate(long definitionId)` — run the validator; on `ok` flip `DRAFT→ACTIVE` and demote the prior `ACTIVE` of the same `code` to `RETIRED`; on `!ok` throw `NadBadRequestException` with the joined problems.
**Tests:** a fixture builder for in-memory definitions; each check has a positive + negative case; the `PROGRAM_WITH_SCHOLARSHIP_V1` shape (from the seed) validates clean; a variant using `ALL_REQUIRED_DOCUMENTS_VERIFIED` fails with `UNBACKED_GUARD_PREDICATE`.
**Commit:** `feat(application): DefinitionValidator + WorkflowService.activate with fail-closed guard check`.

---

## Task 9: `WorkflowService.startDraft`

**Files:** `service/WorkflowService.java` (+`startDraft`); `WorkflowServiceTest.java`.
**Interfaces:** `startDraft(long applicantId, ApplicationType type, long programId, Long scholarshipId, Long intakeId, Actor actor) -> Application`.
- `Actor` — record `(Long staffUserId, Long actingApplicantId, java.util.Set<String> roles)`; a helper `Actor.staff(userId, roles)` / `Actor.student(userId, applicantId)`.
- Validate via injected interfaces: `programService.get(programId)` (exists, ACTIVE); if `scholarshipId != null` → `scholarshipAdminService.get(scholarshipId)` (exists); `type == PROGRAM_ONLY` ⇒ `scholarshipId == null` (else `NadBadRequestException`); `type == PROGRAM_WITH_SCHOLARSHIP` ⇒ both set.
- Authorization: `accessService.requireCapability(applicantId, CREATE_APPLICATION)` for the acting identity (staff path uses `nad:application:create` on the controller instead).
- Resolve definition: `program.workflowDefinitionCode` else `scholarship.workflowDefinitionCode` else default-for-type (`PROGRAM_WITH_SCHOLARSHIP` → `PROGRAM_WITH_SCHOLARSHIP_V1`, `PROGRAM_ONLY` → `PROGRAM_ONLY_V1`); `findActiveByCode`.
- Create `nad_application` (`current_status='DRAFT'`, `version=0`), `nad_wf_instance` (pinned `definition_version`), position at the START stage, materialise START-stage task templates, `event: APPLICATION_CREATED`. One transaction.
**Tests (mocked mappers + services):** happy path creates instance pinned to the active version; `PROGRAM_ONLY` + non-null `scholarshipId` → `NadBadRequestException`; unknown program → `NadBadRequestException`; missing `CREATE_APPLICATION` → `NadForbiddenException`.
**Commit:** `feat(application): WorkflowService.startDraft — definition routing + instance creation`.

---

## Task 10: `WorkflowService.execute` (transition + optimistic lock + auto-chain)

**Files:** `service/WorkflowService.java` (+`execute`, private `applyTransition`, `chainAutoTransitions`); `event/ApplicationTransitioned.java`, `event/LoggingTransitionListener.java`; `WorkflowServiceTest.java`.
**Interfaces:** `execute(long applicationId, String transitionCode, Actor actor, String reason, int expectedVersion) -> TransitionResult(String fromStageCode, String toStageCode, String currentStatus, boolean instanceClosed)`.
- Load application + instance; `transition = findTransitionsFrom(defId, currentStageId)` filtered by `code` — none → `NadBadRequestException("transition not available from this stage")`.
- Actor gate: staff → controller already checked `nad:application:transition`; here check `transition.roleRequired` ∈ `actor.roles()` (null = any staff). Student → `accessService.requireCapability(app.applicantId, <capability named by the definition for that transition>)` — the seed encodes the student-allowed transitions (`submit`, `accept_offer`, `decline_offer`, `withdraw`) and their capability (`SUBMIT_APPLICATION`). Wrong actor → `NadForbiddenException`.
- Blocking tasks: every current-stage task with `blocks_exit=1` must be `DONE`/`SKIPPED` (always) — else `NadBadRequestException("blocking tasks open")`.
- Guard: `guardEvaluator.evaluate(transition.guardJson, ctx)` false → `NadBadRequestException("guard not satisfied: <first failing term>")`.
- Optimistic lock: `compareAndBumpVersion(id, expectedVersion)` returns 0 → `NadConflictException` (add a `409` exception to `nadoumi-identity/exception` if not present — check; spec references `409`).
- Apply (same txn): append `nad_application_stage_history`; `updateStageAndStatus(id, toStageId, toStage.statusLabel)`; `instanceMapper.updateCurrentStage`; if `toStage.stageType == TERMINAL` → `instanceMapper.close`; materialise `toStage` task templates into `nad_application_task`; append `nad_application_event`; publish `ApplicationTransitioned`.
- `chainAutoTransitions` — after applying, look for an `auto=1` transition from the new stage whose guard is already true and with no open blocking tasks; apply it (recursively, each writing its own history); stop at TERMINAL or when none qualifies.
**Tests:** happy transition bumps version + writes exactly one history + one event; wrong `expectedVersion` → conflict; guard-false → bad request, no writes; blocking task open → bad request; auto-transition chains one hop and writes two history rows; TERMINAL closes the instance.
**Commit:** `feat(application): WorkflowService.execute — guarded transition, optimistic lock, auto-chain, events`.

---

## Task 11: `recordDecision` / `completeTask` / `skipTask` / `assign` / `claim`

**Files:** `service/WorkflowService.java` (+ these methods); `WorkflowServiceTest.java`.
**Interfaces:**
- `recordDecision(long appId, String decisionType, String outcome, String rationale, Actor actor) -> ApplicationDecision` — `nad:application:decide` (controller); `rationale` mandatory (`NadBadRequestException` if blank); append-only insert + `event: DECISION_RECORDED`; optimistic bump.
- `completeTask(long appId, long taskId, Actor actor)` — task belongs to app + is `OPEN`; `markDone`; `event: TASK_COMPLETED`.
- `skipTask(long appId, long taskId, String reason, Actor actor)` — reason mandatory; `markSkipped`; `event: TASK_SKIPPED`.
- `assign(long appId, long assigneeUserId, Actor actor)` — `nad:application:assign`; `updateAssignee`; `event: CASE_ASSIGNED`.
- `claim(long appId, Actor actor)` — `nad:application:claim`; only if `assignee_user_id is null`; set to `actor.staffUserId`; `event: CASE_CLAIMED`.
**Tests:** decision without rationale → bad request; complete a task not on the app → not found; claim an already-assigned app → bad request.
**Commit:** `feat(application): decisions, task complete/skip, assign/claim`.

---

## Task 12: `SnapshotAssembler` + submit wiring

**Files:** `service/SnapshotAssembler.java`; `WorkflowService.execute` special-cases the `submit` transition to call it; `WorkflowServiceTest.java` + `ApplicationSnapshotTest` (IT, Task 15).
**Interfaces:** `SnapshotAssembler.assemble(Application app) -> List<ApplicationSnapshot>` — two rows:
- `PROFILE` — `applicantService.get(app.applicantId)` (full aggregate: identity + education + test scores + contacts) + program/scholarship/intake display facts (name, university name, degree, intake term) via the catalog services, serialised with fastjson2.
- `REQUIREMENTS` — the resolved document-requirement list for this application's programme + scholarship + workflow (from `WORKFLOW_PROGRAM_WITH_SCHOLARSHIP.md §6`), as the checklist definition that applied at submit.
Insert both via `ApplicationSnapshotMapper.insert` inside the `submit` transaction. Unique `(application_id, kind)` guarantees one-shot.
**Tests:** `submit` writes both rows; a second `submit` attempt (already past DRAFT) can't re-run; a `PROFILE` edit after submit doesn't change the snapshot JSON.
**Commit:** `feat(application): freeze PROFILE + REQUIREMENTS snapshot on submit`.

---

## Task 13: V33 — seed `PROGRAM_WITH_SCHOLARSHIP_V1`

**Files:** `V33__nad_wf_program_scholarship_v1_seed.sql`; `FlywayMigrationsIT` (30/29); `WorkflowDefinitionSeedTest` (IT).
**Interfaces:** the 13 stages / 15 transitions / 19 task templates from `WORKFLOW_PROGRAM_WITH_SCHOLARSHIP.md §3–§5`, with DA3 guard substitution:
- `submit` guard `{"all":["FIELD_SET:program_id","FIELD_SET:scholarship_id","FIELD_SET:intake_id"]}` (drop `ALL_REQUIRED_DOCUMENTS_ATTACHED`).
- `docs_complete` guard `{"all":["DECISION_RECORDED:DOCUMENTS_COMPLETE:CONFIRMED","ALL_MANDATORY_TASKS_DONE"]}`.
- `package_ready` guard `{"all":["ALL_MANDATORY_TASKS_DONE","DECISION_RECORDED:FEE_SETTLED:CONFIRMED"]}`.
- All other guards verbatim from §4.
- `nad_program.workflow_definition_code` / `nad_scholarship.workflow_definition_code` columns are **added in this migration** (`varchar(64) null`) — the routing hook.
- `status='ACTIVE'` set directly in the seed (the seed represents an already-validated definition; `DefinitionValidatorTest` proves the shape is valid).
Write with explicit `@id` variables (`set @def := last_insert_id();` pattern) or a deterministic sub-select so stage/transition FKs resolve. Idempotent guard (`insert … where not exists`).
**Test:** `WorkflowDefinitionSeedTest` — `findActiveByCode("PROGRAM_WITH_SCHOLARSHIP_V1")` returns a def with 13 stages, 15 transitions; `DefinitionValidator.validate(...)` on the loaded rows → `ok`; each DECISION stage has ≥2 `DECISION_RECORDED` transitions.
**Commit:** `feat(application): V33 seed PROGRAM_WITH_SCHOLARSHIP_V1 (+ workflow_definition_code routing columns)`.

---

## Task 14: V34 — seed `PROGRAM_ONLY_V1`

**Files:** `V34__nad_wf_program_only_v1_seed.sql`; `FlywayMigrationsIT` (31/30); `WorkflowDefinitionSeedTest` (extend).
**Interfaces:** stages `DRAFT(START), SUBMITTED, ELIGIBILITY_REVIEW, DOCUMENT_COLLECTION, PACKAGE_PREPARATION, SUBMITTED_TO_UNIVERSITY, UNIVERSITY_DECISION(DECISION), OFFER_RESPONSE, PRE_DEPARTURE, ENROLLED(TERMINAL), UNSUCCESSFUL(TERMINAL), WITHDRAWN(TERMINAL)`. Transitions: as `PROGRAM_WITH_SCHOLARSHIP_V1` minus `sch_awarded` / `sch_declined_body`, with `uni_offer` retargeted `UNIVERSITY_DECISION → OFFER_RESPONSE` (guard unchanged: `DECISION_RECORDED:UNIVERSITY_OFFER:OFFER` — accepts `OFFER`/`CONDITIONAL_OFFER`). Task templates: drop the scholarship-form + scholarship-eligibility rows. `submit` guard drops `FIELD_SET:scholarship_id`.
**Test:** seed test — 12 stages, no `SCHOLARSHIP_DECISION`; validates clean; `ProgramOnlyWorkflowTest` (Task 15) — `startDraft(PROGRAM_ONLY)` routes here.
**Commit:** `feat(application): V34 seed PROGRAM_ONLY_V1`.

---

## Task 15: Staff API — `StaffApplicationController` + `StaffWorkflowDefinitionController`

**Files:** `web/StaffApplicationController.java`, `web/StaffWorkflowDefinitionController.java`, `web/request/*`, `web/response/*`.
**Interfaces:** per spec §II.6. All `@PreAuthorize` exact perms + `@Log` on mutations. Responses are dedicated records; `current_status` read-only.
- `GET /api/staff/applications` — `nad:application:list`; `ApplicationSearch` filters; for `case_officer` / `document_reviewer` the service applies the scope filter (`assignee_user_id = self OR in-queue`) — implement "in-queue" as `assignee_user_id IS NULL` for now (department queues are a later refinement; note it).
- `GET /api/staff/applications/{id}` — `nad:application:view` (+ scope) → `ApplicationResponse` (application + instance + open tasks + history + events + decisions + snapshot presence flags) + `transitionOptions[]` (`TransitionOptionResponse{code, enabled, blockedReason}` — computed by dry-running each candidate transition's gate/guard).
- `POST /api/staff/applications` — `nad:application:create` → `startDraft` (staff Actor).
- `PUT /api/staff/applications/{id}` — `nad:application:edit`; opportunity/intake only while `current_status='DRAFT'`.
- `POST /api/staff/applications/{id}/transitions/{code}` `{reason?, version}` — `nad:application:transition` → `execute`.
- `POST /api/staff/applications/{id}/decisions` — `nad:application:decide`.
- `POST /api/staff/applications/{id}/tasks/{taskId}/complete` | `/skip {reason}` — `nad:application:transition`.
- `POST /api/staff/applications/{id}/assign {assigneeUserId}` | `/claim` — `nad:application:assign` | `:claim`.
- `GET /api/staff/workflow-definitions` | `/{code}` — `nad:application:view` → `WorkflowDefinitionResponse` (stages, transitions incl. guard strings, task templates).
**Tests:** `StaffApplicationEngineTest` (IT, Task 16) covers these.
**Commit:** `feat(application): staff application API + workflow definition viewer`.

---

## Task 16: `ruoyi-admin` ITs + base wiring + Flyway counts

**Files:** `AbstractNadIntegrationTest` (baseSetup FK-order clears + `nad_application*`, `nad_wf_instance`, then leave `nad_wf_definition/stage/transition/task_template` — they're seed, keep them; but tests that `activate` a bespoke definition must clean up their own rows); `FlywayMigrationsIT` (final 31/30); new IT classes.
**Interfaces / test coverage (spec §II.11):**
- `StaffApplicationEngineTest` — full `PROGRAM_WITH_SCHOLARSHIP_V1` happy path `DRAFT → … → ENROLLED` with a `case_officer` recording the required decisions (incl. `DOCUMENTS_COMPLETE`, `FEE_SETTLED`) and completing blocking tasks in order; asserts `current_status` transitions and history rows.
- `WorkflowGuardTest` — `docs_complete` blocked without the `DOCUMENTS_COMPLETE` decision; `package_ready` blocked without `FEE_SETTLED`; `eligible` blocked without a `NADOUMI_INTERNAL` decision; wrong role → `403`; concurrent transition (two calls, same `version`) → one `200` + one `409`.
- `WorkflowDecisionFanoutTest` — `uni_reject` vs `uni_offer` selected by the recorded `UNIVERSITY_OFFER` outcome.
- `WorkflowWithdrawTest` — `withdraw` from each non-terminal stage → `WITHDRAWN` + one history row + instance closed.
- `WorkflowVersionPinningTest` — seed + `activate` a `PROGRAM_WITH_SCHOLARSHIP_V2` (DRAFT→ACTIVE); a running V1 instance's stage list is unchanged; new applications route to V2.
- `ApplicationSnapshotTest` — `submit` writes `PROFILE` + `REQUIREMENTS`; a subsequent `ApplicantService` profile edit leaves the snapshot JSON byte-identical.
- `ApplicationAuthorizationTest` — a user with no grant can't `startDraft` / `submit` / `GET` another applicant's application (`403`); on-behalf-of holder with `SUBMIT_APPLICATION` succeeds; `case_officer` list is scoped.
- `ProgramOnlyWorkflowTest` — `PROGRAM_ONLY` routes to `PROGRAM_ONLY_V1`, no `SCHOLARSHIP_DECISION` stage, non-null `scholarship_id` rejected.
- `HistoryImmutabilityTest` — no `<update>`/`<delete>` element in the history/event/decision/snapshot mapper XML namespaces.
**Steps:** wire, write, `mvn -o -pl ruoyi-admin verify` (background) green. **Commit:** `test(application): engine ITs — happy path, guards, fan-out, withdraw, version pinning, snapshot, authz`.

---

## Task 17: Documentation

**Files:** `docs/APPLICATION_WORKFLOW.md` (EXISTING → what's built; DA3 substitution table; the `409`/`NadConflictException`), `docs/WORKFLOW_PROGRAM_WITH_SCHOLARSHIP.md` (seed applied; guard substitution; add a `PROGRAM_ONLY_V1` section), `docs/DATABASE_DESIGN.md` (V30–V34 + ledger; final count), `docs/API_DESIGN.md` (staff application surface), `docs/PERMISSION_CATALOGUE.md` (confirm granted `nad:application:*`), `docs/DOMAIN_MODEL.md` (Application aggregate → EXISTING), `docs/PLATFORM_ARCHITECTURE.md §8` (Step 6 → engine ✅, public/UI pending P3/P4).
**Commit:** `docs: application engine — workflow, DB, API, permissions`.

---

## Task 18: Gate + memory

- [ ] `mvn -o -pl ruoyi-admin -am verify` (background) green; final `FlywayMigrationsIT` **31 / 30**.
- [ ] Update `docs/superpowers/specs/...` §II.2 migration numbers (V30–V34) if they drifted.
- [ ] Update `phase-status.md` memory — P2 done, engine, V30–V34.
- [ ] Commit `chore(application): P2 gate green`.

---

## Self-review (authoring time)

- **Spec coverage:** module (T1), `nad_wf_*` (T2), `nad_application*` (T3), perms (T4), definition domain (T5), instance domain + append-only (T6), guard evaluator + provider SPIs (T7), validator + `activate` fail-closed (T8), `startDraft` (T9), `execute` + optimistic lock + auto-chain (T10), decisions/tasks/assign/claim (T11), snapshot (T12), seed V1 ×2 (T13–T14), staff API (T15), ITs incl. all §II.11 named tests (T16), docs (T17), gate (T18). Complete.
- **Placeholders:** none — every task names files, interfaces (method signatures + params), and concrete test cases. Bite-sized RED/GREEN steps are delegated to the executor per the note at the top, following P1's worked pattern.
- **Type consistency:** `Actor` record shape fixed in T9, used T10/T11. `compareAndBumpVersion(id, expected) -> int` (rows updated) consistent T6/T10. Migration totals V30→27/26 … V34→31/30 consistent T2–T4, T13–T14, T16, T18. `NadConflictException` (409) introduced in T10, referenced T16/T17 — flag: confirm it doesn't already exist in `com.nadoumi.identity.exception` before adding.
