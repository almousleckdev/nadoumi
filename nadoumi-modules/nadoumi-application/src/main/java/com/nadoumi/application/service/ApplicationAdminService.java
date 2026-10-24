package com.nadoumi.application.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nadoumi.application.domain.Application;
import com.nadoumi.application.domain.ApplicationDecision;
import com.nadoumi.application.domain.ApplicationEvent;
import com.nadoumi.application.domain.WfDefinition;
import com.nadoumi.application.domain.WfStage;
import com.nadoumi.application.mapper.ApplicationDecisionMapper;
import com.nadoumi.application.mapper.ApplicationEventMapper;
import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.application.mapper.ApplicationSearch;
import com.nadoumi.application.mapper.ApplicationStageHistoryMapper;
import com.nadoumi.application.mapper.ApplicationTaskMapper;
import com.nadoumi.application.mapper.StaffUserRow;
import com.nadoumi.application.mapper.WfInstanceMapper;
import com.nadoumi.application.domain.WfInstance;
import com.nadoumi.application.domain.WfTransition;
import com.nadoumi.application.domain.enums.WfInstanceStatus;
import com.nadoumi.applicant.service.ApplicantService;
import com.nadoumi.common.exception.NadBadRequestException;
import com.ruoyi.common.utils.SecurityUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import com.nadoumi.application.web.request.AdvanceRequest;
import com.nadoumi.application.web.response.NextStepResponse;
import com.nadoumi.application.web.response.StaffApplicationRow;
import com.nadoumi.application.web.response.StaffUserResponse;
import com.nadoumi.application.mapper.WfDefinitionMapper;
import com.nadoumi.application.web.request.AssignRequest;
import com.nadoumi.application.web.request.DecisionRequest;
import com.nadoumi.application.web.request.SkipTaskRequest;
import com.nadoumi.application.web.request.StartApplicationRequest;
import com.nadoumi.application.web.request.TransitionRequest;
import com.nadoumi.application.web.request.UpdateApplicationRequest;
import com.nadoumi.application.web.response.ApplicationDetailResponse;
import com.nadoumi.application.web.response.ApplicationEventResponse;
import com.nadoumi.application.web.response.ApplicationResponse;
import com.nadoumi.application.web.response.DecisionResponse;
import com.nadoumi.application.web.response.StageHistoryResponse;
import com.nadoumi.application.web.response.TaskResponse;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.common.web.PageSupport;
import com.ruoyi.common.utils.AuditActor;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Staff application management (spec §II.6). {@code scopeUserId} restricts
 * {@code case_officer} / {@code document_reviewer} to assigned-or-unclaimed rows;
 * {@code ops_manager} / super admin pass {@code null} for org-wide visibility —
 * the controller decides which, from the caller's held role.
 */
@Service
public class ApplicationAdminService {

    private final ApplicationMapper applicationMapper;
    private final WfDefinitionMapper definitionMapper;
    private final ApplicationTaskMapper taskMapper;
    private final ApplicationStageHistoryMapper historyMapper;
    private final ApplicationEventMapper eventMapper;
    private final ApplicationDecisionMapper decisionMapper;
    private final WorkflowService workflow;
    private final WfInstanceMapper instanceMapper;
    private final GuardEvaluator guards;
    private final ApplicationOpportunityService opportunities;
    private final ApplicantService applicants;

    /** Steps that belong to the student or to the submit flow, never offered to staff. */
    private static final Set<String> NOT_A_STAFF_STEP = Set.of("submit", "accept_offer", "decline_offer", "withdraw");
    private static final String DECISION_PREDICATE = "DECISION_RECORDED";

    public ApplicationAdminService(ApplicationMapper applicationMapper, WfDefinitionMapper definitionMapper,
            ApplicationTaskMapper taskMapper, ApplicationStageHistoryMapper historyMapper,
            ApplicationEventMapper eventMapper, ApplicationDecisionMapper decisionMapper, WorkflowService workflow,
            WfInstanceMapper instanceMapper, GuardEvaluator guards, ApplicationOpportunityService opportunities,
            ApplicantService applicants) {
        this.applicationMapper = applicationMapper;
        this.definitionMapper = definitionMapper;
        this.taskMapper = taskMapper;
        this.historyMapper = historyMapper;
        this.eventMapper = eventMapper;
        this.decisionMapper = decisionMapper;
        this.workflow = workflow;
        this.instanceMapper = instanceMapper;
        this.guards = guards;
        this.opportunities = opportunities;
        this.applicants = applicants;
    }

    @Transactional(readOnly = true)
    public PageResponse<StaffApplicationRow> list(ApplicationSearch filter, int page, int size) {
        page = PageSupport.clampPage(page);
        size = PageSupport.clampSize(size);
        PageHelper.startPage(page + 1, size);
        List<Application> rows = applicationMapper.searchStaff(filter);
        long total = new PageInfo<>(rows).getTotal();
        Map<Long, String> assignees = userNames(rows.stream().map(Application::getAssigneeUserId).toList());
        return PageResponse.of(rows.stream().map(a -> toRow(a, assignees)).toList(), page, size, total);
    }

    @Transactional(readOnly = true)
    public ApplicationDetailResponse get(Long id) {
        Application application = load(id);
        var history = historyMapper.findByApplication(id);
        var events = eventMapper.findByApplication(id);
        var decisions = decisionMapper.findByApplication(id);

        Set<Long> userIds = new LinkedHashSet<>();
        history.forEach(h -> userIds.add(h.getChangedBy()));
        events.forEach(e -> {
            userIds.add(e.getActorUserId());
            userIds.add(assignedUserId(e));
        });
        decisions.forEach(d -> userIds.add(d.getDecidedBy()));
        userIds.add(application.getAssigneeUserId());
        Map<Long, String> names = userNames(userIds);
        Map<Long, String> stageNames = new HashMap<>();
        history.forEach(h -> {
            stageName(stageNames, h.getFromStageId());
            stageName(stageNames, h.getToStageId());
        });

        return new ApplicationDetailResponse(
                toResponse(application),
                taskMapper.findByApplication(id).stream().map(TaskResponse::of).toList(),
                history.stream().map(h -> StageHistoryResponse.of(h, stageNames.get(h.getFromStageId()),
                        stageNames.get(h.getToStageId()), names.get(h.getChangedBy()))).toList(),
                events.stream().map(e -> ApplicationEventResponse.of(e, names.get(e.getActorUserId()),
                        eventDetail(e, names))).toList(),
                decisions.stream().map(d -> DecisionResponse.of(d, names.get(d.getDecidedBy()))).toList(),
                applicantSummary(application.getApplicantId()),
                opportunitySummary(id),
                names.get(application.getAssigneeUserId()));
    }

    private StaffApplicationRow toRow(Application application, Map<Long, String> assigneeNames) {
        var opportunity = opportunities.describe(application.getId());
        WfStage stage = application.getCurrentStageId() == null ? null
                : definitionMapper.findStageById(application.getCurrentStageId());
        return new StaffApplicationRow(application.getId(), application.getReference(), application.getApplicantId(),
                applicantSummary(application.getApplicantId()).name(),
                application.getApplicationType() == null ? null : application.getApplicationType().name(),
                opportunity.title(), opportunity.universityName(), opportunity.intake(),
                stage == null ? null : stage.getName(), application.getCurrentStatus(),
                application.getAssigneeUserId(), assigneeNames.get(application.getAssigneeUserId()),
                application.getSubmittedAt() == null ? null : application.getSubmittedAt().toString());
    }

    private static final String CASE_ASSIGNED = "CASE_ASSIGNED";

    /** The user an assignment event points at, or null for any other event. */
    private static Long assignedUserId(ApplicationEvent event) {
        if (!CASE_ASSIGNED.equals(event.getEventType())) {
            return null;
        }
        try {
            return Long.valueOf(eventNote(event));
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private static String eventDetail(ApplicationEvent event, Map<Long, String> names) {
        Long assigned = assignedUserId(event);
        if (assigned != null) {
            return names.getOrDefault(assigned, "a staff member");
        }
        return eventNote(event);
    }

    /** The engine stores an event's free text as {@code {"note": "..."}}; older rows may hold the bare text. */
    private static String eventNote(ApplicationEvent event) {
        String raw = event.getDetailJson();
        if (raw == null) {
            return null;
        }
        try {
            return com.alibaba.fastjson2.JSON.parseObject(raw).getString("note");
        }
        catch (RuntimeException e) {
            return raw.trim();
        }
    }

    private ApplicationDetailResponse.ApplicantSummary applicantSummary(Long applicantId) {
        var applicant = applicants.get(applicantId);
        return new ApplicationDetailResponse.ApplicantSummary(applicantId, applicant.publicId(),
                (applicant.givenName() + " " + applicant.familyName()).strip(), applicant.email(), applicant.phone(),
                applicant.nationality());
    }

    private ApplicationDetailResponse.OpportunitySummary opportunitySummary(Long applicationId) {
        var description = opportunities.describe(applicationId);
        return new ApplicationDetailResponse.OpportunitySummary(description.kind().name(), description.title(),
                description.universityName(), description.intake());
    }

    private void stageName(Map<Long, String> into, Long stageId) {
        if (stageId != null && !into.containsKey(stageId)) {
            WfStage stage = definitionMapper.findStageById(stageId);
            into.put(stageId, stage == null ? null : stage.getName());
        }
    }

    private Map<Long, String> userNames(Collection<Long> ids) {
        Set<Long> wanted = new LinkedHashSet<>(ids);
        wanted.remove(null);
        Map<Long, String> names = new HashMap<>();
        if (wanted.isEmpty()) {
            return names;
        }
        for (StaffUserRow row : applicationMapper.findUserNames(wanted)) {
            String name = row.nickName() == null || row.nickName().isBlank() ? row.userName() : row.nickName();
            names.put(row.userId(), name);
        }
        return names;
    }

    /** Active staff an application can be handed to. */
    @Transactional(readOnly = true)
    public List<StaffUserResponse> assignees() {
        return applicationMapper.findAssignableStaff().stream()
                .map(r -> new StaffUserResponse(r.userId(), r.nickName() == null || r.nickName().isBlank()
                        ? r.userName() : r.nickName(), r.userName()))
                .toList();
    }

    /** The status changes available from the application's current stage, each with why it is blocked, if it is. */
    @Transactional(readOnly = true)
    public List<NextStepResponse> nextSteps(Long id) {
        Application application = load(id);
        WfInstance instance = instanceMapper.findByApplicationId(id);
        if (instance == null || instance.getStatus() == WfInstanceStatus.CLOSED || application.getCurrentStageId() == null) {
            return List.of();
        }
        List<NextStepResponse> steps = new java.util.ArrayList<>();
        for (WfTransition transition : definitionMapper.findTransitionsFrom(instance.getDefinitionId(),
                application.getCurrentStageId())) {
            if (NOT_A_STAFF_STEP.contains(transition.getCode())) {
                continue;
            }
            WfStage target = definitionMapper.findStageById(transition.getToStageId());
            steps.add(new NextStepResponse(transition.getCode(), target.getName(), target.getStatusLabel(),
                    decisionLabel(transition.getGuardJson()), blockedReason(transition, application)));
        }
        return steps;
    }

    private String blockedReason(WfTransition transition, Application application) {
        if (transition.getRoleRequired() != null && !WorkflowService.holdsRole(transition.getRoleRequired())) {
            return "Needs the " + transition.getRoleRequired().replace('_', ' ') + " role";
        }
        if (!guards.tasksBlockingExitDone(application.getId())) {
            return "Finish the required tasks of this stage first";
        }
        for (String predicate : GuardEvaluator.predicatesIn(transition.getGuardJson())) {
            if (predicate.startsWith(DECISION_PREDICATE + ":")) {
                continue; // taking the step records it
            }
            if (!guards.evaluateOne(predicate, application)) {
                return "Not ready: " + describeUnmet(predicate);
            }
        }
        return null;
    }

    private static String describeUnmet(String predicate) {
        String[] parts = predicate.split(":");
        return switch (parts[0]) {
            case "ALL_MANDATORY_TASKS_DONE" -> "complete the mandatory tasks";
            case "ALL_REQUIRED_DOCUMENTS_ATTACHED" -> "the required documents are not all uploaded";
            case "ALL_REQUIRED_DOCUMENTS_VERIFIED" -> "verify the required documents";
            case "PAYMENT_SETTLED" -> "confirm the payment";
            case "FIELD_SET" -> (parts.length > 1 ? parts[1].replace('_', ' ') : "a required field") + " is missing";
            default -> predicate;
        };
    }

    /** The outcome the step records, for example "Eligible"; null when the step records no decision. */
    private static String decisionLabel(String guardJson) {
        for (String predicate : GuardEvaluator.predicatesIn(guardJson)) {
            String[] parts = predicate.split(":");
            if (DECISION_PREDICATE.equals(parts[0]) && parts.length == 3) {
                String words = parts[2].toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
                return Character.toUpperCase(words.charAt(0)) + words.substring(1);
            }
        }
        return null;
    }

    /**
     * Takes one status change: records the formal decision the step stands for (the engine requires it before the move)
     * with the officer's note as its rationale, then moves the application. The student is emailed by the engine.
     */
    @Transactional(rollbackFor = Exception.class)
    public ApplicationResponse advance(Long id, AdvanceRequest req) {
        Application application = load(id);
        NextStepResponse step = nextSteps(id).stream().filter(s -> s.code().equals(req.code())).findFirst()
                .orElseThrow(() -> new NadBadRequestException("this status change is not available from the current stage"));
        if (step.blockedReason() != null) {
            throw new NadBadRequestException(step.blockedReason());
        }
        WfInstance instance = instanceMapper.findByApplicationId(id);
        WfTransition transition = definitionMapper.findTransition(instance.getDefinitionId(), req.code(),
                application.getCurrentStageId());
        long actor = AuditActor.userId();
        String note = req.note() == null || req.note().isBlank() ? null : req.note().trim();
        for (String predicate : GuardEvaluator.predicatesIn(transition.getGuardJson())) {
            String[] parts = predicate.split(":");
            if (DECISION_PREDICATE.equals(parts[0]) && parts.length == 3 && !guards.evaluateOne(predicate, application)) {
                workflow.recordDecision(id, parts[1], parts[2], note == null ? step.label() : note, actor);
            }
        }
        return toResponse(workflow.execute(id, req.code(), actor, true, note, req.version()));
    }

    @Transactional(rollbackFor = Exception.class)
    public ApplicationResponse create(StartApplicationRequest req) {
        Application application = workflow.startDraft(req.applicantId(), req.applicationType(), req.programId(),
                req.scholarshipId(), req.intakeId(), AuditActor.userId());
        return toResponse(application);
    }

    @Transactional(rollbackFor = Exception.class)
    public ApplicationResponse update(Long id, UpdateApplicationRequest req) {
        Application application = load(id);
        ApplicationShape.validate(application.getApplicationType(), req.programId(), req.scholarshipId(), req.intakeId());
        application.setProgramId(req.programId());
        application.setScholarshipId(req.scholarshipId());
        application.setIntakeId(req.intakeId());
        application.setUpdateBy(AuditActor.username());
        applicationMapper.updateDraftFields(application);
        return toResponse(applicationMapper.findById(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public ApplicationResponse transition(Long id, String code, TransitionRequest req) {
        Application application = workflow.execute(id, code, AuditActor.userId(), true, req.reason(), req.version());
        return toResponse(application);
    }

    @Transactional(rollbackFor = Exception.class)
    public DecisionResponse decide(Long id, DecisionRequest req) {
        ApplicationDecision decision = workflow.recordDecision(id, req.decisionType(), req.outcome(), req.rationale(),
                AuditActor.userId());
        return DecisionResponse.of(decision, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long id, Long taskId) {
        workflow.completeTask(id, taskId, AuditActor.userId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void skipTask(Long id, Long taskId, SkipTaskRequest req) {
        workflow.skipTask(id, taskId, req.reason(), AuditActor.userId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void assign(Long id, AssignRequest req) {
        boolean staff = applicationMapper.findAssignableStaff().stream().anyMatch(r -> r.userId().equals(req.assigneeUserId()));
        if (!staff) {
            throw new NadBadRequestException("the assignee must be an active staff member");
        }
        workflow.assign(id, req.assigneeUserId(), AuditActor.userId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void claim(Long id) {
        workflow.claim(id, AuditActor.userId());
    }

    @Transactional(readOnly = true)
    public List<WfDefinition> listDefinitions() {
        return definitionMapper.list();
    }

    @Transactional(readOnly = true)
    public List<WfStage> definitionStages(Long definitionId) {
        return definitionMapper.findStages(definitionId);
    }

    private Application load(Long id) {
        Application application = applicationMapper.findById(id);
        if (application == null) {
            throw new NadNotFoundException("application not found");
        }
        return application;
    }

    private ApplicationResponse toResponse(Application application) {
        WfStage stage = application.getCurrentStageId() == null ? null
                : definitionMapper.findStageById(application.getCurrentStageId());
        return ApplicationResponse.of(application, stage);
    }
}
