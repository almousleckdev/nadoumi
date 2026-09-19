import { countryOptions, languageOptions } from '~/utils/countries'

/** Country and language choices named in the active locale; recomputed when it changes. */
export function useLocaleOptions() {
  const { locale } = useI18n()
  const countries = computed(() => countryOptions(locale.value))
  const languages = computed(() => languageOptions(locale.value))
  /** The name of a code in the active locale, or the code itself when unknown. */
  const labelIn = (options: { value: string, label: string }[], code: string | null | undefined) =>
    options.find(o => o.value === code)?.label ?? code ?? ''
  return {
    countries,
    languages,
    countryLabel: (code: string | null | undefined) => labelIn(countries.value, code),
    languageLabel: (code: string | null | undefined) => labelIn(languages.value, code),
  }
}
