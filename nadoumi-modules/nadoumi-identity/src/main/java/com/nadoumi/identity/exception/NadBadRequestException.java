package com.nadoumi.identity.exception;

/** Caller input rejected (HTTP 400). */
public class NadBadRequestException extends RuntimeException {

    public NadBadRequestException(String message) {
        super(message);
    }
}
