import { describe, it, expect } from 'vitest'
import type { SelfApplicantBody } from '~/composables/useApplicant'
import { dobProblem, isValidName, latestEligibleDob, normaliseName, validateProfile } from '~/utils/profileRules'

const TODAY = new Date(2026, 8, 19)

describe('date of birth rules', () => {
  it('caps the picker at exactly 17 years ago', () => {
    expect(latestEligibleDob(TODAY)).toBe('2009-09-19')
  })

  it('accepts someone who turns 17 today', () => {
    expect(dobProblem('2009-09-19', TODAY)).toBeNull()
  })

  it('rejects someone who turns 17 tomorrow', () => {
    expect(dobProblem('2009-09-20', TODAY)).toBe('underage')
  })

  it('rejects a future date and an empty one', () => {
    expect(dobProblem('2026-09-20', TODAY)).toBe('future')
    expect(dobProblem('', TODAY)).toBe('required')
  })
})

describe('name rules', () => {
  it('accepts letters, spaces, hyphens and apostrophes', () => {
    expect(isValidName("O'Brien-Smith")).toBe(true)
    expect(isValidName('Zoë')).toBe(true)
  })

  it('rejects digits and symbols', () => {
    expect(isValidName('john2')).toBe(false)
    expect(isValidName('john@doe')).toBe(false)
  })

  it('normalises like the server: trim, collapse spaces, UPPERCASE', () => {
    expect(normaliseName('  mary   ann ')).toBe('MARY ANN')
    expect(normaliseName('o’brien')).toBe('O\'BRIEN')
  })
})

describe('validateProfile', () => {
  const valid: SelfApplicantBody = {
    givenName: 'Ada', familyName: 'Lovelace', dob: '2000-01-01', nationality: 'GB', email: 'a@b.co',
    phone: '+441', gender: 'FEMALE', countryOfOrigin: 'GB', countryOfResidence: 'CN', nativeLanguage: 'en',
    whatsapp: '+441',
  }
  const check = (over: Partial<SelfApplicantBody> = {}, emailNeedsVerification = false) =>
    validateProfile({ ...valid, ...over }, { emailNeedsVerification, today: TODAY })

  it('has no errors for a complete profile', () => {
    expect(check()).toEqual({})
  })

  it('flags every missing required field', () => {
    const errors = check({
      givenName: '', familyName: ' ', dob: '', gender: '', nationality: '', countryOfOrigin: '',
      countryOfResidence: '', nativeLanguage: '', phone: '', whatsapp: '', email: '',
    })
    expect(Object.keys(errors).sort()).toEqual([
      'contactHandle', 'countryOfOrigin', 'countryOfResidence', 'dob', 'email', 'familyName',
      'gender', 'givenName', 'nationality', 'nativeLanguage', 'phone',
    ].sort())
    expect(Object.values(errors).every(c => c === 'required' || c === 'contactHandleRequired')).toBe(true)
  })

  it('reports an invalid name, an underage or future date, and an unverified email', () => {
    expect(check({ givenName: 'john2' }).givenName).toBe('nameInvalid')
    expect(check({ dob: '2015-01-01' }).dob).toBe('dobUnderage')
    expect(check({ dob: '2027-01-01' }).dob).toBe('dobFuture')
    expect(check({}, true).email).toBe('verifyBeforeSave')
  })

  it('accepts WeChat instead of WhatsApp', () => {
    expect(check({ whatsapp: '', wechatId: 'wx_ada' }).contactHandle).toBeUndefined()
  })
})
