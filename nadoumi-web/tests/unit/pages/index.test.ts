import { describe, it, expect, vi } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import Home from '~/pages/index.vue'
import type { Page, ScholarshipCard, UniversitySummary } from '~/types/catalog'

function page<T>(content: T[]): Page<T> {
  return { content, page: 0, size: 12, totalElements: content.length, totalPages: 1 }
}

const uni: UniversitySummary = { id: 1, name: 'Peking University', country: 'CN', city: 'Beijing' }
const sch: ScholarshipCard = {
  id: 9, slug: 'csc-master', title: 'CSC Master Scholarship', country: 'CN',
  fundingModel: 'FULLY', hasStipend: true, featured: false, recommended: false, hot: false,
  levels: ['MASTER'], categories: ['CSC'], intakes: [],
}

const publicGet = vi.fn<(p: string, q?: Record<string, unknown>) => Promise<unknown>>()
mockNuxtImport('useApi', () => () => ({ publicGet, publicPost: vi.fn(), studentFetch: vi.fn() }))

describe('home landing page', () => {
  it('renders the hero, real university + scholarship carousels, the journey and the CTA', async () => {
    publicGet.mockImplementation((resource) =>
      Promise.resolve(resource === 'scholarships' ? page([sch]) : page([uni])),
    )
    const w = await mountSuspended(Home)
    await flushPromises()
    const text = w.text()

    expect(text).toContain('Study abroad with a team that manages the whole journey')
    expect(text).toContain('Featured universities')
    expect(text).toContain('Peking University')
    // real scholarship carousels
    expect(text).toContain('New scholarships')
    expect(text).toContain('Fully funded scholarships')
    expect(text).toContain('CSC Master Scholarship')
    // programme discovery still an honest placeholder (R2)
    expect(text).toContain('Arriving with the Programme catalogue')
    expect(text).toContain('Your journey with Nadoumi')
    expect(text).toContain('Apply now')
  })

  it('shows empty states (not fake cards) when nothing is published', async () => {
    publicGet.mockResolvedValue(page([]))
    const w = await mountSuspended(Home)
    await flushPromises()
    expect(w.text()).toContain('No featured universities are published yet.')
    expect(w.text()).toContain('No scholarships are published yet.')
    expect(w.text()).not.toContain('undefined')
  })
})
