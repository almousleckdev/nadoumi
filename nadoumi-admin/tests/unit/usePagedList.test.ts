import { describe, it, expect, vi } from 'vitest'
import { defineComponent, h } from 'vue'
import { mount } from '@vue/test-utils'
import { testI18n } from '../helpers'
import { usePagedList, type PagedListOptions } from '@/composables/usePagedList'

interface Row { id: number }
interface Filters { q: string, status: string }

function setup(overrides: Partial<PagedListOptions<Row, Filters>> = {}) {
  const fetch = vi.fn().mockResolvedValue({ rows: [{ id: 1 }, { id: 2 }], total: 2 })
  let api!: ReturnType<typeof usePagedList<Row, Filters>>
  const Host = defineComponent({
    setup() {
      api = usePagedList<Row, Filters>({ emptyFilters: () => ({ q: '', status: '' }), fetch, firstPage: 1, size: 10, ...overrides })
      return () => h('div')
    },
  })
  mount(Host, { global: { plugins: [testI18n()] } })
  return { api, fetch }
}

describe('usePagedList', () => {
  it('loads a RuoYi-shaped page into rows and total, passing filters and paging', async () => {
    const { api, fetch } = setup()
    api.filters.q = 'ada'

    await api.load()

    expect(fetch).toHaveBeenCalledWith({ q: 'ada', status: '' }, { page: 1, size: 10, index: 0 })
    expect(api.rows.value).toEqual([{ id: 1 }, { id: 2 }])
    expect(api.total.value).toBe(2)
    expect(api.loading.value).toBe(false)
  })

  it('also reads a Nadoumi-shaped page', async () => {
    const { api } = setup({ fetch: vi.fn().mockResolvedValue({ content: [{ id: 9 }], totalElements: 41 }), firstPage: 0 })

    await api.load()

    expect(api.rows.value).toEqual([{ id: 9 }])
    expect(api.total.value).toBe(41)
  })

  it('reload goes back to the first page', async () => {
    const { api, fetch } = setup()
    api.page.value = 4

    await api.reload()

    expect(fetch).toHaveBeenCalledWith(expect.anything(), { page: 1, size: 10, index: 0 })
  })

  it('gives a 0-based index whatever the first page is', async () => {
    const { api, fetch } = setup()
    api.page.value = 3

    await api.load()

    expect(fetch).toHaveBeenCalledWith(expect.anything(), { page: 3, size: 10, index: 2 })
  })

  it('does not count a false toggle as an active filter', async () => {
    const { api } = setup({ emptyFilters: () => ({ q: '', status: '', mine: false }) as unknown as Filters })

    expect(api.dirty.value).toBe(false)
    ;(api.filters as unknown as { mine: boolean }).mine = true
    expect(api.dirty.value).toBe(true)
  })

  it('starts from page 0 for Nadoumi endpoints', async () => {
    const { api, fetch } = setup({ firstPage: 0 })
    api.page.value = 3

    await api.reload()

    expect(fetch).toHaveBeenCalledWith(expect.anything(), { page: 0, size: 10, index: 0 })
  })

  it('reports dirty while any filter has a value, and clears them all', async () => {
    const { api, fetch } = setup()
    expect(api.dirty.value).toBe(false)
    api.filters.status = 'ACTIVE'
    expect(api.dirty.value).toBe(true)

    await api.clearFilters()

    expect(api.filters).toEqual({ q: '', status: '' })
    expect(api.dirty.value).toBe(false)
    expect(fetch).toHaveBeenCalledWith({ q: '', status: '' }, { page: 1, size: 10, index: 0 })
  })

  it('keeps the error and stops loading when the request fails', async () => {
    const { api } = setup({ fetch: vi.fn().mockRejectedValue(new Error('server down')) })

    await api.load()

    expect(api.error.value).toBe('server down')
    expect(api.loading.value).toBe(false)
  })

  it('clears a previous error on the next successful load', async () => {
    const fetch = vi.fn().mockRejectedValueOnce(new Error('x')).mockResolvedValue({ rows: [], total: 0 })
    const { api } = setup({ fetch })

    await api.load()
    await api.load()

    expect(api.error.value).toBeNull()
  })
})
