import { describe, it, expect, vi } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import UniversityDetail from '~/pages/universities/[id].vue'
import type { UniversityDetail as UDto } from '~/types/catalog'

const fudan: UDto = {
  id: 7,
  name: 'Fudan University',
  nameCn: '复旦大学',
  country: 'CN',
  city: 'Shanghai',
  province: 'Shanghai',
  type: 'PUBLIC',
  featured: true,
  foundedYear: 1905,
  totalStudents: 35000,
  internationalStudents: 4000,
  facultyCount: 3000,
  website: 'https://www.fudan.edu.cn',
  rankingTier: 'Top 50',
  introduction: 'A leading research university in Shanghai.',
  history: null,
  campusInfo: null,
  accommodationInfo: null,
  nearbyInfo: null,
  admissionsEmail: 'admissions@fudan.edu.cn',
  officePhone: null,
  recommended: true,
  rankings: [{ id: 1, source: 'QS', rankPosition: 34, rankYear: 2026, note: null }],
  highlights: [
    { id: 1, kind: 'HIGHLIGHT', text: 'C9 League member' },
    { id: 2, kind: 'ADVANTAGE', text: 'Strong medical school' },
  ],
  gallery: [
    { id: 1, imageUrl: 'https://img.example/campus.jpg', caption: 'Main campus' },
    { id: 2, imageUrl: 'https://img.example/dorm.jpg', caption: 'Dormitory' },
  ],
}

const publicGet = vi.fn((path: string) =>
  path === 'universities/7' ? Promise.resolve(fudan) : Promise.reject(new Error('404')),
)
mockNuxtImport('useApi', () => () => ({ publicGet, studentFetch: vi.fn() }))

describe('public university detail page', () => {
  it('renders the rich profile: facts, prose, highlights, advantages, rankings', async () => {
    const w = await mountSuspended(UniversityDetail, { route: '/universities/7' })
    await flushPromises()
    const text = w.text()

    expect(text).toContain('Fudan University')
    expect(text).toContain('复旦大学')
    expect(text).toContain('Public')
    expect(text).toContain('Shanghai, Shanghai, CN')
    expect(text).toContain('1905')
    expect(text).toContain('35,000')
    expect(text).toContain('A leading research university in Shanghai.')
    expect(text).toContain('C9 League member')
    expect(text).toContain('Strong medical school')
    expect(text).toContain('QS')
    expect(text).toContain('#34')
    // programmes belong on the university page (Step 3 fills the list)
    expect(text).toContain('Programmes')
    // gallery renders campus/dormitory imagery with captions
    expect(text).toContain('Main campus')
    expect(text).toContain('Dormitory')
  })

  it('shows a not-available message when the university is missing', async () => {
    const w = await mountSuspended(UniversityDetail, { route: '/universities/999' })
    await flushPromises()
    expect(w.text()).toContain('not available')
  })
})
