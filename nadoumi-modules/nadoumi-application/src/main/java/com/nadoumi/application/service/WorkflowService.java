package com.nadoumi.application.service;

import com.alibaba.fastjson2.JSONObject;
import com.nadoumi.applicant.service.ApplicantService;
import com.nadoumi.application.domain.Application;
import com.nadoumi.application.domain.ApplicationDecision;
import com.nadoumi.application.domain.ApplicationEvent;
import com.nadoumi.application.domain.ApplicationSnapshot;
import com.nadoumi.application.domain.ApplicationStageHistory;
import com.nadoumi.application.domain.ApplicationTask;
import com.nadoumi.application.domain.WfDefinition;
import com.nadoumi.application.domain.WfInstance;
import com.nadoumi.application.domain.WfStage;
import com.nadoumi.application.domain.WfStageTaskTemplate;
import com.nadoumi.application.domain.WfTransition;
import com.nadoumi.application.domain.enums.ApplicationTaskStatus;
import com.nadoumi.application.domain.enums.ApplicationType;
import com.nadoumi.application.domain.enums.WfDefinitionStatus;
import com.nadoumi.application.domain.enums.WfInstanceStatus;
import com.nadoumi.application.domain.enums.WfStageType;
import com.nadoumi.application.exception.OptimisticLockException;
import com.nadoumi.application.mapper.ApplicationDecisionMapper;
import com.nadoumi.application.mapper.ApplicationEventMapper;
import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.application.mapper.ApplicationSnapshotMapper;
import com.nadoumi.application.mapper.ApplicationStageHistoryMapper;
import com.nadoumi.application.mapper.ApplicationTaskMapper;
import com.nadoumi.application.mapper.WfDefinitionMapper;
import com.nadoumi.application.mapper.WfInstanceMapper;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadForbiddenException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.identity.domain.UserApplicantAccess;
import com.nadoumi.identity.service.UserApplicantAccessService;
import com.nadoumi.program.service.ProgramService;
import com.nadoumi.program.web.response.ProgramResponse;
import com.nadoumi.scholarship.service.ScholarshipAdminService;
import com.nadoumi.scholarship.web.response.ScholarshipResponse;
import com.ruoyi.common.utils.SecurityUtils;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The engine (DA1): data-driven config tables + fixed-dispatch guards, no BPMN.
 * {@code current_status} on {@code nad_application} is written only here (INV9).
 */
@Service
public class WorkflowService {

    private final WfDefinitionMapper definitionMapper;
    private final ApplicationMapper applicationMapper;
    private final WfInstanceMapper instanceMapper;
    private final ApplicationTaskMapper taskMapper;
    private final ApplicationStageHistoryMapper historyMapper;
    private final ApplicationEventMapper eventMapper;
    private final ApplicationDecisionMapper decisionMapper;
    private final ApplicationSnapshotMapper snapshotMapper;
    private final GuardEvaluator guards;
    private final OutboxWriter outboxWriter;
    private final ProgramService programService;
    private final ScholarshipAdminService scholarshipAdminService;
    private final ApplicantService applicantService;
    private final UserApplicantAccessService accessService;

    public WorkflowService(WfDefinitionMapper definitionMapper, ApplicationMapper applicationMapper,
            WfInstanceMapper instanceMapper, ApplicationTaskMapper taskMapper,
            ApplicationStageHistoryMapper historyMapper, ApplicationEventMapper eventMapper,
            ApplicationDecisionMapper decisionMapper, ApplicationSnapshotMapper snapshotMapper,
            GuardEvaluator guards, OutboxWriter outboxWriter, ProgramService programService,
            ScholarshipAdminService scholarshipAdminService, ApplicantService applicantService,
            UserApplicantAccessService accessService) {
        this.definitionMapper = definitionMapper;
        this.applicationMapper = applicationMapper;
        this.instanceMapper = instanceMapper;
        this.taskMapper = taskMapper;
        this.historyMapper = historyMapper;
        this.eventMapper = eventMapper;
        this.decisionMapper = decisionMapper;
        this.snapshotMapper = snapshotMapper;
        this.guards = guards;
        this.outboxWriter = outboxWriter;
        this.programService = programService;
        this.scholarshipAdminService = scholarshipAdminService;
        this.applicantService = applicantService;
        this.accessService = accessService;
    }

    // ---- draft creation ----------------------------------------------------

    @Transactional(rollbackFor = Exception.class)
    public Application startDraft(long applicantId, ApplicationType type, long programId, Long scholarshipId,
            Long intakeId, long actorUserId) {
        if (type == ApplicationType.PROGRAM_ONLY && scholarshipId != null) {
            throw new NadBadRequestException("a PROGRAM_ONLY application may not carry a scholarship_id");
        }
        if (type == ApplicationType.PROGRAM_WITH_SCHOLARSHIP && scholarshipId == null) {
            throw new NadBadRequestException("a PROGRAM_WITH_SCHOLARSHIP application requires a scholarship_id");
        }
        ProgramResponse program = programService.get(programId); // 404 if missing
        if (intakeId != null && program.intakes().stream().noneMatch(i -> i.id().equals(intakeId))) {
            throw new NadBadRequestException("intake does not belong to the programme");
        }
        if (scholarshipId != null) {
            scholarshipAdminService.get(scholarshipId); // 404 if missing
        }

        String definitionCode = type == ApplicationType.PROGRAM_WITH_SCHOLARSHIP
                ? "PROGRAM_WITH_SCHOLARSHIP_V1" : "PROGRAM_ONLY_V1";
        WfDefinition definition = definitionMapper.findActiveByCode(definitionCode);
        if (definition == null) {
            throw new IllegalStateException("no ACTIVE workflow definition for " + definitionCode);
        }
        WfStage start = definitionMapper.findStages(definition.getId()).stream()
                .filter(s -> s.getStageType() == WfStageType.START)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("definition " + definitionCode + " has no START stage"));

        Application application = new Application();
        application.setApplicantId(applicantId);
        application.setApplicationType(type);
        application.setProgramId(programId);
        application.setScholarshipId(scholarshipId);
        application.setIntakeId(intakeId);
        application.setCurrentStageId(start.getId());
        application.setCurrentStatus(start.getStatusLabel());
        application.setVersion(0);
        application.setCreateBy(actorName());
        applicationMapper.insert(application);

        WfInstance instance = new WfInstance();
        instance.setDefinitionId(definition.getId());
        instance.setDefinitionVersion(definition.getVersion());
        instance.setApplicationId(application.getId());
        instance.setCurrentStageId(start.getId());
        instance.setStatus(WfInstanceStatus.RUNNING);
        instanceMapper.insert(instance);
        applicationMapper.linkWorkflowInstance(application.getId(), instance.getId());
        application.setWorkflowInstanceId(instance.getId());

        materializeTasks(application.getId(), start.getId(), actorUserId);
        appendEvent(application.getId(), "APPLICATION_CREATED", actorUserId, null);

        return applicationMapper.findById(application.getId());
    }

    // ---- transitions ---------------------------------------------------------

    /**
     * @param staffActor when {@code true}, the transition's {@code role_required}
     *                   (if any) is enforced against the caller's held roles; when
     *                   {@code false} (a student actor) the caller's capability was
     *                   already checked by {@code NadoumiAccessService} at the
     *                   controller, so no role gate applies here.
     */
    @Transactional(rollbackFor = Exception.class)
    public Application execute(long applicationId, String transitionCode, long actorUserId, boolean staffActor,
            String reason, int expectedVersion) {
        Application application = load(applicationId);
        WfInstance instance = instanceMapper.findByApplicationId(applicationId);
        if (instance == null || instance.getStatus() == WfInstanceStatus.CLOSED) {
            throw new NadBadRequestException("this application's workflow instance is closed");
        }
        WfTransition transition = definitionMapper.findTransition(instance.getDefinitionId(), transitionCode,
                application.getCurrentStageId());
        if (transition == null) {
            throw new NadBadRequestException("no transition '" + transitionCode + "' from the current stage");
        }
        if (staffActor && transition.getRoleRequired() != null && !SecurityUtils.hasRole(transition.getRoleRequired())) {
            throw new NadForbiddenException("this transition requires the '" + transition.getRoleRequired() + "' role");
        }
        if (!guards.tasksBlockingExitDone(applicationId)) {
            throw new NadBadRequestException("a blocking task on the current stage is still open");
        }
        if (!guards.evaluate(transition.getGuardJson(), application)) {
            throw new NadBadRequestException("transition guard not satisfied: " + transition.getGuardJson());
        }

        applyTransition(application, instance, transition, actorUserId, reason, expectedVersion);
        chainAutoTransitions(applicationMapper.findById(applicationId), instance, actorUserId);
        return applicationMapper.findById(applicationId);
    }

    /** One transition's effects, in the caller's transaction. */
    private void applyTransition(Application application, WfInstance instance, WfTransition transition,
            long actorUserId, String reason, int expectedVersion) {
        WfStage toStage = definitionMapper.findStageById(transition.getToStageId());

        ApplicationStageHistory history = new ApplicationStageHistory();
        history.setApplicationId(application.getId());
        history.setFromStageId(transition.getFromStageId());
        history.setToStageId(toStage.getId());
        history.setTransitionCode(transition.getCode());
        history.setChangedBy(actorUserId);
        history.setReason(reason);
        historyMapper.insert(history);

        boolean isSubmit = "submit".equals(transition.getCode());
        application.setCurrentStageId(toStage.getId());
        application.setCurrentStatus(toStage.getStatusLabel());
        if (isSubmit) {
            application.setSubmittedAt(LocalDateTime.now());
        }
        application.setUpdateBy(actorName());
        int rows = applicationMapper.updateWithLock(application, expectedVersion);
        if (rows == 0) {
            throw new OptimisticLockException(
                    "application " + application.getId() + " was changed concurrently — reload and retry");
        }

        instanceMapper.updateStage(application.getId(), toStage.getId());
        if (toStage.getStageType() == WfStageType.TERMINAL) {
            instanceMapper.close(application.getId());
        }

        materializeTasks(application.getId(), toStage.getId(), actorUserId);
        appendEvent(application.getId(), "STAGE_" + transition.getCode().toUpperCase(java.util.Locale.ROOT), actorUserId, reason);

        if (isSubmit) {
            writeSubmitSnapshots(application.getId(), application.getApplicantId());
            emitApplicationSubmitted(applicationMapper.findById(application.getId()));
        }
        else {
            emitApplicationStatusChanged(applicationMapper.findById(application.getId()), toStage.getStatusLabel());
        }
    }

    /** Fires any transition out of the new current stage whose guard is already true and has no open blocking task. */
    private void chainAutoTransitions(Application application, WfInstance instance, long actorUserId) {
        if (instance.getStatus() == WfInstanceStatus.CLOSED) {
            return;
        }
        for (WfTransition candidate : definitionMapper.findTransitionsFrom(instance.getDefinitionId(), application.getCurrentStageId())) {
            if (!candidate.isAuto()) {
                continue;
            }
            if (!guards.tasksBlockingExitDone(application.getId()) || !guards.evaluate(candidate.getGuardJson(), application)) {
                continue;
            }
            applyTransition(application, instance, candidate, actorUserId, "auto", application.getVersion());
            chainAutoTransitions(applicationMapper.findById(application.getId()), instance, actorUserId);
            return;
        }
    }

    // ---- decisions & tasks -----------------------------------------------------

    @Transactional(rollbackFor = Exception.class)
    public ApplicationDecision recordDecision(long applicationId, String decisionType, String outcome,
            String rationale, long actorUserId) {
        load(applicationId); // 404 if missing
        ApplicationDecision decision = new ApplicationDecision();
        decision.setApplicationId(applicationId);
        decision.setDecisionType(decisionType);
        decision.setOutcome(outcome);
        decision.setRationale(rationale);
        decision.setDecidedBy(actorUserId);
        decisionMapper.insert(decision);
        appendEvent(applicationId, "DECISION_RECORDED:" + decisionType, actorUserId, outcome);
        return decision;
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeTask(long applicationId, long taskId, long actorUserId) {
        ApplicationTask task = requireTask(applicationId, taskId);
        taskMapper.updateStatus(taskId, ApplicationTaskStatus.DONE.name(), null, actorName());
        appendEvent(applicationId, "TASK_COMPLETED", actorUserId, task.getTitle());
    }

    @Transactional(rollbackFor = Exception.class)
    public void skipTask(long applicationId, long taskId, String reason, long actorUserId) {
        if (reason == null || reason.isBlank()) {
            throw new NadBadRequestException("a reason is required to skip a task");
        }
        ApplicationTask task = requireTask(applicationId, taskId);
        taskMapper.updateStatus(taskId, ApplicationTaskStatus.SKIPPED.name(), reason, actorName());
        appendEvent(applicationId, "TASK_SKIPPED", actorUserId, task.getTitle() + ": " + reason);
    }

    @Transactional(rollbackFor = Exception.class)
    public void assign(long applicationId, long assigneeUserId, long actorUserId) {
        Application application = load(applicationId);
        application.setAssigneeUserId(assigneeUserId);
        application.setUpdateBy(actorName());
        if (applicationMapper.updateWithLock(application, application.getVersion()) == 0) {
            throw new OptimisticLockException("application " + applicationId + " was changed concurrently — reload and retry");
        }
        appendEvent(applicationId, "CASE_ASSIGNED", actorUserId, String.valueOf(assigneeUserId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void claim(long applicationId, long actorUserId) {
        assign(applicationId, actorUserId, actorUserId);
    }

    // ---- definition validity (activate) -----------------------------------------

    /** The six §II.4 checks. Flips DRAFT to ACTIVE and retires any prior ACTIVE version of the same code. */
    @Transactional(rollbackFor = Exception.class)
    public List<String> activate(long definitionId) {
        WfDefinition definition = definitionMapper.findById(definitionId);
        if (definition == null) {
            throw new NadNotFoundException("workflow definition not found");
        }
        List<WfStage> stages = definitionMapper.findStages(definitionId);
        List<WfTransition> transitions = definitionMapper.findTransitions(definitionId);
        List<String> problems = new ArrayList<>();

        List<WfStage> starts = stages.stream().filter(s -> s.getStageType() == WfStageType.START).toList();
        if (starts.size() != 1) {
            problems.add("exactly one START stage is required, found " + starts.size());
        }
        long terminalCount = stages.stream().filter(s -> s.getStageType() == WfStageType.TERMINAL).count();
        if (terminalCount < 1) {
            problems.add("at least one TERMINAL stage is required");
        }
        Set<Long> withOutgoing = transitions.stream().map(WfTransition::getFromStageId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        for (WfStage stage : stages) {
            if (stage.getStageType() != WfStageType.TERMINAL && !withOutgoing.contains(stage.getId())) {
                problems.add("stage '" + stage.getCode() + "' is non-terminal with no outgoing transition");
            }
        }
        if (!starts.isEmpty()) {
            Set<Long> reachable = reachableFrom(starts.get(0).getId(), transitions);
            for (WfStage stage : stages) {
                if (!reachable.contains(stage.getId())) {
                    problems.add("stage '" + stage.getCode() + "' is unreachable from START");
                }
            }
        }
        for (WfStage stage : stages) {
            if (stage.getStageType() != WfStageType.DECISION) {
                continue;
            }
            long decisionGuarded = transitions.stream()
                    .filter(t -> stage.getId().equals(t.getFromStageId()))
                    .filter(t -> t.getGuardJson() != null && t.getGuardJson().contains("DECISION_RECORDED"))
                    .count();
            if (decisionGuarded < 2) {
                problems.add("DECISION stage '" + stage.getCode() + "' needs >=2 outgoing DECISION_RECORDED-guarded transitions");
            }
        }
        for (WfTransition transition : transitions) {
            for (String unbacked : guards.unbackedPredicates(transition.getGuardJson())) {
                problems.add("transition '" + transition.getCode() + "' references an unbacked predicate: " + unbacked);
            }
        }

        if (!problems.isEmpty()) {
            throw new NadBadRequestException("UNBACKED_GUARD_PREDICATE or structural validity failure: " + problems);
        }

        definitionMapper.findAllVersions(definition.getCode()).stream()
                .filter(d -> d.getStatus() == WfDefinitionStatus.ACTIVE)
                .forEach(d -> definitionMapper.updateStatus(d.getId(), WfDefinitionStatus.RETIRED.name()));
        definitionMapper.updateStatus(definitionId, WfDefinitionStatus.ACTIVE.name());
        return problems;
    }

    private static Set<Long> reachableFrom(long startId, List<WfTransition> transitions) {
        Map<Long, List<Long>> edges = transitions.stream()
                .filter(t -> t.getFromStageId() != null)
                .collect(Collectors.groupingBy(WfTransition::getFromStageId,
                        Collectors.mapping(WfTransition::getToStageId, Collectors.toList())));
        Set<Long> visited = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(startId);
        visited.add(startId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            for (Long next : edges.getOrDefault(current, List.of())) {
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }
        return visited;
    }

    // ---- internals ---------------------------------------------------------------

    private Application load(long applicationId) {
        Application application = applicationMapper.findById(applicationId);
        if (application == null) {
            throw new NadNotFoundException("application not found");
        }
        return application;
    }

    private ApplicationTask requireTask(long applicationId, long taskId) {
        ApplicationTask task = taskMapper.findById(taskId);
        if (task == null || !task.getApplicationId().equals(applicationId)) {
            throw new NadNotFoundException("task not found on this application");
        }
        return task;
    }

    private void materializeTasks(long applicationId, long stageId, long actorUserId) {
        for (WfStageTaskTemplate template : definitionMapper.findTaskTemplates(stageId)) {
            ApplicationTask task = new ApplicationTask();
            task.setApplicationId(applicationId);
            task.setWfStageTaskTemplateId(template.getId());
            task.setTitle(template.getTitle());
            task.setRoleRequired(template.getRoleRequired());
            task.setMandatory(template.isMandatory());
            task.setBlocksExit(template.isBlocksExit());
            task.setStatus(ApplicationTaskStatus.OPEN);
            task.setCreateBy(actorName());
            taskMapper.insert(task);
        }
    }

    private void appendEvent(long applicationId, String eventType, long actorUserId, String detail) {
        ApplicationEvent event = new ApplicationEvent();
        event.setApplicationId(applicationId);
        event.setEventType(eventType);
        event.setActorUserId(actorUserId);
        event.setDetailJson(detail == null ? null : JSONObject.of("note", detail).toJSONString());
        eventMapper.insert(event);
    }

    /** DA4: PROFILE + REQUIREMENTS, written once at submit, never touched again. */
    private void writeSubmitSnapshots(long applicationId, long applicantId) {
        JSONObject profile = new JSONObject();
        profile.put("applicant", applicantService.get(applicantId));
        ApplicationSnapshot profileSnapshot = new ApplicationSnapshot();
        profileSnapshot.setApplicationId(applicationId);
        profileSnapshot.setKind("PROFILE");
        profileSnapshot.setPayloadJson(profile.toJSONString());
        snapshotMapper.insert(profileSnapshot);

        Application application = applicationMapper.findById(applicationId);
        JSONObject requirements = new JSONObject();
        requirements.put("applicationType", application.getApplicationType());
        requirements.put("programId", application.getProgramId());
        requirements.put("scholarshipId", application.getScholarshipId());
        requirements.put("intakeId", application.getIntakeId());
        ApplicationSnapshot requirementsSnapshot = new ApplicationSnapshot();
        requirementsSnapshot.setApplicationId(applicationId);
        requirementsSnapshot.setKind("REQUIREMENTS");
        requirementsSnapshot.setPayloadJson(requirements.toJSONString());
        snapshotMapper.insert(requirementsSnapshot);
    }

    private void emitApplicationSubmitted(Application application) {
        emitApplicationEvent(application, OutboxEventTypes.APPLICATION_SUBMITTED, null);
    }

    private void emitApplicationStatusChanged(Application application, String status) {
        emitApplicationEvent(application, OutboxEventTypes.APPLICATION_STATUS_CHANGED, status);
    }

    /** Payload keys match the templates already seeded in V55 — do not rename without updating them too. */
    private void emitApplicationEvent(Application application, String outboxType, String status) {
        List<Long> recipients = accessService.listForApplicant(application.getApplicantId()).stream()
                .filter(g -> g.getStatus() == com.nadoumi.common.access.AccessGrantStatus.ACTIVE)
                .map(UserApplicantAccess::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream().toList();
        if (recipients.isEmpty()) {
            return;
        }
        JSONObject payload = new JSONObject();
        payload.put("applicationRef", "APP-" + application.getId());
        payload.put("opportunityTitle", opportunityTitle(application));
        if (status != null) {
            payload.put("status", status);
        }
        payload.put("recipientUserIds", recipients);
        outboxWriter.write("application", application.getId(), outboxType, payload.toJSONString());
    }

    private String opportunityTitle(Application application) {
        try {
            ProgramResponse program = programService.get(application.getProgramId());
            if (application.getScholarshipId() != null) {
                ScholarshipResponse scholarship = scholarshipAdminService.get(application.getScholarshipId());
                return program.name() + " + " + scholarship.view().title();
            }
            return program.name();
        }
        catch (RuntimeException e) {
            return "your application";
        }
    }

    private static String actorName() {
        return com.ruoyi.common.utils.AuditActor.username();
    }
}
