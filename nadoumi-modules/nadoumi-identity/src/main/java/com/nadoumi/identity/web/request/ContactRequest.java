package com.nadoumi.identity.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A message from the public website's Contact page. {@code website} is a
 * honeypot — real users never see it, so a non-blank value means a bot and the
 * submission is silently dropped.
 */
public record ContactRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 190) String email,
        @Size(max = 160) String subject,
        @NotBlank @Size(max = 4000) String message,
        @Size(max = 12) String locale,
        String website) {
}
