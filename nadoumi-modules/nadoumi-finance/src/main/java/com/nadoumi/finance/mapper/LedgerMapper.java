package com.nadoumi.finance.mapper;

import com.nadoumi.finance.domain.LedgerEntry;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** Read-only union of {@code nad_revenue} and APPROVED/PAID {@code nad_expense}. */
public interface LedgerMapper {

    /** The matching rows summed per kind and currency ({@code bucket} = kind), for the totals strip. */
    List<com.nadoumi.finance.domain.MoneyTotal> totals(@Param("kind") String kind, @Param("q") String q,
            @Param("currency") String currency, @Param("from") LocalDate from, @Param("to") LocalDate to);

    List<LedgerEntry> search(@Param("kind") String kind, @Param("q") String q, @Param("currency") String currency,
            @Param("from") LocalDate from, @Param("to") LocalDate to);
}
