package com.nadoumi.university.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.identity.exception.NadBadRequestException;
import com.nadoumi.identity.exception.NadNotFoundException;
import com.nadoumi.university.domain.University;
import com.nadoumi.university.domain.enums.UniversityStatus;
import com.nadoumi.university.mapper.UniversityMapper;
import com.nadoumi.university.web.request.UniversityRequest;
import org.junit.jupiter.api.Test;

class UniversityServiceTest {

    private final UniversityMapper mapper = mock(UniversityMapper.class);
    private final UniversityService service = new UniversityService(mapper);

    private static UniversityRequest req(String name, String country) {
        return new UniversityRequest(name, country, "Beijing", "https://x.edu", "T1",
                UniversityStatus.ACTIVE, null);
    }

    @Test
    void create_trims_name_uppercases_country_and_persists() {
        when(mapper.findIdByNameAndCountry("Tsinghua University", "CN")).thenReturn(null);

        service.create(req("  Tsinghua University  ", "cn"));

        verify(mapper).insert(any(University.class));
    }

    @Test
    void create_rejects_a_duplicate_name_in_the_same_country() {
        when(mapper.findIdByNameAndCountry("Peking University", "CN")).thenReturn(99L);

        assertThatThrownBy(() -> service.create(req("Peking University", "CN")))
                .isInstanceOf(NadBadRequestException.class);
        verify(mapper, never()).insert(any());
    }

    @Test
    void update_allows_keeping_the_same_name_on_the_same_row() {
        University existing = new University();
        existing.setId(7L);
        when(mapper.findById(7L)).thenReturn(existing);
        when(mapper.findIdByNameAndCountry("Fudan University", "CN")).thenReturn(7L);

        service.update(7L, req("Fudan University", "CN"));

        verify(mapper).update(any(University.class));
    }

    @Test
    void update_rejects_taking_another_rows_name() {
        University existing = new University();
        existing.setId(7L);
        when(mapper.findById(7L)).thenReturn(existing);
        when(mapper.findIdByNameAndCountry("Zhejiang University", "CN")).thenReturn(12L);

        assertThatThrownBy(() -> service.update(7L, req("Zhejiang University", "CN")))
                .isInstanceOf(NadBadRequestException.class);
        verify(mapper, never()).update(any());
    }

    @Test
    void get_and_delete_throw_when_the_row_is_missing() {
        when(mapper.findById(1L)).thenReturn(null);
        when(mapper.delete(1L)).thenReturn(0);

        assertThatThrownBy(() -> service.get(1L)).isInstanceOf(NadNotFoundException.class);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(NadNotFoundException.class);
    }
}
