package com.nadoumi.program.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.program.domain.enums.ProgramType;
import com.nadoumi.program.web.request.ProgramRequest.OfferingInput;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProgramOfferingRulesTest {

    private static OfferingInput offering(String level, int months, String tuition) {
        return new OfferingInput(level, months, tuition == null ? null : new BigDecimal(tuition), "CNY", null,
                null, null, null, null);
    }

    @Test
    void shouldAcceptDegreeDurationsAtOrAboveTheLevelMinimum() {
        List<OfferingInput> offerings = List.of(
                offering("BACHELOR", 48, "20000"), offering("BACHELOR", 60, "20000"),
                offering("MASTER", 24, "25000"), offering("PHD", 36, "30000"));

        ProgramOfferingRules.validate(ProgramType.DEGREE, List.of("BACHELOR", "MASTER", "PHD"), offerings);
    }

    @Test
    void shouldRejectABachelorShorterThanFourYears() {
        assertThatThrownBy(() -> ProgramOfferingRules.validate(ProgramType.DEGREE, List.of("BACHELOR"),
                List.of(offering("BACHELOR", 36, null))))
                .isInstanceOf(NadBadRequestException.class).hasMessageContaining("at least 48");
    }

    @Test
    void shouldRejectAMasterShorterThanTwoYearsAndAPhdShorterThanThree() {
        assertThatThrownBy(() -> ProgramOfferingRules.validate(ProgramType.DEGREE, List.of("MASTER"),
                List.of(offering("MASTER", 12, null)))).isInstanceOf(NadBadRequestException.class);
        assertThatThrownBy(() -> ProgramOfferingRules.validate(ProgramType.DEGREE, List.of("PHD"),
                List.of(offering("PHD", 24, null)))).isInstanceOf(NadBadRequestException.class);
    }

    @Test
    void shouldRejectAnOfferingForALevelTheProgrammeDoesNotHave() {
        assertThatThrownBy(() -> ProgramOfferingRules.validate(ProgramType.DEGREE, List.of("MASTER"),
                List.of(offering("PHD", 36, null))))
                .isInstanceOf(NadBadRequestException.class).hasMessageContaining("PHD");
    }

    @Test
    void shouldRequireALevel_forDegreeOfferings() {
        assertThatThrownBy(() -> ProgramOfferingRules.validate(ProgramType.DEGREE, List.of("MASTER"),
                List.of(offering(null, 24, null)))).isInstanceOf(NadBadRequestException.class);
    }

    @Test
    void shouldAllowSixAndTwelveMonthOptions_forALanguageProgramme() {
        ProgramOfferingRules.validate(ProgramType.LANGUAGE, List.of(),
                List.of(offering(null, 6, "9000"), offering(null, 12, "16000")));
    }

    @Test
    void shouldRejectALevel_onALanguageProgramme() {
        assertThatThrownBy(() -> ProgramOfferingRules.validate(ProgramType.LANGUAGE, List.of(),
                List.of(offering("MASTER", 12, null)))).isInstanceOf(NadBadRequestException.class);
    }

    @Test
    void shouldRejectTheSameLevelAndDurationTwice() {
        assertThatThrownBy(() -> ProgramOfferingRules.validate(ProgramType.DEGREE, List.of("MASTER"),
                List.of(offering("MASTER", 24, "1"), offering("MASTER", 24, "2"))))
                .isInstanceOf(NadBadRequestException.class).hasMessageContaining("duplicate");
    }

    @Test
    void shouldRejectAnUnknownBillingPeriod() {
        var bad = new OfferingInput("MASTER", 24, new BigDecimal("1"), "CNY", "PER_DECADE", null, null, null, null);
        assertThatThrownBy(() -> ProgramOfferingRules.validate(ProgramType.DEGREE, List.of("MASTER"), List.of(bad)))
                .isInstanceOf(NadBadRequestException.class).hasMessageContaining("PER_DECADE");
    }

    @Test
    void shouldSummariseAsTheShortestDurationAndTheCheapestTuition() {
        var summary = ProgramOfferingRules.summary(List.of(
                offering("MASTER", 36, "30000"), offering("MASTER", 24, "26000")));

        assertThat(summary.durationMonths()).isEqualTo(24);
        assertThat(summary.tuitionAmount()).isEqualByComparingTo("26000");
        assertThat(summary.tuitionCurrency()).isEqualTo("CNY");
    }

    @Test
    void shouldSummariseAnEmptyListAsNothing() {
        var summary = ProgramOfferingRules.summary(List.of());

        assertThat(summary.durationMonths()).isNull();
        assertThat(summary.tuitionAmount()).isNull();
    }
}
