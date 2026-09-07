import { describe, it, expect, vi } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import Home from '~/pages/index.vue'
import type { Page, ProgramCard, ScholarshipCard, UniversitySummary } from '~/types/catalog'

function page<T>(content: T[]): Page<T> {
  return { content, page: 0, size: 12, totalElements: content.length, totalPages: 1 }
}

const uni: UniversitySummary = {
  id: 1, slug: 'peking-university', name: 'Peking University', country: 'CN', city: 'Beijing',
  logoUrl: 'https://res.cloudinary.com/demo/image/upload/v1/pku-logo.jpg',
}
const sch: ScholarshipCard = {
  id: 9, slug: 'csc-master', title: 'CSC Master Scholarship', country: 'CN',
  fundingModel: 'FULLY', hasStipend: true, featured: false, recommended: false, hot: false,
  levels: ['MASTER'], categories: ['CSC'], intakes: [],
  coverUrl: 'https://res.cloudinary.com/demo/image/upload/v1/csc-cover.jpg',
}
const prog: ProgramCard = {
  id: 4, slug: 'peking-university-chinese-language-programme', universityId: 1, universitySlug: 'peking-university', universityName: 'Peking University', name: 'Chinese Language Programme',
  nameCn: null, programType: 'LANGUAGE', levels: [], field: null, teachingLanguage: 'CHINESE',
  durationMonths: 12, tuitionAmount: null, tuitionCurrency: null, summary: null,
  featured: false, hot: true,
  imageUrl: 'https://res.cloudinary.com/demo/image/upload/v1/clp.jpg',
}

const publicGet = vi.fn<(p: string, q?: Record<string, unknown>) => Promise<unknown>>()
mockNuxtImport('useApi', () => () => ({ publicGet, publicPost: vi.fn(), studentFetch: vi.fn() }))

describe('home landing page', () => {
  it('renders the hero, real university + scholarship carousels, the journey and the CTA', async () => {
    publicGet.mockImplementation((resource) => {
      if (resource === 'scholarships') return Promise.resolve(page([sch]))
      if (resource === 'programs') return Promise.resolve(page([prog]))
      return Promise.resolve(page([uni]))
    })
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
    // real hot-programmes carousel (R2)
    expect(text).toContain('Hot programmes and language courses')
    expect(text).toContain('Chinese Language Programme')
    expect(text).toContain('Your journey with Nadoumi')
    expect(text).toContain('Apply now')

    // cards render the resolved (absolute) media URLs verbatim, not the /media proxy
    const srcs = w.findAll('img').map(i => i.attributes('src'))
    expect(srcs).toContain('https://res.cloudinary.com/demo/image/upload/v1/pku-logo.jpg')
    expect(srcs.some(s => s?.startsWith('/media/'))).toBe(false)
  })

  it('shows empty states (not fake cards) when nothing is published', async () => {
    publicGet.mockResolvedValue(page([]))
    const w = await mountSuspended(Home)
    await flushPromises()
    expect(w.text()).toContain('No featured universities are published yet.')
    expect(w.text()).toContain('No scholarships are published yet.')
    expect(w.text()).toContain('No programmes are featured yet.')
    expect(w.text()).not.toContain('undefined')
  })
})
