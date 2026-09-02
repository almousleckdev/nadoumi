import { describe, it, expect, vi } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import Home from '~/pages/index.vue'
import type { Page, UniversitySummary } from '~/types/catalog'

const page: Page<UniversitySummary> = {
  content: [
    { id: 1, name: 'Peking University', nameCn: '北京大学', country: 'CN', city: 'Beijing' },
    { id: 2, name: 'University of Malaya', country: 'MY', city: 'Kuala Lumpur' },
  ],
  page: 0, size: 6, totalElements: 2, totalPages: 1,
}

const publicGet = vi.fn((path: string) =>
  path === 'universities' ? Promise.resolve(page) : Promise.reject(new Error('404')),
)
mockNuxtImport('useApi', () => () => ({ publicGet, publicPost: vi.fn(), studentFetch: vi.fn() }))

describe('home landing page', () => {
  it('renders the hero, the how-it-works steps and a featured-universities strip', async () => {
    const w = await mountSuspended(Home)
    await flushPromises()
    const text = w.text()

    expect(text).toContain('Study abroad with Nadoumi')
    expect(text).toContain('How it works')
    expect(text).toContain('Discover opportunities')
    expect(text).toContain('Featured universities')
    expect(text).toContain('Peking University')
    expect(text).toContain('University of Malaya')
  })

  it('shows the empty note when no universities are published', async () => {
    publicGet.mockResolvedValueOnce({ ...page, content: [], totalElements: 0 })
    const w = await mountSuspended(Home)
    await flushPromises()
    expect(w.text()).toContain('Published universities will appear here')
  })
})
