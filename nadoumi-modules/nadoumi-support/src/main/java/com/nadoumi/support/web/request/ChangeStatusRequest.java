package com.nadoumi.support.web.request;

import com.nadoumi.support.domain.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull TicketStatus status) {
}
