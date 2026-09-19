/**
 * ISO 3166-1 alpha-2 country codes and ISO 639-1 language codes offered in the
 * profile forms. Names come from `Intl.DisplayNames` in the active locale, so no
 * translated data files are shipped and every locale is covered.
 */
export interface Option { value: string, label: string }

const COUNTRY_CODES = [
  'AF', 'AL', 'DZ', 'AD', 'AO', 'AG', 'AR', 'AM', 'AU', 'AT', 'AZ', 'BS', 'BH', 'BD', 'BB', 'BY', 'BE', 'BZ', 'BJ', 'BT',
  'BO', 'BA', 'BW', 'BR', 'BN', 'BG', 'BF', 'BI', 'CV', 'KH', 'CM', 'CA', 'CF', 'TD', 'CL', 'CN', 'CO', 'KM', 'CG', 'CD',
  'CR', 'CI', 'HR', 'CU', 'CY', 'CZ', 'DK', 'DJ', 'DM', 'DO', 'EC', 'EG', 'SV', 'GQ', 'ER', 'EE', 'SZ', 'ET', 'FJ', 'FI',
  'FR', 'GA', 'GM', 'GE', 'DE', 'GH', 'GR', 'GD', 'GT', 'GN', 'GW', 'GY', 'HT', 'HN', 'HK', 'HU', 'IS', 'IN', 'ID', 'IR',
  'IQ', 'IE', 'IL', 'IT', 'JM', 'JP', 'JO', 'KZ', 'KE', 'KI', 'KW', 'KG', 'LA', 'LV', 'LB', 'LS', 'LR', 'LY', 'LI', 'LT',
  'LU', 'MO', 'MG', 'MW', 'MY', 'MV', 'ML', 'MT', 'MH', 'MR', 'MU', 'MX', 'FM', 'MD', 'MC', 'MN', 'ME', 'MA', 'MZ', 'MM',
  'NA', 'NR', 'NP', 'NL', 'NZ', 'NI', 'NE', 'NG', 'KP', 'MK', 'NO', 'OM', 'PK', 'PW', 'PS', 'PA', 'PG', 'PY', 'PE', 'PH',
  'PL', 'PT', 'QA', 'RO', 'RU', 'RW', 'KN', 'LC', 'VC', 'WS', 'SM', 'ST', 'SA', 'SN', 'RS', 'SC', 'SL', 'SG', 'SK', 'SI',
  'SB', 'SO', 'ZA', 'KR', 'SS', 'ES', 'LK', 'SD', 'SR', 'SE', 'CH', 'SY', 'TW', 'TJ', 'TZ', 'TH', 'TL', 'TG', 'TO', 'TT',
  'TN', 'TR', 'TM', 'TV', 'UG', 'UA', 'AE', 'GB', 'US', 'UY', 'UZ', 'VU', 'VA', 'VE', 'VN', 'YE', 'ZM', 'ZW',
] as const

const LANGUAGE_CODES = [
  'ar', 'bn', 'bg', 'my', 'zh', 'hr', 'cs', 'da', 'nl', 'en', 'et', 'fi', 'fr', 'ka', 'de', 'el', 'gu', 'ha', 'he', 'hi',
  'hu', 'id', 'it', 'ja', 'kk', 'km', 'ko', 'ku', 'ky', 'lo', 'lv', 'lt', 'mk', 'ms', 'mn', 'ne', 'no', 'fa', 'pl', 'pt',
  'pa', 'ro', 'ru', 'sr', 'si', 'sk', 'sl', 'so', 'es', 'sw', 'sv', 'tl', 'ta', 'te', 'th', 'tr', 'tk', 'uk', 'ur', 'uz',
  'vi', 'yo', 'zu', 'am', 'az', 'be', 'sq', 'hy', 'af', 'ps', 'ti', 'ln', 'ig', 'rw', 'mg',
] as const

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
export const isCountryCode = (code: string): boolean => (COUNTRY_CODES as readonly string[]).includes(code)
