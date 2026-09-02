import { describe, it, expect } from 'vitest'
import { passwordPolicyKey, passwordChecks } from '~/utils/passwordPolicy'

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
})

describe('passwordChecks', () => {
  it('reports a strong password with all rules passing', () => {
    const r = passwordChecks('Abcdef1!')
    expect(r.strong).toBe(true)
    expect(r.firstError).toBeNull()
    expect(r.rules.every(x => x.ok)).toBe(true)
  })

  it('adds a personal-term rule only when forbidden terms are supplied', () => {
    expect(passwordChecks('Abcdef1!').rules.some(x => x.key === 'validation.password.noPersonal')).toBe(false)
    const r = passwordChecks('Lovelace1!', { forbidden: ['Ada', 'Lovelace'] })
    expect(r.strong).toBe(false)
    expect(r.firstError).toBe('validation.password.noPersonal')
  })

  it('reports a confirm mismatch last', () => {
    const r = passwordChecks('Abcdef1!', { confirm: 'Abcdef2@' })
    expect(r.strong).toBe(true) // policy itself is fine
    expect(r.firstError).toBe('validation.password.mismatch')
  })

  it('short forbidden terms (<3 chars) are ignored', () => {
    expect(passwordChecks('Abcdef1!', { forbidden: ['Ab'] }).firstError).toBeNull()
  })
})
