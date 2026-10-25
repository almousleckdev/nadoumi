package com.nadoumi.common.exception;

/** The request is valid but the current state of the data forbids it (HTTP 409). */
public class NadConflictException extends RuntimeException {

    public NadConflictException(String message) {
        super(message);
    }
}
