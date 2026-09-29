import { describe, it, expect } from 'vitest'
import { numberOrNull, textOrNull } from '@/utils/formValues'

describe('numberOrNull', () => {
  it('returns null for empty input', () => {
    expect(numberOrNull('')).toBeNull()
    expect(numberOrNull(null)).toBeNull()
    expect(numberOrNull(undefined)).toBeNull()
  })

  it('converts numeric strings and keeps numbers, including zero', () => {
    expect(numberOrNull('42')).toBe(42)
    expect(numberOrNull(7.5)).toBe(7.5)
    expect(numberOrNull(0)).toBe(0)
  })
})

describe('textOrNull', () => {
  it('returns null for blank input', () => {
    expect(textOrNull('')).toBeNull()
    expect(textOrNull('   ')).toBeNull()
  })

  it('trims surrounding whitespace', () => {
    expect(textOrNull('  Beijing ')).toBe('Beijing')
  })
})
