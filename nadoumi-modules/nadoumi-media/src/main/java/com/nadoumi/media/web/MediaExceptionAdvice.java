package com.nadoumi.media.web;

import com.nadoumi.media.NadMediaNotFoundException;
import com.nadoumi.media.validation.MediaValidationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps {@link MediaValidationException} to an RFC 9457 {@code application/problem+json}
 * response, mirroring {@code nadoumi-identity}'s {@code NadApiExceptionHandler}: a
 * bare {@link ProblemDetail} with the reason's status and a human-readable detail.
 * The {@code reason} code is added as a problem-detail extension member so a client
 * can tell {@code TYPE_MISMATCH} from {@code BAD_NAME} (both HTTP 422).
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.nadoumi.media")
public class MediaExceptionAdvice {

    @ExceptionHandler(MediaValidationException.class)
    public ProblemDetail mediaValidation(MediaValidationException e) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.valueOf(e.reason().status()));
        pd.setDetail(e.getMessage());
        pd.setProperty("reason", e.reason().name());
        return pd;
    }

    @ExceptionHandler(NadMediaNotFoundException.class)
    public ProblemDetail mediaNotFound(NadMediaNotFoundException e) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setDetail(e.getMessage());
        return pd;
    }
}
