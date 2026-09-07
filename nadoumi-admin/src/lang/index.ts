import { createI18n } from 'vue-i18n'
import en from './en'
import zh from './zh'

// The admin console is English-only for now. The zh bundle is a stub; a locale
// switcher will return once it is fully translated. Pinned here so a stale
// `nadoumi-admin-locale=zh` in a browser can't leave the UI half-Chinese.
export const i18n = createI18n({
  legacy: false,
  locale: 'en',
  fallbackLocale: 'en',
  messages: { en, zh },
})

export function setLocale(locale: 'en' | 'zh') {
  i18n.global.locale.value = locale
}
