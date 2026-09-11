import { describe, it, expect } from 'vitest'
import enRaw from '~~/i18n/locales/en.json?raw'
import frRaw from '~~/i18n/locales/fr.json?raw'
import esRaw from '~~/i18n/locales/es.json?raw'
import arRaw from '~~/i18n/locales/ar.json?raw'
import zhRaw from '~~/i18n/locales/zh.json?raw'

const en = JSON.parse(enRaw) as Record<string, unknown>
const fr = JSON.parse(frRaw) as Record<string, unknown>
const es = JSON.parse(esRaw) as Record<string, unknown>
const ar = JSON.parse(arRaw) as Record<string, unknown>
const zh = JSON.parse(zhRaw) as Record<string, unknown>

function keys(o: Record<string, unknown>, prefix = ''): string[] {
  return Object.entries(o).flatMap(([k, v]) =>
    v && typeof v === 'object'
      ? keys(v as Record<string, unknown>, `${prefix}${k}.`)
      : [`${prefix}${k}`],
  )
}

function flatten(o: Record<string, unknown>, prefix = ''): Record<string, string> {
  const out: Record<string, string> = {}
  for (const [k, v] of Object.entries(o)) {
    const key = prefix ? `${prefix}.${k}` : k
    if (v && typeof v === 'object') Object.assign(out, flatten(v as Record<string, unknown>, key))
    else if (typeof v === 'string') out[key] = v
  }
  return out
}

const TOKEN = /\{[^}]*\}/g
const locales = { fr, es, ar, zh } as const

describe('i18n locale files', () => {
  it('fr, es, ar, zh have exactly the same key set as en', () => {
    const base = new Set(keys(en))
    for (const [name, loc] of Object.entries(locales)) {
      const got = new Set(keys(loc as Record<string, unknown>))
      expect({ name, missing: [...base].filter(k => !got.has(k)) }).toEqual({ name, missing: [] })
      expect({ name, extra: [...got].filter(k => !base.has(k)) }).toEqual({ name, extra: [] })
    }
  })

  it('every {placeholder} in en survives translation, unchanged, in every locale', () => {
    const enFlat = flatten(en)
    for (const [name, loc] of Object.entries(locales)) {
      const locFlat = flatten(loc as Record<string, unknown>)
      const mismatches = Object.keys(enFlat)
        .map(k => ({
          key: k,
          en: [...(enFlat[k]?.match(TOKEN) ?? [])].sort(),
          got: [...(locFlat[k]?.match(TOKEN) ?? [])].sort(),
        }))
        .filter(({ en: e, got }) => JSON.stringify(e) !== JSON.stringify(got))
      expect({ name, mismatches }).toEqual({ name, mismatches: [] })
    }
  })

  // Project rule: never use " - " (space-hyphen-space) as a separator in UI copy.
  it('no locale uses " - " as a separator', () => {
    for (const [name, loc] of Object.entries({ en, ...locales })) {
      const hits = Object.entries(flatten(loc as Record<string, unknown>))
        .filter(([, v]) => v.includes(' - '))
        .map(([k]) => k)
      expect({ name, hits }).toEqual({ name, hits: [] })
    }
  })
})
