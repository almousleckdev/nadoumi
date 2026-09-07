import request from '@/utils/request'
import type { Page } from './applicant'

/*--- expenses -- */

export type ExpenseStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'PAID' | 'REJECTED'
export const EXPENSE_STATUSES: ExpenseStatus[] = ['DRAFT', 'SUBMITTED', 'APPROVED', 'PAID', 'REJECTED']

/** Allowed forward moves, mirrors ExpenseStatus.canMoveTo on the backend. */
export const EXPENSE_TRANSITIONS: Record<ExpenseStatus, ExpenseStatus[]> = {
  DRAFT: ['SUBMITTED', 'REJECTED'],
  SUBMITTED: ['APPROVED', 'REJECTED', 'DRAFT'],
  APPROVED: ['PAID', 'REJECTED'],
  PAID: [],
  REJECTED: ['DRAFT'],
}

export const PAYMENT_METHODS = ['CASH', 'BANK_TRANSFER', 'CARD', 'WECHAT', 'ALIPAY', 'OTHER'] as const

export interface ExpenseCategory {
  id: number
  code: string
  name: string
  sortOrder: number
  active: boolean
}

export interface Expense {
  id: number
  receiptNo: string | null
  categoryId: number | null
  categoryName: string | null
  title: string
  description: string | null
  amount: number
  currency: string
  spentOn: string
  vendor: string | null
  paymentMethod: string | null
  status: ExpenseStatus
  submittedByName: string | null
  approvedByName: string | null
  approvedAt: string | null
  paidAt: string | null
  notes: string | null
  createTime: string | null
  updateTime: string | null
}

export interface ExpenseInput {
  categoryId?: number | null
  /** Free-typed category; used only when categoryId is null (find-or-create). */
  categoryName?: string | null
  title: string
  description?: string | null
  amount: number
  currency: string
  spentOn: string
  vendor?: string | null
  paymentMethod?: string | null
  notes?: string | null
}

const E = '/api/staff/expenses'

export const listExpenses = (params: {
  q?: string
  status?: string
  categoryId?: number
  from?: string
  to?: string
  page?: number
  size?: number
}) => request.get<unknown, Page<Expense>>(E, { params })

export const getExpense = (id: number | string) => request.get<unknown, Expense>(`${E}/${id}`)
export const listExpenseCategories = () => request.get<unknown, ExpenseCategory[]>(`${E}/categories`)
export const createExpense = (body: ExpenseInput) => request.post<unknown, Expense>(E, body)
export const updateExpense = (id: number | string, body: ExpenseInput) =>
  request.put<unknown, Expense>(`${E}/${id}`, body)
export const changeExpenseStatus = (id: number | string, status: ExpenseStatus) =>
  request.put<unknown, Expense>(`${E}/${id}/status`, { status })
export const deleteExpense = (id: number | string) => request.delete(`${E}/${id}`)
/** Absolute URL of the printable receipt (opened in a new tab). */
export const expenseReceiptUrl = (id: number | string) => {
  const base = (import.meta.env.VITE_APP_BASE_API || '/dev-api').replace(/\/$/, '')
  return `${base}${E}/${id}/receipt`
}

/* --- revenue -- */

export const REVENUE_SOURCES = [
  'APPLICATION_FEE', 'SERVICE_FEE', 'COMMISSION', 'TUITION_SHARE', 'OTHER',
] as const

export interface Revenue {
  id: number
  source: string
  title: string
  description: string | null
  amount: number
  currency: string
  receivedOn: string
  reference: string | null
  relatedType: string | null
  relatedId: number | null
  recordedByName: string | null
  notes: string | null
  createTime: string | null
  updateTime: string | null
}

export interface RevenueInput {
  source: string
  title: string
  description?: string | null
  amount: number
  currency: string
  receivedOn: string
  reference?: string | null
  relatedType?: string | null
  relatedId?: number | null
  notes?: string | null
}

const R = '/api/staff/revenue'

export const listRevenue = (params: {
  q?: string
  source?: string
  from?: string
  to?: string
  page?: number
  size?: number
}) => request.get<unknown, Page<Revenue>>(R, { params })

export const getRevenue = (id: number | string) => request.get<unknown, Revenue>(`${R}/${id}`)
export const createRevenue = (body: RevenueInput) => request.post<unknown, Revenue>(R, body)
export const updateRevenue = (id: number | string, body: RevenueInput) =>
  request.put<unknown, Revenue>(`${R}/${id}`, body)
export const deleteRevenue = (id: number | string) => request.delete(`${R}/${id}`)

/*---- derived summary --- */

export interface CurrencyLine {
  currency: string
  revenue: number
  expenses: number
  net: number
}
export interface FinanceBucket {
  label: string
  currency: string
  total: number
  count: number
}
export interface FinanceSummary {
  from: string | null
  to: string | null
  byCurrency: CurrencyLine[]
  revenueBySource: FinanceBucket[]
  expenseByCategory: FinanceBucket[]
}

export const getFinanceSummary = (params: { from?: string, to?: string } = {}) =>
  request.get<unknown, FinanceSummary>('/api/staff/finance/summary', { params })
