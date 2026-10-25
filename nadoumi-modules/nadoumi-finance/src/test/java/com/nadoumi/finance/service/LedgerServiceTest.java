package com.nadoumi.finance.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LedgerServiceTest {

    @Test
    void shouldQuoteCellsWithCommasAndQuotes() {
        assertThat(LedgerService.cell("Rent, October")).isEqualTo("\"Rent, October\"");
        assertThat(LedgerService.cell("The \"big\" one")).isEqualTo("\"The \"\"big\"\" one\"");
        assertThat(LedgerService.cell(null)).isEmpty();
    }

    @Test
    void shouldNeutraliseSpreadsheetFormulas() {
        assertThat(LedgerService.cell("=SUM(A1:A9)")).isEqualTo("'=SUM(A1:A9)");
        assertThat(LedgerService.cell("+86 138")).isEqualTo("'+86 138");
        assertThat(LedgerService.cell("@cmd")).isEqualTo("'@cmd");
    }
}
