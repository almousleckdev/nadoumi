package com.nadoumi.applicant.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Ask for a confirmation code to be sent to a new contact email. */
public record EmailCodeRequest(@NotBlank @Email @Size(max = 120) String email) {
}
