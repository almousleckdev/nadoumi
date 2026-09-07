package com.nadoumi.hr.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Move a task to {@code status}, with an optional note recorded on the event. */
public record TaskStatusRequest(@NotBlank String status, @Size(max = 2000) String note) {
}
