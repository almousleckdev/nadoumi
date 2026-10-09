package com.nadoumi.identity.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.identity.access.SessionRevoker;
import com.nadoumi.identity.mapper.NadIdentityMapper;
import com.nadoumi.identity.mapper.UserApplicantAccessMapper;
import com.nadoumi.common.erasure.AccountRetirementParticipant;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.junit.jupiter.api.Test;

class AccountRetirementTest {

    private final NadIdentityMapper identity = mock(NadIdentityMapper.class);
    private final UserApplicantAccessMapper access = mock(UserApplicantAccessMapper.class);
    private final SessionRevoker sessions = mock(SessionRevoker.class);
    private final AccountRetirementParticipant participant = mock(AccountRetirementParticipant.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<AccountRetirementParticipant> participants = mock(ObjectProvider.class);

    private final AccountRetirement retirement = retirement();

    private AccountRetirement retirement() {
        when(participants.orderedStream()).thenAnswer(i -> Stream.of(participant));
        return new AccountRetirement(identity, access, sessions, participants);
    }

    @Test
    void shouldRetireAStudentWithNoApplicantLeft_andEndTheirSessions() {
        when(access.countForUser(50L)).thenReturn(0);
        when(identity.selectStudentEmail(50L)).thenReturn("ada@example.com");
        when(identity.removeAccountRow(50L, "10")).thenReturn(1);

        retirement.retireOrphans(List.of(50L), 3L);

        verify(participant).retire(50L);
        verify(sessions).revokeAll(50L, null);
        verify(identity).removeAccountRow(50L, "10");
        verify(identity, never()).anonymiseAccount(anyLong(), anyString(), anyString());
    }

    @Test
    void shouldKeepAnAnonymisedShell_whenAForeignKeyStillPointsAtTheAccount() {
        when(access.countForUser(50L)).thenReturn(0);
        when(identity.selectStudentEmail(50L)).thenReturn("ada@example.com");
        when(identity.removeAccountRow(50L, "10")).thenThrow(new org.springframework.dao.DataIntegrityViolationException("fk"));

        retirement.retireOrphans(List.of(50L), 3L);

        verify(identity).anonymiseAccount(50L, "10", "3");
        verify(sessions).revokeAll(50L, null);
    }

    @Test
    void shouldKeepAUserWhoStillHasAnotherApplicant() {
        when(access.countForUser(50L)).thenReturn(1);

        retirement.retireOrphans(List.of(50L), 3L);

        verify(identity, never()).anonymiseAccount(anyLong(), anyString(), anyString());
        verify(sessions, never()).revokeAll(anyLong(), any());
    }

    @Test
    void shouldNotTouchSessions_whenTheUserIsNotAStudent() {
        when(access.countForUser(9L)).thenReturn(0);
        when(identity.selectStudentEmail(9L)).thenReturn(null); // staff and closed accounts have no live student email

        retirement.retireOrphans(List.of(9L), 3L);

        verify(participant, never()).retire(anyLong());
        verify(identity, never()).anonymiseAccount(anyLong(), anyString(), anyString());
        verify(sessions, never()).revokeAll(anyLong(), any());
    }

    @Test
    void shouldDeleteAStaffAccountCompletely_afterRemovingWhatItOwns() {
        when(identity.selectUserType(8L)).thenReturn("00");
        when(identity.removeAccountRow(8L, "00")).thenReturn(1);

        retirement.retireStaff(8L, 3L);

        verify(participant).retire(8L);
        verify(sessions).revokeAll(8L, null);
        verify(identity).removeAccountRow(8L, "00");
        verify(identity, never()).anonymiseAccount(anyLong(), anyString(), anyString());
    }

    @Test
    void shouldRefuseToRetireAStudentThroughTheStaffPath() {
        when(identity.selectUserType(50L)).thenReturn("10");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> retirement.retireStaff(50L, 3L))
                .isInstanceOf(com.nadoumi.common.exception.NadBadRequestException.class);
        verify(identity, never()).removeAccountRow(anyLong(), anyString());
    }

    @Test
    void shouldKeepAnAnonymisedShellForStaff_whenPayrollOrAnotherRecordStillReferencesThem() {
        when(identity.selectUserType(8L)).thenReturn("00");
        when(identity.removeAccountRow(8L, "00")).thenThrow(new org.springframework.dao.DataIntegrityViolationException("fk"));

        retirement.retireStaff(8L, 3L);

        verify(identity).anonymiseAccount(8L, "00", "3");
    }
}
