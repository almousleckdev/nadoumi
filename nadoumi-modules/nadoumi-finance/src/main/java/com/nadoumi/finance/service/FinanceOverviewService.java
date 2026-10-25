package com.nadoumi.finance.service;

import com.nadoumi.finance.domain.MoneyTotal;
import com.nadoumi.finance.mapper.ExpenseMapper;
import com.nadoumi.finance.mapper.RevenueMapper;
import com.nadoumi.finance.web.response.FinanceOverview;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The finance dashboard read model: month on month, year to date, trend, sources, and what is owed in and out. */
@Service
public class FinanceOverviewService {

    static final String DEFAULT_CURRENCY = "CNY";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int TREND_MONTHS = 12;

    private final RevenueMapper revenue;
    private final ExpenseMapper expenses;
    private final ObjectProvider<ReceivablesSource> receivables;

    public FinanceOverviewService(RevenueMapper revenue, ExpenseMapper expenses, ObjectProvider<ReceivablesSource> receivables) {
        this.revenue = revenue;
        this.expenses = expenses;
        this.receivables = receivables;
    }

    @Transactional(readOnly = true)
    public FinanceOverview overview(String currency) {
        return overview(currency, LocalDate.now(BUSINESS_ZONE));
    }

    FinanceOverview overview(String requestedCurrency, LocalDate today) {
        String currency = requestedCurrency == null || requestedCurrency.isBlank()
                ? DEFAULT_CURRENCY : requestedCurrency.trim().toUpperCase(Locale.ROOT);

        YearMonth thisMonth = YearMonth.from(today);
        YearMonth prevMonth = thisMonth.minusMonths(1);
        LocalDate yearStart = LocalDate.of(today.getYear(), 1, 1);

        BigDecimal revMonth = revenue.sumInRange(currency, thisMonth.atDay(1), today);
        BigDecimal expMonth = expenses.sumInRange(currency, thisMonth.atDay(1), today);
        BigDecimal revPrev = revenue.sumInRange(currency, prevMonth.atDay(1), prevMonth.atEndOfMonth());
        BigDecimal expPrev = expenses.sumInRange(currency, prevMonth.atDay(1), prevMonth.atEndOfMonth());
        BigDecimal revYtd = revenue.sumInRange(currency, yearStart, today);
        BigDecimal expYtd = expenses.sumInRange(currency, yearStart, today);

        MoneyTotal receivable = pick(receivables.stream().flatMap(r -> r.awaitingConfirmation().stream()).toList(), currency);
        MoneyTotal payable = pick(expenses.payablesByCurrency(), currency);

        var kpis = new FinanceOverview.Kpis(
                revenue.sumInRange(currency, today, today),
                revMonth, revPrev, expMonth, expPrev, revMonth.subtract(expMonth), revPrev.subtract(expPrev),
                revYtd, expYtd, revYtd.subtract(expYtd),
                receivable == null ? BigDecimal.ZERO : nz(receivable.getTotal()), receivable == null ? 0 : receivable.getCount(),
                payable == null ? BigDecimal.ZERO : nz(payable.getTotal()), payable == null ? 0 : payable.getCount());

        TreeSet<String> currencies = new TreeSet<>();
        currencies.add(DEFAULT_CURRENCY);
        currencies.addAll(revenue.currencies());
        currencies.addAll(expenses.currencies());

        return new FinanceOverview(currency, List.copyOf(currencies), today.toString(), kpis,
                trend(currency, thisMonth),
                buckets(revenue.totalsBySource(yearStart, today), currency),
                buckets(expenses.totalsByCategory(yearStart, today), currency));
    }

    private List<FinanceOverview.Month> trend(String currency, YearMonth last) {
        YearMonth first = last.minusMonths(TREND_MONTHS - 1L);
        Map<String, BigDecimal> rev = byMonth(revenue.monthlyTotals(currency, first.atDay(1), last.atEndOfMonth()));
        Map<String, BigDecimal> exp = byMonth(expenses.monthlyTotals(currency, first.atDay(1), last.atEndOfMonth()));
        List<FinanceOverview.Month> months = new ArrayList<>();
        for (YearMonth m = first; !m.isAfter(last); m = m.plusMonths(1)) {
            BigDecimal r = rev.getOrDefault(m.toString(), BigDecimal.ZERO);
            BigDecimal e = exp.getOrDefault(m.toString(), BigDecimal.ZERO);
            months.add(new FinanceOverview.Month(m.toString(), r, e, r.subtract(e)));
        }
        return months;
    }

    private static Map<String, BigDecimal> byMonth(List<MoneyTotal> rows) {
        return rows.stream().collect(Collectors.toMap(MoneyTotal::getBucket, t -> nz(t.getTotal()), BigDecimal::add));
    }

    private static List<FinanceOverview.Bucket> buckets(List<MoneyTotal> rows, String currency) {
        return rows.stream().filter(t -> currency.equals(t.getCurrency()))
                .map(t -> new FinanceOverview.Bucket(t.getBucket(), nz(t.getTotal()), t.getCount())).toList();
    }

    private static MoneyTotal pick(List<MoneyTotal> rows, String currency) {
        return rows.stream().filter(t -> currency.equals(t.getCurrency())).findFirst().orElse(null);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
