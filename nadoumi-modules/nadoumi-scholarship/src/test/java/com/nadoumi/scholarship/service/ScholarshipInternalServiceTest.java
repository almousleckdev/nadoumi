package com.nadoumi.scholarship.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.scholarship.domain.Scholarship;
import com.nadoumi.scholarship.domain.ScholarshipInternal;
import com.nadoumi.scholarship.mapper.ScholarshipMapper;
import com.nadoumi.scholarship.web.request.ScholarshipInternalRequest;
import com.nadoumi.university.service.UniversityService;
import com.nadoumi.university.web.response.UniversityResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ScholarshipInternalServiceTest {

    private final ScholarshipMapper mapper = mock(ScholarshipMapper.class);
    private final UniversityService universityService = mock(UniversityService.class);
    private final ScholarshipInternalService service = new ScholarshipInternalService(mapper, universityService);

    @Test
    void shouldReportNotFound_whenTheScholarshipDoesNotExist() {
        when(mapper.findById(7L)).thenReturn(null);

        assertThatThrownBy(() -> service.get(7L)).isInstanceOf(NadNotFoundException.class);
        assertThatThrownBy(() -> service.put(7L, new ScholarshipInternalRequest(null, null, null, null, null, null, null)))
                .isInstanceOf(NadNotFoundException.class);
        verify(mapper, never()).upsertInternal(any(), any(), anyString());
    }

    @Test
    void shouldReturnAnEmptyLinkage_whenNoneWasRecordedYet() {
        when(mapper.findById(7L)).thenReturn(new Scholarship());
        when(mapper.findInternal(7L)).thenReturn(null);

        var response = service.get(7L);

        assertThat(response.universityId()).isNull();
        assertThat(response.universityName()).isNull();
        verify(universityService, never()).get(any());
    }

    @Test
    void shouldResolveThePartnerUniversityName_whenALinkageExists() {
        when(mapper.findById(7L)).thenReturn(new Scholarship());
        when(mapper.findInternal(7L)).thenReturn(new ScholarshipInternal(3L, 11L, null, "NEGOTIATING", null, null, null));
        UniversityResponse university = mock(UniversityResponse.class);
        when(university.name()).thenReturn("Tsinghua University");
        when(universityService.get(3L)).thenReturn(university);

        var response = service.get(7L);

        assertThat(response.universityName()).isEqualTo("Tsinghua University");
        assertThat(response.internalStatus()).isEqualTo("NEGOTIATING");
    }

    @Test
    void shouldRejectALinkageToAnUnknownUniversity() {
        when(mapper.findById(7L)).thenReturn(new Scholarship());
        when(universityService.get(99L)).thenThrow(new NadNotFoundException("university not found"));

        assertThatThrownBy(() -> service.put(7L, new ScholarshipInternalRequest(99L, null, null, null, null, null, null)))
                .isInstanceOf(NadNotFoundException.class);
        verify(mapper, never()).upsertInternal(any(), any(), anyString());
    }

    @Test
    void shouldDefaultTheInternalStatusToDraft_whenNoneIsGiven() {
        when(mapper.findById(7L)).thenReturn(new Scholarship());

        service.put(7L, new ScholarshipInternalRequest(null, null, null, null, "note", "terms", "{}"));

        ArgumentCaptor<ScholarshipInternal> saved = ArgumentCaptor.forClass(ScholarshipInternal.class);
        verify(mapper).upsertInternal(eq(7L), saved.capture(), any());
        assertThat(saved.getValue().internalStatus()).isEqualTo("DRAFT");
        assertThat(saved.getValue().confidentialTerms()).isEqualTo("terms");
    }
}
