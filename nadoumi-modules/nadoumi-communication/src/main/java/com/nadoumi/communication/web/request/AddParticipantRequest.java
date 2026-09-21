package com.nadoumi.communication.web.request;

import jakarta.validation.constraints.NotNull;

public record AddParticipantRequest(@NotNull Long userId, @NotNull String role) {
}
