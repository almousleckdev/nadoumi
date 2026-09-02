package com.nadoumi.identity.exception;

/** Business-rule rejection in the access-grant lifecycle (maps to HTTP 409/422). */
public class GrantException extends RuntimeException {

    public GrantException(String message) {
        super(message);
    }
}
