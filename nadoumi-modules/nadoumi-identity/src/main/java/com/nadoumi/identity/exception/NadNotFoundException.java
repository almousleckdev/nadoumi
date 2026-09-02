package com.nadoumi.identity.exception;

/** Resource missing or not visible to the caller (HTTP 404). */
public class NadNotFoundException extends RuntimeException {

    public NadNotFoundException(String message) {
        super(message);
    }
}
