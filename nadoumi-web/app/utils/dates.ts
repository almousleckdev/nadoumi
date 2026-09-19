const pad = (n: number) => String(n).padStart(2, '0')

/** A local date as `YYYY-MM-DD`, the format date inputs and the API use. */
export const isoDate = (d: Date): string => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

/** `today` plus whole months, clamped to the end of the month like the server's `LocalDate.plusMonths`. */
export function addMonths(today: Date, months: number): string {
  const first = new Date(today.getFullYear(), today.getMonth() + months, 1)
  const lastDay = new Date(first.getFullYear(), first.getMonth() + 1, 0).getDate()
  return isoDate(new Date(first.getFullYear(), first.getMonth(), Math.min(today.getDate(), lastDay)))
}

/** "2021-09 to Present" style range; `present` is the translated word for an open end. */
export function formatPeriod(start: string | null, end: string | null, current: boolean, present: string): string {
  const month = (iso: string | null) => (iso ?? '').slice(0, 7)
  const to = current ? present : month(end)
  return [month(start), to].filter(Boolean).join(' – ')
}
