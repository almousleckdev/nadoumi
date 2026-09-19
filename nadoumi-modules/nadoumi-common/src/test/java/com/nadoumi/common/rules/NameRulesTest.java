package com.nadoumi.common.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nadoumi.common.exception.NadBadRequestException;
import org.junit.jupiter.api.Test;

class NameRulesTest {

    @Test
    void shouldUppercase_whenNameIsLowercase() {
        assertThat(NameRules.normalize("ahmed")).isEqualTo("AHMED");
    }

    @Test
    void shouldTrimAndCollapseSpaces() {
        assertThat(NameRules.normalize("  mary   jane ")).isEqualTo("MARY JANE");
    }

    @Test
    void shouldKeepHyphensAndApostrophes_whenPresent() {
        assertThat(NameRules.normalize("o'brien-smith")).isEqualTo("O'BRIEN-SMITH");
    }

    @Test
    void shouldFoldTypographicApostrophe() {
        assertThat(NameRules.normalize("o’brien")).isEqualTo("O'BRIEN");
    }

    @Test
    void shouldAcceptNonLatinLetters() {
        assertThat(NameRules.normalize("Zoë")).isEqualTo("ZOË");
    }

    @Test
    void shouldRejectName_whenItContainsDigitsOrSymbols() {
        assertThatThrownBy(() -> NameRules.normalize("john2")).isInstanceOf(NadBadRequestException.class);
        assertThatThrownBy(() -> NameRules.normalize("john@doe")).isInstanceOf(NadBadRequestException.class);
    }

    @Test
    void shouldRejectName_whenBlank() {
        assertThatThrownBy(() -> NameRules.normalize("   ")).isInstanceOf(NadBadRequestException.class);
        assertThatThrownBy(() -> NameRules.normalize(null)).isInstanceOf(NadBadRequestException.class);
    }
}
