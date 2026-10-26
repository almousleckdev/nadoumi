package com.nadoumi.finance.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nadoumi.common.text.Texts;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.common.web.PageSupport;
import com.nadoumi.finance.domain.LedgerEntry;
import com.nadoumi.finance.mapper.LedgerMapper;
import com.nadoumi.finance.domain.LedgerTotal;
import com.nadoumi.finance.domain.MoneyTotal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The money ledger: every revenue entry and every approved or paid expense on one list, newest first. */
@Service
public class LedgerService {

    /** The most rows a CSV export contains; a wider window is cut here rather than loading the whole table. */
    static final int EXPORT_LIMIT = 5000;
    private static final String CSV_HEADER = "Date,Type,Category,Title,Reference,Currency,Amount";

    private final LedgerMapper mapper;

    public LedgerService(LedgerMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<LedgerEntry> list(String kind, String q, String currency, LocalDate from, LocalDate to, int page, int size) {
        page = PageSupport.clampPage(page);
        size = PageSupport.clampSize(size);
        PageHelper.startPage(page + 1, size);
        List<LedgerEntry> rows = mapper.search(Texts.blankToNull(kind), Texts.blankToNull(q), Texts.blankToNull(currency), from, to);
        long total = new PageInfo<>(rows).getTotal();
        return PageResponse.of(rows, page, size, total);
    }

    /** The rows matching the filter, added up per currency: what came in, what went out, and the difference. */
    @Transactional(readOnly = true)
    public List<LedgerTotal> totals(String kind, String q, String currency, LocalDate from, LocalDate to) {
        Map<String, BigDecimal[]> byCurrency = new TreeMap<>();
        Map<String, Long> entries = new TreeMap<>();
        for (MoneyTotal row : mapper.totals(Texts.blankToNull(kind), Texts.blankToNull(q), Texts.blankToNull(currency), from, to)) {
            BigDecimal[] sums = byCurrency.computeIfAbsent(row.getCurrency(), c -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
            sums["REVENUE".equals(row.getBucket()) ? 0 : 1] = sums["REVENUE".equals(row.getBucket()) ? 0 : 1].add(row.getTotal());
            entries.merge(row.getCurrency(), row.getCount(), Long::sum);
        }
        return byCurrency.entrySet().stream()
                .map(e -> new LedgerTotal(e.getKey(), e.getValue()[0], e.getValue()[1],
                        e.getValue()[0].subtract(e.getValue()[1]), entries.get(e.getKey())))
                .toList();
    }

    /** The same rows as a CSV, expenses written as negative amounts. */
    @Transactional(readOnly = true)
    public String exportCsv(String kind, String q, String currency, LocalDate from, LocalDate to) {
        PageHelper.startPage(1, EXPORT_LIMIT, false);
        List<LedgerEntry> rows = mapper.search(Texts.blankToNull(kind), Texts.blankToNull(q), Texts.blankToNull(currency), from, to);
        StringBuilder csv = new StringBuilder(CSV_HEADER).append('\n');
        for (LedgerEntry e : rows) {
            String signed = ("EXPENSE".equals(e.getKind()) ? e.getAmount().negate() : e.getAmount()).toPlainString();
            csv.append(e.getOccurredOn()).append(',').append(e.getKind()).append(',').append(cell(e.getCategory())).append(',')
                    .append(cell(e.getTitle())).append(',').append(cell(e.getReference())).append(',').append(e.getCurrency())
                    .append(',').append(signed).append('\n');
        }
        return csv.toString();
    }

    /** A CSV cell: quoted when needed, and a leading formula character is neutralised so a spreadsheet never runs it. */
    static String cell(String value) {
        if (value == null) {
            return "";
        }
        String v = value;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
