package com.nadoumi.identity.event;

/**
 * Published inside the registration transaction once the student account exists,
 * so a listener can create the student's primary applicant profile atomically.
 * The email is verified; the applicant's legal names are filled in later, during onboarding.
 */
public record StudentRegisteredEvent(long userId, String email, String username) {
}
