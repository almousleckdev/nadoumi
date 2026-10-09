package com.nadoumi.identity.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.student.StudentRemovalParticipant;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.identity.access.SessionRevoker;
import com.nadoumi.identity.mapper.NadIdentityMapper;
import com.nadoumi.identity.mapper.UserApplicantAccessMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;

class StudentRemovalServiceTest {

    private static final long STUDENT = 50L;
    private static final long STAFF = 3L;

    private final NadIdentityMapper identity = mock(NadIdentityMapper.class);
    private final UserApplicantAccessMapper access = mock(UserApplicantAccessMapper.class);
    private final SessionRevoker sessions = mock(SessionRevoker.class);
    private final CurrentCaller caller = mock(CurrentCaller.class);
    private final StudentRemovalParticipant participant = mock(StudentRemovalParticipant.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<StudentRemovalParticipant> participants = mock(ObjectProvider.class);

    private StudentRemovalService service() {
        when(participants.orderedStream()).thenAnswer(i -> java.util.stream.Stream.of(participant));
        when(caller.isStaff()).thenReturn(true);
        when(caller.requireUserId()).thenReturn(STAFF);
        when(identity.selectUserType(STUDENT)).thenReturn("10");
        when(access.accessibleApplicantIds(STUDENT)).thenReturn(List.of(7L));
        when(access.ownedApplicantIds(STUDENT)).thenReturn(List.of(7L));
        when(identity.softDeleteStudent(eq(STUDENT), anyString())).thenReturn(1);
        return new StudentRemovalService(identity, access, sessions, caller, participants);
    }

    @Test
    void shouldDeleteAccountRevokeGrantsAndSessions_whenNoParticipantObjects() {
        service().remove(STUDENT);

        InOrder order = inOrder(participant, identity, access, sessions);
        order.verify(participant).beforeRemove(any());
        order.verify(identity).softDeleteStudent(STUDENT, "3");
        order.verify(access).revokeAllForUser(STUDENT, STAFF, "3");
        order.verify(participant).afterRemove(new StudentRemovalParticipant.Subject(STUDENT, List.of(7L), List.of(7L)));
        order.verify(sessions).revokeAll(STUDENT, null);
    }

    @Test
    void shouldLeaveTheAccountUntouched_whenAParticipantVetoes() {
        StudentRemovalService service = service();
        doThrow(new NadBadRequestException("has applications")).when(participant).beforeRemove(any());

        assertThatThrownBy(() -> service.remove(STUDENT)).isInstanceOf(NadBadRequestException.class);

        verify(identity, never()).softDeleteStudent(anyLong(), anyString());
        verify(sessions, never()).revokeAll(anyLong(), any());
    }

    @Test
    void shouldRefuse_whenTheCallerIsNotStaff() {
        StudentRemovalService service = service();
        when(caller.isStaff()).thenReturn(false);

        assertThatThrownBy(() -> service.remove(STUDENT)).isInstanceOf(AccessDeniedException.class);
        verify(identity, never()).softDeleteStudent(anyLong(), anyString());
    }

    @Test
    void shouldReportNotFound_whenTheUserIsNotAStudent() {
        StudentRemovalService service = service();
        when(identity.selectUserType(STUDENT)).thenReturn("00");

        assertThatThrownBy(() -> service.remove(STUDENT)).isInstanceOf(NadNotFoundException.class);
        verify(identity, never()).softDeleteStudent(anyLong(), anyString());
    }

    @Test
    void shouldRejectADoubleDelete_whenTheAccountIsAlreadyGone() {
        StudentRemovalService service = service();
        when(identity.softDeleteStudent(eq(STUDENT), anyString())).thenReturn(0);

        assertThatThrownBy(() -> service.remove(STUDENT)).isInstanceOf(NadBadRequestException.class);
        verify(participant, never()).afterRemove(any());
    }
}
