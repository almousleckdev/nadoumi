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

    /** The Nadoumi service fee a student pays once admission is confirmed, before the admission letter and JW202 are released. */
    String SERVICE_FEE = "SERVICE_FEE";

    boolean isSettled(long applicationId, String kind);

    /**
     * The student has paid and reported it (transaction number and receipt) or staff already confirmed it. This is what
     * lets a student submit: Nadoumi staff verify the money afterwards, from the submitted application.
     */
    boolean isPaidOrReported(long applicationId, String kind);
}
