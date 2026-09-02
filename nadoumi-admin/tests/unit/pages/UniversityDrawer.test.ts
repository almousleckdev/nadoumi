import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({
  createUniversity: vi.fn(),
  updateUniversity: vi.fn(),
}))
vi.mock('@/api/university', () => api)

import UniversityDrawer from '@/views/universities/UniversityDrawer.vue'

const existing = {
  id: 5, name: 'Fudan University', country: 'CN', city: 'Shanghai',
  website: null, rankingTier: null, logoDocumentId: null, status: 'ACTIVE' as const,
  createdAt: null, updatedAt: null,
}

function mountDrawer(university: typeof existing | null) {
  return mount(UniversityDrawer, { props: { modelValue: true, university }, ...mountOpts() })
}

describe('UniversityDrawer', () => {
  beforeEach(() => {
    api.createUniversity.mockReset()
    api.updateUniversity.mockReset()
  })

  it('creates when no university is passed, normalising country to upper-case', async () => {
    api.createUniversity.mockResolvedValue({ ...existing, id: 9 })
    const w = mountDrawer(null)
    const vm = w.vm as unknown as { form: Record<string, unknown>, save: () => Promise<void> }
    vm.form.name = '  Nanjing University  '
    vm.form.country = 'cn'
    await vm.save()
    await flushPromises()

    expect(api.createUniversity).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'Nanjing University', country: 'CN', status: 'ACTIVE' }),
    )
    expect(api.updateUniversity).not.toHaveBeenCalled()
    expect(w.emitted('saved')?.[0]).toEqual([{ ...existing, id: 9 }])
    expect(w.emitted('update:modelValue')?.at(-1)).toEqual([false])
  })

  it('updates when a university is passed, pre-filling the form', async () => {
    api.updateUniversity.mockResolvedValue(existing)
    const w = mountDrawer(existing)
    await flushPromises()
    const vm = w.vm as unknown as { form: Record<string, unknown>, save: () => Promise<void> }
    expect(vm.form.name).toBe('Fudan University')
    expect(vm.form.city).toBe('Shanghai')

    vm.form.city = 'Yangpu'
    await vm.save()
    await flushPromises()
    expect(api.updateUniversity).toHaveBeenCalledWith(5, expect.objectContaining({ city: 'Yangpu' }))
  })

  it('declares name, country and status as required, with a country-format rule', () => {
    const w = mountDrawer(null)
    const vm = w.vm as unknown as { rules: Record<string, Array<Record<string, unknown>>> }
    expect(vm.rules.name.some(r => r.required)).toBe(true)
    expect(vm.rules.status.some(r => r.required)).toBe(true)
    expect(vm.rules.country.some(r => r.required)).toBe(true)
    expect(vm.rules.country.some(r => r.pattern instanceof RegExp)).toBe(true)
  })
})
