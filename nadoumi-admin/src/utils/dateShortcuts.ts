/** The quick ranges finance screens offer next to a date-range picker. Dates are the user's local calendar days. */
export interface RangeShortcut {
  text: string
  value: () => [Date, Date]
}

const startOfMonth = (d: Date) => new Date(d.getFullYear(), d.getMonth(), 1)
const endOfMonth = (d: Date) => new Date(d.getFullYear(), d.getMonth() + 1, 0)

/** `labels` are the translated captions, in the order: this month, last month, last 30 days, this year. */
export function rangeShortcuts(labels: [string, string, string, string], now: () => Date = () => new Date()): RangeShortcut[] {
  const [thisMonth, lastMonth, last30, thisYear] = labels
  return [
    { text: thisMonth, value: () => [startOfMonth(now()), endOfMonth(now())] },
    {
      text: lastMonth,
      value: () => {
        const first = startOfMonth(now())
        const prev = new Date(first.getFullYear(), first.getMonth() - 1, 1)
        return [prev, endOfMonth(prev)]
      },
    },
    {
      text: last30,
      value: () => {
        const end = now()
        return [new Date(end.getFullYear(), end.getMonth(), end.getDate() - 29), end]
      },
    },
    { text: thisYear, value: () => [new Date(now().getFullYear(), 0, 1), new Date(now().getFullYear(), 11, 31)] },
  ]
}
