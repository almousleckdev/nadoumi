package com.nadoumi.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.application.domain.Application;
import com.nadoumi.application.domain.WfInstance;
import com.nadoumi.application.domain.WfTransition;
import com.nadoumi.application.domain.enums.WfInstanceStatus;
import com.nadoumi.application.exception.DocumentsRequiredException;
import com.nadoumi.application.exception.PaymentRequiredException;
import com.nadoumi.application.mapper.ApplicationEventMapper;
import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.application.mapper.WfDefinitionMapper;
import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.common.exception.NadForbiddenException;
import com.nadoumi.identity.access.CurrentCaller;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * A draft can always be saved, but it leaves DRAFT only through the engine's submit gate (fee paid, mandatory documents
 * in) and a student can run only the few transitions meant for students. These tests cover every way in: the
 * student's submit, the student's generic transition call, and a staff submit.
 */
class ApplicationSubmitPaymentGateTest {

    private static final long ID = 7L;

    private final ApplicationMapper applicationMapper = mock(ApplicationMapper.class);
    private final WorkflowService workflow = mock(WorkflowService.class);
    private final CurrentCaller caller = mock(CurrentCaller.class);
    private final StudentApplicationService service = new StudentApplicationService(
            applicationMapper, mock(WfDefinitionMapper.class), mock(ApplicationEventMapper.class), workflow,
            mock(NadoumiAccessService.class), caller, opportunities(), applicants());

    private Application draft() {
        Application a = new Application();
        a.setId(ID);
        a.setCurrentStatus("DRAFT");
        a.setVersion(3);
        when(applicationMapper.findById(ID)).thenReturn(a);
        when(caller.requireUserId()).thenReturn(1L);
        return a;
    }

    private static ApplicationOpportunityService opportunities() {
        var opportunities = mock(ApplicationOpportunityService.class);
        when(opportunities.describe(org.mockito.ArgumentMatchers.anyLong())).thenReturn(
                new ApplicationOpportunityService.Description("NAD1", ApplicationOpportunityService.Kind.PROGRAM, "MBBS", "Fudan University", "Autumn"));
        return opportunities;
    }

    private static com.nadoumi.applicant.service.ApplicantService applicants() {
        var applicants = mock(com.nadoumi.applicant.service.ApplicantService.class);
        var applicant = mock(com.nadoumi.applicant.web.response.ApplicantResponse.class);
        when(applicant.givenName()).thenReturn("Amina");
        when(applicant.familyName()).thenReturn("Diallo");
        when(applicants.get(org.mockito.ArgumentMatchers.any())).thenReturn(applicant);
        return applicants;
    }

    @Test
    void shouldRunTheSubmitTransition_forAStudentSubmit() {
        Application a = draft();
        when(workflow.execute(eq(ID), eq("submit"), eq(1L), eq(false), any(), eq(3))).thenReturn(a);

        assertThat(service.submit(ID)).isNotNull();
    }

    @Test
    void shouldRefuseTheSubmitTransition_throughTheGenericTransitionEndpoint() {
        draft();

        assertThatThrownBy(() -> service.transition(ID, "submit", null)).isInstanceOf(NadForbiddenException.class);

        verify(workflow, never()).execute(anyLong(), any(), anyLong(), anyBoolean(), any(), anyInt());
    }

    @Test
    void shouldRefuseEveryStaffTransition_whenAStudentAsksForIt() {
        draft();

        for (String code : List.of("claim", "eligible", "ineligible", "docs_complete", "package_ready", "provider_responded",
                "university_responded", "uni_offer", "sch_awarded", "enrolled")) {
            assertThatThrownBy(() -> service.transition(ID, code, null)).isInstanceOf(NadForbiddenException.class);
        }
        verify(workflow, never()).execute(anyLong(), any(), anyLong(), anyBoolean(), any(), anyInt());
    }

    @Test
    void shouldLetAStudentRespondToAnOffer_butNeverWithdraw() {
        Application a = draft();
        when(workflow.execute(eq(ID), any(), eq(1L), eq(false), any(), eq(3))).thenReturn(a);

        service.transition(ID, "accept_offer", null);
        service.transition(ID, "decline_offer", null);
        // the application fee is not refundable, so a student cannot withdraw
        assertThatThrownBy(() -> service.transition(ID, "withdraw", "Changed plans")).isInstanceOf(NadForbiddenException.class);

        verify(workflow).execute(eq(ID), eq("accept_offer"), eq(1L), eq(false), any(), eq(3));
        verify(workflow).execute(eq(ID), eq("decline_offer"), eq(1L), eq(false), any(), eq(3));
    }

    @Test
    void shouldGateTheSubmitTransitionInTheEngine_whoeverRunsIt() {
        WorkflowServiceHarness h = new WorkflowServiceHarness();
        Application a = new Application();
        a.setId(ID);
        a.setCurrentStageId(10L);
        a.setVersion(0);
        when(h.applicationMapper.findById(ID)).thenReturn(a);
        WfInstance instance = new WfInstance();
        instance.setDefinitionId(3L);
        instance.setStatus(WfInstanceStatus.RUNNING);
        when(h.instanceMapper.findByApplicationId(ID)).thenReturn(instance);
        WfTransition submit = new WfTransition();
        submit.setCode("submit");
        when(h.definitionMapper.findTransition(3L, "submit", 10L)).thenReturn(submit);
        doThrow(new PaymentRequiredException("fee")).when(h.submitGate).require(ID);

        // a student, and staff acting for the student, both stop at the gate
        assertThatThrownBy(() -> h.service.execute(ID, "submit", 1L, false, null, 0)).isInstanceOf(PaymentRequiredException.class);
        assertThatThrownBy(() -> h.service.execute(ID, "submit", 2L, true, null, 0)).isInstanceOf(PaymentRequiredException.class);

        doThrow(new DocumentsRequiredException("docs")).when(h.submitGate).require(ID);
        assertThatThrownBy(() -> h.service.execute(ID, "submit", 2L, true, null, 0)).isInstanceOf(DocumentsRequiredException.class);
        verify(h.historyMapper, never()).insert(any());
    }
}
