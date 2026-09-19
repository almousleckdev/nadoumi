import { countryOptions, languageOptions } from '~/utils/countries'

/** Country and language choices named in the active locale; recomputed when it changes. */
export function useLocaleOptions() {
  const { locale } = useI18n()
  return {
    countries: computed(() => countryOptions(locale.value)),
    languages: computed(() => languageOptions(locale.value)),
  }
}
