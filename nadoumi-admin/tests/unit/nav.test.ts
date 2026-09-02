import { describe, it, expect } from 'vitest'
import { NAV, implementedPaths, type NavItem } from '@/config/nav'
import en from '@/lang/en'

const allItems: NavItem[] = NAV.flatMap(g => g.items)

describe('nav manifest', () => {
  it('exposes exactly the two built screens as implemented routes', () => {
    expect(implementedPaths().sort()).toEqual(['/applicants', '/dashboard'])
  })

  it('every item is either implemented or planned, with a path and an icon', () => {
    for (const item of allItems) {
      expect(['implemented', 'planned']).toContain(item.status)
      expect(item.path.startsWith('/')).toBe(true)
      expect(item.icon.length).toBeGreaterThan(0)
    }
  })

  it('paths are unique', () => {
    const paths = allItems.map(i => i.path)
    expect(new Set(paths).size).toBe(paths.length)
  })

  it('every item and group has a matching i18n label', () => {
    for (const g of NAV) {
      if (g.key) expect(en.nav.groups[g.key as keyof typeof en.nav.groups]).toBeTruthy()
    }
    for (const item of allItems) {
      expect(en.nav.items[item.key as keyof typeof en.nav.items]).toBeTruthy()
    }
  })

  it('planned items still carry a permission token so they gate correctly', () => {
    for (const item of allItems.filter(i => i.status === 'planned')) {
      expect(item.perm, `${item.key} needs a perm`).toBeTruthy()
    }
  })
})
