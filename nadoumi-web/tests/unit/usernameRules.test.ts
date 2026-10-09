import { describe, it, expect } from 'vitest'
import { normalizeUsername, usernameProblem } from '~/utils/usernameRules'

describe('usernameProblem', () => {
  it('accepts a short lower-case handle with digits, dots and underscores', () => {
    for (const ok of ['ada', 'ada_l', 'ada.l9', '9lives', 'a'.repeat(20)]) expect(usernameProblem(ok)).toBeNull()
  })

  it('names what is wrong', () => {
    expect(usernameProblem('ab')).toBe('tooShort')
    expect(usernameProblem('a'.repeat(21))).toBe('tooLong')
    expect(usernameProblem('_ada')).toBe('characters')
    expect(usernameProblem('ada l')).toBe('characters')
    expect(usernameProblem('ada@x.com')).toBe('characters')
    expect(usernameProblem('admin')).toBe('reserved')
  })

  it('normalises case and surrounding space', () => {
    expect(normalizeUsername('  Ada_L ')).toBe('ada_l')
  })
})
