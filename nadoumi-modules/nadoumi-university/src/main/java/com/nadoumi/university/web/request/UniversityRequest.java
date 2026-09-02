package com.nadoumi.university.web.request;

import com.nadoumi.university.domain.enums.UniversityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Create / update body for {@code /api/staff/universities}. */
public record UniversityRequest(
        @NotBlank @Size(max = 200)
        String name,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "country must be an ISO alpha-2 code")
        String country,
        @Size(max = 120)
        String city,
        @Size(max = 255)
        String website,
        @Size(max = 24)
        String rankingTier,
        @NotNull
        UniversityStatus status,
        @Size(max = 500)
        String remark) {
}
