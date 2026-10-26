package com.nadoumi.finance.service;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nadoumi.finance.domain.MoneyTotal;
import com.nadoumi.finance.mapper.LedgerMapper;
import java.math.BigDecimal;
import java.util.List;
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

    private static MoneyTotal total(String currency, String kind, String amount, long count) {
        MoneyTotal t = new MoneyTotal();
        t.setCurrency(currency);
        t.setBucket(kind);
        t.setTotal(new BigDecimal(amount));
        t.setCount(count);
        return t;
    }

    @Test
    void shouldAddUpWhatCameInAndWentOut_perCurrency() {
        LedgerMapper mapper = mock(LedgerMapper.class);
        when(mapper.totals(any(), any(), any(), any(), any())).thenReturn(List.of(
                total("CNY", "REVENUE", "1000.50", 4), total("CNY", "EXPENSE", "400.25", 2), total("USD", "REVENUE", "50", 1)));

        var totals = new LedgerService(mapper).totals(null, null, null, null, null);

        assertThat(totals).hasSize(2);
        var cny = totals.get(0);
        assertThat(cny.currency()).isEqualTo("CNY");
        assertThat(cny.revenue()).isEqualByComparingTo("1000.50");
        assertThat(cny.expenses()).isEqualByComparingTo("400.25");
        assertThat(cny.net()).isEqualByComparingTo("600.25");
        assertThat(cny.entries()).isEqualTo(6);
        assertThat(totals.get(1).net()).isEqualByComparingTo("50");
        assertThat(totals.get(1).expenses()).isEqualByComparingTo("0");
    }
}
