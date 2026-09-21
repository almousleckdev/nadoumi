package com.nadoumi.identity.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Ask for a confirmation code to be sent to a new sign-in email. */
public record EmailChangeCodeRequest(@NotBlank @Email @Size(max = 120) String newEmail) {
}
