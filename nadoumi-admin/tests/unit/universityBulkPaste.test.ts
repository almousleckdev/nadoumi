import { describe, it, expect } from 'vitest'
import { parseDepartments, parseRankings } from '@/views/universities/bulkPaste'
import { blankUniversityForm, formFromUniversity, DEFAULT_OFFICE_PHONE, DEFAULT_WEBSITE } from '@/views/universities/universityForm'
import type { University } from '@/api/university'

describe('parseDepartments', () => {
  it('reads English and Chinese names split by pipe, tab or a Latin-to-CJK boundary', () => {
    const { rows, skipped } = parseDepartments('School of Medicine | 医学院\nBusiness School\tBS 商学院\nSchool of Arts艺术学院')
    expect(rows.map(r => [r.name, r.nameCn])).toEqual([
      ['School of Medicine', '医学院'],
      ['Business School', 'BS 商学院'],
      ['School of Arts', '艺术学院'],
    ])
    expect(skipped).toBe(0)
  })

  it('strips list markers and skips blanks, duplicates and names already on the form', () => {
    const existing = [{ id: 1, name: 'Law School', nameCn: '' }]
    const { rows, skipped } = parseDepartments('- Law School\n1. Engineering\n\n• engineering', existing)
    expect(rows.map(r => r.name)).toEqual(['Engineering'])
    expect(skipped).toBe(2)
  })
})

describe('parseRankings', () => {
  it('parses comma, pipe and spaced lines, defaulting the year', () => {
    const { rows, skipped } = parseRankings('QS, 245, 2024\nTHE | 301 | 2025 | 301-350 band\nARWU #401', 2026)
    expect(rows).toEqual([
      { source: 'QS', rankPosition: 245, rankYear: 2024, note: null },
      { source: 'THE', rankPosition: 301, rankYear: 2025, note: '301-350 band' },
      { source: 'ARWU', rankPosition: 401, rankYear: 2026, note: null },
    ])
    expect(skipped).toBe(0)
  })

  it('skips lines without a positive rank or with an out-of-range year', () => {
    const { rows, skipped } = parseRankings('QS, abc\nTHE, 0\nARWU, 10, 1800\nUS News, 50, 2026', 2026)
    expect(rows.map(r => r.source)).toEqual(['US News'])
    expect(skipped).toBe(3)
  })
})

describe('university form contact defaults', () => {
  it('pre-fills Nadoumi contact details on a new university', () => {
    const form = blankUniversityForm()
    expect(form.website).toBe(DEFAULT_WEBSITE)
    expect(form.admissionsEmail).toBe('team@nadoumiconsulting.com')
    expect(form.officePhone).toBe(DEFAULT_OFFICE_PHONE)
  })

  it('keeps a stored value and fills only the empty ones when editing', () => {
    const u = { website: 'https://uni.edu', admissionsEmail: null, officePhone: '', rankings: [], gallery: [], highlights: [] } as unknown as University
    const form = formFromUniversity(u)
    expect(form.website).toBe('https://uni.edu')
    expect(form.admissionsEmail).toBe('team@nadoumiconsulting.com')
    expect(form.officePhone).toBe(DEFAULT_OFFICE_PHONE)
  })
})
