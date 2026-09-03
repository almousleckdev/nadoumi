import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({
  listScholarships: vi.fn(),
  deleteScholarship: vi.fn(),
}))
vi.mock('@/api/scholarship', async (orig) => ({
  ...(await orig<typeof import('@/api/scholarship')>()),
  ...api,
  createScholarship: vi.fn(),
  updateScholarship: vi.fn(),
  getScholarship: vi.fn(),
  listScholarshipCategories: vi.fn().mockResolvedValue([]),
}))

const confirm = vi.hoisted(() => vi.fn())
vi.mock('@/composables/useConfirm', () => ({ useConfirm: () => ({ confirm }) }))

import Scholarships from '@/views/scholarships/index.vue'
import { useUserStore } from '@/stores/user'

const row = {
  view: {
    id: 7, slug: 'csc-master', title: 'CSC Master', country: 'CN', fundingModel: 'FULLY',
    hasStipend: true, featured: true, recommended: false, hot: false,
    requiresFinancialProof: false, requiresFoundationYear: false,
    levels: ['MASTER'], categories: ['CSC'], intakes: [], fees: [],
    stipends: [], accommodation: [], coverage: [], documentRequirements: [],
  },
  status: 'ACTIVE', publishStatus: 'PUBLISHED',
}
const page = { content: [row], page: 0, size: 20, totalElements: 1, totalPages: 1 }

const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/:x(.*)*', component: { template: '<div />' } }],
})

function mountList() {
  return mount(Scholarships, {
    global: { ...mountOpts().global, plugins: [...mountOpts().global.plugins, router] },
  })
}

describe('Scholarships list', () => {
  beforeEach(async () => {
    setActivePinia(createPinia())
    useUserStore().permissions = ['*:*:*']
    api.listScholarships.mockReset().mockResolvedValue(page)
    api.deleteScholarship.mockReset().mockResolvedValue(undefined)
    confirm.mockReset()
    await router.push('/scholarships')
  })

  it('loads on mount and passes the rows to the table', async () => {
    const w = mountList()
    await flushPromises()
    expect(api.listScholarships).toHaveBeenCalled()
    expect(w.findComponent({ name: 'ElTable' }).props('data')).toEqual(page.content)
    expect(w.text()).toContain('CSC Master')
  })

  it('re-queries from page 0 when a filter is applied', async () => {
    const w = mountList()
    await flushPromises()
    const vm = w.vm as unknown as { query: { funding: string, page: number }, applyFilters: () => void }
    vm.query.funding = 'FULLY'
    vm.query.page = 2
    vm.applyFilters()
    await flushPromises()
    expect(vm.query.page).toBe(0)
    expect(api.listScholarships).toHaveBeenLastCalledWith(expect.objectContaining({ funding: 'FULLY', page: 0 }))
  })

  it('deletes only after the confirm resolves true', async () => {
    const w = mountList()
    await flushPromises()
    const vm = w.vm as unknown as { onDelete: (s: unknown) => Promise<void> }

    confirm.mockResolvedValueOnce(false)
    await vm.onDelete(row)
    expect(api.deleteScholarship).not.toHaveBeenCalled()

    confirm.mockResolvedValueOnce(true)
    await vm.onDelete(row)
    await flushPromises()
    expect(api.deleteScholarship).toHaveBeenCalledWith(7)
  })
})
