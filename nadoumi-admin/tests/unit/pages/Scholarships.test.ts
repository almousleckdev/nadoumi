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
import ScholarshipDrawer from '@/views/scholarships/ScholarshipDrawer.vue'
import ImageUpload from '@/components/ui/ImageUpload.vue'
import { useUserStore } from '@/stores/user'
import type { Scholarship } from '@/api/scholarship'

const row = {
  view: {
    id: 7, slug: 'csc-master', title: 'CSC Master', country: 'CN', fundingModel: 'FULLY',
    hasStipend: true, featured: true, recommended: false, hot: false,
    requiresFinancialProof: false, requiresFoundationYear: false,
    heroMediaId: 201, coverMediaId: 202,
    heroUrl: 'https://res.cloudinary.com/hero.png', coverUrl: 'https://res.cloudinary.com/cover.png',
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

describe('ScholarshipDrawer media uploads', () => {
  beforeEach(() => setActivePinia(createPinia()))

  function mountDrawer(scholarship: Scholarship | null) {
    return mount(ScholarshipDrawer, { props: { modelValue: true, scholarship }, ...mountOpts() })
  }

  it('defers the hero/cover uploads until the scholarship has an id', async () => {
    const creating = mountDrawer(null)
    await flushPromises()
    const uploads = creating.findAllComponents(ImageUpload)
    expect(uploads.length).toBe(2)
    expect(uploads.every(u => u.props('deferred') === true)).toBe(true)

    const editing = mountDrawer(row as unknown as Scholarship)
    await flushPromises()
    const editUploads = editing.findAllComponents(ImageUpload)
    expect(editUploads.every(u => u.props('deferred') === false)).toBe(true)
    expect(editUploads[0]!.props('previewUrl')).toBe('https://res.cloudinary.com/hero.png')
  })
})
