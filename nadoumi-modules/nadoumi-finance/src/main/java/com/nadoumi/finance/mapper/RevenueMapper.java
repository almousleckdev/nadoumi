package com.nadoumi.finance.mapper;

import com.nadoumi.finance.domain.MoneyTotal;
import com.nadoumi.finance.domain.Revenue;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** MyBatis mapper for {@code nad_revenue}. */
public interface RevenueMapper {

    void insert(Revenue revenue);

    int update(Revenue revenue);

    Revenue findById(@Param("id") long id);

    int deleteById(@Param("id") long id);

    /** How many revenue rows already reference this related record with this reference (idempotency check). */
    int countByRelated(@Param("relatedType") String relatedType, @Param("relatedId") long relatedId,
            @Param("reference") String reference);

    List<Revenue> search(@Param("q") String q, @Param("source") String source,
            @Param("from") LocalDate from, @Param("to") LocalDate to);

    List<MoneyTotal> totalsByCurrency(@Param("from") LocalDate from, @Param("to") LocalDate to);

    List<MoneyTotal> totalsBySource(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Sum of the currency's revenue received in the window (both ends inclusive); never null. */
    java.math.BigDecimal sumInRange(@Param("currency") String currency, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Revenue per calendar month ({@code bucket} = yyyy-MM) for one currency. */
    List<MoneyTotal> monthlyTotals(@Param("currency") String currency, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Every currency that has revenue, for the currency picker. */
    List<String> currencies();
}
