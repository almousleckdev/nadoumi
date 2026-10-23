package com.nadoumi.program.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Pasted majors, one per line; a line may carry the Chinese name after a tab or a pipe. */
public record MajorBulkRequest(
        @NotBlank @Size(max = 20000) String text,
        @Size(max = 32) String discipline) {
}
