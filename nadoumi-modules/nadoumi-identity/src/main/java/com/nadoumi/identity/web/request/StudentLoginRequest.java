package com.nadoumi.identity.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Student sign-in (spec Revision 2, D-R2-2): email-first. */
public record StudentLoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        String code,
        String uuid) {
}
