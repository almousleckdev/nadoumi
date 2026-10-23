package com.nadoumi.program.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.program.domain.Major;
import com.nadoumi.program.mapper.MajorMapper;
import com.nadoumi.program.web.request.MajorBulkRequest;
import com.nadoumi.program.web.request.MajorRequest;
import org.junit.jupiter.api.Test;

class MajorServiceTest {

    private final MajorMapper mapper = mock(MajorMapper.class);
    private final MajorService service = new MajorService(mapper);

    private static Major major(long id, String name) {
        Major m = new Major();
        m.setId(id);
        m.setName(name);
        return m;
    }

    @Test
    void shouldParseAPastedLine_withOrWithoutAChineseName() {
        assertThat(MajorService.parseLine("Computer Science")).containsExactly("Computer Science", null);
        assertThat(MajorService.parseLine("1. Computer Science | 计算机科学")).containsExactly("Computer Science", "计算机科学");
        assertThat(MajorService.parseLine("- Clinical Medicine 临床医学")).containsExactly("Clinical Medicine", "临床医学");
        assertThat(MajorService.parseLine("   ")).isNull();
    }

    @Test
    void shouldTreatSpacingDifferencesAsTheSameMajor() {
        assertThat(MajorService.normalise("  Computer   Science ")).isEqualTo("Computer Science");
        assertThat(MajorService.parseLine("Computer   Science | 计算机科学")[0]).isEqualTo("Computer Science");
    }

    @Test
    void shouldRejectADuplicateName_onCreate() {
        when(mapper.findByName("Finance")).thenReturn(major(1, "Finance"));

        assertThatThrownBy(() -> service.create(new MajorRequest(" Finance ", null, null)))
                .isInstanceOf(NadBadRequestException.class).hasMessageContaining("already exists");
        verify(mapper, never()).insert(any());
    }

    @Test
    void shouldCreateTheMissingMajors_andReuseTheExistingOnes_whenAListIsPasted() {
        when(mapper.findByName("Finance")).thenReturn(major(1, "Finance"));

        var result = service.bulk(new MajorBulkRequest("Finance\nMarketing\n\nmarketing\n", "business"));

        assertThat(result.majors()).hasSize(2); // the repeated line is skipped
        assertThat(result.created()).isEqualTo(1);
        verify(mapper, times(1)).insert(any(Major.class));
    }

    @Test
    void shouldRefuseToDeleteAMajorThatProgrammesUse() {
        when(mapper.findById(5L)).thenReturn(major(5, "Law"));
        when(mapper.countLinks(5L)).thenReturn(3);

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(NadBadRequestException.class).hasMessageContaining("merge");
        verify(mapper, never()).delete(5L);
    }

    @Test
    void shouldMoveTheLinksThenDeleteTheDuplicate_onMerge() {
        when(mapper.findById(5L)).thenReturn(major(5, "Computer science"));
        when(mapper.findById(6L)).thenReturn(major(6, "Computer Science"));

        var target = service.merge(5L, 6L);

        assertThat(target.id()).isEqualTo(6L);
        verify(mapper).moveLinks(5L, 6L);
        verify(mapper).delete(5L);
        assertThatThrownBy(() -> service.merge(6L, 6L)).isInstanceOf(NadBadRequestException.class);
    }
}
