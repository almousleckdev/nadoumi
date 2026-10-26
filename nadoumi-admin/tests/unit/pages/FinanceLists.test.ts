import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const finance = vi.hoisted(() => ({
  listLedger: vi.fn(), getLedgerTotals: vi.fn(), exportLedger: vi.fn(),
  listRevenue: vi.fn(), getRevenueTotals: vi.fn(), deleteRevenue: vi.fn(),
  listExpenses: vi.fn(), getExpenseTotals: vi.fn(), listExpenseCategories: vi.fn(),
  deleteExpense: vi.fn(), changeExpenseStatus: vi.fn(), expenseReceiptUrl: vi.fn(),
  REVENUE_SOURCES: ['APPLICATION_FEE', 'SERVICE_FEE'],
  EXPENSE_STATUSES: ['DRAFT', 'APPROVED'], EXPENSE_TRANSITIONS: { DRAFT: [], APPROVED: [] },
}))
vi.mock('@/api/finance', () => finance)
const hr = vi.hoisted(() => ({ getPayroll: vi.fn() }))
vi.mock('@/api/hr', () => hr)
const payments = vi.hoisted(() => ({ listPayments: vi.fn(), deletePayment: vi.fn(), PAYMENT_STATUSES: ['PROOF_SUBMITTED'], PAYMENT_KINDS: ['APPLICATION_FEE', 'SERVICE_FEE'] }))
vi.mock('@/api/payment', () => payments)

import Ledger from '@/views/finance/ledger.vue'
import Revenue from '@/views/finance/revenue.vue'
import Expenses from '@/views/finance/expenses.vue'
import Payroll from '@/views/finance/payroll.vue'
import PaymentsPage from '@/views/payments/index.vue'

const empty = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

const mountPage = async (component: object) => {
  const w = mount(component, { ...mountOpts(), attachTo: document.body })
  await flushPromises()
  return w
}

beforeEach(() => {
  setActivePinia(createPinia())
  for (const group of [finance, hr, payments]) {
    Object.values(group).forEach(fn => typeof fn === 'function' && 'mockReset' in fn && fn.mockReset())
  }
  finance.listLedger.mockResolvedValue(empty)
  finance.listRevenue.mockResolvedValue(empty)
  finance.listExpenses.mockResolvedValue(empty)
  finance.listExpenseCategories.mockResolvedValue([])
  payments.listPayments.mockResolvedValue(empty)
})

describe('totals above the lists', () => {
  it('shows what came in, what went out and the difference per currency on the ledger', async () => {
    finance.getLedgerTotals.mockResolvedValue([{ currency: 'CNY', revenue: 1000.5, expenses: 400.25, net: 600.25, entries: 6 }])
    const w = await mountPage(Ledger)
    const text = w.get('[data-test="totals"]').text()
    expect(text).toContain('CNY 1,000.5')
    expect(text).toContain('CNY 400.25')
    expect(text).toContain('CNY 600.25')
    w.unmount()
  })

  it('asks for the revenue totals with the same filters as the list', async () => {
    finance.getRevenueTotals.mockResolvedValue([{ currency: 'CNY', total: 8000, count: 20 }])
    const w = await mountPage(Revenue)
    expect(finance.getRevenueTotals).toHaveBeenCalledWith(expect.objectContaining({ q: undefined, source: undefined }))
    expect(w.get('[data-test="totals"]').text()).toContain('CNY 8,000')
    w.unmount()
  })

  it('asks for the expense totals and survives a failure of the totals call', async () => {
    finance.getExpenseTotals.mockRejectedValue(new Error('boom'))
    const w = await mountPage(Expenses)
    expect(finance.getExpenseTotals).toHaveBeenCalled()
    expect(w.find('[data-test="totals"]').exists()).toBe(false)
    expect(finance.listExpenses).toHaveBeenCalled()
    w.unmount()
  })
})

describe('payments queue', () => {
  it('shows what each payment is for and can filter by it', async () => {
    payments.listPayments.mockResolvedValue({
      ...empty, totalElements: 1,
      content: [{ id: 1, applicationId: 5, applicationRef: 'NAD2027001', applicantName: 'Amina Diallo', kind: 'SERVICE_FEE', amount: 1500,
        currency: 'CNY', reference: 'NAD-AB12CD34', status: 'PROOF_SUBMITTED', studentTxnNo: 'TXN123456', hasProof: true,
        createdAt: '2026-10-26T10:00:00', submittedAt: '2026-10-26T10:05:00', expiresAt: '2026-10-26T10:30:00', verifiedAt: null, rejectionReason: null }],
    })
    const w = await mountPage(PaymentsPage)
    expect(w.text()).toContain('Nadoumi service fee')
    expect(w.find('[data-test="kind-filter"]').exists()).toBe(true)
    w.unmount()
  })
})

describe('payroll search', () => {
  it('narrows the table by name, department or position', async () => {
    hr.getPayroll.mockResolvedValue({
      byCurrency: [],
      rows: [
        { employeeId: 1, name: 'Amina Diallo', deptName: 'Admissions', positionTitle: 'Case officer', employmentStatus: 'ACTIVE', salaryAmount: 5000, salaryCurrency: 'CNY', payFrequency: 'MONTHLY', monthlyEquivalent: 5000 },
        { employeeId: 2, name: 'Li Wei', deptName: 'Finance', positionTitle: 'Accountant', employmentStatus: 'ACTIVE', salaryAmount: 6000, salaryCurrency: 'CNY', payFrequency: 'MONTHLY', monthlyEquivalent: 6000 },
      ],
    })
    const w = await mountPage(Payroll)
    expect(w.text()).toContain('Amina Diallo')
    expect(w.text()).toContain('Li Wei')

    const input = w.get('input[data-test="payroll-search"]')
    await input.setValue('account')
    expect(w.text()).not.toContain('Amina Diallo')
    expect(w.text()).toContain('Li Wei')
    w.unmount()
  })
})
