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

  it('never lists a bare language code, even when the browser has no name for it (Chrome lacks Fula, Chechen...)', () => {
    const real = Intl.DisplayNames
    class Sparse {
      constructor(private readonly locales: string[], private readonly opts: Intl.DisplayNamesOptions) {}
      of(code: string) { return ['ff', 'ce', 'ab'].includes(code) ? undefined : new real(this.locales, this.opts).of(code) }
    }
    ;(Intl as unknown as { DisplayNames: unknown }).DisplayNames = Sparse
    try {
      const options = languageOptions('en')
      expect(options.filter(o => o.label.toLowerCase() === o.value.toLowerCase())).toEqual([])
      expect(options.find(o => o.value === 'ff')?.label).toBe('Fula')
      expect(options.find(o => o.value === 'ce')?.label).toBe('Chechen')
    }
    finally {
      ;(Intl as unknown as { DisplayNames: unknown }).DisplayNames = real
    }
  })

  it('leaves out dead, liturgical and constructed languages nobody speaks natively', () => {
    const codes = languageOptions('en').map(o => o.value)
    for (const dead of ['ae', 'cu', 'la', 'pi', 'vo', 'io', 'ia', 'ie']) expect(codes).not.toContain(dead)
    for (const real of ['fr', 'ar', 'en', 'zh', 'es', 'ff', 'wo', 'sw']) expect(codes).toContain(real)
  })
})
