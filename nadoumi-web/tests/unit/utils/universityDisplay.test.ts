import { describe, expect, it } from 'vitest'
import { cheapestProgram, internationalShare, levelsOffered, linkHref, mapUrl } from '~/utils/universityDisplay'
import type { ProgramCard } from '~/types/catalog'

const program = (levels: string[], tuitionAmount: number | null, programType = 'DEGREE') =>
  ({ id: 1, levels, tuitionAmount, programType }) as unknown as ProgramCard

describe('internationalShare', () => {
  it('is the rounded percentage of international students', () => {
    expect(internationalShare(35000, 6300)).toBe('18%')
    expect(internationalShare(1000, 5)).toBe('1%')
  })

  it('is empty when a figure is missing or the total is zero', () => {
    expect(internationalShare(null, 10)).toBe('')
    expect(internationalShare(0, 10)).toBe('')
    expect(internationalShare(1000, undefined)).toBe('')
  })

  it('never exceeds 100%', () => {
    expect(internationalShare(100, 400)).toBe('100%')
  })
})

describe('levelsOffered', () => {
  it('lists each level once, DIPLOMA to PHD', () => {
    expect(levelsOffered([program(['MASTER', 'PHD'], null), program(['BACHELOR', 'MASTER'], null), program([], null, 'LANGUAGE')]))
      .toEqual(['BACHELOR', 'MASTER', 'PHD'])
  })
})

describe('cheapestProgram', () => {
  it('returns the programme with the lowest tuition, ignoring unpriced ones', () => {
    const a = program(['MASTER'], 30000)
    const b = { ...program(['MASTER'], 18000), id: 2 }
    expect(cheapestProgram([a, program(['PHD'], null), b])?.id).toBe(2)
    expect(cheapestProgram([program(['PHD'], null)])).toBeNull()
  })
})

describe('links', () => {
  it('builds a map search url from the address', () => {
    expect(mapUrl('5 Yiheyuan Road, Beijing')).toBe('https://www.google.com/maps/search/?api=1&query=5%20Yiheyuan%20Road%2C%20Beijing')
  })

  it('only returns http(s) urls so a stored value can never become a script link', () => {
    expect(linkHref('https://facebook.com/pku')).toBe('https://facebook.com/pku')
    expect(linkHref('javascript:alert(1)')).toBeNull()
    expect(linkHref('pku_official')).toBeNull()
  })
})
