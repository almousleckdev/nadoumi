import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({
  listUniversities: vi.fn(),
  deleteUniversity: vi.fn(),
}))
vi.mock('@/api/university', () => ({ ...api, createUniversity: vi.fn(), updateUniversity: vi.fn(), getUniversity: vi.fn() }))

const confirm = vi.hoisted(() => vi.fn())
vi.mock('@/composables/useConfirm', () => ({ useConfirm: () => ({ confirm }) }))

import Universities from '@/views/universities/index.vue'
import { useUserStore } from '@/stores/user'

const page = {
  content: [
    { id: 1, name: 'Tsinghua', country: 'CN', city: 'Beijing', website: null, rankingTier: null, logoDocumentId: null, status: 'ACTIVE', createdAt: null, updatedAt: null },
  ],
  page: 0, size: 20, totalElements: 1, totalPages: 1,
}

const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/:x(.*)*', component: { template: '<div />' } }],
})

function mountList() {
  return mount(Universities, {
    global: { ...mountOpts().global, plugins: [...mountOpts().global.plugins, router] },
  })
}

describe('Universities list', () => {
  beforeEach(async () => {
    setActivePinia(createPinia())
    useUserStore().permissions = ['*:*:*']
    api.listUniversities.mockReset().mockResolvedValue(page)
    api.deleteUniversity.mockReset().mockResolvedValue(undefined)
    confirm.mockReset()
    await router.push('/universities')
  })

  it('loads on mount and passes the rows to the table', async () => {
    const w = mountList()
    await flushPromises()
    expect(api.listUniversities).toHaveBeenCalled()
    expect(w.findComponent({ name: 'ElTable' }).props('data')).toEqual(page.content)
  })

  it('re-queries from page 0 when a filter is applied', async () => {
    const w = mountList()
    await flushPromises()
    const vm = w.vm as unknown as { query: { q: string, page: number }, applyFilters: () => void }
    vm.query.q = 'fudan'
    vm.query.page = 3
    vm.applyFilters()
    await flushPromises()
    expect(vm.query.page).toBe(0)
    expect(api.listUniversities).toHaveBeenLastCalledWith(expect.objectContaining({ q: 'fudan', page: 0 }))
  })

  it('deletes only after the confirm resolves true', async () => {
    const w = mountList()
    await flushPromises()
    const vm = w.vm as unknown as { onDelete: (u: unknown) => Promise<void> }

    confirm.mockResolvedValueOnce(false)
    await vm.onDelete(page.content[0])
    expect(api.deleteUniversity).not.toHaveBeenCalled()

    confirm.mockResolvedValueOnce(true)
    await vm.onDelete(page.content[0])
    await flushPromises()
    expect(api.deleteUniversity).toHaveBeenCalledWith(1)
  })
})
