package com.nadoumi.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nadoumi.application.domain.Application;
import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.application.service.ApplicationOpportunityService.Description;
import com.nadoumi.application.service.ApplicationOpportunityService.Kind;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.program.service.ProgramService;
import com.nadoumi.program.web.response.ProgramResponse;
import com.nadoumi.scholarship.service.ScholarshipAdminService;
import com.nadoumi.scholarship.web.response.PublicScholarshipResponse;
import com.nadoumi.scholarship.web.response.ScholarshipResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The confidentiality rule for documents a student sees: the university is named only on a plain programme application. */
class ApplicationOpportunityServiceTest {

    private static final long APPLICATION = 42L;

    private final ApplicationMapper applications = mock(ApplicationMapper.class);
    private final ProgramService programs = mock(ProgramService.class);
    private final ScholarshipAdminService scholarships = mock(ScholarshipAdminService.class);
    private final ApplicationOpportunityService service =
            new ApplicationOpportunityService(applications, programs, scholarships);

    @BeforeEach
    void stubCatalog() {
        ProgramResponse program = mock(ProgramResponse.class);
        when(program.name()).thenReturn("MBBS");
        when(program.universityName()).thenReturn("Fudan University");
        ProgramResponse.Intake intake = new ProgramResponse.Intake(9L, "AUTUMN_SEPTEMBER", null, null);
        when(program.intakes()).thenReturn(List.of(intake));
        when(programs.get(1L)).thenReturn(program);

        PublicScholarshipResponse view = mock(PublicScholarshipResponse.class);
        when(view.title()).thenReturn("CSC Master Scholarship");
        ScholarshipResponse scholarship = mock(ScholarshipResponse.class);
        when(scholarship.view()).thenReturn(view);
        when(scholarships.get(2L)).thenReturn(scholarship);
    }

    private void application(Long programId, Long scholarshipId) {
        Application a = new Application();
        a.setId(APPLICATION);
        a.setProgramId(programId);
        a.setScholarshipId(scholarshipId);
        a.setIntakeId(9L);
        when(applications.findById(APPLICATION)).thenReturn(a);
    }

    @Test
    void shouldNameTheUniversity_whenItIsAPlainProgrammeApplication() {
        application(1L, null);

        Description d = service.describe(APPLICATION);

        assertThat(d.kind()).isEqualTo(Kind.PROGRAM);
        assertThat(d.title()).isEqualTo("MBBS");
        assertThat(d.universityName()).isEqualTo("Fudan University");
        assertThat(d.intake()).isEqualTo("AUTUMN_SEPTEMBER");
    }

    @Test
    void shouldHideTheUniversity_whenTheApplicationIsForAScholarshipOnly() {
        application(null, 2L);

        Description d = service.describe(APPLICATION);

        assertThat(d.kind()).isEqualTo(Kind.SCHOLARSHIP);
        assertThat(d.title()).isEqualTo("CSC Master Scholarship");
        assertThat(d.universityName()).isNull();
    }

    @Test
    void shouldHideTheUniversity_whenAProgrammeApplicationAlsoInvolvesAScholarship() {
        application(1L, 2L);

        Description d = service.describe(APPLICATION);

        assertThat(d.title()).isEqualTo("MBBS + CSC Master Scholarship");
        assertThat(d.universityName()).isNull(); // the stricter rule: the partner link must not be inferable
    }

    @Test
    void shouldFail_whenTheApplicationDoesNotExist() {
        assertThatThrownBy(() -> service.describe(999L)).isInstanceOf(NadNotFoundException.class);
    }
}
