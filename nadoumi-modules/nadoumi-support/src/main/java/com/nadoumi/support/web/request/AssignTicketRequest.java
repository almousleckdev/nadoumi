package com.nadoumi.support.web.request;

import jakarta.validation.constraints.NotNull;

/** The staff user the ticket is (re)assigned to. Must be a staff account. */
public record AssignTicketRequest(@NotNull Long assigneeUserId) {
}
