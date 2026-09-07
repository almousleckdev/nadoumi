package com.nadoumi.university.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Create / update body for a university's academic departments. */
public record DepartmentRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 160) String nameCn,
        @PositiveOrZero Integer sortOrder) {
}
