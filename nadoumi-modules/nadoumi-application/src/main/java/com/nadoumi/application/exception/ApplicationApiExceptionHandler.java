package com.nadoumi.application.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The only status this module's own exception type needs beyond what {@code
 * NadApiExceptionHandler} (nadoumi-identity) already maps — {@link
 * OptimisticLockException} to 409, per the transition-execution contract
 * (docs/APPLICATION_WORKFLOW.md §3.3).
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RestControllerAdvice(basePackages = "com.nadoumi.application")
public class ApplicationApiExceptionHandler {

    @ExceptionHandler(OptimisticLockException.class)
    public ProblemDetail conflict(OptimisticLockException e) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setDetail(e.getMessage());
        return pd;
    }

    @ExceptionHandler(PaymentRequiredException.class)
    public ProblemDetail paymentRequired(PaymentRequiredException e) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.PAYMENT_REQUIRED);
        pd.setDetail(e.getMessage());
        return pd;
    }

    @ExceptionHandler(DocumentsRequiredException.class)
    public ProblemDetail documentsRequired(DocumentsRequiredException e) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setDetail(e.getMessage());
        return pd;
    }
}
