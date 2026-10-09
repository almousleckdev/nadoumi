/**
 * ISO 3166-1 alpha-2 country codes and ISO 639-1 language codes offered in the
 * profile forms. The code lists themselves come from `i18n-iso-countries` and
 * `iso-639-1` (never hand-maintained here); the display names come from
 * `Intl.DisplayNames` in the active locale, so no translated data files are
 * shipped and every locale is covered.
 */
import { getAlpha2Codes } from 'i18n-iso-countries'
import ISO6391 from 'iso-639-1'

export interface Option { value: string, label: string }

export const COUNTRY_CODES = Object.keys(getAlpha2Codes())
export const LANGUAGE_CODES = ISO6391.getAllCodes()

/**
 * ISO 639-1 also lists dead, liturgical and constructed languages (Avestan, Old Church Slavonic, Latin, Pali,
 * Volapük, Ido, Interlingua, Interlingue). Nobody has them as a native language, so they are not offered.
 */
const NOT_A_NATIVE_LANGUAGE = new Set(['ae', 'cu', 'la', 'pi', 'vo', 'io', 'ia', 'ie'])

/**
 * `fallback: 'none'` makes an unknown code yield undefined instead of echoing the code back. Browsers ship
 * different language data: Chrome has no name for Fula, Chechen and about fifty others, and the default
 * behaviour would list them as the bare codes "ff", "ce"...
 */
function displayNames(locale: string, type: 'region' | 'language'): Intl.DisplayNames | null {
  try {
    return new Intl.DisplayNames([locale], { type, fallback: 'none' })
  }
  catch {
    return null
  }
}

/** The browser's name in the active locale, else the ISO table's English name; never the bare code. */
function nameOf(names: Intl.DisplayNames | null, code: string, type: 'region' | 'language'): string | null {
  const local = names?.of(code)
  if (local && local.toLowerCase() !== code.toLowerCase()) return local
  const english = type === 'language' ? ISO6391.getName(code) : null
  return english || null
}

function toOptions(codes: readonly string[], locale: string, type: 'region' | 'language'): Option[] {
  const names = displayNames(locale, type)
  return codes
    .filter(code => type !== 'language' || !NOT_A_NATIVE_LANGUAGE.has(code))
    .flatMap((value) => {
      const label = nameOf(names, value, type)
      return label ? [{ value, label }] : []
    })
    .sort((a, b) => a.label.localeCompare(b.label, locale))
}

export const countryOptions = (locale: string): Option[] => toOptions(COUNTRY_CODES, locale, 'region')
export const languageOptions = (locale: string): Option[] => toOptions(LANGUAGE_CODES, locale, 'language')
export const isCountryCode = (code: string): boolean => COUNTRY_CODES.includes(code)
