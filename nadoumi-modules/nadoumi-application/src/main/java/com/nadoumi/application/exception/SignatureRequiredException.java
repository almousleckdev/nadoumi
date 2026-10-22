package com.nadoumi.application.exception;

/** The application cannot be submitted until the applicant has signed the form (HTTP 409). */
public class SignatureRequiredException extends RuntimeException {

    public SignatureRequiredException(String message) {
        super(message);
    }
}
