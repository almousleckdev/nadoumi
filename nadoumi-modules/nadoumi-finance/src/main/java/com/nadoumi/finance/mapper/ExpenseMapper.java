package com.nadoumi.finance.mapper;

import com.nadoumi.finance.domain.Expense;
import com.nadoumi.finance.domain.ExpenseCategory;
import com.nadoumi.finance.domain.MoneyTotal;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** MyBatis mapper for {@code nad_expense} and {@code nad_expense_category}. */
public interface ExpenseMapper {

    void insert(Expense expense);

    int update(Expense expense);

    Expense findById(@Param("id") long id);

    int deleteById(@Param("id") long id);

    List<Expense> search(@Param("q") String q, @Param("status") String status,
            @Param("categoryId") Long categoryId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    Integer maxReceiptSeq(@Param("prefix") String prefix);

    /** Per-currency totals for expenses within the window; {@code status} optional (defaults to PAID + APPROVED). */
    List<MoneyTotal> totalsByCurrency(@Param("from") LocalDate from, @Param("to") LocalDate to);

    List<MoneyTotal> totalsByCategory(@Param("from") LocalDate from, @Param("to") LocalDate to);

    List<ExpenseCategory> listCategories();

    /** Case-insensitive lookup used when a staff member types a new category name. */
    Long findCategoryIdByName(@Param("name") String name);

    /** Inserts an ad-hoc category; the generated id is set on {@code category}. */
    int insertCategory(ExpenseCategory category);
}
