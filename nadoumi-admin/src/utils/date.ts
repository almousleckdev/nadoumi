/** Today as `yyyy-mm-dd` (UTC), the format the API and the date pickers use. */
export function todayIso(now: Date = new Date()): string {
  return now.toISOString().slice(0, 10)
}

function parse(value: string): Date {
  return new Date(value.replace(' ', 'T'))
}

export function formatDate(value: string | null | undefined): string {
  if (!value) return ''
  const date = parse(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString()
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return ''
  const date = parse(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}

const SECONDS_PER_MINUTE = 60
const MINUTES_PER_HOUR = 60
const HOURS_PER_DAY = 24

export function formatDuration(seconds: number): string {
  if (seconds < SECONDS_PER_MINUTE) return `${seconds}s`
  const minutes = Math.floor(seconds / SECONDS_PER_MINUTE)
  if (minutes < MINUTES_PER_HOUR) return `${minutes}m`
  const hours = Math.floor(minutes / MINUTES_PER_HOUR)
  const restMinutes = minutes % MINUTES_PER_HOUR
  if (hours < HOURS_PER_DAY) return restMinutes ? `${hours}h ${restMinutes}m` : `${hours}h`
  return `${Math.floor(hours / HOURS_PER_DAY)}d ${hours % HOURS_PER_DAY}h`
}
