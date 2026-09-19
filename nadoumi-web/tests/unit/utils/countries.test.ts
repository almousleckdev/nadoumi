import { describe, it, expect } from 'vitest'
import { countryOptions, isCountryCode, languageOptions } from '~/utils/countries'

describe('countries and languages', () => {
  it('lists countries with localised names, sorted', () => {
    const en = countryOptions('en')
    expect(en.find(o => o.value === 'CN')?.label).toBe('China')
    expect(en.map(o => o.label)).toEqual([...en.map(o => o.label)].sort((a, b) => a.localeCompare(b, 'en')))
  })

  it('localises the names', () => {
    expect(countryOptions('fr').find(o => o.value === 'EG')?.label).toBe('Égypte')
  })

  it('has unique codes', () => {
    const codes = countryOptions('en').map(o => o.value)
    expect(new Set(codes).size).toBe(codes.length)
  })

  it('lists languages', () => {
    expect(languageOptions('en').find(o => o.value === 'ar')?.label).toBe('Arabic')
    expect(languageOptions('en').find(o => o.value === 'zh')).toBeTruthy()
  })

  it('recognises a valid country code only', () => {
    expect(isCountryCode('CN')).toBe(true)
    expect(isCountryCode('XX')).toBe(false)
  })
})
