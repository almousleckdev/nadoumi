package com.nadoumi.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.student.StudentRemovalParticipant.Subject;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApplicationStudentRemovalGuardTest {

    private final ApplicationMapper applications = mock(ApplicationMapper.class);
    private final ApplicationStudentRemovalGuard guard = new ApplicationStudentRemovalGuard(applications);

    @Test
    void shouldRefuse_whenTheStudentHasApplications() {
        when(applications.countByApplicantIds(List.of(7L))).thenReturn(2);

        assertThatThrownBy(() -> guard.beforeRemove(new Subject(50L, List.of(7L), List.of(7L))))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("2 applications");
    }

    @Test
    void shouldAllow_whenTheStudentHasNoApplications() {
        when(applications.countByApplicantIds(List.of(7L))).thenReturn(0);

        assertThatCode(() -> guard.beforeRemove(new Subject(50L, List.of(7L), List.of(7L)))).doesNotThrowAnyException();
    }

    @Test
    void shouldNotQuery_whenTheStudentHasNoApplicant() {
        guard.beforeRemove(new Subject(50L, List.of(), List.of()));

        verify(applications, never()).countByApplicantIds(anyList());
    }
}
