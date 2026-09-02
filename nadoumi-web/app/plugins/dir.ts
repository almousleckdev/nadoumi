export default defineNuxtPlugin(() => {
  const { locale } = useI18n()
  const dirFor = (code: string) => (code === 'ar' ? 'rtl' : 'ltr')

  useHead({ htmlAttrs: { lang: () => locale.value, dir: () => dirFor(locale.value) } })

  if (import.meta.client) {
    watch(locale, (code: string) => {
      document.documentElement.lang = code
      document.documentElement.dir = dirFor(code)
    }, { immediate: true })
  }
})
