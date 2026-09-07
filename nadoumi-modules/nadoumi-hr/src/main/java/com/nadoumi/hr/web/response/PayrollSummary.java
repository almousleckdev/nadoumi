package com.nadoumi.hr.web.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Read-only compensation roll-up over {@code nad_employee}. Every salary is
 * normalised to a monthly-equivalent for the per-currency totals; amounts are
 * never summed across currencies.
 */
public record PayrollSummary(
        List<CurrencyLine> byCurrency,
        List<Row> rows) {

    public record CurrencyLine(String currency, BigDecimal monthly, BigDecimal annual, int headcount) {
    }

    public record Row(
            long employeeId,
            String name,
            String deptName,
            String positionTitle,
            String employmentStatus,
            BigDecimal salaryAmount,
            String salaryCurrency,
            String payFrequency,
            BigDecimal monthlyEquivalent) {
    }
}
