package com.nadoumi.media;

/**
 * A media asset does not exist, or must not be revealed on the route that was
 * used (e.g. a PROTECTED asset requested through the PUBLIC {@code /api/media/{id}}
 * indirection). Mapped to HTTP 404 by {@code MediaExceptionAdvice}. Module-local
 * on purpose — {@code nadoumi-media} does not depend on {@code nadoumi-identity}.
 */
public class NadMediaNotFoundException extends RuntimeException {

    public NadMediaNotFoundException(String message) {
        super(message);
    }
}
