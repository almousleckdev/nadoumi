package com.nadoumi.identity.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * RFC 9457 {@code application/problem+json} for the Nadoumi {@code /api/**} surface
 * (API_DESIGN §5). Ordered ahead of RuoYi's global {@code AjaxResult} handler so
 * Nadoumi controllers get real status codes; the {@code /system|/monitor|/tool}
 * endpoints are outside {@code basePackages} and keep RuoYi's handler.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.nadoumi")
public class NadApiExceptionHandler {

    @ExceptionHandler(NadNotFoundException.class)
    public ProblemDetail notFound(NadNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({ AccessDeniedException.class, NadForbiddenException.class })
    public ProblemDetail forbidden(Exception e) {
        return problem(HttpStatus.FORBIDDEN, e instanceof NadForbiddenException ? e.getMessage() : "access denied");
    }

    @ExceptionHandler(GrantException.class)
    public ProblemDetail conflict(GrantException e) {
        return problem(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ProblemDetail duplicate(DuplicateKeyException e) {
        return problem(HttpStatus.CONFLICT, "conflicts with an existing record");
    }

    @ExceptionHandler({ NadBadRequestException.class, ConstraintViolationException.class,
            IllegalArgumentException.class, IllegalStateException.class })
    public ProblemDetail badRequest(Exception e) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalidBody(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .findFirst().orElse("invalid request body");
        return problem(HttpStatus.BAD_REQUEST, detail);
    }

    private static ProblemDetail problem(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setDetail(detail);
        return pd;
    }
}
