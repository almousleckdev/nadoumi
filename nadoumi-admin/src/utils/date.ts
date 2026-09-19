/** Today as `yyyy-mm-dd` (UTC), the format the API and the date pickers use. */
export function todayIso(now: Date = new Date()): string {
  return now.toISOString().slice(0, 10)
}
