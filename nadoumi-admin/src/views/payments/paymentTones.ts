import type { PaymentStatus } from '@/api/payment'

type Tone = 'neutral' | 'info' | 'success' | 'warning' | 'danger'

/** StatusBadge tones for payment statuses; PROOF_SUBMITTED is the one waiting on staff. */
export const PAYMENT_TONES: Record<PaymentStatus, Tone> = {
  PENDING: 'neutral',
  PROOF_SUBMITTED: 'warning',
  CONFIRMED: 'success',
  REJECTED: 'danger',
  EXPIRED: 'neutral',
  CANCELLED: 'neutral',
}
