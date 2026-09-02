// Nadoumi public website + student experience.
// SSR for the SEO catalog pages; the Nitro server layer is the BFF that holds the
// student JWT in an httpOnly cookie (docs/FRONTEND_ARCHITECTURE.md §3.2).
export default defineNuxtConfig({
  compatibilityDate: '2025-01-01',
  future: { compatibilityVersion: 4 },
  ssr: true,
  modules: ['@nuxt/eslint', '@nuxtjs/tailwindcss', '@nuxt/fonts', '@nuxtjs/i18n'],
  css: ['~/assets/css/main.css'],
  // ui/ primitives are referenced unprefixed (e.g. <NDropdown>, <NSpinner>); keep
  // the directory flat in the component registry rather than the default Ui* prefix.
  components: [
    { path: '~/components/ui', pathPrefix: false },
    // auth/ components keep their file name as the tag (<AuthCaptcha>), not the
    // default path-prefixed <AuthAuthCaptcha>.
    { path: '~/components/auth', pathPrefix: false },
    // dashboard/ chrome is referenced unprefixed too (<DashboardShell>,
    // <ApplicantSwitcher>, <SectionCard>), matching the ui/ and auth/ entries.
    { path: '~/components/dashboard', pathPrefix: false },
    '~/components',
  ],
  fonts: {
    families: [
      { name: 'Plus Jakarta Sans', provider: 'google', weights: [600, 700] },
      { name: 'Inter', provider: 'google', weights: [400, 500, 600] },
      { name: 'Noto Sans Arabic', provider: 'google', weights: [400, 600] },
    ],
  },
  tailwindcss: { cssPath: '~/assets/css/main.css' },
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
    // fallbackLocale lives in i18n/i18n.config.ts (the vue-i18n config); the
    // @nuxtjs/i18n@9 module options type does not accept it at this level.
    strategy: 'prefix_except_default',
    locales: [
      { code: 'en', language: 'en', file: 'en.json', name: 'English' },
      { code: 'fr', language: 'fr', file: 'fr.json', name: 'Français' },
      { code: 'ar', language: 'ar', file: 'ar.json', dir: 'rtl', name: 'العربية' },
      { code: 'zh', language: 'zh-CN', file: 'zh.json', name: '中文' },
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
