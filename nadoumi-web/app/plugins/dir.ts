// Maps a locale code to its writing direction. Named export so it is unit-testable
// without executing the plugin (Nuxt only consumes the default export).
export const dirFor = (code: string) => (code === 'ar' ? 'rtl' : 'ltr')

export default defineNuxtPlugin({
  name: 'nadoumi:dir',
  dependsOn: ['i18n:plugin'],
  setup(nuxtApp) {
    const i18n = nuxtApp.$i18n as { locale: Ref<string> }

    useHead({
      htmlAttrs: {
        lang: () => unref(i18n.locale),
        dir: () => dirFor(unref(i18n.locale)),
      },
    })

    if (import.meta.client) {
      watch(
        () => unref(i18n.locale),
        (code: string) => {
          document.documentElement.lang = code
          document.documentElement.dir = dirFor(code)
        },
        { immediate: true },
      )
    }
  },
})
