package com.nadoumi.application.service;

/**
 * Backs {@code PAYMENT_SETTLED(kind)}. No bean exists until the Payment module
 * (platform Step 9) ships — {@link GuardEvaluator#activate} rejects any definition
 * that references it while this is unbacked (DA3, fail-closed). The seeded
 * definitions gate on a recorded {@code FEE_SETTLED} decision instead.
 */
public interface PaymentGuardProvider {

    boolean isSettled(long applicationId, String kind);
}
