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

function displayNames(locale: string, type: 'region' | 'language'): Intl.DisplayNames | null {
  try {
    return new Intl.DisplayNames([locale], { type })
  }
  catch {
    return null
  }
}

function toOptions(codes: readonly string[], locale: string, type: 'region' | 'language'): Option[] {
  const names = displayNames(locale, type)
  return codes
    .map(value => ({ value, label: names?.of(value) ?? value }))
    .sort((a, b) => a.label.localeCompare(b.label, locale))
}

export const countryOptions = (locale: string): Option[] => toOptions(COUNTRY_CODES, locale, 'region')
export const languageOptions = (locale: string): Option[] => toOptions(LANGUAGE_CODES, locale, 'language')
export const isCountryCode = (code: string): boolean => COUNTRY_CODES.includes(code)
