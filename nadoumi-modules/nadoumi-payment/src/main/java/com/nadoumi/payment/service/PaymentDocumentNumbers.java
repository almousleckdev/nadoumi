package com.nadoumi.payment.service;

import com.nadoumi.payment.domain.ApplicationPayment;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Document numbers are derived from ids that already exist, so they are unique without a second counter and the
 * same payment always gets the same number: {@code NAD-RCT-2026-000007}, {@code NAD-APP-2026-000042}.
 */
final class PaymentDocumentNumbers {

    private PaymentDocumentNumbers() {
    }

    static String receipt(ApplicationPayment payment) {
        return number("NAD-RCT", year(payment), payment.getId());
    }

    static String applicationForm(ApplicationPayment payment) {
        return number("NAD-APP", year(payment), payment.getApplicationId());
    }

    private static int year(ApplicationPayment payment) {
        LocalDateTime at = payment.getVerifiedAt() != null ? payment.getVerifiedAt() : payment.getCreateTime();
        return at.getYear();
    }

    private static String number(String prefix, int year, long id) {
        return String.format(Locale.ROOT, "%s-%d-%06d", prefix, year, id);
    }
}
