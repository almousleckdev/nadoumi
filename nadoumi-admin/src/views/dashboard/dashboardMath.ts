import type { FinanceSummary } from '@/api/finance'
import type { Task } from '@/api/hr'
import type { Program } from '@/api/program'
import { humanize } from '@/utils/text'

export interface Slice {
  label: string
  value: number
}

export const DEFAULT_CURRENCY = 'CNY'
export const MAX_TOP_UNIVERSITIES = 8

const UNKNOWN_UNIVERSITY = 'Unknown'
const OPEN_TASK_STATUSES = new Set(['PENDING', 'IN_PROGRESS'])
const PRIORITY_ORDER = ['HIGH', 'MEDIUM', 'LOW']

export function formatMoney(currency: string, value: number): string {
  return `${currency} ${value.toLocaleString('en-US', { maximumFractionDigits: 0 })}`
}

export function leadingRevenue(summary: FinanceSummary | null): { text: string, currency: string } {
  const lines = summary?.byCurrency ?? []
  if (!lines.length) return { text: formatMoney(DEFAULT_CURRENCY, 0), currency: DEFAULT_CURRENCY }
  const top = [...lines].sort((a, b) => Number(b.revenue) - Number(a.revenue))[0]!
  const currency = top.currency ?? DEFAULT_CURRENCY
  return { text: formatMoney(currency, Number(top.revenue ?? 0)), currency }
}

export function summarizeFinance(summary: FinanceSummary | null) {
  const lines = summary?.byCurrency ?? []
  const top = lines.length
    ? [...lines].sort((a, b) => (Number(b.revenue) + Number(b.expenses)) - (Number(a.revenue) + Number(a.expenses)))[0]
    : null
  const currency = top?.currency ?? DEFAULT_CURRENCY
  const revenue = Number(top?.revenue ?? 0)
  const expenses = Number(top?.expenses ?? 0)
  const net = Number(top?.net ?? 0)
  return {
    currency,
    revenue: formatMoney(currency, revenue),
    expenses: formatMoney(currency, expenses),
    net: formatMoney(currency, net),
    margin: revenue > 0 ? `${Math.round((net / revenue) * 100)}%` : '0%',
  }
}

export function countBy<T>(items: T[], key: (item: T) => string): Map<string, number> {
  const counts = new Map<string, number>()
  for (const item of items) counts.set(key(item), (counts.get(key(item)) ?? 0) + 1)
  return counts
}

export function openTasks(tasks: Task[]): Task[] {
  return tasks.filter(task => OPEN_TASK_STATUSES.has(task.status))
}

export function prioritySlices(tasks: Task[], label: (priority: string) => string): Slice[] {
  const counts = countBy(tasks, task => task.priority)
  return PRIORITY_ORDER.filter(priority => counts.has(priority)).map(priority => ({
    label: label(priority),
    value: counts.get(priority)!,
  }))
}

export function topUniversitySlices(programs: Program[]): Slice[] {
  const counts = countBy(programs, program => program.universityName || UNKNOWN_UNIVERSITY)
  return [...counts.entries()]
    .map(([label, value]) => ({ label, value }))
    .sort((a, b) => b.value - a.value)
    .slice(0, MAX_TOP_UNIVERSITIES)
}

export function statusSlices(statuses: string[]): Slice[] {
  return [...countBy(statuses, status => status).entries()].map(([status, value]) => ({ label: humanize(status), value }))
}
