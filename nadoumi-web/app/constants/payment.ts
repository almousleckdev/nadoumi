/** Limits for the payment screenshot; they mirror the server's PAYMENT_PROOF media category. */
export const PAYMENT_PROOF_MAX_MB = 5
export const PAYMENT_PROOF_ACCEPT = 'image/jpeg,image/png,application/pdf'
export const PAYMENT_PROOF_MIME = /^(image\/(jpeg|png)|application\/pdf)$/

/** The bank / wallet transaction number a student types from their receipt. */
export const TRANSACTION_NO_PATTERN = /^[A-Za-z0-9_-]{6,40}$/
