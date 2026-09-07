import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({
  listPrograms: vi.fn(),
  deleteProgram: vi.fn(),
}))
vi.mock('@/api/program', async (orig) => ({
  ...(await orig<typeof import('@/api/program')>()),
  ...api,
  createProgram: vi.fn(),
  updateProgram: vi.fn(),
  getProgram: vi.fn(),
}))
vi.mock('@/api/university', () => ({
  listUniversities: vi.fn().mockResolvedValue({ content: [], page: 0, size: 200, totalElements: 0, totalPages: 0 }),
  listDepartments: vi.fn().mockResolvedValue([]),
}))

const confirm = vi.hoisted(() => vi.fn())
vi.mock('@/composables/useConfirm', () => ({ useConfirm: () => ({ confirm }) }))

import Programs from '@/views/programs/index.vue'
import ProgramDrawer from '@/views/programs/ProgramDrawer.vue'
import ImageUpload from '@/components/ui/ImageUpload.vue'
import { useUserStore } from '@/stores/user'
import type { Program } from '@/api/program'

const row = {
  id: 4, universityId: 7, universityName: 'Fudan University', name: 'MBA', nameCn: null,
  programType: 'DEGREE', levels: ['MASTER'], field: 'Business', teachingLanguage: 'ENGLISH', durationMonths: 24,
  tuitionAmount: 38000, tuitionCurrency: 'USD', summary: null, featured: false, hot: true,
  status: 'ACTIVE', publishStatus: 'PUBLISHED', remark: null, createdAt: null, updatedAt: null,
  imageMediaId: 301, imageUrl: 'https://res.cloudinary.com/program.png',
  majors: [], intakes: [],
}
const page = { content: [row], page: 0, size: 20, totalElements: 1, totalPages: 1 }

const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/:x(.*)*', component: { template: '<div />' } }],
})

function mountList() {
  return mount(Programs, {
    global: { ...mountOpts().global, plugins: [...mountOpts().global.plugins, router] },
  })
}

describe('Programs list', () => {
  beforeEach(async () => {
    setActivePinia(createPinia())
    useUserStore().permissions = ['*:*:*']
    api.listPrograms.mockReset().mockResolvedValue(page)
    api.deleteProgram.mockReset().mockResolvedValue(undefined)
    confirm.mockReset()
    await router.push('/programs')
  })

  it('loads on mount and passes the rows to the table', async () => {
    const w = mountList()
    await flushPromises()
    expect(api.listPrograms).toHaveBeenCalled()
    expect(w.findComponent({ name: 'ElTable' }).props('data')).toEqual(page.content)
    expect(w.text()).toContain('MBA')
    expect(w.text()).toContain('Fudan University')
  })

  it('re-queries from page 0 when a filter is applied', async () => {
    const w = mountList()
    await flushPromises()
    const vm = w.vm as unknown as { query: { type: string, page: number }, applyFilters: () => void }
    vm.query.type = 'MASTER'
    vm.query.page = 2
    vm.applyFilters()
    await flushPromises()
    expect(vm.query.page).toBe(0)
    expect(api.listPrograms).toHaveBeenLastCalledWith(expect.objectContaining({ type: 'MASTER', page: 0 }))
  })

  it('deletes only after the confirm resolves true', async () => {
    const w = mountList()
    await flushPromises()
    const vm = w.vm as unknown as { onDelete: (p: unknown) => Promise<void> }

    confirm.mockResolvedValueOnce(false)
    await vm.onDelete(row)
    expect(api.deleteProgram).not.toHaveBeenCalled()

    confirm.mockResolvedValueOnce(true)
    await vm.onDelete(row)
    await flushPromises()
    expect(api.deleteProgram).toHaveBeenCalledWith(4)
  })
})

describe('ProgramDrawer image upload', () => {
  beforeEach(() => setActivePinia(createPinia()))

  function mountDrawer(program: Program | null) {
    return mount(ProgramDrawer, { props: { modelValue: true, program }, ...mountOpts() })
  }

  it('defers the programme image upload until the programme has an id', async () => {
    const creating = mountDrawer(null)
    await flushPromises()
    const uploads = creating.findAllComponents(ImageUpload)
    expect(uploads.length).toBe(1)
    expect(uploads[0]!.props('deferred')).toBe(true)

    const editing = mountDrawer(row as unknown as Program)
    await flushPromises()
    const editUpload = editing.findComponent(ImageUpload)
    expect(editUpload.props('deferred')).toBe(false)
    expect(editUpload.props('previewUrl')).toBe('https://res.cloudinary.com/program.png')
  })
})
