import type { PaymentState } from '~/types/payment'

const SECONDS_PER_MINUTE = 60
const PAD = 2

/** "14:32" for a countdown; never negative. */
export function formatCountdown(totalSeconds: number): string {
  const safe = Math.max(0, Math.floor(totalSeconds))
  const minutes = Math.floor(safe / SECONDS_PER_MINUTE)
  const seconds = safe % SECONDS_PER_MINUTE
  return `${String(minutes).padStart(PAD, '0')}:${String(seconds).padStart(PAD, '0')}`
}

/** The states in which the student can open (or reopen) the payment QR. */
const STARTABLE: readonly PaymentState[] = ['NOT_STARTED', 'REJECTED', 'EXPIRED', 'CANCELLED']

export function isStartable(state: PaymentState): boolean {
  return STARTABLE.includes(state)
}

/** The last stretch of the window, when the countdown should draw attention. */
export const URGENT_SECONDS = 60
