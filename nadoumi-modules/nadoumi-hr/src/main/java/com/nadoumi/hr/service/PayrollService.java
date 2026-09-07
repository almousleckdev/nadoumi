package com.nadoumi.hr.service;

import com.nadoumi.hr.domain.Employee;
import com.nadoumi.hr.mapper.EmployeeMapper;
import com.nadoumi.hr.web.response.PayrollSummary;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Compensation roll-up. Read-only; a pay-run ledger arrives with the Payment module. */
@Service
public class PayrollService {

    private static final BigDecimal WEEKS_PER_MONTH = new BigDecimal("4.333333");
    private static final BigDecimal HOURS_PER_MONTH = new BigDecimal("160");
    private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12");

    private final EmployeeMapper mapper;

    public PayrollService(EmployeeMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PayrollSummary summary() {
        List<Employee> employees = mapper.payrollRows();
        List<PayrollSummary.Row> rows = new ArrayList<>();
        Map<String, BigDecimal> monthlyByCcy = new LinkedHashMap<>();
        Map<String, Integer> headcount = new LinkedHashMap<>();

        for (Employee e : employees) {
            String ccy = e.getSalaryCurrency() == null ? "CNY" : e.getSalaryCurrency().toUpperCase(Locale.ROOT);
            BigDecimal monthly = toMonthly(e.getSalaryAmount(), e.getPayFrequency());
            rows.add(new PayrollSummary.Row(
                    e.getId(), e.getNickName(), e.getDeptName(), e.getPositionTitle(),
                    e.getEmploymentStatus(), e.getSalaryAmount(), ccy, e.getPayFrequency(), monthly));
            monthlyByCcy.merge(ccy, monthly, BigDecimal::add);
            headcount.merge(ccy, 1, Integer::sum);
        }

        List<PayrollSummary.CurrencyLine> lines = monthlyByCcy.entrySet().stream()
                .map(en -> new PayrollSummary.CurrencyLine(
                        en.getKey(),
                        en.getValue().setScale(2, RoundingMode.HALF_UP),
                        en.getValue().multiply(MONTHS_PER_YEAR).setScale(2, RoundingMode.HALF_UP),
                        headcount.getOrDefault(en.getKey(), 0)))
                .toList();
        return new PayrollSummary(lines, rows);
    }

    private static BigDecimal toMonthly(BigDecimal amount, String frequency) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        String f = frequency == null ? "MONTHLY" : frequency.toUpperCase(Locale.ROOT);
        return switch (f) {
            case "ANNUAL" -> amount.divide(MONTHS_PER_YEAR, 2, RoundingMode.HALF_UP);
            case "WEEKLY" -> amount.multiply(WEEKS_PER_MONTH).setScale(2, RoundingMode.HALF_UP);
            case "HOURLY" -> amount.multiply(HOURS_PER_MONTH).setScale(2, RoundingMode.HALF_UP);
            default -> amount.setScale(2, RoundingMode.HALF_UP);
        };
    }
}
