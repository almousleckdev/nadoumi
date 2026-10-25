package com.nadoumi.payment.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Why a payment is being deleted for good; kept in the operation log. */
public record DeletePaymentRequest(@NotBlank @Size(min = 5, max = 300) String reason) {
}
