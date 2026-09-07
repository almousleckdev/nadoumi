package com.nadoumi.finance.web.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Derived earnings view for a date window. Money is never summed across
 * currencies — {@code net} is computed per currency (revenue − approved/paid
 * expenses).
 */
public record FinanceSummary(
        String from,
        String to,
        List<CurrencyLine> byCurrency,
        List<Bucket> revenueBySource,
        List<Bucket> expenseByCategory) {

    public record CurrencyLine(String currency, BigDecimal revenue, BigDecimal expenses, BigDecimal net) {
    }

    public record Bucket(String label, String currency, BigDecimal total, long count) {
    }
}
