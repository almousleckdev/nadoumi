package com.nadoumi.scholarship.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.scholarship.domain.Scholarship;
import com.nadoumi.scholarship.domain.ScholarshipCategory;
import com.nadoumi.scholarship.domain.enums.FundingModel;
import com.nadoumi.scholarship.mapper.ScholarshipMapper;
import com.nadoumi.scholarship.mapper.ScholarshipSearch;
import com.nadoumi.university.service.UniversityService;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScholarshipAdminServiceTest {

    private final ScholarshipMapper mapper = mock(ScholarshipMapper.class);
    private final ScholarshipAdminService service = new ScholarshipAdminService(
            mapper, mock(UniversityService.class), mock(MediaGateway.class),
            mock(OutboxWriter.class), mock(ScholarshipChildrenWriter.class));

    private static Scholarship row(long id) {
        Scholarship s = new Scholarship();
        s.setId(id);
        s.setTitle("CSC " + id);
        s.setCountry("CN");
        s.setFundingModel(FundingModel.FULLY);
        return s;
    }

    @Test
    void shouldReturnLevelsAndCategoriesOnEachListRow_whenStaffListsScholarships() {
        var filter = new ScholarshipSearch(null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), null, null, null, null, null, null);
        when(mapper.searchStaff(any())).thenReturn(List.of(row(1)));
        when(mapper.findLevels(1L)).thenReturn(List.of("MASTER", "PHD"));
        when(mapper.findCategories(1L)).thenReturn(List.of(new ScholarshipCategory(5L, "CSC", "CSC")));

        var page = service.list(filter, 0, 20);

        var view = page.content().get(0).view();
        assertThat(view.levels()).containsExactly("MASTER", "PHD");
        assertThat(view.categories()).containsExactly("CSC");
    }
}
