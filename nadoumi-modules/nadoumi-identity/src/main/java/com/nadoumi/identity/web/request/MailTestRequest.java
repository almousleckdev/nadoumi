package com.nadoumi.identity.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Where to send the one-shot outbound-mail test. */
public record MailTestRequest(@NotBlank @Email String to) {
}
