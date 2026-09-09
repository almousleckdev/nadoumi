package com.nadoumi.identity.exception;

import com.ruoyi.common.exception.RateLimitExceededException;
import com.ruoyi.common.exception.user.CaptchaException;
import com.ruoyi.common.exception.user.CaptchaExpireException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * RFC 9457 {@code application/problem+json} for the Nadoumi {@code /api/**} surface
 * (API_DESIGN §5). Ordered ahead of RuoYi's global {@code AjaxResult} handler so
 * Nadoumi controllers get real status codes; the {@code /system|/monitor|/tool}
 * endpoints are outside {@code basePackages} and keep RuoYi's handler.
 *
 * <p>Deliberately no blanket {@code @ExceptionHandler(Exception.class)}: this advice
 * runs at {@code HIGHEST_PRECEDENCE}, so a catch-all here would pre-empt the
 * per-module advices (e.g. {@code MediaExceptionAdvice}). Genuinely unexpected
 * causes fall through to RuoYi's handler, which now returns a sanitized generic
 * message. The handlers below cover the framework exceptions no module advice owns,
 * so they surface as real 4xx problem+json instead of a leaked 500 message.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.nadoumi")
public class NadApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(NadApiExceptionHandler.class);

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

    @ExceptionHandler({ CaptchaException.class, CaptchaExpireException.class })
    public ProblemDetail captcha(Exception e) {
        return problem(HttpStatus.BAD_REQUEST, "the captcha answer is incorrect or has expired");
    }

    /**
     * SMTP is down or misconfigured (e.g. bad credentials). Surface a real 502 so
     * the caller knows nothing was sent, and log the cause so operators can see
     * which SMTP failure it was.
     */
    @ExceptionHandler(MailException.class)
    public ProblemDetail mail(MailException e) {
        log.error("outbound email failed on the /api surface", e);
        return problem(HttpStatus.BAD_GATEWAY, "the email could not be sent right now, please try again shortly");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalidBody(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .findFirst().orElse("invalid request body");
        return problem(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail unreadableBody(HttpMessageNotReadableException e) {
        return problem(HttpStatus.BAD_REQUEST, "malformed request body");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail missingParam(MissingServletRequestParameterException e) {
        return problem(HttpStatus.BAD_REQUEST, e.getParameterName() + " is required");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail typeMismatch(MethodArgumentTypeMismatchException e) {
        return problem(HttpStatus.BAD_REQUEST, e.getName() + " has an invalid value");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail methodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "method not supported");
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> tooManyRequests(RateLimitExceededException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Integer.toString(e.getRetryAfterSeconds()))
                .body(problem(HttpStatus.TOO_MANY_REQUESTS, e.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail dataAccess(DataAccessException e) {
        log.error("data access failure on the /api surface", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred");
    }

    private static ProblemDetail problem(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setDetail(detail);
        return pd;
    }
}
