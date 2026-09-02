package com.nadoumi.identity.exception;

/** Caller authenticated but not allowed here (HTTP 403). */
public class NadForbiddenException extends RuntimeException {

    public NadForbiddenException(String message) {
        super(message);
    }
}
