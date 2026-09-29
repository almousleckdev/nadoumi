package com.nadoumi.common.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextsTest {

    @Test
    void shouldReturnNull_whenValueIsNull() {
        assertThat(Texts.blankToNull(null)).isNull();
    }

    @Test
    void shouldReturnNull_whenValueIsBlank() {
        assertThat(Texts.blankToNull("   \t")).isNull();
    }

    @Test
    void shouldReturnNull_whenValueIsOnlyIdeographicSpace() {
        assertThat(Texts.blankToNull("　　")).isNull();
    }

    @Test
    void shouldTrimSurroundingWhitespace_whenValueHasText() {
        assertThat(Texts.blankToNull("  Beijing 　")).isEqualTo("Beijing");
    }

    @Test
    void shouldKeepInnerWhitespace() {
        assertThat(Texts.blankToNull(" a  b ")).isEqualTo("a  b");
    }

    @Test
    void shouldReturnNull_whenStringifyingNull() {
        assertThat(Texts.stringOrNull(null)).isNull();
    }

    @Test
    void shouldUseToString_whenStringifyingValue() {
        assertThat(Texts.stringOrNull(42L)).isEqualTo("42");
    }
}
