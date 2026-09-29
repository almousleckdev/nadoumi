import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({
  createScholarship: vi.fn(),
  updateScholarship: vi.fn(),
  getScholarship: vi.fn(),
  listScholarshipCategories: vi.fn(),
}))
vi.mock('@/api/scholarship', async orig => ({ ...(await orig<typeof import('@/api/scholarship')>()), ...api }))
vi.mock('@/api/fx', () => ({ cnyToUsdRate: vi.fn().mockResolvedValue(0.14), DEFAULT_CNY_USD_RATE: 0.1381 }))

const message = vi.hoisted(() => ({ warning: vi.fn(), success: vi.fn() }))
vi.mock('element-plus', async (orig) => {
  const actual = await orig<typeof import('element-plus')>()
  return { ...actual, ElMessage: { ...actual.ElMessage, warning: message.warning, success: message.success } }
})

import ScholarshipDrawer from '@/views/scholarships/ScholarshipDrawer.vue'
import Drawer from '@/components/ui/Drawer.vue'
import type { Scholarship } from '@/api/scholarship'

const full = {
  view: {
    id: 7, slug: 'csc-master', title: 'CSC Master', country: 'CN', fundingModel: 'FULLY',
    hasStipend: true, featured: true, recommended: false, hot: false,
    requiresFinancialProof: false, requiresFoundationYear: false,
    heroMediaId: 201, coverMediaId: 202,
    levels: ['MASTER'], categories: ['CSC'],
    intakes: [{ term: 'AUTUMN_SEPTEMBER', applicationOpen: '2026-01-01', applicationClose: '2026-04-01' }],
    fees: [{ kind: 'REGISTRATION', amountRmb: 400, amountUsd: 55, currency: 'CNY', note: null }],
    stipends: [{ level: 'MASTER', amountRmb: 3000, amountUsd: 414, currency: 'CNY', frequency: 'MONTHLY', durationMonths: 24, conditions: null }],
    accommodation: [], coverage: [{ kind: 'TUITION', detail: null }], documentRequirements: [],
  },
  status: 'ACTIVE', publishStatus: 'PUBLISHED', remark: 'note',
} as unknown as Scholarship

function mountDrawer(scholarship: Scholarship | null) {
  return mount(ScholarshipDrawer, { props: { modelValue: true, scholarship }, ...mountOpts() })
}

describe('ScholarshipDrawer', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    Object.values(api).forEach(fn => fn.mockReset())
    message.warning.mockReset()
    message.success.mockReset()
    api.listScholarshipCategories.mockResolvedValue([{ code: 'CSC', name: 'CSC' }])
    api.getScholarship.mockResolvedValue(full)
    api.updateScholarship.mockResolvedValue(full)
    api.createScholarship.mockResolvedValue(full)
  })

  it('loads the full aggregate and saves the edited scholarship with the mapped payload', async () => {
    const w = mountDrawer({ ...full, view: { ...full.view, fees: [], stipends: [] } } as Scholarship)
    await flushPromises()

    w.findComponent(Drawer).vm.$emit('save')
    await flushPromises()

    expect(api.getScholarship).toHaveBeenCalledWith(7)
    expect(api.updateScholarship).toHaveBeenCalledOnce()
    const [id, payload] = api.updateScholarship.mock.calls[0]!
    expect(id).toBe(7)
    expect(payload).toMatchObject({
      title: 'CSC Master', country: 'CN', levels: ['MASTER'], categoryCodes: ['CSC'], hasStipend: true,
      fees: [{ kind: 'REGISTRATION', amount: 400, currency: 'CNY', note: null }],
      intakes: [{ term: 'AUTUMN_SEPTEMBER', applicationOpen: '2026-01-01', applicationClose: '2026-04-01' }],
    })
    expect(message.success).toHaveBeenCalled()
    expect(w.emitted('saved')).toHaveLength(1)
    expect(w.emitted('update:modelValue')?.at(-1)).toEqual([false])
  })

  it('refuses to create a scholarship without a level', async () => {
    const w = mountDrawer(null)
    await flushPromises()
    const inputs = w.findAllComponents({ name: 'ElInput' })
    await inputs[0]!.setValue('New award')
    await inputs[2]!.setValue('cn')

    w.findComponent(Drawer).vm.$emit('save')
    await flushPromises()

    expect(message.warning).toHaveBeenCalled()
    expect(api.createScholarship).not.toHaveBeenCalled()
  })

  it('prunes selected categories that the funding model no longer allows', async () => {
    const w = mountDrawer(full)
    await flushPromises()
    const selects = w.findAllComponents({ name: 'ElSelect' })
    await selects[0]!.vm.$emit('update:modelValue', 'SELF')
    await flushPromises()

    w.findComponent(Drawer).vm.$emit('save')
    await flushPromises()

    expect(api.updateScholarship.mock.calls[0]![1]).toMatchObject({ fundingModel: 'SELF', categoryCodes: [] })
  })
})
