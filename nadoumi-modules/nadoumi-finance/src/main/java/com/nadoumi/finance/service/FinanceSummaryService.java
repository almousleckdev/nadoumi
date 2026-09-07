package com.nadoumi.finance.service;

import com.nadoumi.finance.domain.MoneyTotal;
import com.nadoumi.finance.mapper.ExpenseMapper;
import com.nadoumi.finance.mapper.RevenueMapper;
import com.nadoumi.finance.web.response.FinanceSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Derived earnings read model. Revenue and APPROVED/PAID expenses are grouped by
 * currency; {@code net = revenue − expenses} per currency. Currencies are never
 * summed together.
 */
@Service
public class FinanceSummaryService {

    private final RevenueMapper revenueMapper;
    private final ExpenseMapper expenseMapper;

    public FinanceSummaryService(RevenueMapper revenueMapper, ExpenseMapper expenseMapper) {
        this.revenueMapper = revenueMapper;
        this.expenseMapper = expenseMapper;
    }

    @Transactional(readOnly = true)
    public FinanceSummary summary(LocalDate from, LocalDate to) {
        Map<String, BigDecimal[]> byCcy = new LinkedHashMap<>(); // currency -> [revenue, expenses]
        for (MoneyTotal t : revenueMapper.totalsByCurrency(from, to)) {
            byCcy.computeIfAbsent(t.getCurrency(), k -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO })[0] =
                    nz(t.getTotal());
        }
        for (MoneyTotal t : expenseMapper.totalsByCurrency(from, to)) {
            byCcy.computeIfAbsent(t.getCurrency(), k -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO })[1] =
                    nz(t.getTotal());
        }

        List<FinanceSummary.CurrencyLine> lines = new ArrayList<>();
        byCcy.forEach((ccy, rc) ->
                lines.add(new FinanceSummary.CurrencyLine(ccy, rc[0], rc[1], rc[0].subtract(rc[1]))));

        return new FinanceSummary(
                from == null ? null : from.toString(),
                to == null ? null : to.toString(),
                lines,
                buckets(revenueMapper.totalsBySource(from, to)),
                buckets(expenseMapper.totalsByCategory(from, to)));
    }

    private static List<FinanceSummary.Bucket> buckets(List<MoneyTotal> rows) {
        List<FinanceSummary.Bucket> out = new ArrayList<>();
        for (MoneyTotal t : rows) {
            out.add(new FinanceSummary.Bucket(t.getBucket(), t.getCurrency(), nz(t.getTotal()), t.getCount()));
        }
        return out;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
