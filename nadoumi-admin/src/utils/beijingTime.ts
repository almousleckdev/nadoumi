/** Interviews are set in Beijing time (China has no daylight saving, so the offset is fixed at +08:00). */
export const BEIJING_ZONE = 'Asia/Shanghai'
export const BEIJING_OFFSET = '+08:00'

const PARTS = ['year', 'month', 'day', 'hour', 'minute'] as const

/** `yyyy-MM-ddTHH:mm` for an instant as the clock in Beijing shows it. */
export function toBeijingLocal(instant: Date): string {
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: BEIJING_ZONE, hourCycle: 'h23', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
  }).formatToParts(instant)
  const get = (type: (typeof PARTS)[number]) => parts.find(p => p.type === type)!.value
  return `${get('year')}-${get('month')}-${get('day')}T${get('hour')}:${get('minute')}`
}

/** A Beijing wall time (`yyyy-MM-ddTHH:mm[:ss]`) as an ISO date-time that carries the Beijing offset. */
export function beijingWallTimeToIso(wall: string): string {
  const withSeconds = wall.length === 16 ? `${wall}:00` : wall
  return `${withSeconds}${BEIJING_OFFSET}`
}

/** Whether a Beijing wall time is still ahead of `now`. */
export function isFutureInBeijing(wall: string, now: Date = new Date()): boolean {
  return new Date(beijingWallTimeToIso(wall)).getTime() > now.getTime()
}

/** "Mon, 2 Nov 2026, 15:00" as the clock in Beijing shows an instant. */
export function formatBeijing(iso: string): string {
  return new Intl.DateTimeFormat('en-GB', {
    timeZone: BEIJING_ZONE, hourCycle: 'h23', weekday: 'short', day: 'numeric', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  }).format(new Date(iso))
}

/** The same instant on the viewer's own clock, with the zone name. */
export function formatLocal(iso: string): string {
  return new Intl.DateTimeFormat(undefined, {
    weekday: 'short', day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', timeZoneName: 'short',
  }).format(new Date(iso))
}
