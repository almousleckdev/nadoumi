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

    List<Revenue> search(@Param("q") String q, @Param("source") String source,
            @Param("from") LocalDate from, @Param("to") LocalDate to);

    List<MoneyTotal> totalsByCurrency(@Param("from") LocalDate from, @Param("to") LocalDate to);

    List<MoneyTotal> totalsBySource(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
