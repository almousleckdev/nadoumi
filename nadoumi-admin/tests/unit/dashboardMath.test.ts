import { describe, it, expect } from 'vitest'
import {
  formatMoney, leadingRevenue, summarizeFinance, openTasks, prioritySlices, topUniversitySlices,
  statusSlices, countBy, MAX_TOP_UNIVERSITIES,
} from '@/views/dashboard/dashboardMath'
import type { FinanceSummary } from '@/api/finance'

const summary = (lines: Array<Record<string, string | number>>) => ({ byCurrency: lines }) as unknown as FinanceSummary

describe('formatMoney', () => {
  it('groups thousands and drops decimals', () => {
    expect(formatMoney('CNY', 12345.67)).toBe('CNY 12,346')
    expect(formatMoney('USD', 0)).toBe('USD 0')
  })
})

describe('leadingRevenue', () => {
  it('falls back to zero yuan when there is no data', () => {
    expect(leadingRevenue(null)).toEqual({ text: 'CNY 0', currency: 'CNY' })
    expect(leadingRevenue(summary([]))).toEqual({ text: 'CNY 0', currency: 'CNY' })
  })

  it('picks the currency with the largest revenue', () => {
    const s = summary([
      { currency: 'USD', revenue: '500' },
      { currency: 'CNY', revenue: '9000' },
    ])
    expect(leadingRevenue(s)).toEqual({ text: 'CNY 9,000', currency: 'CNY' })
  })
})

describe('summarizeFinance', () => {
  it('returns zeroed figures when there is no data', () => {
    expect(summarizeFinance(null)).toEqual({
      currency: 'CNY', revenue: 'CNY 0', expenses: 'CNY 0', net: 'CNY 0', margin: '0%',
    })
  })

  it('uses the currency with the most activity and computes the margin', () => {
    const s = summary([
      { currency: 'USD', revenue: '100', expenses: '20', net: '80' },
      { currency: 'CNY', revenue: '10000', expenses: '4000', net: '6000' },
    ])
    expect(summarizeFinance(s)).toEqual({
      currency: 'CNY', revenue: 'CNY 10,000', expenses: 'CNY 4,000', net: 'CNY 6,000', margin: '60%',
    })
  })

  it('reports a zero margin when nothing was earned', () => {
    const s = summary([{ currency: 'CNY', revenue: '0', expenses: '500', net: '-500' }])
    expect(summarizeFinance(s).margin).toBe('0%')
  })
})

describe('openTasks', () => {
  it('keeps pending and in-progress tasks only', () => {
    const tasks = [{ status: 'PENDING' }, { status: 'IN_PROGRESS' }, { status: 'DONE' }, { status: 'CLOSED' }]
    expect(openTasks(tasks as never)).toHaveLength(2)
  })
})

describe('prioritySlices', () => {
  it('orders high, medium, low and skips priorities with no tasks', () => {
    const tasks = [{ priority: 'LOW' }, { priority: 'HIGH' }, { priority: 'HIGH' }]
    expect(prioritySlices(tasks as never, p => `label-${p}`)).toEqual([
      { label: 'label-HIGH', value: 2 },
      { label: 'label-LOW', value: 1 },
    ])
  })
})

describe('topUniversitySlices', () => {
  it('counts programmes per university, largest first, unknown when unnamed', () => {
    const programs = [
      { universityName: 'Fudan' }, { universityName: 'Fudan' }, { universityName: 'Tsinghua' }, { universityName: null },
    ]
    expect(topUniversitySlices(programs as never)).toEqual([
      { label: 'Fudan', value: 2 },
      { label: 'Tsinghua', value: 1 },
      { label: 'Unknown', value: 1 },
    ])
  })

  it('keeps only the top universities', () => {
    const programs = Array.from({ length: MAX_TOP_UNIVERSITIES + 3 }, (_, i) => ({ universityName: `U${i}` }))
    expect(topUniversitySlices(programs as never)).toHaveLength(MAX_TOP_UNIVERSITIES)
  })
})

describe('statusSlices', () => {
  it('humanizes the label and counts each status', () => {
    expect(statusSlices(['NEW', 'IN_REVIEW', 'NEW'])).toEqual([
      { label: 'New', value: 2 },
      { label: 'In review', value: 1 },
    ])
  })
})

describe('countBy', () => {
  it('counts occurrences in first-seen order', () => {
    expect([...countBy(['b', 'a', 'b'], x => x)]).toEqual([['b', 2], ['a', 1]])
  })
})
