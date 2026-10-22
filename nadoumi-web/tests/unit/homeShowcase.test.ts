import { describe, expect, it } from 'vitest'
import { PROGRAM_TABS, SCHOLARSHIP_TABS } from '~/data/homeShowcase'

describe('home showcase tabs', () => {
  it('covers every scholarship category the home page promises', () => {
    expect(SCHOLARSHIP_TABS.map(tab => tab.key)).toEqual(['featured', 'recommended', 'hot', 'full', 'partial', 'self'])
  })

  it('maps each funding tab to the matching funding filter', () => {
    const funding = Object.fromEntries(SCHOLARSHIP_TABS.map(tab => [tab.key, tab.query.funding]))
    expect(funding).toMatchObject({ full: 'FULLY', partial: 'PARTIAL', self: 'SELF' })
  })

  it('only links "view all" with filters the catalog page understands', () => {
    const scholarshipFilters = SCHOLARSHIP_TABS.map(tab => tab.viewAll).filter(Boolean)
    expect(scholarshipFilters.every(qs => qs!.startsWith('funding='))).toBe(true)
    expect(PROGRAM_TABS.map(tab => tab.viewAll).filter(Boolean)).toEqual(['featured=true'])
  })
})
