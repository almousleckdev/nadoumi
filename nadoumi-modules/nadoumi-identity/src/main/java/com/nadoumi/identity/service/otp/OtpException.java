package com.nadoumi.identity.service.otp;

import com.nadoumi.identity.exception.NadBadRequestException;

/**
 * A bad, expired or exhausted OTP or verification ticket. Maps to HTTP 400
 * {@code application/problem+json} via {@code NadApiExceptionHandler}. The message
 * is deliberately generic so it never reveals whether an email is registered.
 */
public class OtpException extends NadBadRequestException {

    public OtpException(String message) {
        super(message);
    }
}
