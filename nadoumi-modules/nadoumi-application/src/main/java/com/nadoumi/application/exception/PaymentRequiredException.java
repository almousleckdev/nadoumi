package com.nadoumi.application.exception;

/** The application cannot be submitted until its Nadoumi application fee is confirmed (HTTP 402). */
public class PaymentRequiredException extends RuntimeException {

    public PaymentRequiredException(String message) {
        super(message);
    }
}
