package com.nadoumi.identity.service.otp;

/** Why an email one-time code was issued. Keeps register and reset codes separate. */
public enum OtpPurpose {
    REGISTER,
    PASSWORD_RESET
}
