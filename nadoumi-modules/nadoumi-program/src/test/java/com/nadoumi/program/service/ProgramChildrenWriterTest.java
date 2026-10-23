package com.nadoumi.program.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.program.domain.Program;
import com.nadoumi.program.domain.ProgramIntake;
import com.nadoumi.program.domain.ProgramMajor;
import com.nadoumi.program.domain.enums.ProgramType;
import com.nadoumi.program.mapper.ProgramMapper;
import com.nadoumi.program.web.request.ProgramRequest;
import com.nadoumi.university.service.DepartmentService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProgramChildrenWriterTest {

    private static final long PROGRAM_ID = 4L;
    private static final long UNIVERSITY_ID = 9L;

    private final ProgramMapper mapper = mock(ProgramMapper.class);
    private final DepartmentService departmentService = mock(DepartmentService.class);
    private final MajorService majors = mock(MajorService.class);
    private final ProgramChildrenWriter writer = new ProgramChildrenWriter(mapper, departmentService, majors);

    private static Program program(ProgramType type) {
        Program p = new Program();
        p.setId(PROGRAM_ID);
        p.setUniversityId(UNIVERSITY_ID);
        p.setProgramType(type);
        return p;
    }

    private static ProgramRequest request(List<String> levels, List<ProgramRequest.MajorInput> majors,
            List<ProgramRequest.IntakeInput> intakes) {
        return new ProgramRequest(UNIVERSITY_ID, "CS", null, ProgramType.DEGREE, levels,
                null, null, null,                       // field, termLength, teachingLanguage
                null, null, null, null, null,           // summary, admission, documents, career, scholarships
                null, null, null, null, null,           // minHsk, minIelts, minToefl, application fee + currency
                null,                                   // imageMediaId
                false, false, null, null, null,         // featured, hot, status, publishStatus, remark
                majors, intakes, null);
    }

    private static ProgramRequest.MajorInput major(String name, Long departmentId, String level) {
        return new ProgramRequest.MajorInput(name, null, departmentId, level);
    }

    @BeforeEach
    void stubCatalogue() {
        when(majors.findOrCreate(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(call -> catalogueMajor(1L, call.getArgument(0), call.getArgument(1)));
    }

    static com.nadoumi.program.domain.Major catalogueMajor(long id, String name, String nameCn) {
        com.nadoumi.program.domain.Major m = new com.nadoumi.program.domain.Major();
        m.setId(id);
        m.setName(name);
        m.setNameCn(nameCn);
        return m;
    }

    @Test
    void shouldRequireAtLeastOneLevel_forADegreeProgramme() {
        assertThatThrownBy(() -> writer.replace(program(ProgramType.DEGREE), request(List.of(), null, null)))
                .isInstanceOf(NadBadRequestException.class).hasMessageContaining("at least one level");
        assertThatThrownBy(() -> writer.replace(program(ProgramType.DEGREE), request(null, null, null)))
                .isInstanceOf(NadBadRequestException.class);
    }

    @Test
    void shouldRejectAnUnknownLevel() {
        assertThatThrownBy(() -> writer.replace(program(ProgramType.DEGREE), request(List.of("DOCTORATE"), null, null)))
                .isInstanceOf(NadBadRequestException.class).hasMessageContaining("unknown programme level");
    }

    @Test
    void shouldStoreLevelsDistinctAndInAcademicOrder() {
        Program program = program(ProgramType.DEGREE);

        writer.replace(program, request(List.of("phd", "BACHELOR", " master ", "BACHELOR"), null, null));

        verify(mapper).insertLevel(PROGRAM_ID, "BACHELOR", 0);
        verify(mapper).insertLevel(PROGRAM_ID, "MASTER", 1);
        verify(mapper).insertLevel(PROGRAM_ID, "PHD", 2);
        assertThat(program.getLevels()).containsExactly("BACHELOR", "MASTER", "PHD");
    }

    @Test
    void shouldIgnoreLevelsAndMajors_forANonDegreeProgramme() {
        writer.replace(program(ProgramType.LANGUAGE),
                request(List.of("MASTER"), List.of(major("AI", null, null)), null));

        verify(mapper).deleteLevels(PROGRAM_ID);
        verify(mapper).deleteMajors(PROGRAM_ID);
        verify(mapper, never()).insertLevel(anyLong(), anyString(), anyInt());
        verify(mapper, never()).insertMajor(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldSkipBlankMajorsAndKeepTheOrderOfTheRest() {
        writer.replace(program(ProgramType.DEGREE), request(List.of("MASTER"),
                List.of(major("  ", null, null), major(" AI ", null, "master"), major("Data", null, null)), null));

        ArgumentCaptor<ProgramMajor> saved = ArgumentCaptor.forClass(ProgramMajor.class);
        verify(mapper, times(2)).insertMajor(saved.capture());
        assertThat(saved.getAllValues()).extracting(ProgramMajor::getName).containsExactly("AI", "Data");
        assertThat(saved.getAllValues()).extracting(ProgramMajor::getSortOrder).containsExactly(0, 1);
        assertThat(saved.getAllValues().get(0).getLevel()).isEqualTo("MASTER");
    }

    @Test
    void shouldLinkAPickedCatalogueMajor_andUseItsTitle() {
        when(majors.require(7L)).thenReturn(catalogueMajor(7L, "Computer Science", "计算机科学"));
        var captor = org.mockito.ArgumentCaptor.forClass(com.nadoumi.program.domain.ProgramMajor.class);

        writer.replace(program(ProgramType.DEGREE), request(List.of("BACHELOR"),
                List.of(new ProgramRequest.MajorInput(null, null, null, "BACHELOR", 7L)), null));

        verify(mapper).insertMajor(captor.capture());
        assertThat(captor.getValue().getMajorId()).isEqualTo(7L);
        assertThat(captor.getValue().getName()).isEqualTo("Computer Science");
        assertThat(captor.getValue().getNameCn()).isEqualTo("计算机科学");
        verify(majors, never()).findOrCreate(anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldKeepTheProgrammesOwnTitle_whenItOverridesTheCatalogueOne() {
        when(majors.require(7L)).thenReturn(catalogueMajor(7L, "Computer Science", "计算机科学"));
        var captor = org.mockito.ArgumentCaptor.forClass(com.nadoumi.program.domain.ProgramMajor.class);

        writer.replace(program(ProgramType.DEGREE), request(List.of("MASTER"),
                List.of(new ProgramRequest.MajorInput("Software Engineering", null, null, "MASTER", 7L)), null));

        verify(mapper).insertMajor(captor.capture());
        assertThat(captor.getValue().getMajorId()).isEqualTo(7L);
        assertThat(captor.getValue().getName()).isEqualTo("Software Engineering");
    }

    @Test
    void shouldDropARepeatedMajorAtTheSameLevel_butKeepItAtAnotherLevel() {
        when(majors.require(7L)).thenReturn(catalogueMajor(7L, "Computer Science", null));

        writer.replace(program(ProgramType.DEGREE), request(List.of("BACHELOR", "MASTER"), List.of(
                new ProgramRequest.MajorInput(null, null, null, "BACHELOR", 7L),
                new ProgramRequest.MajorInput(null, null, null, "BACHELOR", 7L),
                new ProgramRequest.MajorInput(null, null, null, "MASTER", 7L)), null));

        verify(mapper, times(2)).insertMajor(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldAddATypedMajorToTheCatalogue_soEveryRowIsLinked() {
        var captor = org.mockito.ArgumentCaptor.forClass(com.nadoumi.program.domain.ProgramMajor.class);

        writer.replace(program(ProgramType.DEGREE), request(List.of("BACHELOR"), List.of(major("Finance", null, null)), null));

        verify(majors).findOrCreate("Finance", null);
        verify(mapper).insertMajor(captor.capture());
        assertThat(captor.getValue().getMajorId()).isEqualTo(1L);
    }

    @Test
    void shouldRejectAMajorInADepartmentOfAnotherUniversity() {
        when(departmentService.belongsToUniversity(33L, UNIVERSITY_ID)).thenReturn(false);

        assertThatThrownBy(() -> writer.replace(program(ProgramType.DEGREE),
                request(List.of("MASTER"), List.of(major("AI", 33L, null)), null)))
                .isInstanceOf(NadBadRequestException.class).hasMessageContaining("department 33");
    }

    @Test
    void shouldAcceptAMajorInADepartmentOfTheSameUniversity() {
        when(departmentService.belongsToUniversity(33L, UNIVERSITY_ID)).thenReturn(true);

        writer.replace(program(ProgramType.DEGREE), request(List.of("MASTER"), List.of(major("AI", 33L, null)), null));

        verify(mapper).insertMajor(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectAMajorLevelThatIsNotOneOfTheProgrammeLevels() {
        assertThatThrownBy(() -> writer.replace(program(ProgramType.DEGREE),
                request(List.of("MASTER"), List.of(major("AI", null, "PHD")), null)))
                .isInstanceOf(NadBadRequestException.class).hasMessageContaining("major level PHD");
    }

    @Test
    void shouldSkipBlankIntakeTermsAndTrimTheRest() {
        var open = LocalDate.of(2026, 1, 1);
        var close = LocalDate.of(2026, 4, 1);
        writer.replace(program(ProgramType.DEGREE), request(List.of("MASTER"), null, List.of(
                new ProgramRequest.IntakeInput(" ", null, null),
                new ProgramRequest.IntakeInput(" AUTUMN_SEPTEMBER ", open, close))));

        ArgumentCaptor<ProgramIntake> saved = ArgumentCaptor.forClass(ProgramIntake.class);
        verify(mapper).insertIntake(saved.capture());
        assertThat(saved.getValue().getTerm()).isEqualTo("AUTUMN_SEPTEMBER");
        assertThat(saved.getValue().getApplicationClose()).isEqualTo(close);
        assertThat(saved.getValue().getSortOrder()).isZero();
    }

    @Test
    void shouldClearEveryChildCollection_evenWhenTheRequestOmitsThem() {
        writer.replace(program(ProgramType.LANGUAGE), request(null, null, null));

        verify(mapper).deleteLevels(eq(PROGRAM_ID));
        verify(mapper).deleteMajors(eq(PROGRAM_ID));
        verify(mapper).deleteIntakes(eq(PROGRAM_ID));
    }

    @Test
    void shouldStoreEachOfferingWithDefaultPeriodsAndCurrency() {
        var offering = new ProgramRequest.OfferingInput(" master ", 24, new java.math.BigDecimal("26000"), null, null,
                new java.math.BigDecimal("1200"), null, null, " shared room ");
        var req = new ProgramRequest(UNIVERSITY_ID, "CS", null, ProgramType.DEGREE, List.of("MASTER"),
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                false, false, null, null, null, null, null, List.of(offering));

        writer.replace(program(ProgramType.DEGREE), req);

        ArgumentCaptor<com.nadoumi.program.domain.ProgramOffering> saved =
                ArgumentCaptor.forClass(com.nadoumi.program.domain.ProgramOffering.class);
        verify(mapper).insertOffering(saved.capture());
        var o = saved.getValue();
        assertThat(o.getLevel()).isEqualTo("MASTER");
        assertThat(o.getTuitionCurrency()).isEqualTo("CNY");
        assertThat(o.getTuitionPeriod()).isEqualTo("PER_YEAR");
        assertThat(o.getAccommodationPeriod()).isEqualTo("PER_MONTH");
        assertThat(o.getNote()).isEqualTo("shared room");
    }

    @Test
    void shouldRejectAnOfferingBelowTheLevelMinimum_beforeWritingAnything() {
        var tooShort = new ProgramRequest.OfferingInput("BACHELOR", 36, null, null, null, null, null, null, null);
        var req = new ProgramRequest(UNIVERSITY_ID, "CS", null, ProgramType.DEGREE, List.of("BACHELOR"),
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                false, false, null, null, null, null, null, List.of(tooShort));

        assertThatThrownBy(() -> writer.replace(program(ProgramType.DEGREE), req))
                .isInstanceOf(NadBadRequestException.class);
        verify(mapper, never()).insertOffering(org.mockito.ArgumentMatchers.any());
    }
}
