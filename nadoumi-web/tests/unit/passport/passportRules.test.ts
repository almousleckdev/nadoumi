import { describe, it, expect } from 'vitest'
import { addMonths } from '~/utils/dates'
import { isValidForAdmission, validatePassport, type PassportForm } from '~/utils/passportRules'

const TODAY = new Date(2026, 8, 19)
const valid: PassportForm = { passportNo: 'P1234567', issueDate: '2024-01-01', expiryDate: '2034-01-01' }

describe('six-month validity', () => {
  it('accepts a passport valid for more than six months', () => {
    expect(isValidForAdmission('2027-03-20', TODAY)).toBe(true)
  })

  it('rejects one that is exactly six months away, or sooner, or missing', () => {
    expect(isValidForAdmission('2027-03-19', TODAY)).toBe(false)
    expect(isValidForAdmission('2026-12-01', TODAY)).toBe(false)
    expect(isValidForAdmission('', TODAY)).toBe(false)
  })

  it('clamps to the end of the month like the server', () => {
    expect(addMonths(new Date(2026, 7, 31), 6)).toBe('2027-02-28')
  })
})

describe('validatePassport', () => {
  const check = (over: Partial<PassportForm> = {}) => validatePassport({ ...valid, ...over }, TODAY)

  it('has no errors for a valid passport', () => {
    expect(check()).toEqual({})
  })

  it('flags every missing field, and never asks for a name or birth date', () => {
    expect(Object.keys(check({ passportNo: '', issueDate: '', expiryDate: '' })).sort())
      .toEqual(['expiryDate', 'issueDate', 'passportNo'])
  })

  it('rejects a malformed number', () => {
    expect(check({ passportNo: 'AB-1' }).passportNo).toBe('numberInvalid')
  })

  it('rejects a future issue date and an expiry not after issue', () => {
    expect(check({ issueDate: '2026-09-20' }).issueDate).toBe('issueFuture')
    expect(check({ expiryDate: '2024-01-01' }).expiryDate).toBe('expiryBeforeIssue')
  })

  it('rejects a passport that does not stay valid for more than six months', () => {
    expect(check({ expiryDate: '2027-01-01' }).expiryDate).toBe('expiryTooSoon')
  })
})
