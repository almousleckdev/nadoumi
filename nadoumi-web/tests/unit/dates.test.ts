import { describe, it, expect } from 'vitest'
import { formatTimestamp } from '~/utils/dates'

describe('formatTimestamp', () => {
  it('returns an empty string for null, undefined and empty input', () => {
    expect(formatTimestamp(null, 'en')).toBe('')
    expect(formatTimestamp(undefined, 'en')).toBe('')
    expect(formatTimestamp('', 'en')).toBe('')
  })

  it('formats an ISO timestamp with a medium date by default', () => {
    const expected = new Intl.DateTimeFormat('en', { dateStyle: 'medium' }).format(new Date('2026-09-19T10:30:00'))
    expect(formatTimestamp('2026-09-19T10:30:00', 'en')).toBe(expected)
  })

  it('accepts the space-separated timestamps the API returns', () => {
    const expected = new Intl.DateTimeFormat('en', { dateStyle: 'medium' }).format(new Date('2026-09-19T10:30:00'))
    expect(formatTimestamp('2026-09-19 10:30:00', 'en')).toBe(expected)
  })

  it('honours custom format options', () => {
    const options: Intl.DateTimeFormatOptions = { dateStyle: 'medium', timeStyle: 'short' }
    const expected = new Intl.DateTimeFormat('fr', options).format(new Date('2026-09-19T10:30:00'))
    expect(formatTimestamp('2026-09-19T10:30:00', 'fr', options)).toBe(expected)
  })

  it('returns an empty string instead of throwing for an unparseable value', () => {
    expect(formatTimestamp('not a date', 'en')).toBe('')
  })

  it('returns an empty string instead of throwing for an invalid locale', () => {
    expect(formatTimestamp('2026-09-19T10:30:00', 'not_a_locale!!')).toBe('')
  })
})
