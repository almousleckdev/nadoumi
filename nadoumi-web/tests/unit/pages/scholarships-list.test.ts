import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import ScholarshipsList from '~/pages/scholarships/index.vue'
import type { Page, ScholarshipCard, ScholarshipFacets } from '~/types/catalog'

function page(content: ScholarshipCard[], total = content.length): Page<ScholarshipCard> {
  return { content, page: 0, size: 12, totalElements: total, totalPages: Math.max(1, Math.ceil(total / 12)) }
}
const facets: ScholarshipFacets = {
  levels: [{ value: 'MASTER', count: 2 }], categories: [{ value: 'CSC', count: 1 }],
  fundingModels: [{ value: 'FULLY', count: 2 }], teachingLanguages: [{ value: 'ENGLISH', count: 2 }],
}
const row: ScholarshipCard = {
  id: 1, slug: 'csc-master', title: 'CSC Master Scholarship', country: 'CN', city: 'Beijing',
  fundingModel: 'FULLY', hasStipend: true, deadline: '2026-03-31',
  featured: true, recommended: false, hot: false,
  levels: ['MASTER'], categories: ['CSC'], intakes: [{ term: 'AUTUMN_SEPTEMBER' }],
}

const publicGet = vi.fn<(p: string, q?: Record<string, unknown>) => Promise<unknown>>()
mockNuxtImport('useApi', () => () => ({ publicGet, publicPost: vi.fn(), studentFetch: vi.fn() }))

beforeEach(() => publicGet.mockReset())

describe('scholarships discovery table', () => {
  it('renders a table of scholarships with an Apply action + result count from the real API', async () => {
    publicGet.mockImplementation((resource: string) =>
      Promise.resolve(resource === 'scholarships/facets' ? facets : page([row])),
    )
    const w = await mountSuspended(ScholarshipsList)
    await flushPromises()

    expect(publicGet).toHaveBeenCalledWith('scholarships', expect.objectContaining({ page: 0, size: 12 }))
    expect(w.find('table').exists()).toBe(true)
    expect(w.text()).toContain('CSC Master Scholarship')
    expect(w.text()).toContain('Fully funded')
    expect(w.text()).toContain('1 results')
    expect(w.findAll('a').some(a => a.text() === 'Apply' && a.attributes('href') === '/scholarships/csc-master')).toBe(true)
  })

  it('re-queries with a level filter and shows a removable chip', async () => {
    publicGet.mockImplementation((resource: string) =>
      Promise.resolve(resource === 'scholarships/facets' ? facets : page([row])),
    )
    const w = await mountSuspended(ScholarshipsList)
    await flushPromises()

    const masterBox = w.findAll('input[type="checkbox"]').find(c => c.element.parentElement?.textContent?.includes("Master's"))
    await masterBox!.setValue(true)
    await flushPromises()

    expect(publicGet).toHaveBeenCalledWith('scholarships', expect.objectContaining({ level: 'MASTER' }))
    expect(w.text()).toContain("Master's ×")
    expect(w.text()).toContain('Clear filters')
  })

  it('shows an honest empty state (no fake rows) when nothing is published', async () => {
    publicGet.mockImplementation((resource: string) =>
      Promise.resolve(resource === 'scholarships/facets' ? facets : page([], 0)),
    )
    const w = await mountSuspended(ScholarshipsList)
    await flushPromises()
    expect(w.text()).toContain('No scholarships published yet')
  })
})
