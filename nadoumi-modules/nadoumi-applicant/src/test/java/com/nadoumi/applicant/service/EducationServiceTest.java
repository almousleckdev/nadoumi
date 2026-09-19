package com.nadoumi.applicant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.applicant.domain.ApplicantEducation;
import com.nadoumi.applicant.domain.enums.EducationLevel;
import com.nadoumi.applicant.mapper.ApplicantMapper;
import com.nadoumi.applicant.web.request.EducationRequest;
import com.nadoumi.common.access.ApplicantCapability;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadNotFoundException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

class EducationServiceTest {

    private static final long ID = 5L;

    private final ApplicantMapper mapper = mock(ApplicantMapper.class);
    private final ApplicantAccessGuard guard = mock(ApplicantAccessGuard.class);
    private final EducationService service = new EducationService(mapper, guard);

    private static EducationRequest request(LocalDate start, LocalDate end, boolean current) {
        return new EducationRequest("  Cairo High School ", "eg", "Cairo", EducationLevel.HIGH_SCHOOL,
                null, "Science", null, null, start, end, current);
    }

    @Test
    void shouldStoreTheRecordWithANormalisedCountry() {
        service.add(ID, request(LocalDate.of(2018, 9, 1), LocalDate.of(2021, 6, 30), false));

        ArgumentCaptor<ApplicantEducation> stored = ArgumentCaptor.forClass(ApplicantEducation.class);
        verify(mapper).insertEducation(stored.capture());
        assertThat(stored.getValue().getInstitution()).isEqualTo("Cairo High School");
        assertThat(stored.getValue().getCountry()).isEqualTo("EG");
        assertThat(stored.getValue().getApplicantId()).isEqualTo(ID);
        assertThat(stored.getValue().isCurrent()).isFalse();
    }

    @Test
    void shouldAcceptAnOpenEndedCurrentRecord() {
        service.add(ID, request(LocalDate.of(2024, 9, 1), null, true));

        verify(mapper).insertEducation(any());
    }

    @Test
    void shouldRejectBadDatesAndStoreNothing() {
        assertThatThrownBy(() -> service.add(ID, request(LocalDate.of(2022, 1, 1), LocalDate.of(2021, 1, 1), false)))
                .isInstanceOf(NadBadRequestException.class);
        verify(mapper, never()).insertEducation(any());
    }

    @Test
    void shouldRequireTheEditCapabilityToWrite_andViewToRead() {
        doThrow(new AccessDeniedException("no")).when(guard).require(ID, ApplicantCapability.EDIT_PROFILE);
        doThrow(new AccessDeniedException("no")).when(guard).require(ID, ApplicantCapability.VIEW_PROFILE);

        assertThatThrownBy(() -> service.add(ID, request(null, null, true))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.list(ID)).isInstanceOf(AccessDeniedException.class);
        verify(mapper, never()).insertEducation(any());
    }

    @Test
    void shouldNotUpdateAnotherApplicantsRecord() {
        ApplicantEducation other = new ApplicantEducation();
        other.setId(9L);
        other.setApplicantId(99L);
        when(mapper.findEducationById(9L)).thenReturn(other);

        assertThatThrownBy(() -> service.update(ID, 9L, request(null, null, true))).isInstanceOf(NadNotFoundException.class);
        verify(mapper, never()).updateEducation(any());
    }

    @Test
    void shouldReportNotFoundWhenDeletingSomethingThatIsNotTheirs() {
        when(mapper.deleteEducation(9L, ID)).thenReturn(0);

        assertThatThrownBy(() -> service.delete(ID, 9L)).isInstanceOf(NadNotFoundException.class);
    }
}
