import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { ref } from 'vue'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({
  getFinanceSummary: vi.fn(),
  listTasks: vi.fn(),
  listPrograms: vi.fn(),
  listUniversities: vi.fn(),
  listScholarships: vi.fn(),
  listUsers: vi.fn(),
  listMyNotifications: vi.fn(),
  refreshStats: vi.fn(),
}))
vi.mock('@/api/finance', () => ({ getFinanceSummary: api.getFinanceSummary }))
vi.mock('@/api/hr', () => ({ listTasks: api.listTasks }))
vi.mock('@/api/program', () => ({ listPrograms: api.listPrograms }))
vi.mock('@/api/university', () => ({ listUniversities: api.listUniversities }))
vi.mock('@/api/scholarship', () => ({ listScholarships: api.listScholarships }))
vi.mock('@/api/system', () => ({ listUsers: api.listUsers }))
vi.mock('@/api/notification', () => ({ listMyNotifications: api.listMyNotifications }))
vi.mock('@/composables/useApplicantStats', () => ({
  useApplicantStats: () => ({
    data: ref({ total: 120, new30d: 8, incomplete: 3, active: 40, sampled: false, sampledCount: 0, recent: [{ status: 'NEW' }, { status: 'NEW' }, { status: 'IN_REVIEW' }] }),
    loading: ref(false),
    error: ref(null),
    refresh: api.refreshStats,
  }),
}))

import Dashboard from '@/views/dashboard.vue'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/:x(.*)*', component: { template: '<div />' } }],
})

const financeSummary = {
  byCurrency: [
    { currency: 'CNY', revenue: '10000', expenses: '4000', net: '6000' },
    { currency: 'USD', revenue: '100', expenses: '20', net: '80' },
  ],
}

async function mountDashboard() {
  const w = mount(Dashboard, {
    global: { ...mountOpts().global, plugins: [...mountOpts().global.plugins, router] },
  })
  await flushPromises()
  return w
}

describe('Dashboard', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    Object.values(api).forEach(fn => fn.mockReset())
    api.getFinanceSummary.mockResolvedValue(financeSummary)
    api.listTasks.mockResolvedValue({ content: [
      { status: 'PENDING', priority: 'HIGH' }, { status: 'IN_PROGRESS', priority: 'HIGH' },
      { status: 'DONE', priority: 'LOW' }, { status: 'PENDING', priority: 'LOW' },
    ] })
    api.listPrograms.mockResolvedValue({ totalElements: 30, content: [
      { universityName: 'Fudan' }, { universityName: 'Fudan' }, { universityName: 'Tsinghua' },
    ] })
    api.listUniversities.mockResolvedValue({ totalElements: 12, content: [] })
    api.listScholarships.mockResolvedValue({ totalElements: 9, content: [] })
    api.listUsers.mockResolvedValue({ total: 250, rows: [] })
    api.listMyNotifications.mockResolvedValue({ content: [{ type: 'APPLICATION_UPDATE' }, { type: 'APPLICATION_UPDATE' }] })
  })

  it('shows the no-access state and fetches nothing scoped for an account without permissions', async () => {
    useUserStore().permissions = []
    const w = await mountDashboard()
    expect(w.text()).toContain('Nothing to show here yet')
    expect(api.listUsers).not.toHaveBeenCalled()
    expect(api.listUniversities).not.toHaveBeenCalled()
    expect(api.listPrograms).not.toHaveBeenCalled()
    expect(api.listScholarships).not.toHaveBeenCalled()
    expect(api.getFinanceSummary).not.toHaveBeenCalled()
    expect(api.listTasks).not.toHaveBeenCalled()
    expect(api.refreshStats).not.toHaveBeenCalled()
  })

  it('fetches only what a finance-only account may see', async () => {
    useUserStore().permissions = ['nad:finance:view']
    const w = await mountDashboard()
    expect(api.getFinanceSummary).toHaveBeenCalledTimes(3)
    expect(api.listUsers).not.toHaveBeenCalled()
    expect(api.listPrograms).not.toHaveBeenCalled()
    expect(api.listTasks).not.toHaveBeenCalled()
    expect(api.refreshStats).not.toHaveBeenCalled()
    expect(w.text()).toContain('CNY 10,000')
  })

  it('shows the largest-revenue currency net margin for finance', async () => {
    useUserStore().permissions = ['nad:finance:view']
    const w = await mountDashboard()
    expect(w.text()).toContain('CNY 4,000')
    expect(w.text()).toContain('CNY 6,000')
    expect(w.text()).toContain('60%')
  })

  it('shows platform totals for an account that holds each list permission', async () => {
    useUserStore().permissions = ['system:user:list', 'nad:university:list', 'nad:program:list', 'nad:scholarship:list']
    const w = await mountDashboard()
    expect(api.listUsers).toHaveBeenCalledWith({ userType: '10', pageNum: 1, pageSize: 1 })
    expect(w.text()).toContain('250')
    expect(w.text()).toContain('12')
    expect(w.text()).toContain('30')
    expect(w.text()).toContain('9')
  })

  it('counts open tasks only and groups the catalogue by university', async () => {
    useUserStore().permissions = ['nad:task:list', 'nad:program:list']
    const w = await mountDashboard()
    expect(api.listTasks).toHaveBeenCalledWith({ page: 0, size: 8 })
    expect(w.text()).toContain('Fudan')
    expect(w.text()).toContain('Tsinghua')
  })

  it('always loads the callers own activity feed', async () => {
    useUserStore().permissions = ['nad:task:list']
    await mountDashboard()
    expect(api.listMyNotifications).toHaveBeenCalledWith({ page: 0, size: 8 })
  })

  it('shows the roadmap only to the platform owner', async () => {
    useUserStore().permissions = ['nad:task:list']
    expect((await mountDashboard()).text()).not.toContain('Payroll')
    useUserStore().permissions = ['*:*:*']
    expect((await mountDashboard()).text()).toContain('Payroll')
  })

  it('refreshes every permitted source when the refresh button is pressed', async () => {
    useUserStore().permissions = ['*:*:*']
    const w = await mountDashboard()
    api.getFinanceSummary.mockClear()
    api.refreshStats.mockClear()
    await w.findAll('button').find(b => b.text() === 'Refresh')!.trigger('click')
    await flushPromises()
    expect(api.refreshStats).toHaveBeenCalledOnce()
    expect(api.getFinanceSummary).toHaveBeenCalledTimes(3)
  })
})
