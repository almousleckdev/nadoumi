package com.nadoumi.identity.event;

/**
 * Published inside the registration transaction once the student account exists,
 * so a listener can create the student's primary applicant profile atomically.
 * Names are already normalised (trimmed, UPPERCASE); the email is verified.
 */
public record StudentRegisteredEvent(long userId, String email, String givenName, String familyName) {
}
