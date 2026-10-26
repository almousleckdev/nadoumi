import { describe, expect, it } from 'vitest'
import { beijingWallTimeToIso, formatBeijing, isFutureInBeijing, toBeijingLocal } from '@/utils/beijingTime'

describe('beijingTime', () => {
  it('reads an instant as the Beijing clock shows it, whatever the browser zone is', () => {
    expect(toBeijingLocal(new Date('2026-11-02T07:00:00Z'))).toBe('2026-11-02T15:00')
    expect(toBeijingLocal(new Date('2026-11-02T20:30:00Z'))).toBe('2026-11-03T04:30')
  })

  it('turns a Beijing wall time into an instant that carries the +08:00 offset', () => {
    expect(beijingWallTimeToIso('2026-11-02T15:00')).toBe('2026-11-02T15:00:00+08:00')
    expect(new Date(beijingWallTimeToIso('2026-11-02T15:00:00')).toISOString()).toBe('2026-11-02T07:00:00.000Z')
  })

  it('judges "in the future" on the Beijing clock', () => {
    const now = new Date('2026-11-02T07:00:00Z') // 15:00 in Beijing
    expect(isFutureInBeijing('2026-11-02T14:59', now)).toBe(false)
    expect(isFutureInBeijing('2026-11-02T15:01', now)).toBe(true)
  })

  it('formats an instant on the Beijing clock', () => {
    expect(formatBeijing('2026-11-02T07:00:00Z')).toContain('15:00')
  })
})
