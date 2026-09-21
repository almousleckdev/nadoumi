package com.nadoumi.identity.service.otp;

/** Why an email one-time code was issued. Keeps register and reset codes separate. */
public enum OtpPurpose {
    REGISTER,
    PASSWORD_RESET,
    /** Proves ownership of an applicant's contact email; issued only to an authenticated student. */
    APPLICANT_EMAIL,
    /** Proves ownership of a new login email; issued only to an authenticated student. */
    EMAIL_CHANGE
}
