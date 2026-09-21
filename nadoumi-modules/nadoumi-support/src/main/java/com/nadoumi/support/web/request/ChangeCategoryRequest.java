package com.nadoumi.support.web.request;

import com.nadoumi.support.domain.enums.TicketCategory;
import jakarta.validation.constraints.NotNull;

public record ChangeCategoryRequest(@NotNull TicketCategory category) {
}
