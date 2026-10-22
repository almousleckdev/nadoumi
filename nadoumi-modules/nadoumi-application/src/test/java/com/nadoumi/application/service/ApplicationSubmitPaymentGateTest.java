package com.nadoumi.application.service;

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
import com.nadoumi.application.exception.DocumentsRequiredException;
import com.nadoumi.application.exception.PaymentRequiredException;
import com.nadoumi.application.mapper.ApplicationEventMapper;
import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.application.mapper.WfDefinitionMapper;
import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.identity.access.CurrentCaller;
import java.util.List;
import org.junit.jupiter.api.Test;

/** A draft can always be saved, but is submitted only once the fee is paid and the mandatory documents are uploaded. */
class ApplicationSubmitPaymentGateTest {

    private static final long ID = 7L;

    private final ApplicationMapper applicationMapper = mock(ApplicationMapper.class);
    private final WorkflowService workflow = mock(WorkflowService.class);
    private final CurrentCaller caller = mock(CurrentCaller.class);
    private final PaymentGuardProvider payments = mock(PaymentGuardProvider.class);
    private final ApplicationChecklistService checklist = mock(ApplicationChecklistService.class);
    private final StudentApplicationService service = new StudentApplicationService(
            applicationMapper, mock(WfDefinitionMapper.class), mock(ApplicationEventMapper.class), workflow,
            mock(NadoumiAccessService.class), caller, payments, checklist);

    private Application draft() {
        Application a = new Application();
        a.setId(ID);
        a.setCurrentStatus("DRAFT");
        a.setVersion(3);
        when(applicationMapper.findById(ID)).thenReturn(a);
        when(caller.requireUserId()).thenReturn(1L);
        return a;
    }

    @Test
    void shouldRefuseToSubmit_whenTheApplicationFeeIsNotSettled() {
        draft();
        when(payments.isSettled(ID, PaymentGuardProvider.APPLICATION_FEE)).thenReturn(false);

        assertThatThrownBy(() -> service.submit(ID)).isInstanceOf(PaymentRequiredException.class);

        verify(workflow, never()).execute(anyLong(), any(), anyLong(), anyBoolean(), any(), anyInt());
    }

    @Test
    void shouldRunTheSubmitTransition_whenTheApplicationFeeIsSettled() {
        Application a = draft();
        when(payments.isSettled(ID, PaymentGuardProvider.APPLICATION_FEE)).thenReturn(true);
        when(workflow.execute(eq(ID), eq("submit"), eq(1L), eq(false), any(), eq(3))).thenReturn(a);

        assertThat(service.submit(ID)).isNotNull();

        verify(workflow).execute(eq(ID), eq("submit"), eq(1L), eq(false), any(), eq(3));
    }

    @Test
    void shouldRefuseToSubmit_whenAMandatoryDocumentIsMissing() {
        draft();
        when(payments.isSettled(ID, PaymentGuardProvider.APPLICATION_FEE)).thenReturn(true);
        when(checklist.missingMandatory(ID)).thenReturn(List.of("PASSPORT", "TRANSCRIPT"));

        assertThatThrownBy(() -> service.submit(ID))
                .isInstanceOf(DocumentsRequiredException.class)
                .hasMessageContaining("PASSPORT").hasMessageContaining("TRANSCRIPT");

        verify(workflow, never()).execute(anyLong(), any(), anyLong(), anyBoolean(), any(), anyInt());
    }
}
