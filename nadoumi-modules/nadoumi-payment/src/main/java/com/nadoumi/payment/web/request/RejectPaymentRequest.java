package com.nadoumi.payment.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Why a payment proof was not accepted; shown to the student. */
public record RejectPaymentRequest(@NotBlank @Size(min = 3, max = 300) String reason) {
}
