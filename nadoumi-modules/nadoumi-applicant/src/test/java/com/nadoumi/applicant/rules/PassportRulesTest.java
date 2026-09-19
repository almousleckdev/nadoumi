package com.nadoumi.applicant.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nadoumi.common.exception.NadBadRequestException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PassportRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 19);

    // ---- validity ----

    @Test
    void shouldAccept_whenExpiryIsMoreThanSixMonthsAway() {
        assertThatCode(() -> PassportRules.requireAcceptable(TODAY.minusYears(2), TODAY.plusMonths(6).plusDays(1), TODAY))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldReject_whenExpiryIsExactlySixMonthsAway() {
        assertThatThrownBy(() -> PassportRules.requireAcceptable(TODAY.minusYears(2), TODAY.plusMonths(6), TODAY))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("more than six months");
    }

    @Test
    void shouldReject_whenPassportIsExpired() {
        assertThatThrownBy(() -> PassportRules.requireAcceptable(TODAY.minusYears(11), TODAY.minusDays(1), TODAY))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("more than six months");
    }

    @Test
    void shouldReject_whenIssueDateIsInTheFuture() {
        assertThatThrownBy(() -> PassportRules.requireAcceptable(TODAY.plusDays(1), TODAY.plusYears(10), TODAY))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("issue date");
    }

    @Test
    void shouldReject_whenExpiryIsNotAfterIssue() {
        assertThatThrownBy(() -> PassportRules.requireAcceptable(TODAY.minusDays(1), TODAY.minusDays(2), TODAY))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("after the issue date");
    }

    @Test
    void shouldReportValidity_forStoredDates() {
        assertThat(PassportRules.isValidForAdmission(TODAY.plusMonths(7), TODAY)).isTrue();
        assertThat(PassportRules.isValidForAdmission(TODAY.plusMonths(5), TODAY)).isFalse();
        assertThat(PassportRules.isValidForAdmission(null, TODAY)).isFalse();
    }

    // ---- comparison ----

    @Test
    void shouldMatch_whenPassportAndProfileAgree() {
        assertThat(PassportRules.mismatches("ANNA MARIA", "ERIKSSON", LocalDate.of(1974, 8, 12),
                "ANNA MARIA", "ERIKSSON", LocalDate.of(1974, 8, 12))).isEmpty();
    }

    @Test
    void shouldIgnoreMrzTransliterations_whenComparingNames() {
        // MRZ drops apostrophes, turns hyphens into spaces and strips accents
        assertThat(PassportRules.mismatches("JEAN-LUC", "O'BRIEN", LocalDate.of(1990, 1, 1),
                "JEAN LUC", "OBRIEN", LocalDate.of(1990, 1, 1))).isEmpty();
        assertThat(PassportRules.mismatches("ZOË", "MÜLLER", LocalDate.of(1990, 1, 1),
                "ZOE", "MULLER", LocalDate.of(1990, 1, 1))).isEmpty();
    }

    @Test
    void shouldReportEachDifferingField() {
        assertThat(PassportRules.mismatches("AHMED", "HASSAN", LocalDate.of(2000, 5, 1),
                "AHMAD", "HASSAN", LocalDate.of(2000, 5, 2))).isEqualTo(List.of("givenName", "dob"));
    }

    @Test
    void shouldReportEverythingMissing_whenPassportDataIsAbsent() {
        assertThat(PassportRules.mismatches("AHMED", "HASSAN", LocalDate.of(2000, 5, 1), null, null, null))
                .isEqualTo(List.of("givenName", "familyName", "dob"));
    }
}
