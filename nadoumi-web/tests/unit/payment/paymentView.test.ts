import { describe, expect, it } from 'vitest'
import { formatCountdown, isStartable } from '~/utils/paymentView'
import { TRANSACTION_NO_PATTERN } from '~/constants/payment'

describe('paymentView', () => {
  it('formats a countdown as mm:ss and never goes negative', () => {
    expect(formatCountdown(900)).toBe('15:00')
    expect(formatCountdown(65)).toBe('01:05')
    expect(formatCountdown(-4)).toBe('00:00')
  })

  it('offers a new payment only from the resting states that allow it', () => {
    for (const s of ['NOT_STARTED', 'REJECTED', 'EXPIRED', 'CANCELLED'] as const) expect(isStartable(s)).toBe(true)
    for (const s of ['PENDING', 'PROOF_SUBMITTED', 'CONFIRMED', 'NOT_REQUIRED'] as const) expect(isStartable(s)).toBe(false)
  })

  it('accepts bank transaction numbers and rejects short or spaced ones', () => {
    expect(TRANSACTION_NO_PATTERN.test('BANK1234567')).toBe(true)
    expect(TRANSACTION_NO_PATTERN.test('123')).toBe(false)
    expect(TRANSACTION_NO_PATTERN.test('has space 123456')).toBe(false)
  })
})
