package com.nadoumi.application.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Take the step {@code code}; {@code note} is kept as the reason and as the rationale of any decision it records. */
public record AdvanceRequest(@NotBlank String code, String note, @NotNull Integer version) {
}
