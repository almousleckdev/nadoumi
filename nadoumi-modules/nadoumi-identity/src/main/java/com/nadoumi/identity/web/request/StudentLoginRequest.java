package com.nadoumi.identity.web.request;

import jakarta.validation.constraints.NotBlank;

public record StudentLoginRequest(
        @NotBlank String username,
        @NotBlank String password,
        String code,
        String uuid) {
}
