import { describe, it, expect, vi } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import ProgramDetail from '~/pages/programs/[slug].vue'
import type { ProgramDetail as PDto } from '~/types/catalog'

const mba: PDto = {
  id: 11,
  slug: 'fudan-university-mba',
  universityId: 7,
  universitySlug: 'fudan-university',
  universityName: 'Fudan University',
  name: 'MBA',
  nameCn: '工商管理硕士',
  programType: 'MASTER',
  field: 'Business',
  teachingLanguage: 'ENGLISH',
  durationMonths: 24,
  tuitionAmount: 38000,
  tuitionCurrency: 'USD',
  summary: 'A two-year MBA taught in English.',
  featured: false,
  hot: true,
  imageUrl: 'https://res.cloudinary.com/demo/image/upload/v1/mba.jpg',
  majors: [
    { id: 1, name: 'Finance', nameCn: null },
    { id: 2, name: 'Marketing', nameCn: null },
  ],
  intakes: [
    { id: 1, term: 'AUTUMN_SEPTEMBER', applicationOpen: '2026-03-01', applicationClose: '2026-06-30' },
  ],
}

const publicGet = vi.fn((path: string) =>
  path === 'programs/fudan-university-mba' ? Promise.resolve(mba) : Promise.reject(new Error('404')),
)
mockNuxtImport('useApi', () => () => ({ publicGet, studentFetch: vi.fn() }))

describe('public programme detail page', () => {
  it('renders the programme: name, university link, majors, intakes', async () => {
    const w = await mountSuspended(ProgramDetail, { route: '/programs/fudan-university-mba' })
    await flushPromises()
    const text = w.text()

    expect(text).toContain('MBA')
    expect(text).toContain('工商管理硕士')
    expect(text).toContain('Fudan University')
    expect(text).toContain('A two-year MBA taught in English.')
    expect(text).toContain('Finance')
    expect(text).toContain('Marketing')
    expect(text).toContain('Autumn (September)')

    // the resolved (absolute) programme image is used verbatim — not routed through /media
    const srcs = w.findAll('img').map(i => i.attributes('src'))
    expect(srcs).toContain('https://res.cloudinary.com/demo/image/upload/v1/mba.jpg')
    expect(srcs.some(s => s?.startsWith('/media/'))).toBe(false)
  })

  it('shows a not-available message when the programme is missing', async () => {
    const w = await mountSuspended(ProgramDetail, { route: '/programs/ghost-mba' })
    await flushPromises()
    expect(w.text()).toContain('not available')
  })
})
