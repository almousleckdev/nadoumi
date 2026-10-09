package com.nadoumi.communication.web.request;

import jakarta.validation.constraints.NotNull;

/** The other side of a private chat: a student for staff callers, a staff member for student callers. */
public record OpenDirectRequest(@NotNull Long userId) {
}
