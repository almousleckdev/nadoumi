export const MS_PER_DAY = 86_400_000
/** Red once this many days or fewer remain. */
export const DEADLINE_URGENT_DAYS = 10
/** Amber once this many days or fewer remain (and more than DEADLINE_URGENT_DAYS). */
export const DEADLINE_SOON_DAYS = 45

export type DeadlineTone = 'passed' | 'urgent' | 'soon' | 'ok'

/** Whole days from now until 23:59:59 on `date` (yyyy-mm-dd), or null when there's no deadline (rolling). */
export function daysUntilDeadline(date: string | null | undefined): number | null {
  if (!date) return null
  const end = new Date(`${date}T23:59:59`).getTime()
  return Math.ceil((end - Date.now()) / MS_PER_DAY)
}

/** Urgency bucket for a day count. `null` (rolling/unset) reads as 'ok' — nothing to warn about. */
export function deadlineTone(days: number | null): DeadlineTone {
  if (days == null) return 'ok'
  if (days < 0) return 'passed'
  if (days <= DEADLINE_URGENT_DAYS) return 'urgent'
  if (days <= DEADLINE_SOON_DAYS) return 'soon'
  return 'ok'
}
