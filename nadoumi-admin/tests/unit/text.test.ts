import { describe, it, expect } from 'vitest'
import { humanize, titleCase } from '@/utils/text'

describe('titleCase', () => {
  it('capitalises the first letter and lowercases the rest', () => {
    expect(titleCase('PUBLIC')).toBe('Public')
    expect(titleCase('fully')).toBe('Fully')
  })

  it('keeps underscores untouched', () => {
    expect(titleCase('NON_DEGREE')).toBe('Non_degree')
  })

  it('returns empty and nullish input unchanged', () => {
    expect(titleCase('')).toBe('')
    expect(titleCase(null as unknown as string)).toBeNull()
  })
})

describe('humanize', () => {
  it('title-cases and turns underscores into spaces', () => {
    expect(humanize('NON_DEGREE')).toBe('Non degree')
    expect(humanize('AUTUMN_SEPTEMBER')).toBe('Autumn september')
  })

  it('returns empty and nullish input unchanged', () => {
    expect(humanize('')).toBe('')
    expect(humanize(null as unknown as string)).toBeNull()
  })
})
