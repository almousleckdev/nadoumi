import { describe, it, expect } from 'vitest'
import { passwordPolicyKey } from '~/utils/passwordPolicy'

describe('passwordPolicyKey', () => {
  it('accepts a compliant password', () => {
    expect(passwordPolicyKey('Abcdef1!')).toBeNull()
  })

  it('flags each rule in order', () => {
    expect(passwordPolicyKey('Ab1!')).toBe('validation.password.tooShort')
    expect(passwordPolicyKey('a'.repeat(33) + 'B1!')).toBe('validation.password.tooLong')
    expect(passwordPolicyKey('abcdef1!')).toBe('validation.password.needUpper')
    expect(passwordPolicyKey('ABCDEF1!')).toBe('validation.password.needLower')
    expect(passwordPolicyKey('Abcdefg!')).toBe('validation.password.needDigit')
    expect(passwordPolicyKey('Abcdefg1')).toBe('validation.password.needSpecial')
  })

  it('rejects reuse of the current password', () => {
    expect(passwordPolicyKey('Abcdef1!', 'Abcdef1!')).toBe('validation.password.sameAsCurrent')
  })

  it('allows a different new password against a current one', () => {
    expect(passwordPolicyKey('Zxcvbn2@', 'Abcdef1!')).toBeNull()
  })
})
