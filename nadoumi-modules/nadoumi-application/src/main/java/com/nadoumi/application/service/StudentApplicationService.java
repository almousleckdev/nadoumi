package com.nadoumi.application.service;

import com.nadoumi.application.domain.Application;
import com.nadoumi.application.domain.ApplicationEvent;
import com.nadoumi.application.domain.ApplicationStageHistory;
import com.nadoumi.application.domain.WfStage;
import com.nadoumi.application.mapper.ApplicationEventMapper;
import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.application.mapper.ApplicationStageHistoryMapper;
import com.nadoumi.application.mapper.WfDefinitionMapper;
import com.nadoumi.application.web.request.StartApplicationRequest;
import com.nadoumi.application.web.request.UpdateApplicationRequest;
import com.nadoumi.application.web.response.StudentApplicationResponse;
import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadForbiddenException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.identity.access.CurrentCaller;
import com.ruoyi.common.utils.AuditActor;
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
    private final ApplicationOpportunityService opportunities;
    private final com.nadoumi.applicant.service.ApplicantService applicants;
    private final PaymentGuardProvider payments;
    private final ApplicationStageHistoryMapper history;

    public StudentApplicationService(ApplicationMapper applicationMapper, WfDefinitionMapper definitionMapper,
            ApplicationEventMapper eventMapper, WorkflowService workflow, NadoumiAccessService access,
            CurrentCaller caller, ApplicationOpportunityService opportunities,
            com.nadoumi.applicant.service.ApplicantService applicants, PaymentGuardProvider payments,
            ApplicationStageHistoryMapper history) {
        this.applicationMapper = applicationMapper;
        this.definitionMapper = definitionMapper;
        this.eventMapper = eventMapper;
        this.workflow = workflow;
        this.access = access;
        this.caller = caller;
        this.opportunities = opportunities;
        this.applicants = applicants;
        this.payments = payments;
        this.history = history;
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
        ApplicationShape.validate(application.getApplicationType(), req.programId(), req.scholarshipId(), req.intakeId());
        application.setProgramId(req.programId());
        application.setScholarshipId(req.scholarshipId());
        application.setIntakeId(req.intakeId());
        application.setUpdateBy(AuditActor.username());
        applicationMapper.updateDraftFields(application);
        return toResponse(applicationMapper.findById(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public StudentApplicationResponse submit(Long id) {
        Application current = load(id);
        // the fee and the mandatory documents are checked by the engine itself (SubmitGate), for every actor
        Application updated = workflow.execute(id, "submit", caller.requireUserId(), false, null, current.getVersion());
        return toResponse(updated);
    }

    /**
     * The only transitions a student may run directly; everything else belongs to staff or to {@link #submit}.
     * There is no withdrawal: the application fee is not refundable, so a submitted application stays with Nadoumi.
     * {@code resubmit} returns a case from "Documents required" to review on the same application: it is not a submit,
     * so no new application and no new fee can come out of it.
     */
    static final java.util.Set<String> STUDENT_TRANSITIONS = java.util.Set.of("accept_offer", "decline_offer", "resubmit");

    private static final String DOCUMENTS_REQUIRED = "DOCUMENTS_REQUIRED";
    private static final String RESPONSE_DECISION = "APPLICANT_RESPONSE";
    private static final java.util.Map<String, String> RESPONSE_OUTCOMES = java.util.Map.of(
            "accept_offer", "ACCEPTED_BY_APPLICANT", "decline_offer", "DECLINED_BY_APPLICANT");

    @Transactional(rollbackFor = Exception.class)
    public StudentApplicationResponse transition(Long id, String code, String reason) {
        if (!STUDENT_TRANSITIONS.contains(code)) {
            throw new NadForbiddenException("this action is not available to students");
        }
        Application current = load(id);
        long userId = caller.requireUserId();
        String outcome = RESPONSE_OUTCOMES.get(code);
        if (outcome != null) {
            // the student's answer is the formal decision the engine requires before it moves the case
            workflow.recordDecision(id, RESPONSE_DECISION, outcome, reason, userId);
        }
        Application updated = workflow.execute(id, code, userId, false, reason, current.getVersion());
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
        // ApplicationOpportunityService leaves the university out of anything that involves a scholarship
        var opportunity = opportunities.describe(application.getId());
        var applicant = applicants.get(application.getApplicantId());
        return StudentApplicationResponse.of(application, stage, timeline, opportunity.title(),
                opportunity.universityName(), opportunity.intake(),
                (applicant.givenName() + " " + applicant.familyName()).strip(), paymentNeeded(application),
                actionNote(application));
    }

    /** The reason staff gave when they sent the case back for documents; it is written for the student to read. */
    private String actionNote(Application application) {
        if (!DOCUMENTS_REQUIRED.equals(application.getCurrentStatus())) {
            return null;
        }
        return history.findByApplication(application.getId()).stream()
                .filter(h -> h.getToStageId().equals(application.getCurrentStageId()))
                .reduce((first, second) -> second)
                .map(ApplicationStageHistory::getReason)
                .orElse(null);
    }

    /** Submitted and still open, yet no paid or reported fee on record: staff rejected or deleted it. */
    private boolean paymentNeeded(Application application) {
        String status = application.getCurrentStatus();
        boolean open = status != null && !"DRAFT".equals(status) && !status.startsWith("CLOSED_");
        return open && !payments.isPaidOrReported(application.getId(), PaymentGuardProvider.APPLICATION_FEE);
    }
}
