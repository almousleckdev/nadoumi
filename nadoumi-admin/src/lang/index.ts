import { createI18n } from 'vue-i18n'
import en from './en'
import zh from './zh'

const STORAGE_KEY = 'nadoumi-admin-locale'

export const i18n = createI18n({
  legacy: false,
  locale: localStorage.getItem(STORAGE_KEY) || 'en',
  fallbackLocale: 'en',
  messages: { en, zh },
})

export function setLocale(locale: 'en' | 'zh') {
  i18n.global.locale.value = locale
  localStorage.setItem(STORAGE_KEY, locale)
}
