package com.nadoumi.finance.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        Long categoryId,
        /** Free-typed category name — used only when {@code categoryId} is null; find-or-created. */
        @Size(max = 120) String categoryName,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull LocalDate spentOn,
        @Size(max = 200) String vendor,
        String paymentMethod,
        @Size(max = 1000) String notes) {
}
