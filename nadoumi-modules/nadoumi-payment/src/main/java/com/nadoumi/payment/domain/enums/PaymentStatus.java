package com.nadoumi.payment.domain.enums;

/** Lifecycle of one payment attempt ({@code nad_application_payment.status}). */
public enum PaymentStatus {
    /** The QR is open; the student has until {@code expires_at} to pay and report the payment. */
    PENDING,
    /** The student reported the payment; waiting for staff. */
    PROOF_SUBMITTED,
    /** Staff verified that the money arrived. */
    CONFIRMED,
    /** Staff refused the proof. */
    REJECTED,
    /** The window ran out before any proof was submitted. */
    EXPIRED,
    /** The student closed the session. */
    CANCELLED;

    /** A payment that still blocks starting another one for the same application. */
    public boolean isOpen() {
        return this == PENDING || this == PROOF_SUBMITTED;
    }
}
