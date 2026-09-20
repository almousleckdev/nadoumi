package com.nadoumi.application.web.request;

import jakarta.validation.constraints.NotNull;

public record TransitionRequest(String reason, @NotNull Integer version) {
}
