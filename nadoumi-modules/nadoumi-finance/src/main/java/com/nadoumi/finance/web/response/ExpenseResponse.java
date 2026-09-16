package com.nadoumi.finance.web.response;

import com.nadoumi.finance.domain.Expense;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Staff-facing view of an expense. Omits the raw actor ids/usernames
 * ({@code submittedBy}, {@code approvedBy}, {@code createBy}, {@code updateBy})
 * that {@link Expense} carries for audit purposes — callers get the resolved
 * display names instead.
 */
public record ExpenseResponse(
        long id,
        String receiptNo,
        Long categoryId,
        String categoryName,
        String title,
        String description,
        BigDecimal amount,
        String currency,
        LocalDate spentOn,
        String vendor,
        String paymentMethod,
        String status,
        String submittedByName,
        String approvedByName,
        LocalDateTime approvedAt,
        LocalDateTime paidAt,
        String notes,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    public static ExpenseResponse from(Expense e) {
        return new ExpenseResponse(
                e.getId(), e.getReceiptNo(), e.getCategoryId(), e.getCategoryName(),
                e.getTitle(), e.getDescription(), e.getAmount(), e.getCurrency(), e.getSpentOn(),
                e.getVendor(), e.getPaymentMethod(), e.getStatus(),
                e.getSubmittedByName(), e.getApprovedByName(), e.getApprovedAt(), e.getPaidAt(),
                e.getNotes(), e.getCreateTime(), e.getUpdateTime());
    }
}
