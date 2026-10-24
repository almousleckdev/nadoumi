import { describe, expect, it } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import RecommendedCard from '~/components/dashboard/overview/RecommendedCard.vue'
import type { ScholarshipCard } from '~/types/catalog'

const scholarship = (id: number, over: Partial<ScholarshipCard> = {}): ScholarshipCard => ({
  id, slug: `csc-${id}`, title: `CSC Master ${id}`, country: 'CN', fundingModel: 'FULLY', hasStipend: true,
  featured: false, recommended: true, hot: false, levels: ['MASTER'], categories: ['CSC'], intakes: [],
  deadline: '2026-12-31', coverUrl: 'https://cdn.example/cover.jpg', ...over,
})

describe('dashboard RecommendedCard', () => {
  it('lists each scholarship as a table row with its country, funding and deadline', async () => {
    const w = await mountSuspended(RecommendedCard, { props: { items: [scholarship(1), scholarship(2)], pending: false, error: null } })

    expect(w.find('table').exists()).toBe(true)
    expect(w.findAll('[data-test="table-row"]')).toHaveLength(2)
    expect(w.text()).toContain('CSC Master 1')
    expect(w.text()).toContain('CN')
    expect(w.text()).toContain('Fully funded')
    expect(w.text()).toContain('2026-12-31')
    expect(w.find('a[href="/scholarships/csc-1"]').exists()).toBe(true)
  })

  it('shows an empty message and uses neutral link colours', async () => {
    const w = await mountSuspended(RecommendedCard, { props: { items: [], pending: false, error: null } })

    expect(w.text().length).toBeGreaterThan(0)
    expect(w.findAll('[data-test="table-row"]')).toHaveLength(0)
    expect(w.html()).not.toContain('text-brand-')
  })
})
