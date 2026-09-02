import { describe, it, expect } from 'vitest'
import { authErrorMessage } from '~/utils/authErrors'

// identity translator: returns the key so assertions can check the mapping
const t = (k: string) => k

describe('authErrorMessage', () => {
  it('passes a client-side validation key straight through', () => {
    expect(authErrorMessage('validation.password.tooShort', t)).toBe('validation.password.tooShort')
    expect(authErrorMessage('errors.captcha', t)).toBe('errors.captcha')
  })

  it('maps a network failure (no status)', () => {
    expect(authErrorMessage(Object.assign(new Error('fetch failed'), { name: 'FetchError' }), t)).toBe('errors.network')
    expect(authErrorMessage(new TypeError('Failed to fetch'), t)).toBe('errors.network')
  })

  it('maps rate limiting and server faults by status', () => {
    expect(authErrorMessage({ statusCode: 429 }, t)).toBe('errors.rateLimited')
    expect(authErrorMessage({ statusCode: 502, data: { detail: 'x' } }, t)).toBe('errors.server')
  })

  it('turns a backend password-policy key into a validation key', () => {
    expect(authErrorMessage({ statusCode: 400, data: { detail: 'password.needSpecial' } }, t))
      .toBe('validation.password.needSpecial')
  })

  it('maps known backend detail phrases', () => {
    const cases: [string, string][] = [
      ['email already registered', 'errors.emailTaken'],
      ['email or password is incorrect', 'errors.badCredentials'],
      ['current password is incorrect', 'errors.currentPasswordWrong'],
      ['verification code is invalid or expired', 'errors.otpInvalid'],
      ['verification ticket is invalid or expired', 'errors.otpExpired'],
      ['email verification does not match this address', 'errors.otpEmailMismatch'],
      ['student registration is disabled', 'errors.registrationDisabled'],
      ['Captcha code error', 'errors.captcha'],
    ]
    for (const [detail, key] of cases) {
      expect(authErrorMessage({ statusCode: 400, data: { detail } }, t)).toBe(key)
    }
  })

  it('falls back to a generic message for anything unrecognised', () => {
    expect(authErrorMessage({ statusCode: 400, data: { detail: 'something odd' } }, t)).toBe('errors.validation')
    expect(authErrorMessage({}, t)).toBe('errors.generic')
    expect(authErrorMessage(undefined, t)).toBe('errors.generic')
  })
})
