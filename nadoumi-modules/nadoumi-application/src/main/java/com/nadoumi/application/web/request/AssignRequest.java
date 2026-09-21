package com.nadoumi.application.web.request;

import jakarta.validation.constraints.NotNull;

public record AssignRequest(@NotNull Long assigneeUserId) {
}
