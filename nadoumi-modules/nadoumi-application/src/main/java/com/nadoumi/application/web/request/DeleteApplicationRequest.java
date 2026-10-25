package com.nadoumi.application.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Why an application is being deleted for good; kept in the operation log. */
public record DeleteApplicationRequest(@NotBlank @Size(min = 5, max = 300) String reason) {
}
