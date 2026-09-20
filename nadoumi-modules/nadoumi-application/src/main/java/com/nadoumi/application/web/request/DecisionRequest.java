package com.nadoumi.application.web.request;

import jakarta.validation.constraints.NotBlank;

public record DecisionRequest(
        @NotBlank String decisionType,
        @NotBlank String outcome,
        @NotBlank String rationale) {
}
