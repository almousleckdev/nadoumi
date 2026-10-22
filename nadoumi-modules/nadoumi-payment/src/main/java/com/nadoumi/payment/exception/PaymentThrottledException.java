package com.nadoumi.payment.exception;

/** Asked again too soon (HTTP 429). */
public class PaymentThrottledException extends RuntimeException {

    public PaymentThrottledException(String message) {
        super(message);
    }
}
