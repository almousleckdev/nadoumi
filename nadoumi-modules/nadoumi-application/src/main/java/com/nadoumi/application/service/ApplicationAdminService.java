package com.nadoumi.application.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nadoumi.application.domain.Application;
import com.nadoumi.application.domain.ApplicationDecision;
import com.nadoumi.application.domain.ApplicationEvent;
import com.nadoumi.application.domain.ApplicationStageHistory;
import com.nadoumi.application.domain.ApplicationTask;
import com.nadoumi.application.domain.WfDefinition;
import com.nadoumi.application.domain.WfStage;
import com.nadoumi.application.domain.enums.ApplicationType;
import com.nadoumi.application.mapper.ApplicationDecisionMapper;
import com.nadoumi.application.mapper.ApplicationEventMapper;
import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.application.mapper.ApplicationSearch;
import com.nadoumi.application.mapper.ApplicationStageHistoryMapper;
import com.nadoumi.application.mapper.ApplicationTaskMapper;
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

    public ApplicationAdminService(ApplicationMapper applicationMapper, WfDefinitionMapper definitionMapper,
            ApplicationTaskMapper taskMapper, ApplicationStageHistoryMapper historyMapper,
            ApplicationEventMapper eventMapper, ApplicationDecisionMapper decisionMapper, WorkflowService workflow) {
        this.applicationMapper = applicationMapper;
        this.definitionMapper = definitionMapper;
        this.taskMapper = taskMapper;
        this.historyMapper = historyMapper;
        this.eventMapper = eventMapper;
        this.decisionMapper = decisionMapper;
        this.workflow = workflow;
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> list(ApplicationSearch filter, int page, int size) {
        page = PageSupport.clampPage(page);
        size = PageSupport.clampSize(size);
        PageHelper.startPage(page + 1, size);
        List<Application> rows = applicationMapper.searchStaff(filter);
        long total = new PageInfo<>(rows).getTotal();
        return PageResponse.of(rows.stream().map(this::toResponse).toList(), page, size, total);
    }

    @Transactional(readOnly = true)
    public ApplicationDetailResponse get(Long id) {
        Application application = load(id);
        return new ApplicationDetailResponse(
                toResponse(application),
                taskMapper.findByApplication(id).stream().map(TaskResponse::of).toList(),
                historyMapper.findByApplication(id).stream().map(StageHistoryResponse::of).toList(),
                eventMapper.findByApplication(id).stream().map(ApplicationEventResponse::of).toList(),
                decisionMapper.findByApplication(id).stream().map(DecisionResponse::of).toList());
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
        return DecisionResponse.of(decision);
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
