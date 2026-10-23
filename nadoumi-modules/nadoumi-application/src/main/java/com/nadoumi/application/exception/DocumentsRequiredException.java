package com.nadoumi.application.exception;

/** The application cannot be submitted while a mandatory document has not been uploaded (HTTP 409). */
public class DocumentsRequiredException extends RuntimeException {

    public DocumentsRequiredException(String message) {
        super(message);
    }
}
