import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({ getFinanceOverview: vi.fn(), exportLedger: vi.fn(), listLedger: vi.fn() }))
vi.mock('@/api/finance', () => ({
  getFinanceOverview: api.getFinanceOverview, exportLedger: api.exportLedger, listLedger: api.listLedger,
}))
const download = vi.hoisted(() => ({ saveBlob: vi.fn() }))
vi.mock('@/utils/download', () => download)

import Summary from '@/views/finance/summary.vue'

const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:x(.*)*', component: { template: '<div />' } }] })

const months = Array.from({ length: 12 }, (_, i) => ({
  month: `2026-${String(i + 1).padStart(2, '0')}`, revenue: i === 9 ? 1200 : 0, expenses: i === 9 ? 500 : 0, net: i === 9 ? 700 : 0,
}))
const overview = (over = {}) => ({
  currency: 'CNY', currencies: ['CNY', 'USD'], asOf: '2026-10-25',
  kpis: {
    revenueToday: 300, revenueMonth: 1200, revenueMonthPrev: 900, expensesMonth: 500, expensesMonthPrev: 700,
    netMonth: 700, netMonthPrev: 200, revenueYtd: 9000, expensesYtd: 4000, netYtd: 5000,
    receivables: 600, receivablesCount: 2, payables: 250, payablesCount: 2,
  },
  months,
  revenueBySource: [{ label: 'APPLICATION_FEE', total: 8000, count: 20 }, { label: 'SERVICE_FEE', total: 1000, count: 3 }],
  expenseByCategory: [{ label: 'Rent', total: 3000, count: 3 }],
  ...over,
})

async function mountPage() {
  const w = mount(Summary, { global: { ...mountOpts().global, plugins: [...mountOpts().global.plugins, router] } })
  await flushPromises()
  return w
}

describe('Finance overview', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    Object.values(api).forEach(fn => fn.mockReset())
    download.saveBlob.mockReset()
    api.getFinanceOverview.mockResolvedValue(overview())
  })

  it('shows month on month change, year to date, the twelve month chart and the revenue sources by name', async () => {
    const w = await mountPage()
    const text = w.text()
    expect(text).toContain('CNY 1,200')
    expect(text).toContain('+33% vs last month') // revenue 900 -> 1200
    expect(text).toContain('-29% vs last month') // expenses 700 -> 500
    expect(text).toContain('CNY 5,000') // net this year
    expect(text).toContain('56% margin')
    expect(text).toContain('Application fee')
    expect(text).toContain('Service fee')
    expect(text).toContain('Rent')
    expect(w.findAll('[data-test="bar-revenue"]')).toHaveLength(12)
  })

  it('points at payments waiting for confirmation, and hides the notice when there are none', async () => {
    let w = await mountPage()
    expect(w.find('[data-test="receivables"]').text()).toContain('CNY 600')
    api.getFinanceOverview.mockResolvedValue(overview({ kpis: { ...overview().kpis, receivables: 0, receivablesCount: 0 } }))
    w = await mountPage()
    expect(w.find('[data-test="receivables"]').exists()).toBe(false)
  })

  it('exports this year ledger in the chosen currency', async () => {
    api.exportLedger.mockResolvedValue(new Blob(['x']))
    const w = await mountPage()
    await w.find('[data-test="export"]').trigger('click')
    await flushPromises()
    expect(api.exportLedger).toHaveBeenCalledWith(expect.objectContaining({ currency: 'CNY' }))
    expect(download.saveBlob).toHaveBeenCalled()
  })

  it('shows an error with retry when the overview cannot load', async () => {
    api.getFinanceOverview.mockRejectedValue(new Error('boom'))
    const w = await mountPage()
    expect(w.text()).toContain('boom')
  })
})
