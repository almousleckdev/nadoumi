import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({
  getFinanceOverview: vi.fn(),
  listTasks: vi.fn(),
  listPrograms: vi.fn(),
  listUniversities: vi.fn(),
  listScholarships: vi.fn(),
  listUsers: vi.fn(),
  listMyNotifications: vi.fn(),
  listApplications: vi.fn(),
  listPayments: vi.fn(),
}))
vi.mock('@/api/finance', () => ({ getFinanceOverview: api.getFinanceOverview }))
vi.mock('@/api/hr', () => ({ listTasks: api.listTasks }))
vi.mock('@/api/program', () => ({ listPrograms: api.listPrograms }))
vi.mock('@/api/university', () => ({ listUniversities: api.listUniversities }))
vi.mock('@/api/scholarship', () => ({ listScholarships: api.listScholarships }))
vi.mock('@/api/system', () => ({ listUsers: api.listUsers }))
vi.mock('@/api/notification', () => ({ listMyNotifications: api.listMyNotifications }))
vi.mock('@/api/application', () => ({ listApplications: api.listApplications }))
vi.mock('@/api/payment', () => ({ listPayments: api.listPayments }))

import Dashboard from '@/views/dashboard.vue'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/:x(.*)*', component: { template: '<div />' } }],
})

const months = Array.from({ length: 12 }, (_, i) => ({
  month: `2026-${String(i + 1).padStart(2, '0')}`, revenue: i === 9 ? 1200 : 0, expenses: i === 9 ? 500 : 0, net: i === 9 ? 700 : 0,
}))
const overview = {
  currency: 'CNY', currencies: ['CNY'], asOf: '2026-10-25',
  kpis: {
    revenueToday: 300, revenueMonth: 1200, revenueMonthPrev: 900, expensesMonth: 500, expensesMonthPrev: 700,
    netMonth: 700, netMonthPrev: 200, revenueYtd: 9000, expensesYtd: 4000, netYtd: 5000,
    receivables: 600, receivablesCount: 2, payables: 250, payablesCount: 3,
  },
  months, revenueBySource: [], expenseByCategory: [],
}

async function mountDashboard() {
  const w = mount(Dashboard, { global: { ...mountOpts().global, plugins: [...mountOpts().global.plugins, router] } })
  await flushPromises()
  return w
}

describe('Dashboard', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    Object.values(api).forEach(fn => fn.mockReset())
    api.getFinanceOverview.mockResolvedValue(overview)
    api.listTasks.mockResolvedValue({ content: [
      { id: 1, title: 'Call the Fudan office', status: 'PENDING', priority: 'HIGH', dueDate: '2020-01-01' },
      { id: 2, title: 'Review scholarship list', status: 'IN_PROGRESS', priority: 'LOW', dueDate: '2999-01-01' },
      { id: 3, title: 'Done already', status: 'COMPLETED', priority: 'LOW', dueDate: null },
    ] })
    api.listPrograms.mockResolvedValue({ totalElements: 30, content: [] })
    api.listUniversities.mockResolvedValue({ totalElements: 12, content: [] })
    api.listScholarships.mockResolvedValue({ totalElements: 9, content: [] })
    api.listUsers.mockResolvedValue({ total: 250, rows: [] })
    api.listPayments.mockResolvedValue({ totalElements: 4, content: [] })
    api.listMyNotifications.mockResolvedValue({ content: [
      { id: 1, type: 'APPLICATION_UPDATE', title: 'Application NAD2026001 moved to review', body: '', createdAt: '2026-10-25T08:00:00', read: false },
    ] })
    api.listApplications.mockImplementation(async (params: { status?: string }) => {
      if (params.status === 'IN_REVIEW') return { totalElements: 5, content: [] }
      if (params.status === 'OFFER') return { totalElements: 2, content: [] }
      if (params.status) return { totalElements: 0, content: [] }
      return { totalElements: 7, content: [{
        id: 31, reference: 'NAD2026007', applicantName: 'Amina Diallo', opportunityTitle: 'MBBS', universityName: 'Fudan University',
        currentStatus: 'IN_REVIEW', assigneeName: null, submittedAt: '2026-10-24T10:00:00',
      }] }
    })
  })

  it('shows the no-access state and fetches nothing scoped for an account without permissions', async () => {
    useUserStore().permissions = []
    const w = await mountDashboard()
    expect(w.text()).toContain('Nothing to show here yet')
    for (const fn of [api.listUsers, api.listUniversities, api.listPrograms, api.listScholarships, api.getFinanceOverview,
      api.listTasks, api.listApplications, api.listPayments]) {
      expect(fn).not.toHaveBeenCalled()
    }
  })

  it('shows a finance-only account the money figures and nothing else', async () => {
    useUserStore().permissions = ['nad:finance:view']
    const w = await mountDashboard()
    const text = w.text()
    expect(api.getFinanceOverview).toHaveBeenCalledTimes(1)
    expect(text).toContain('CNY 1,200')
    expect(text).toContain('+33% vs last month')
    expect(text).toContain('Bills to pay')
    expect(api.listUsers).not.toHaveBeenCalled()
    expect(api.listApplications).not.toHaveBeenCalled()
    expect(api.listTasks).not.toHaveBeenCalled()
  })

  it('puts what needs a person first: payments to confirm, applications in review, own tasks with the overdue count, bills', async () => {
    useUserStore().permissions = ['*:*:*']
    const w = await mountDashboard()
    const att = (key: string) => w.find(`[data-test="attention-${key}"]`).text()
    expect(att('payments')).toContain('4')
    expect(att('review')).toContain('5')
    expect(att('tasks')).toContain('2')
    expect(att('tasks')).toContain('1 overdue')
    expect(att('bills')).toContain('3')
    expect(api.listTasks).toHaveBeenCalledWith(expect.objectContaining({ mine: true }))
  })

  it('shows the pipeline, the latest applications, own open tasks, activity and the catalogue to the platform owner', async () => {
    useUserStore().permissions = ['*:*:*']
    const w = await mountDashboard()
    const text = w.text()
    expect(w.findAll('[data-test="funnel-row"]')).toHaveLength(7)
    for (const part of ['Amina Diallo', 'Fudan University', 'Call the Fudan office', 'Review scholarship list', 'moved to review', 'Total universities', '250']) {
      expect(text).toContain(part)
    }
    expect(text).not.toContain('Done already')
  })

  it('does not ask for applications, payments or tasks without their permissions', async () => {
    useUserStore().permissions = ['nad:university:list']
    await mountDashboard()
    expect(api.listApplications).not.toHaveBeenCalled()
    expect(api.listPayments).not.toHaveBeenCalled()
    expect(api.listTasks).not.toHaveBeenCalled()
    expect(api.listUniversities).toHaveBeenCalled()
  })

  it('always loads the callers own activity feed', async () => {
    useUserStore().permissions = ['nad:university:list']
    await mountDashboard()
    expect(api.listMyNotifications).toHaveBeenCalledWith({ page: 0, size: 6 })
  })

  it('refreshes every permitted source when the refresh button is pressed', async () => {
    useUserStore().permissions = ['*:*:*']
    const w = await mountDashboard()
    api.getFinanceOverview.mockClear()
    api.listTasks.mockClear()
    await w.findAll('button').find(b => b.text() === 'Refresh')!.trigger('click')
    await flushPromises()
    expect(api.getFinanceOverview).toHaveBeenCalledTimes(1)
    expect(api.listTasks).toHaveBeenCalledTimes(1)
  })
})
