package com.nadoumi.payment.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps this module's own exception to the consistent problem response. */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RestControllerAdvice(basePackages = "com.nadoumi.payment")
public class PaymentApiExceptionHandler {

    @ExceptionHandler(PaymentConflictException.class)
    public ProblemDetail conflict(PaymentConflictException e) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setDetail(e.getMessage());
        return pd;
    }
}
