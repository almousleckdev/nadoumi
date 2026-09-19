import { describe, it, expect } from 'vitest'
import { expandMrzDate, parsePassportMrz } from '~/services/passport/mrz'

// ICAO 9303 specimen passport (issuing state "UTO" is fictitious)
const LINE1 = 'P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<'
const LINE2 = 'L898902C36UTO7408122F1204159ZE184226B<<<<<10'
const TODAY = new Date(2026, 8, 19)

describe('parsePassportMrz', () => {
  it('reads the holder data from a valid machine-readable zone', () => {
    expect(parsePassportMrz(`${LINE1}\n${LINE2}`, TODAY)).toMatchObject({
      documentNumber: 'L898902C3',
      surname: 'ERIKSSON',
      givenNames: 'ANNA MARIA',
      dateOfBirth: '1974-08-12',
      expiryDate: '2012-04-15',
    })
  })

  it('finds the zone among other OCR text, spaces and lowercase noise', () => {
    const noisy = `REPUBLIC OF UTOPIA\npassport no\n${LINE1.toLowerCase()}\n${LINE2.slice(0, 20)} ${LINE2.slice(20)}\n`

    expect(parsePassportMrz(noisy, TODAY)?.surname).toBe('ERIKSSON')
  })

  it('reads the filler when OCR turns "<" into a lookalike', () => {
    expect(parsePassportMrz(`${LINE1.replaceAll('<', '«')}\n${LINE2}`, TODAY)?.givenNames).toBe('ANNA MARIA')
  })

  it('corrects letter/digit confusions in the numeric fields', () => {
    const misread = 'L898902C36UTO74O8122F12O4159ZE184226B<<<<<10'

    expect(parsePassportMrz(`${LINE1}\n${misread}`, TODAY)?.dateOfBirth).toBe('1974-08-12')
  })

  it('returns null rather than data when a check digit does not validate', () => {
    const tampered = 'L898902C37UTO7408122F1204159ZE184226B<<<<<10'

    expect(parsePassportMrz(`${LINE1}\n${tampered}`, TODAY)).toBeNull()
  })

  it('returns null for text with no machine-readable zone', () => {
    expect(parsePassportMrz('just some words\nand another line', TODAY)).toBeNull()
    expect(parsePassportMrz('', TODAY)).toBeNull()
  })
})

describe('expandMrzDate', () => {
  it('never puts a birth date in the future', () => {
    expect(expandMrzDate('080101', 'birth', TODAY)).toBe('2008-01-01')
    expect(expandMrzDate('300101', 'birth', TODAY)).toBe('1930-01-01')
  })

  it('always puts an expiry date in this century', () => {
    expect(expandMrzDate('350101', 'expiry', TODAY)).toBe('2035-01-01')
  })
})
