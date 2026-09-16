package com.nadoumi.media.web;

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
 *
 * <p>A media-not-found condition is thrown as the shared
 * {@code com.nadoumi.common.exception.NadNotFoundException} instead of a
 * module-local type, so {@code NadApiExceptionHandler} maps it like every
 * other module's 404s — no duplicate handler needed here for that case.</p>
 *
 * <p>Global on purpose: the upload endpoints live in each owning domain module
 * ({@code com.nadoumi.university.web}, {@code …scholarship.web}, {@code …program.web},
 * {@code …applicant.web}), so a package-scoped advice would leave their
 * {@code MediaValidationException} unmapped and RuoYi's generic
 * {@code GlobalExceptionHandler} would render it as {@code 200 {code:500}}. A
 * type-specific {@link ExceptionHandler} on this {@link Ordered#HIGHEST_PRECEDENCE}
 * advice wins over that handler.</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class MediaExceptionAdvice {

    @ExceptionHandler(MediaValidationException.class)
    public ProblemDetail mediaValidation(MediaValidationException e) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.valueOf(e.reason().status()));
        pd.setDetail(e.getMessage());
        pd.setProperty("reason", e.reason().name());
        return pd;
    }
}
