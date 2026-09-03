import { describe, it, expect, vi } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import Home from '~/pages/index.vue'
import type { Page, UniversitySummary } from '~/types/catalog'

function page(content: UniversitySummary[]): Page<UniversitySummary> {
  return { content, page: 0, size: 12, totalElements: content.length, totalPages: 1 }
}

const publicGet = vi.fn<(p: string, q?: Record<string, unknown>) => Promise<unknown>>()
mockNuxtImport('useApi', () => () => ({ publicGet, publicPost: vi.fn(), studentFetch: vi.fn() }))

describe('home landing page', () => {
  it('renders the hero, real university carousels, honest placeholders, the journey and the CTA', async () => {
    publicGet.mockImplementation((_p, q) =>
      Promise.resolve(page(
        q?.featured
          ? [{ id: 1, name: 'Peking University', nameCn: '北京大学', country: 'CN', city: 'Beijing' }]
          : [{ id: 2, name: 'Fudan University', country: 'CN', city: 'Shanghai' }],
      )),
    )
    const w = await mountSuspended(Home)
    await flushPromises()
    const text = w.text()

    // hero
    expect(text).toContain('Study abroad with a team that manages the whole journey')
    expect(text).toContain('Explore scholarships')
    expect(text).toContain('Explore universities')
    // real data
    expect(text).toContain('Featured universities')
    expect(text).toContain('Peking University')
    expect(text).toContain('Recommended universities')
    expect(text).toContain('Fudan University')
    // honest placeholders — no fake cards, a clear "arriving with" note
    expect(text).toContain('Arriving with the Scholarship catalogue')
    expect(text).toContain('Arriving with the Programme catalogue')
    expect(text).toContain('Arriving with the Partnership module')
    // journey + CTA
    expect(text).toContain('Your journey with Nadoumi')
    expect(text).toContain('Pay the application fee')
    expect(text).toContain('Apply now')
    expect(text).toContain('Contact us')
  })

  it('shows an empty state (not fake cards) when no universities are published', async () => {
    publicGet.mockResolvedValue(page([]))
    const w = await mountSuspended(Home)
    await flushPromises()
    expect(w.text()).toContain('No featured universities are published yet.')
    expect(w.text()).not.toContain('undefined')
  })
})
