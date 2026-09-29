import { describe, it, expect } from 'vitest'
import { formatDate, formatDateTime, formatDuration, todayIso } from '@/utils/date'

describe('todayIso', () => {
  it('formats a date as yyyy-mm-dd', () => {
    expect(todayIso(new Date('2026-09-19T15:30:00Z'))).toBe('2026-09-19')
  })

  it('defaults to today', () => {
    expect(todayIso()).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  })
})

describe('formatDate', () => {
  it('returns an empty string for null, undefined and empty input', () => {
    expect(formatDate(null)).toBe('')
    expect(formatDate(undefined)).toBe('')
    expect(formatDate('')).toBe('')
  })

  it('formats an ISO timestamp as a locale date', () => {
    expect(formatDate('2026-09-19T10:30:00')).toBe(new Date('2026-09-19T10:30:00').toLocaleDateString())
  })

  it('accepts the space-separated timestamps the API returns', () => {
    expect(formatDate('2026-09-19 10:30:00')).toBe(new Date('2026-09-19T10:30:00').toLocaleDateString())
  })

  it('returns the raw value when it is not a date', () => {
    expect(formatDate('not a date')).toBe('not a date')
  })
})

describe('formatDateTime', () => {
  it('formats a timestamp as a locale date and time', () => {
    expect(formatDateTime('2026-09-19T10:30:00')).toBe(new Date('2026-09-19T10:30:00').toLocaleString())
  })

  it('returns an empty string for empty input', () => {
    expect(formatDateTime(null)).toBe('')
  })

  it('returns the raw value when it is not a date', () => {
    expect(formatDateTime('nope')).toBe('nope')
  })
})

describe('formatDuration', () => {
  it('shows seconds under a minute', () => {
    expect(formatDuration(45)).toBe('45s')
  })

  it('shows minutes under an hour', () => {
    expect(formatDuration(600)).toBe('10m')
  })

  it('shows hours with the remaining minutes', () => {
    expect(formatDuration(3600)).toBe('1h')
    expect(formatDuration(4500)).toBe('1h 15m')
  })

  it('shows days with the remaining hours', () => {
    expect(formatDuration(90000)).toBe('1d 1h')
  })
})
