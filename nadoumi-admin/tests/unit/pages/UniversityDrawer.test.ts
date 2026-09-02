import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { mountOpts } from '../../helpers'
import type { University } from '@/api/university'

const api = vi.hoisted(() => ({
  createUniversity: vi.fn(),
  updateUniversity: vi.fn(),
}))
vi.mock('@/api/university', () => api)

import UniversityDrawer from '@/views/universities/UniversityDrawer.vue'

const existing: University = {
  id: 5, name: 'Fudan University', nameCn: '复旦大学', country: 'CN', type: 'PUBLIC',
  city: 'Shanghai', province: 'Shanghai', foundedYear: 1905, totalStudents: 35000,
  internationalStudents: 4000, facultyCount: 3000, website: 'https://fudan.edu.cn',
  rankingTier: 'Top 50', introduction: 'A leading university.', history: null,
  campusInfo: null, accommodationInfo: null, nearbyInfo: null,
  admissionsEmail: null, officePhone: null, logoDocumentId: null, bannerDocumentId: null,
  recommended: true, featured: false, status: 'ACTIVE', publishStatus: 'PUBLISHED',
  remark: null, createdAt: null, updatedAt: null,
  rankings: [{ id: 1, source: 'QS', rankPosition: 34, rankYear: 2026, note: null }],
  highlights: [{ id: 1, kind: 'HIGHLIGHT', text: 'C9 League member' }],
}

function mountDrawer(university: University | null) {
  return mount(UniversityDrawer, { props: { modelValue: true, university }, ...mountOpts() })
}

describe('UniversityDrawer', () => {
  beforeEach(() => {
    api.createUniversity.mockReset()
    api.updateUniversity.mockReset()
  })

  it('creates a rich profile, normalising country and dropping blank children', async () => {
    api.createUniversity.mockResolvedValue({ ...existing, id: 9 })
    const w = mountDrawer(null)
    const vm = w.vm as unknown as {
      form: Record<string, unknown>
      save: () => Promise<void>
    }
    vm.form.name = '  Nanjing University  '
    vm.form.country = 'cn'
    vm.form.type = 'PUBLIC'
    vm.form.foundedYear = 1902
    ;(vm.form.highlights as unknown[]).push({ kind: 'HIGHLIGHT', text: '  ' }) // blank -> dropped
    ;(vm.form.highlights as unknown[]).push({ kind: 'ADVANTAGE', text: 'Strong law school' })
    ;(vm.form.rankings as unknown[]).push({ source: 'QS', rankPosition: 120, rankYear: 2026, note: null })
    await vm.save()
    await flushPromises()

    expect(api.createUniversity).toHaveBeenCalledWith(expect.objectContaining({
      name: 'Nanjing University', country: 'CN', type: 'PUBLIC', foundedYear: 1902,
      status: 'ACTIVE', publishStatus: 'DRAFT',
    }))
    const body = api.createUniversity.mock.calls[0]![0]
    expect(body.highlights).toEqual([{ kind: 'ADVANTAGE', text: 'Strong law school' }])
    expect(body.rankings).toEqual([{ source: 'QS', rankPosition: 120, rankYear: 2026, note: null }])
    expect(w.emitted('update:modelValue')?.at(-1)).toEqual([false])
  })

  it('pre-fills every section (incl. children) when editing', async () => {
    api.updateUniversity.mockResolvedValue(existing)
    const w = mountDrawer(existing)
    await flushPromises()
    const vm = w.vm as unknown as { form: Record<string, unknown>, save: () => Promise<void> }
    expect(vm.form.name).toBe('Fudan University')
    expect(vm.form.nameCn).toBe('复旦大学')
    expect(vm.form.introduction).toBe('A leading university.')
    expect((vm.form.rankings as unknown[]).length).toBe(1)
    expect((vm.form.highlights as unknown[]).length).toBe(1)

    ;(vm.form.rankings as Array<{ note: string | null }>)[0]!.note = 'Asia #5'
    await vm.save()
    await flushPromises()
    const body = api.updateUniversity.mock.calls[0]![1]
    expect(body.rankings[0].note).toBe('Asia #5')
  })

  it('declares required fields incl. the publication status', () => {
    const w = mountDrawer(null)
    const vm = w.vm as unknown as { rules: Record<string, Array<Record<string, unknown>>> }
    expect(vm.rules.name.some(r => r.required)).toBe(true)
    expect(vm.rules.status.some(r => r.required)).toBe(true)
    expect(vm.rules.publishStatus.some(r => r.required)).toBe(true)
    expect(vm.rules.country.some(r => r.pattern instanceof RegExp)).toBe(true)
  })
})
