import { describe, it, expect } from 'vitest'
import { todayIso } from '@/utils/date'

describe('todayIso', () => {
  it('formats a date as yyyy-mm-dd', () => {
    expect(todayIso(new Date('2026-09-19T15:30:00Z'))).toBe('2026-09-19')
  })

  it('defaults to today', () => {
    expect(todayIso()).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  })
})
