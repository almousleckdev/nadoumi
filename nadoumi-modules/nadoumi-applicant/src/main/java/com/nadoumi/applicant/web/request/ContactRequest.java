package com.nadoumi.applicant.web.request;

import com.nadoumi.applicant.domain.enums.ContactRelation;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContactRequest(
        @NotNull
        ContactRelation relation,
        @NotBlank @Size(max = 150)
        String name,
        @Email @Size(max = 120)
        String email,
        @Size(max = 32)
        String phone) {
}
