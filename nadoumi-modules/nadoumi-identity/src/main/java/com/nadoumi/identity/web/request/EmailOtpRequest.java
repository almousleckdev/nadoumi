package com.nadoumi.identity.web.request;

import com.nadoumi.identity.service.otp.OtpPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Request an email one-time code. Captcha fields are honoured when captcha is on. */
public record EmailOtpRequest(
        @NotBlank @Email String email,
        @NotNull OtpPurpose purpose,
        String code,
        String uuid) {
}
