package com.nadoumi.applicant.rules;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nadoumi.common.exception.NadBadRequestException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AgeRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 19);

    @Test
    void shouldAccept_whenExactlySeventeenToday() {
        assertThatCode(() -> AgeRules.requireAdultEnough(LocalDate.of(2009, 9, 19), TODAY)).doesNotThrowAnyException();
    }

    @Test
    void shouldReject_whenSeventeenTomorrow() {
        assertThatThrownBy(() -> AgeRules.requireAdultEnough(LocalDate.of(2009, 9, 20), TODAY))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("17");
    }

    @Test
    void shouldReject_whenDateIsInTheFuture() {
        assertThatThrownBy(() -> AgeRules.requireAdultEnough(TODAY.plusDays(1), TODAY))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("future");
    }

    @Test
    void shouldReject_whenTooYoung() {
        assertThatThrownBy(() -> AgeRules.requireAdultEnough(LocalDate.of(2015, 1, 1), TODAY))
                .isInstanceOf(NadBadRequestException.class);
    }

    @Test
    void shouldAccept_whenNullBecauseTheFieldIsOptionalUntilOnboardingCompletes() {
        assertThatCode(() -> AgeRules.requireAdultEnough(null, TODAY)).doesNotThrowAnyException();
    }
}
