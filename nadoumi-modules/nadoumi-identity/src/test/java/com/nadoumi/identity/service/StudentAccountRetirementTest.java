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
import com.nadoumi.common.erasure.StudentRetirementParticipant;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.junit.jupiter.api.Test;

class StudentAccountRetirementTest {

    private final NadIdentityMapper identity = mock(NadIdentityMapper.class);
    private final UserApplicantAccessMapper access = mock(UserApplicantAccessMapper.class);
    private final SessionRevoker sessions = mock(SessionRevoker.class);
    private final StudentRetirementParticipant participant = mock(StudentRetirementParticipant.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<StudentRetirementParticipant> participants = mock(ObjectProvider.class);

    private final StudentAccountRetirement retirement = retirement();

    private StudentAccountRetirement retirement() {
        when(participants.orderedStream()).thenAnswer(i -> Stream.of(participant));
        return new StudentAccountRetirement(identity, access, sessions, participants);
    }

    @Test
    void shouldRetireAStudentWithNoApplicantLeft_andEndTheirSessions() {
        when(access.countForUser(50L)).thenReturn(0);
        when(identity.selectStudentEmail(50L)).thenReturn("ada@example.com");
        when(identity.removeStudentRow(50L)).thenReturn(1);

        retirement.retireOrphans(List.of(50L), 3L);

        verify(participant).retire(50L);
        verify(sessions).revokeAll(50L, null);
        verify(identity).removeStudentRow(50L);
        verify(identity, never()).softDeleteStudent(anyLong(), anyString());
    }

    @Test
    void shouldKeepAnAnonymisedShell_whenAForeignKeyStillPointsAtTheAccount() {
        when(access.countForUser(50L)).thenReturn(0);
        when(identity.selectStudentEmail(50L)).thenReturn("ada@example.com");
        when(identity.removeStudentRow(50L)).thenThrow(new org.springframework.dao.DataIntegrityViolationException("fk"));

        retirement.retireOrphans(List.of(50L), 3L);

        verify(identity).softDeleteStudent(50L, "3");
        verify(sessions).revokeAll(50L, null);
    }

    @Test
    void shouldKeepAUserWhoStillHasAnotherApplicant() {
        when(access.countForUser(50L)).thenReturn(1);

        retirement.retireOrphans(List.of(50L), 3L);

        verify(identity, never()).softDeleteStudent(anyLong(), anyString());
        verify(sessions, never()).revokeAll(anyLong(), any());
    }

    @Test
    void shouldNotTouchSessions_whenTheUserIsNotAStudent() {
        when(access.countForUser(9L)).thenReturn(0);
        when(identity.selectStudentEmail(9L)).thenReturn(null); // staff and closed accounts have no live student email

        retirement.retireOrphans(List.of(9L), 3L);

        verify(participant, never()).retire(anyLong());
        verify(identity, never()).softDeleteStudent(anyLong(), anyString());
        verify(sessions, never()).revokeAll(anyLong(), any());
    }
}
