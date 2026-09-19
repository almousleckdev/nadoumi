package com.nadoumi.applicant.rules;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nadoumi.applicant.domain.enums.ChinaVisaType;
import com.nadoumi.common.exception.NadBadRequestException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class WorkRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 19);
    private static final LocalDate START = LocalDate.of(2023, 1, 1);

    @Test
    void shouldAcceptAFinishedJobAndACurrentJob() {
        assertThatCode(() -> WorkRules.validate(START, LocalDate.of(2024, 1, 1), false, "EG", null, TODAY)).doesNotThrowAnyException();
        assertThatCode(() -> WorkRules.validate(START, null, true, "EG", null, TODAY)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectBadDates() {
        assertThatThrownBy(() -> WorkRules.validate(TODAY.plusDays(1), null, true, "EG", null, TODAY))
                .isInstanceOf(NadBadRequestException.class);
        assertThatThrownBy(() -> WorkRules.validate(START, LocalDate.of(2022, 1, 1), false, "EG", null, TODAY))
                .isInstanceOf(NadBadRequestException.class);
        assertThatThrownBy(() -> WorkRules.validate(START, TODAY.plusDays(1), false, "EG", null, TODAY))
                .isInstanceOf(NadBadRequestException.class);
    }

    @Test
    void shouldRequireAnEndDateUnlessCurrentAndForbidOneWhenCurrent() {
        assertThatThrownBy(() -> WorkRules.validate(START, null, false, "EG", null, TODAY))
                .isInstanceOf(NadBadRequestException.class);
        assertThatThrownBy(() -> WorkRules.validate(START, LocalDate.of(2024, 1, 1), true, "EG", null, TODAY))
                .isInstanceOf(NadBadRequestException.class);
    }

    @Test
    void shouldRequireTheWorkVisaTypeForWorkInChina() {
        assertThatThrownBy(() -> WorkRules.validate(START, null, true, "CN", null, TODAY))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("visa");
        assertThatCode(() -> WorkRules.validate(START, null, true, "CN", ChinaVisaType.Z, TODAY)).doesNotThrowAnyException();
    }
}
