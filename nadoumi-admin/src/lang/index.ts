import { createI18n } from 'vue-i18n'
import en from './en'
import zh from './zh'

export type AdminLocale = 'en' | 'zh'
const STORAGE_KEY = 'nadoumi-admin-locale'

function initialLocale(): AdminLocale {
  if (typeof window === 'undefined') return 'en'
  const stored = window.localStorage.getItem(STORAGE_KEY)
  return stored === 'zh' ? 'zh' : 'en'
}

// English default, Chinese the only other supported locale (CLAUDE.local.md /
// docs/DEVELOPMENT_GUIDELINES.md §4). The choice persists across sessions via
// localStorage so a reload keeps the console in the language the user picked.
export const i18n = createI18n({
  legacy: false,
  locale: initialLocale(),
  fallbackLocale: 'en',
  messages: { en, zh },
})

export function setLocale(locale: AdminLocale) {
  i18n.global.locale.value = locale
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(STORAGE_KEY, locale)
  }
}
