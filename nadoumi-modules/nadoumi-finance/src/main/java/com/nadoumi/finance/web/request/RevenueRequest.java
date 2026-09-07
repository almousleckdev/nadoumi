package com.nadoumi.finance.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueRequest(
        @NotBlank String source,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull LocalDate receivedOn,
        @Size(max = 120) String reference,
        String relatedType,
        Long relatedId,
        @Size(max = 1000) String notes) {
}
