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
import java.util.List;
import org.junit.jupiter.api.Test;

class StudentAccountRetirementTest {

    private final NadIdentityMapper identity = mock(NadIdentityMapper.class);
    private final UserApplicantAccessMapper access = mock(UserApplicantAccessMapper.class);
    private final SessionRevoker sessions = mock(SessionRevoker.class);
    private final StudentAccountRetirement retirement = new StudentAccountRetirement(identity, access, sessions);

    @Test
    void shouldRetireAStudentWithNoApplicantLeft_andEndTheirSessions() {
        when(access.countForUser(50L)).thenReturn(0);
        when(identity.softDeleteStudent(50L, "3")).thenReturn(1);

        retirement.retireOrphans(List.of(50L), 3L);

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
        when(identity.softDeleteStudent(9L, "3")).thenReturn(0); // the update only matches user_type 10

        retirement.retireOrphans(List.of(9L), 3L);

        verify(sessions, never()).revokeAll(anyLong(), any());
    }
}
