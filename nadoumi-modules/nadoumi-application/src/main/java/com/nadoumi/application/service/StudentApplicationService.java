package com.nadoumi.application.service;

import com.nadoumi.application.domain.Application;
import com.nadoumi.application.domain.ApplicationEvent;
import com.nadoumi.application.domain.WfStage;
import com.nadoumi.application.mapper.ApplicationEventMapper;
import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.application.mapper.WfDefinitionMapper;
import com.nadoumi.application.web.request.StartApplicationRequest;
import com.nadoumi.application.web.request.UpdateApplicationRequest;
import com.nadoumi.application.web.response.StudentApplicationResponse;
import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadForbiddenException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.identity.access.CurrentCaller;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public/student application surface (spec §II.7). A student sees only a safe
 * timeline projection — no internal notes, decision rationales, or assignee
 * identity (enforced here, not by hiding fields in a frontend).
 */
@Service
public class StudentApplicationService {

    private final ApplicationMapper applicationMapper;
    private final WfDefinitionMapper definitionMapper;
    private final ApplicationEventMapper eventMapper;
    private final WorkflowService workflow;
    private final NadoumiAccessService access;
    private final CurrentCaller caller;

    public StudentApplicationService(ApplicationMapper applicationMapper, WfDefinitionMapper definitionMapper,
            ApplicationEventMapper eventMapper, WorkflowService workflow, NadoumiAccessService access,
            CurrentCaller caller) {
        this.applicationMapper = applicationMapper;
        this.definitionMapper = definitionMapper;
        this.eventMapper = eventMapper;
        this.workflow = workflow;
        this.access = access;
        this.caller = caller;
    }

    @Transactional(rollbackFor = Exception.class)
    public StudentApplicationResponse create(StartApplicationRequest req) {
        if (req.applicantId() == null) {
            throw new NadBadRequestException("applicantId is required");
        }
        if (!access.canAccessApplicant(req.applicantId(), "CREATE_APPLICATION")) {
            throw new NadForbiddenException("no CREATE_APPLICATION grant for this applicant");
        }
        Application application = workflow.startDraft(req.applicantId(), req.applicationType(), req.programId(),
                req.scholarshipId(), req.intakeId(), caller.requireUserId());
        return toResponse(application);
    }

    /** Every application belonging to an applicant the caller can see. */
    @Transactional(readOnly = true)
    public List<StudentApplicationResponse> listMine() {
        return access.accessibleApplicantIds().stream()
                .flatMap(applicantId -> applicationMapper.findByApplicant(applicantId).stream())
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentApplicationResponse get(Long id) {
        return toResponse(load(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public StudentApplicationResponse updateDraft(Long id, UpdateApplicationRequest req) {
        Application application = load(id);
        if (!"DRAFT".equals(application.getCurrentStatus())) {
            throw new NadBadRequestException("the opportunity can only be changed while the application is a draft");
        }
        application.setProgramId(req.programId());
        application.setScholarshipId(req.scholarshipId());
        application.setIntakeId(req.intakeId());
        application.setUpdateBy(com.ruoyi.common.utils.AuditActor.username());
        applicationMapper.updateDraftFields(application);
        return toResponse(applicationMapper.findById(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public StudentApplicationResponse submit(Long id) {
        Application current = load(id);
        Application updated = workflow.execute(id, "submit", caller.requireUserId(), false, null, current.getVersion());
        return toResponse(updated);
    }

    /** {@code accept_offer} | {@code decline_offer} | {@code withdraw}. */
    @Transactional(rollbackFor = Exception.class)
    public StudentApplicationResponse transition(Long id, String code, String reason) {
        if ("withdraw".equals(code) && (reason == null || reason.isBlank())) {
            throw new NadBadRequestException("a reason is required to withdraw");
        }
        Application current = load(id);
        Application updated = workflow.execute(id, code, caller.requireUserId(), false, reason, current.getVersion());
        return toResponse(updated);
    }

    private Application load(Long id) {
        Application application = applicationMapper.findById(id);
        if (application == null) {
            throw new NadNotFoundException("application not found");
        }
        return application;
    }

    private StudentApplicationResponse toResponse(Application application) {
        WfStage stage = application.getCurrentStageId() == null ? null
                : definitionMapper.findStageById(application.getCurrentStageId());
        List<String> timeline = eventMapper.findByApplication(application.getId()).stream()
                .map(ApplicationEvent::getEventType)
                .toList();
        return StudentApplicationResponse.of(application, stage, timeline);
    }
}
