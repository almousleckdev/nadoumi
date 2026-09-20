package com.nadoumi.application.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nadoumi.applicant.service.ApplicantService;
import com.nadoumi.application.domain.Application;
import com.nadoumi.application.domain.WfInstance;
import com.nadoumi.application.domain.WfStage;
import com.nadoumi.application.domain.WfTransition;
import com.nadoumi.application.domain.enums.ApplicationType;
import com.nadoumi.application.domain.enums.WfInstanceStatus;
import com.nadoumi.application.domain.enums.WfStageType;
import com.nadoumi.application.mapper.ApplicationDecisionMapper;
import com.nadoumi.application.mapper.ApplicationEventMapper;
import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.application.mapper.ApplicationSnapshotMapper;
import com.nadoumi.application.mapper.ApplicationStageHistoryMapper;
import com.nadoumi.application.mapper.ApplicationTaskMapper;
import com.nadoumi.application.mapper.WfDefinitionMapper;
import com.nadoumi.application.mapper.WfInstanceMapper;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.identity.service.UserApplicantAccessService;
import com.nadoumi.program.service.ProgramService;
import com.nadoumi.scholarship.service.ScholarshipAdminService;
import java.util.List;
import java.util.Optional;

/**
 * Wires a real {@link WorkflowService} (and a real {@link GuardEvaluator}) against
 * Mockito mocks for every collaborator, so tests exercise the engine's actual
 * transition/guard/history logic rather than a mocked black box. Shared across the
 * spec's nine required test classes to avoid repeating a 12-dependency constructor.
 */
class WorkflowServiceHarness {

    final WfDefinitionMapper definitionMapper = mock(WfDefinitionMapper.class);
    final ApplicationMapper applicationMapper = mock(ApplicationMapper.class);
    final WfInstanceMapper instanceMapper = mock(WfInstanceMapper.class);
    final ApplicationTaskMapper taskMapper = mock(ApplicationTaskMapper.class);
    final ApplicationStageHistoryMapper historyMapper = mock(ApplicationStageHistoryMapper.class);
    final ApplicationEventMapper eventMapper = mock(ApplicationEventMapper.class);
    final ApplicationDecisionMapper decisionMapper = mock(ApplicationDecisionMapper.class);
    final ApplicationSnapshotMapper snapshotMapper = mock(ApplicationSnapshotMapper.class);
    final OutboxWriter outboxWriter = mock(OutboxWriter.class);
    final ProgramService programService = mock(ProgramService.class);
    final ScholarshipAdminService scholarshipAdminService = mock(ScholarshipAdminService.class);
    final ApplicantService applicantService = mock(ApplicantService.class);
    final UserApplicantAccessService accessService = mock(UserApplicantAccessService.class);

    final GuardEvaluator guards = new GuardEvaluator(taskMapper, decisionMapper, Optional.empty(), Optional.empty());

    final WorkflowService service = new WorkflowService(definitionMapper, applicationMapper, instanceMapper,
            taskMapper, historyMapper, eventMapper, decisionMapper, snapshotMapper, guards, outboxWriter,
            programService, scholarshipAdminService, applicantService, accessService);

    /** Everything defaults to "there is nothing to materialise / notify" so a test only stubs what it asserts on. */
    void stubQuietDefaults() {
        when(definitionMapper.findTaskTemplates(org.mockito.ArgumentMatchers.anyLong())).thenReturn(List.of());
        when(taskMapper.findByApplication(org.mockito.ArgumentMatchers.anyLong())).thenReturn(List.of());
        when(accessService.listForApplicant(org.mockito.ArgumentMatchers.anyLong())).thenReturn(List.of());
        when(applicationMapper.updateWithLock(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(1);
    }

    static Application application(long id, long applicantId, long stageId, int version) {
        Application a = new Application();
        a.setId(id);
        a.setApplicantId(applicantId);
        a.setApplicationType(ApplicationType.PROGRAM_ONLY);
        a.setProgramId(100L);
        a.setCurrentStageId(stageId);
        a.setCurrentStatus("IN_REVIEW");
        a.setVersion(version);
        return a;
    }

    static WfInstance instance(long applicationId, long definitionId, long stageId) {
        WfInstance i = new WfInstance();
        i.setId(500L);
        i.setApplicationId(applicationId);
        i.setDefinitionId(definitionId);
        i.setDefinitionVersion(1);
        i.setCurrentStageId(stageId);
        i.setStatus(WfInstanceStatus.RUNNING);
        return i;
    }

    static WfStage stage(long id, String code, WfStageType type) {
        WfStage s = new WfStage();
        s.setId(id);
        s.setCode(code);
        s.setName(code);
        s.setStageType(type);
        s.setStatusLabel(code);
        return s;
    }

    static WfTransition transition(String code, Long from, long to, String guardJson, String roleRequired) {
        WfTransition t = new WfTransition();
        t.setCode(code);
        t.setFromStageId(from);
        t.setToStageId(to);
        t.setGuardJson(guardJson);
        t.setRoleRequired(roleRequired);
        return t;
    }
}
