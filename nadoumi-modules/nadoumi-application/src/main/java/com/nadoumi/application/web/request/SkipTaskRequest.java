package com.nadoumi.application.web.request;

import jakarta.validation.constraints.NotBlank;

public record SkipTaskRequest(@NotBlank String reason) {
}
