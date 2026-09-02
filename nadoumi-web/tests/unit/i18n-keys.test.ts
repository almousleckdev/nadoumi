import { describe, it, expect } from 'vitest'
import enRaw from '~~/i18n/locales/en.json?raw'
import frRaw from '~~/i18n/locales/fr.json?raw'
import arRaw from '~~/i18n/locales/ar.json?raw'
import zhRaw from '~~/i18n/locales/zh.json?raw'

const en = JSON.parse(enRaw) as Record<string, unknown>
const fr = JSON.parse(frRaw) as Record<string, unknown>
const ar = JSON.parse(arRaw) as Record<string, unknown>
const zh = JSON.parse(zhRaw) as Record<string, unknown>

function keys(o: Record<string, unknown>, prefix = ''): string[] {
  return Object.entries(o).flatMap(([k, v]) =>
    v && typeof v === 'object'
      ? keys(v as Record<string, unknown>, `${prefix}${k}.`)
      : [`${prefix}${k}`],
  )
}

describe('i18n locale files', () => {
  it('fr, ar, zh have exactly the same key set as en', () => {
    const base = new Set(keys(en))
    for (const [name, loc] of [['fr', fr], ['ar', ar], ['zh', zh]] as const) {
      const got = new Set(keys(loc as Record<string, unknown>))
      expect({ name, missing: [...base].filter(k => !got.has(k)) }).toEqual({ name, missing: [] })
      expect({ name, extra: [...got].filter(k => !base.has(k)) }).toEqual({ name, extra: [] })
    }
  })
})
