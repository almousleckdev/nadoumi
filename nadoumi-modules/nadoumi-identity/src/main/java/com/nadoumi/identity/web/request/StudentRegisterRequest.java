package com.nadoumi.identity.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentRegisterRequest(
        @NotBlank @Size(min = 2, max = 20) String username,
        @NotBlank @Size(min = 5, max = 20) String password,
        @Size(max = 30) String nickName,
        @Email @Size(max = 120) String email,
        String code,
        String uuid) {
}
