package com.nadoumi.applicant.rules;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nadoumi.applicant.domain.enums.ChinaVisaType;
import com.nadoumi.applicant.domain.enums.EducationLevel;
import com.nadoumi.common.exception.NadBadRequestException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ResidenceRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 19);

    @Test
    void shouldAcceptAStudentOutsideChinaWithNoChinaDetails() {
        assertThatCode(() -> ResidenceRules.validate(false, "EG", null, null, null, TODAY)).doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptAStudentInChinaWithLevelVisaAndFutureExpiry() {
        assertThatCode(() -> ResidenceRules.validate(true, "CN", EducationLevel.BACHELOR, ChinaVisaType.X1, TODAY.plusMonths(8), TODAY))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRequireTheChinaDetails() {
        assertThatThrownBy(() -> ResidenceRules.validate(true, "CN", null, ChinaVisaType.X1, TODAY.plusDays(30), TODAY))
                .isInstanceOf(NadBadRequestException.class);
        assertThatThrownBy(() -> ResidenceRules.validate(true, "CN", EducationLevel.BACHELOR, null, TODAY.plusDays(30), TODAY))
                .isInstanceOf(NadBadRequestException.class);
        assertThatThrownBy(() -> ResidenceRules.validate(true, "CN", EducationLevel.BACHELOR, ChinaVisaType.X1, null, TODAY))
                .isInstanceOf(NadBadRequestException.class);
    }

    @Test
    void shouldRejectAnExpiredVisa() {
        assertThatThrownBy(() -> ResidenceRules.validate(true, "CN", EducationLevel.BACHELOR, ChinaVisaType.X1, TODAY, TODAY))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void shouldKeepTheInChinaFlagAndTheCountryConsistent() {
        assertThatThrownBy(() -> ResidenceRules.validate(true, "EG", EducationLevel.BACHELOR, ChinaVisaType.X1, TODAY.plusDays(30), TODAY))
                .isInstanceOf(NadBadRequestException.class);
        assertThatThrownBy(() -> ResidenceRules.validate(false, "CN", null, null, null, TODAY))
                .isInstanceOf(NadBadRequestException.class);
    }
}
