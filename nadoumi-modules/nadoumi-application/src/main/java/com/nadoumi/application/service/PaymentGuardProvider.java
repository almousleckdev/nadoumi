package com.nadoumi.application.service;

/**
 * Whether an application's payment of a given kind is settled. Implemented by the Payment module
 * ({@code nadoumi-payment}); this module owns the interface so it never depends on the implementation.
 *
 * <p>It backs {@code PAYMENT_SETTLED(kind)} in workflow guards and, directly, the submit gate in
 * {@link StudentApplicationService#submit}: an application whose fee is not settled cannot be submitted.
 * A kind with no fee configured counts as settled.</p>
 */
public interface PaymentGuardProvider {

    /** The Nadoumi application fee a student pays before submitting. */
    String APPLICATION_FEE = "APPLICATION_FEE";

    boolean isSettled(long applicationId, String kind);
}
