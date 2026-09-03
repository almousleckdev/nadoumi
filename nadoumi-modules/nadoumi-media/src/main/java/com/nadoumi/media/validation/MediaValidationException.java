package com.nadoumi.media.validation;

/**
 * Upload rejected at the {@link MediaValidation} boundary. Each {@link Reason}
 * carries the HTTP status the module's {@code @RestControllerAdvice} maps it to
 * (spec §I.5): size, MIME allow-list, magic-byte sniff and filename checks.
 */
public class MediaValidationException extends RuntimeException {

    /** Rejection cause and its RFC 9457 HTTP status. */
    public enum Reason {
        TOO_LARGE(413),
        DISALLOWED_TYPE(415),
        TYPE_MISMATCH(422),
        BAD_NAME(422);

        private final int status;

        Reason(int status) {
            this.status = status;
        }

        public int status() {
            return status;
        }
    }

    private final Reason reason;

    public MediaValidationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
