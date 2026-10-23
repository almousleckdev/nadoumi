package com.nadoumi.payment.exception;

/** The request is valid but the payment is not in a state that allows it (HTTP 409). */
public class PaymentConflictException extends RuntimeException {

    public PaymentConflictException(String message) {
        super(message);
    }
}
