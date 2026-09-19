package com.nadoumi.applicant.rules;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nadoumi.common.exception.NadBadRequestException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EducationRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 19);

    @Test
    void shouldAcceptAFinishedRecord() {
        assertThatCode(() -> EducationRules.validate(LocalDate.of(2018, 9, 1), LocalDate.of(2022, 6, 30), false, TODAY))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptACurrentRecordWithNoEndOrAFutureExpectedEnd() {
        assertThatCode(() -> EducationRules.validate(LocalDate.of(2024, 9, 1), null, true, TODAY)).doesNotThrowAnyException();
        assertThatCode(() -> EducationRules.validate(LocalDate.of(2024, 9, 1), LocalDate.of(2028, 6, 30), true, TODAY))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectAFutureStartOrAnEndBeforeTheStart() {
        assertThatThrownBy(() -> EducationRules.validate(TODAY.plusDays(1), null, true, TODAY))
                .isInstanceOf(NadBadRequestException.class);
        assertThatThrownBy(() -> EducationRules.validate(LocalDate.of(2020, 1, 1), LocalDate.of(2019, 1, 1), false, TODAY))
                .isInstanceOf(NadBadRequestException.class);
    }

    @Test
    void shouldRejectAFinishedRecordThatEndsInTheFuture() {
        assertThatThrownBy(() -> EducationRules.validate(LocalDate.of(2024, 9, 1), TODAY.plusYears(1), false, TODAY))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("finished");
    }
}
