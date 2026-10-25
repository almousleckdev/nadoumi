package com.nadoumi.finance.web.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * The finance dashboard for one currency: headline numbers, the last twelve months, and where the money came from and
 * went. Currencies are never added together, so each response covers exactly one.
 */
public record FinanceOverview(
        String currency,
        List<String> currencies,
        String asOf,
        Kpis kpis,
        List<Month> months,
        List<Bucket> revenueBySource,
        List<Bucket> expenseByCategory) {

    /**
     * {@code receivables} is money customers have reported paying that staff have not confirmed yet; {@code payables} is
     * expenses submitted or approved but not paid. {@code *Prev} values are the same measure for the previous month.
     */
    public record Kpis(
            BigDecimal revenueToday,
            BigDecimal revenueMonth, BigDecimal revenueMonthPrev,
            BigDecimal expensesMonth, BigDecimal expensesMonthPrev,
            BigDecimal netMonth, BigDecimal netMonthPrev,
            BigDecimal revenueYtd, BigDecimal expensesYtd, BigDecimal netYtd,
            BigDecimal receivables, long receivablesCount,
            BigDecimal payables, long payablesCount) {
    }

    /** {@code month} is yyyy-MM. */
    public record Month(String month, BigDecimal revenue, BigDecimal expenses, BigDecimal net) {
    }

    public record Bucket(String label, BigDecimal total, long count) {
    }
}
