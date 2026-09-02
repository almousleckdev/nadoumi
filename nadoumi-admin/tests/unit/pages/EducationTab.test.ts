import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({
  listEducation: vi.fn(),
  addEducation: vi.fn(),
  updateEducation: vi.fn(),
  deleteEducation: vi.fn(),
}))
vi.mock('@/api/applicant', () => api)

const confirm = vi.hoisted(() => vi.fn())
vi.mock('@/composables/useConfirm', () => ({ useConfirm: () => ({ confirm }) }))

import EducationTab from '@/views/applicants/tabs/EducationTab.vue'

const rows = [
  { id: 1, institution: 'MIT', level: 'BSC', field: 'CS', gpa: 3.9, gpaScale: 4, startDate: null, endDate: null },
]

function mountTab(canEdit = true) {
  return mount(EducationTab, { props: { id: '7', canEdit }, ...mountOpts() })
}

describe('EducationTab', () => {
  beforeEach(() => {
    Object.values(api).forEach(f => f.mockReset())
    confirm.mockReset()
    api.listEducation.mockResolvedValue(rows)
  })

  it('loads on mount, passes rows to the table and emits the count', async () => {
    const w = mountTab()
    await flushPromises()
    expect(api.listEducation).toHaveBeenCalledWith('7')
    expect(w.findComponent({ name: 'ElTable' }).props('data')).toEqual(rows)
    expect(w.emitted('count')?.at(-1)).toEqual([1])
  })

  it('hides the add toolbar when canEdit is false and shows it when true', async () => {
    const off = mountTab(false)
    await flushPromises()
    expect(off.find('.tab-toolbar').exists()).toBe(false)

    const on = mountTab(true)
    await flushPromises()
    expect(on.find('.tab-toolbar').exists()).toBe(true)
  })

  it('adds an education record then reloads', async () => {
    api.addEducation.mockResolvedValue({ id: 2 })
    const w = mountTab()
    await flushPromises()

    const vm = w.vm as unknown as {
      openAdd: () => void
      form: Record<string, unknown>
      save: () => Promise<void>
    }
    vm.openAdd()
    vm.form.institution = 'Cambridge'
    await vm.save()
    await flushPromises()

    expect(api.addEducation).toHaveBeenCalledWith('7', expect.objectContaining({ institution: 'Cambridge' }))
    expect(api.listEducation).toHaveBeenCalledTimes(2) // initial + reload
  })

  it('deletes only after the confirm resolves true', async () => {
    api.deleteEducation.mockResolvedValue(undefined)
    const w = mountTab()
    await flushPromises()
    const vm = w.vm as unknown as { onDelete: (r: unknown) => Promise<void> }

    confirm.mockResolvedValueOnce(false)
    await vm.onDelete(rows[0])
    expect(api.deleteEducation).not.toHaveBeenCalled()

    confirm.mockResolvedValueOnce(true)
    await vm.onDelete(rows[0])
    await flushPromises()
    expect(api.deleteEducation).toHaveBeenCalledWith('7', 1)
  })
})
