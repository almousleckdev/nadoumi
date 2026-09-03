import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import UniversitiesList from '~/pages/universities/index.vue'
import type { Page, UniversitySummary } from '~/types/catalog'

function page(content: UniversitySummary[], total = content.length): Page<UniversitySummary> {
  return { content, page: 0, size: 12, totalElements: total, totalPages: Math.max(1, Math.ceil(total / 12)) }
}

const publicGet = vi.fn<(p: string, q?: Record<string, unknown>) => Promise<unknown>>()
mockNuxtImport('useApi', () => () => ({ publicGet, publicPost: vi.fn(), studentFetch: vi.fn() }))

beforeEach(() => publicGet.mockReset())

describe('universities discovery list', () => {
  it('renders cards + a result count from the real API and passes paging params', async () => {
    publicGet.mockResolvedValue(page([
      { id: 1, slug: 'peking-university', name: 'Peking University', country: 'CN', city: 'Beijing' },
      { id: 2, slug: 'fudan-university', name: 'Fudan University', country: 'CN', city: 'Shanghai' },
    ]))
    const w = await mountSuspended(UniversitiesList)
    await flushPromises()

    expect(publicGet).toHaveBeenCalledWith('universities', expect.objectContaining({ page: 0, size: 12 }))
    expect(w.text()).toContain('Peking University')
    expect(w.text()).toContain('Fudan University')
    expect(w.text()).toContain('2 results')
  })

  it('re-queries with a filter and shows a removable chip', async () => {
    publicGet.mockResolvedValue(page([{ id: 1, slug: 'peking-university', name: 'Peking University', country: 'CN' }]))
    const w = await mountSuspended(UniversitiesList)
    await flushPromises()

    const typeSelect = w.get('select')
    await typeSelect.setValue('PUBLIC')
    await flushPromises()

    expect(publicGet).toHaveBeenLastCalledWith('universities', expect.objectContaining({ type: 'PUBLIC' }))
    expect(w.text()).toContain('Public')
    expect(w.text()).toContain('Clear filters')
  })

  it('shows an empty state (no fake cards) when the API returns nothing', async () => {
    publicGet.mockResolvedValue(page([], 0))
    const w = await mountSuspended(UniversitiesList)
    await flushPromises()
    expect(w.text()).toContain('No universities yet')
  })
})
