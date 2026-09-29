package com.nadoumi.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.application.domain.Application;
import com.nadoumi.common.access.AccessGrantStatus;
import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.identity.domain.UserApplicantAccess;
import com.nadoumi.identity.service.UserApplicantAccessService;
import com.nadoumi.program.service.ProgramService;
import com.nadoumi.scholarship.service.ScholarshipAdminService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ApplicationOutboxEmitterTest {

    private final OutboxWriter outboxWriter = mock(OutboxWriter.class);
    private final UserApplicantAccessService accessService = mock(UserApplicantAccessService.class);
    private final ProgramService programService = mock(ProgramService.class);
    private final ScholarshipAdminService scholarshipAdminService = mock(ScholarshipAdminService.class);
    private final ApplicationOutboxEmitter emitter =
            new ApplicationOutboxEmitter(outboxWriter, accessService, programService, scholarshipAdminService);

    private static Application application() {
        Application a = new Application();
        a.setId(77L);
        a.setApplicantId(5L);
        a.setProgramId(11L);
        return a;
    }

    private static UserApplicantAccess grant(long userId, AccessGrantStatus status) {
        UserApplicantAccess g = new UserApplicantAccess();
        g.setUserId(userId);
        g.setStatus(status);
        return g;
    }

    @Test
    void shouldWriteNothing_whenThereIsNoActiveRecipient() {
        when(accessService.listForApplicant(5L)).thenReturn(List.of(grant(9L, AccessGrantStatus.REVOKED)));

        emitter.submitted(application());

        verify(outboxWriter, never()).write(anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    void shouldFallBackToAGenericTitle_whenTheProgrammeCannotBeResolved() {
        when(accessService.listForApplicant(5L)).thenReturn(List.of(grant(9L, AccessGrantStatus.ACTIVE)));
        when(programService.get(11L)).thenThrow(new IllegalStateException("catalogue down"));

        emitter.statusChanged(application(), "IN_REVIEW");

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(outboxWriter).write(eq("application"), eq(77L), eq(OutboxEventTypes.APPLICATION_STATUS_CHANGED), payload.capture());
        assertThat(payload.getValue())
                .contains("\"opportunityTitle\":\"your application\"")
                .contains("\"status\":\"IN_REVIEW\"")
                .contains("APP-77");
    }

    @Test
    void shouldNotifyEachActiveRecipientOnce_whenSubmitted() {
        when(accessService.listForApplicant(5L)).thenReturn(List.of(
                grant(9L, AccessGrantStatus.ACTIVE), grant(9L, AccessGrantStatus.ACTIVE), grant(4L, AccessGrantStatus.ACTIVE)));
        when(programService.get(11L)).thenThrow(new IllegalStateException("skip title"));

        emitter.submitted(application());

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(outboxWriter).write(eq("application"), eq(77L), eq(OutboxEventTypes.APPLICATION_SUBMITTED), payload.capture());
        assertThat(payload.getValue()).contains("\"recipientUserIds\":[9,4]").doesNotContain("\"status\"");
    }
}
