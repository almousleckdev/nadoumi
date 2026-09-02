// Nadoumi public website + student experience.
// SSR for the SEO catalog pages; the Nitro server layer is the BFF that holds the
// student JWT in an httpOnly cookie (docs/FRONTEND_ARCHITECTURE.md §3.2).
export default defineNuxtConfig({
  compatibilityDate: '2025-01-01',
  future: { compatibilityVersion: 4 },
  ssr: true,
  modules: ['@nuxt/eslint', '@nuxtjs/i18n'],
  css: ['~/assets/css/main.css'],
  runtimeConfig: {
    // server-only: where the BFF forwards /api/** calls.
    // Override at deploy time with NUXT_BACKEND_BASE_URL.
    backendBaseUrl: 'http://localhost:8080',
    public: {
      siteName: 'Nadoumi',
    },
  },
  i18n: {
    defaultLocale: 'en',
    strategy: 'prefix_except_default',
    locales: [
      { code: 'en', language: 'en', file: 'en.json' },
      { code: 'fr', language: 'fr', file: 'fr.json' },
      { code: 'ar', language: 'ar', file: 'ar.json', dir: 'rtl' },
      { code: 'zh', language: 'zh-CN', file: 'zh.json' },
    ],
  },
  app: {
    head: {
      htmlAttrs: { lang: 'en' },
      titleTemplate: '%s · Nadoumi',
      meta: [{ name: 'viewport', content: 'width=device-width, initial-scale=1' }],
    },
  },
})
