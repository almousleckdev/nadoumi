/** Interviews are fixed in Beijing time; the student also sees the same moment on their own clock. */
export const BEIJING_ZONE = 'Asia/Shanghai'

const FORMAT: Intl.DateTimeFormatOptions = {
  weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
}

/** The zone of the device, for example `Africa/Conakry`. */
export function viewerZone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone
}

/** An instant as the clock in `zone` shows it, with the zone's short name. */
export function formatInZone(iso: string, zone: string, locale: string): string {
  return new Intl.DateTimeFormat(locale, { ...FORMAT, timeZone: zone, timeZoneName: 'short' }).format(new Date(iso))
}

/** Whether the student's device already shows Beijing time, so one line is enough. */
export function viewerIsInBeijing(): boolean {
  return viewerZone() === BEIJING_ZONE
}

/** The interview that is still on: the scheduled one, or `null` when none is. */
export function currentInterview<T extends { status: string }>(interviews: readonly T[]): T | null {
  return interviews.find(i => i.status === 'SCHEDULED') ?? null
}
