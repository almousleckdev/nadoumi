import { describe, it, expect } from 'vitest'
import { daysUntilDeadline, deadlineTone } from '~/utils/deadline'

describe('daysUntilDeadline', () => {
  it('returns null for a rolling (unset) deadline', () => {
    expect(daysUntilDeadline(null)).toBeNull()
    expect(daysUntilDeadline(undefined)).toBeNull()
    expect(daysUntilDeadline('')).toBeNull()
  })

  it('counts whole days to the end of the given date', () => {
    const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000)
    const iso = tomorrow.toISOString().slice(0, 10)
    expect(daysUntilDeadline(iso)).toBeGreaterThanOrEqual(1)
  })
})

describe('deadlineTone', () => {
  it('treats a rolling (null) deadline as ok', () => {
    expect(deadlineTone(null)).toBe('ok')
  })

  it('is passed once the deadline is behind us', () => {
    expect(deadlineTone(-1)).toBe('passed')
  })

  it('is urgent at the 10-day boundary and soon just past it', () => {
    expect(deadlineTone(10)).toBe('urgent')
    expect(deadlineTone(11)).toBe('soon')
  })

  it('is soon at the 45-day boundary and ok just past it', () => {
    expect(deadlineTone(45)).toBe('soon')
    expect(deadlineTone(46)).toBe('ok')
  })
})
