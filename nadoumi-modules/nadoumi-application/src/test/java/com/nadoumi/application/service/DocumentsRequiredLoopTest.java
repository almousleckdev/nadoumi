package com.nadoumi.application.service;

import static com.nadoumi.application.service.WorkflowServiceHarness.application;
import static com.nadoumi.application.service.WorkflowServiceHarness.instance;
import static com.nadoumi.application.service.WorkflowServiceHarness.stage;
import static com.nadoumi.application.service.WorkflowServiceHarness.transition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.application.domain.Application;
import com.nadoumi.application.domain.ApplicationTask;
import com.nadoumi.application.domain.WfStageTaskTemplate;
import com.nadoumi.application.domain.enums.ApplicationTaskStatus;
import com.nadoumi.application.domain.enums.WfStageType;
import com.nadoumi.application.mapper.ApplicationEventMapper;
import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.application.mapper.WfDefinitionMapper;
import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadForbiddenException;
import com.nadoumi.identity.access.CurrentCaller;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * "Documents required" sends the case back to the student on the SAME application: the student's step is a plain
 * transition, never a submit, so no second application and no second fee can come out of it.
 */
class DocumentsRequiredLoopTest {

    private static final long APPLICATION_ID = 1L;
    private static final long DEFINITION_ID = 7L;
    private static final long DOCUMENTS_REQUIRED = 30L;
    private static final long UNDER_REVIEW = 31L;

    @Test
    void shouldReturnTheCaseToReview_withoutRunningTheSubmitGate() {
        WorkflowServiceHarness h = new WorkflowServiceHarness();
        h.stubQuietDefaults();
        when(h.applicationMapper.findById(APPLICATION_ID)).thenReturn(application(APPLICATION_ID, 9L, DOCUMENTS_REQUIRED, 4));
        when(h.instanceMapper.findByApplicationId(APPLICATION_ID)).thenReturn(instance(APPLICATION_ID, DEFINITION_ID, DOCUMENTS_REQUIRED));
        when(h.definitionMapper.findTransition(DEFINITION_ID, "resubmit", DOCUMENTS_REQUIRED))
                .thenReturn(transition("resubmit", DOCUMENTS_REQUIRED, UNDER_REVIEW, null, null));
        when(h.definitionMapper.findStageById(UNDER_REVIEW)).thenReturn(stage(UNDER_REVIEW, "UNDER_REVIEW", WfStageType.NORMAL));

        Application result = h.service.execute(APPLICATION_ID, "resubmit", 50L, false, null, 4);

        assertThat(result.getCurrentStageId()).isEqualTo(UNDER_REVIEW);
        verify(h.submitGate, never()).require(anyLong());
        verify(h.applicationMapper, never()).insert(any());
    }

    @Test
    void shouldRefuseASecondResubmit_onceTheCaseIsBackInReview() {
        WorkflowServiceHarness h = new WorkflowServiceHarness();
        h.stubQuietDefaults();
        when(h.applicationMapper.findById(APPLICATION_ID)).thenReturn(application(APPLICATION_ID, 9L, UNDER_REVIEW, 5));
        when(h.instanceMapper.findByApplicationId(APPLICATION_ID)).thenReturn(instance(APPLICATION_ID, DEFINITION_ID, UNDER_REVIEW));

        assertThatThrownBy(() -> h.service.execute(APPLICATION_ID, "resubmit", 50L, false, null, 5))
                .isInstanceOf(NadBadRequestException.class);
    }

    @Test
    void shouldNotHandOutTheStageChecklistTwice_whenTheCaseReturnsToAStage() {
        WorkflowServiceHarness h = new WorkflowServiceHarness();
        h.stubQuietDefaults();
        WfStageTaskTemplate template = new WfStageTaskTemplate();
        template.setId(77L);
        template.setTitle("Review each uploaded document");
        when(h.definitionMapper.findTaskTemplates(UNDER_REVIEW)).thenReturn(List.of(template));
        ApplicationTask existing = new ApplicationTask();
        existing.setWfStageTaskTemplateId(77L);
        existing.setStatus(ApplicationTaskStatus.DONE);
        when(h.taskMapper.findByApplication(APPLICATION_ID)).thenReturn(List.of(existing));
        when(h.applicationMapper.findById(APPLICATION_ID)).thenReturn(application(APPLICATION_ID, 9L, DOCUMENTS_REQUIRED, 4));
        when(h.instanceMapper.findByApplicationId(APPLICATION_ID)).thenReturn(instance(APPLICATION_ID, DEFINITION_ID, DOCUMENTS_REQUIRED));
        when(h.definitionMapper.findTransition(DEFINITION_ID, "resubmit", DOCUMENTS_REQUIRED))
                .thenReturn(transition("resubmit", DOCUMENTS_REQUIRED, UNDER_REVIEW, null, null));
        when(h.definitionMapper.findStageById(UNDER_REVIEW)).thenReturn(stage(UNDER_REVIEW, "UNDER_REVIEW", WfStageType.NORMAL));

        h.service.execute(APPLICATION_ID, "resubmit", 50L, false, null, 4);

        verify(h.taskMapper, never()).insert(any());
    }

    @Test
    void shouldLetAStudentResubmit_butRecordTheirOfferAnswerAsTheDecision() {
        ApplicationMapper applicationMapper = mock(ApplicationMapper.class);
        WorkflowService workflow = mock(WorkflowService.class);
        CurrentCaller caller = mock(CurrentCaller.class);
        StudentApplicationService service = new StudentApplicationService(applicationMapper, mock(WfDefinitionMapper.class),
                mock(ApplicationEventMapper.class), workflow, mock(NadoumiAccessService.class), caller, opportunities(),
                applicants(), mock(PaymentGuardProvider.class),
            mock(com.nadoumi.application.mapper.ApplicationStageHistoryMapper.class));
        Application a = application(APPLICATION_ID, 9L, DOCUMENTS_REQUIRED, 4);
        when(applicationMapper.findById(APPLICATION_ID)).thenReturn(a);
        when(caller.requireUserId()).thenReturn(1L);
        when(workflow.execute(eq(APPLICATION_ID), any(), eq(1L), eq(false), any(), eq(4))).thenReturn(a);

        service.transition(APPLICATION_ID, "resubmit", null);
        service.transition(APPLICATION_ID, "accept_offer", "Happy to accept");

        verify(workflow).execute(APPLICATION_ID, "resubmit", 1L, false, null, 4);
        verify(workflow).recordDecision(APPLICATION_ID, "APPLICANT_RESPONSE", "ACCEPTED_BY_APPLICANT", "Happy to accept", 1L);
        assertThatThrownBy(() -> service.transition(APPLICATION_ID, "mark_ready", null)).isInstanceOf(NadForbiddenException.class);
        verify(workflow, never()).execute(eq(APPLICATION_ID), eq("mark_ready"), anyLong(), anyBoolean(), any(), anyInt());
    }

    private static ApplicationOpportunityService opportunities() {
        var opportunities = mock(ApplicationOpportunityService.class);
        when(opportunities.describe(anyLong())).thenReturn(new ApplicationOpportunityService.Description("NAD1",
                ApplicationOpportunityService.Kind.PROGRAM, "MBBS", "Fudan University", "Autumn"));
        return opportunities;
    }

    private static com.nadoumi.applicant.service.ApplicantService applicants() {
        var applicants = mock(com.nadoumi.applicant.service.ApplicantService.class);
        var applicant = mock(com.nadoumi.applicant.web.response.ApplicantResponse.class);
        when(applicant.givenName()).thenReturn("Amina");
        when(applicant.familyName()).thenReturn("Diallo");
        when(applicants.get(any())).thenReturn(applicant);
        return applicants;
    }
}
