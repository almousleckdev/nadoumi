import { createI18n } from 'vue-i18n'
import en from '@/lang/en'

// A fresh, non-persisting i18n instance for component tests (the app's own
// src/lang/index.ts touches localStorage and is shared mutable state).
export function testI18n() {
  return createI18n({ legacy: false, locale: 'en', fallbackLocale: 'en', messages: { en } })
}

export function mountOpts() {
  return { global: { plugins: [testI18n()] } }
}
