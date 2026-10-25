package com.nadoumi.finance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nadoumi.finance.domain.MoneyTotal;
import com.nadoumi.finance.mapper.ExpenseMapper;
import com.nadoumi.finance.mapper.RevenueMapper;
import com.nadoumi.finance.web.response.FinanceOverview;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class FinanceOverviewServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 25);

    private final RevenueMapper revenue = mock(RevenueMapper.class);
    private final ExpenseMapper expenses = mock(ExpenseMapper.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<ReceivablesSource> receivables = mock(ObjectProvider.class);
    private final FinanceOverviewService service = new FinanceOverviewService(revenue, expenses, receivables);

    private static MoneyTotal total(String bucket, String currency, String amount, long count) {
        MoneyTotal t = new MoneyTotal();
        t.setBucket(bucket);
        t.setCurrency(currency);
        t.setTotal(new BigDecimal(amount));
        t.setCount(count);
        return t;
    }

    @Test
    void shouldBuildMonthOnMonthYearToDateAndTwelveMonthTrend_forOneCurrency() {
        // this month 1 Oct..25 Oct, previous month September, year to date from 1 Jan
        when(revenue.sumInRange("CNY", TODAY, TODAY)).thenReturn(new BigDecimal("300"));
        when(revenue.sumInRange("CNY", LocalDate.of(2026, 10, 1), TODAY)).thenReturn(new BigDecimal("1200"));
        when(revenue.sumInRange("CNY", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))).thenReturn(new BigDecimal("900"));
        when(revenue.sumInRange("CNY", LocalDate.of(2026, 1, 1), TODAY)).thenReturn(new BigDecimal("9000"));
        when(expenses.sumInRange("CNY", LocalDate.of(2026, 10, 1), TODAY)).thenReturn(new BigDecimal("500"));
        when(expenses.sumInRange("CNY", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))).thenReturn(new BigDecimal("700"));
        when(expenses.sumInRange("CNY", LocalDate.of(2026, 1, 1), TODAY)).thenReturn(new BigDecimal("4000"));
        when(revenue.monthlyTotals(eq("CNY"), any(), any())).thenReturn(List.of(total("2026-10", "CNY", "1200", 4), total("2026-09", "CNY", "900", 3)));
        when(expenses.monthlyTotals(eq("CNY"), any(), any())).thenReturn(List.of(total("2026-10", "CNY", "500", 2)));
        when(expenses.payablesByCurrency()).thenReturn(List.of(total(null, "CNY", "250", 2), total(null, "USD", "10", 1)));
        ReceivablesSource source = mock(ReceivablesSource.class);
        when(source.awaitingConfirmation()).thenReturn(List.of(total(null, "CNY", "600", 2)));
        when(receivables.stream()).thenReturn(java.util.stream.Stream.of(source));
        when(revenue.currencies()).thenReturn(List.of("CNY", "USD"));
        when(revenue.totalsBySource(any(), any())).thenReturn(List.of(total("APPLICATION_FEE", "CNY", "8000", 20), total("SERVICE_FEE", "USD", "5", 1)));
        when(expenses.totalsByCategory(any(), any())).thenReturn(List.of(total("Rent", "CNY", "3000", 3)));

        FinanceOverview o = service.overview(null, TODAY);

        assertThat(o.currency()).isEqualTo("CNY");
        assertThat(o.currencies()).containsExactly("CNY", "USD");
        assertThat(o.kpis().revenueToday()).isEqualByComparingTo("300");
        assertThat(o.kpis().revenueMonth()).isEqualByComparingTo("1200");
        assertThat(o.kpis().netMonth()).isEqualByComparingTo("700");
        assertThat(o.kpis().netMonthPrev()).isEqualByComparingTo("200");
        assertThat(o.kpis().netYtd()).isEqualByComparingTo("5000");
        assertThat(o.kpis().receivables()).isEqualByComparingTo("600");
        assertThat(o.kpis().receivablesCount()).isEqualTo(2);
        assertThat(o.kpis().payables()).isEqualByComparingTo("250");
        assertThat(o.months()).hasSize(12);
        assertThat(o.months().get(0).month()).isEqualTo("2025-11");
        assertThat(o.months().get(11).month()).isEqualTo("2026-10");
        assertThat(o.months().get(11).net()).isEqualByComparingTo("700");
        assertThat(o.months().get(10).revenue()).isEqualByComparingTo("900");
        assertThat(o.months().get(5).revenue()).isEqualByComparingTo("0");
        assertThat(o.revenueBySource()).extracting(FinanceOverview.Bucket::label).containsExactly("APPLICATION_FEE"); // USD row left out
    }

    @Test
    void shouldReportZeroReceivables_whenNoPaymentModuleIsPresent() {
        when(receivables.stream()).thenReturn(java.util.stream.Stream.empty());
        when(revenue.sumInRange(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(expenses.sumInRange(any(), any(), any())).thenReturn(BigDecimal.ZERO);

        FinanceOverview o = service.overview("usd", TODAY);

        assertThat(o.currency()).isEqualTo("USD");
        assertThat(o.kpis().receivables()).isEqualByComparingTo("0");
        assertThat(o.kpis().payables()).isEqualByComparingTo("0");
    }
}
