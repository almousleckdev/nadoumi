package com.nadoumi.application.exception;

/** The application cannot be submitted until the applicant has finished onboarding and the application form data (HTTP 409). */
public class ProfileIncompleteException extends RuntimeException {

    public ProfileIncompleteException(String message) {
        super(message);
    }
}
