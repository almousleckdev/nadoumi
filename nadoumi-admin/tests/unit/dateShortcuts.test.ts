import { describe, expect, it } from 'vitest'
import { rangeShortcuts } from '@/utils/dateShortcuts'

const day = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
const NOW = new Date(2026, 9, 26) // 26 October 2026

describe('rangeShortcuts', () => {
  const [thisMonth, lastMonth, last30, thisYear] = rangeShortcuts(['a', 'b', 'c', 'd'], () => NOW)

  it('covers this month from the first to the last day', () => {
    expect(thisMonth.value().map(day)).toEqual(['2026-10-01', '2026-10-31'])
  })

  it('covers the whole of last month, also across a year boundary', () => {
    expect(lastMonth.value().map(day)).toEqual(['2026-09-01', '2026-09-30'])
    const [, january] = rangeShortcuts(['a', 'b', 'c', 'd'], () => new Date(2027, 0, 15))
    expect(january.value().map(day)).toEqual(['2026-12-01', '2026-12-31'])
  })

  it('covers 30 days ending today, and the calendar year', () => {
    expect(last30.value().map(day)).toEqual(['2026-09-27', '2026-10-26'])
    expect(thisYear.value().map(day)).toEqual(['2026-01-01', '2026-12-31'])
  })
})
