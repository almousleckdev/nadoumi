package com.nadoumi.identity.web.request;

import com.nadoumi.identity.service.otp.OtpPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Exchange an email one-time code for a single-use verification ticket. */
public record EmailOtpVerifyRequest(
        @NotBlank @Email String email,
        @NotNull OtpPurpose purpose,
        @NotBlank String otp) {
}
