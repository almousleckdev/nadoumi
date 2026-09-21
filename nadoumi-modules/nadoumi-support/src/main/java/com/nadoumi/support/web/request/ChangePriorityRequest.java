package com.nadoumi.support.web.request;

import com.nadoumi.support.domain.enums.TicketPriority;
import jakarta.validation.constraints.NotNull;

public record ChangePriorityRequest(@NotNull TicketPriority priority) {
}
