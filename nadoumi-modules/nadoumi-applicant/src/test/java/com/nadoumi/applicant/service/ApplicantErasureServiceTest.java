package com.nadoumi.applicant.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.applicant.domain.Applicant;
import com.nadoumi.applicant.mapper.ApplicantErasureMapper;
import com.nadoumi.applicant.mapper.ApplicantMapper;
import com.nadoumi.common.erasure.ApplicantErasureParticipant;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.identity.service.AccountRetirement;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;

class ApplicantErasureServiceTest {

    private static final long APPLICANT = 7L;
    private static final long STAFF = 3L;

    private final ApplicantMapper applicants = mock(ApplicantMapper.class);
    private final ApplicantErasureMapper erasure = mock(ApplicantErasureMapper.class);
    private final AccountRetirement accounts = mock(AccountRetirement.class);
    private final ApplicantErasureParticipant participant = mock(ApplicantErasureParticipant.class);
    private final MediaGateway media = mock(MediaGateway.class);
    private final CurrentCaller caller = mock(CurrentCaller.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<ApplicantErasureParticipant> participants = mock(ObjectProvider.class);

    private ApplicantErasureService service() {
        when(participants.orderedStream()).thenAnswer(i -> Stream.of(participant));
        when(caller.isStaff()).thenReturn(true);
        when(caller.requireUserId()).thenReturn(STAFF);
        Applicant applicant = new Applicant();
        applicant.setId(APPLICANT);
        applicant.setPhotoMediaId(11L);
        applicant.setPassportMediaId(12L);
        when(applicants.findById(APPLICANT)).thenReturn(applicant);
        when(accounts.usersOf(APPLICANT)).thenReturn(List.of(50L));
        return new ApplicantErasureService(applicants, erasure, accounts, participants, media, caller);
    }

    @Test
    void shouldEraseInForeignKeyOrder_andRetireTheOrphanedLoginLast() {
        service().erase(APPLICANT);

        InOrder order = inOrder(accounts, participant, erasure, media);
        order.verify(accounts).eraseGrants(APPLICANT);
        order.verify(participant).erase(APPLICANT);
        order.verify(erasure).deleteContacts(APPLICANT);
        order.verify(erasure).deleteApplicant(APPLICANT);
        order.verify(media).softDelete(11L, STAFF);
        order.verify(accounts).retireOrphans(List.of(50L), STAFF);
        verify(media).softDelete(12L, STAFF);
    }

    @Test
    void shouldRefuse_whenTheCallerIsNotStaff() {
        ApplicantErasureService service = service();
        when(caller.isStaff()).thenReturn(false);

        assertThatThrownBy(() -> service.erase(APPLICANT)).isInstanceOf(AccessDeniedException.class);
        verify(erasure, never()).deleteApplicant(anyLong());
    }

    @Test
    void shouldReportNotFound_whenTheApplicantDoesNotExist() {
        ApplicantErasureService service = service();
        when(applicants.findById(APPLICANT)).thenReturn(null);

        assertThatThrownBy(() -> service.erase(APPLICANT)).isInstanceOf(NadNotFoundException.class);
        verify(accounts, never()).eraseGrants(anyLong());
    }
}
