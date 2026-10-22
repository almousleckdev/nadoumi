import { describe, expect, it } from 'vitest'
import { PROGRAM_RAILS, SCHOLARSHIP_FUNDING_RAILS, SCHOLARSHIP_HIGHLIGHT_RAILS } from '~/data/homeShowcase'

describe('home rails', () => {
  it('gives Featured, Recommended and Hot scholarships their own sections', () => {
    expect(SCHOLARSHIP_HIGHLIGHT_RAILS.map(r => Object.keys(r.query)[0])).toEqual(['featured', 'recommended', 'hot'])
  })

  it('gives every funding type its own section', () => {
    expect(SCHOLARSHIP_FUNDING_RAILS.map(r => r.query.funding)).toEqual(['FULLY', 'PARTIAL', 'SELF'])
  })

  it('only links "view all" with filters the catalog pages understand', () => {
    expect(SCHOLARSHIP_FUNDING_RAILS.every(r => r.viewAll?.startsWith('funding='))).toBe(true)
    expect(PROGRAM_RAILS.map(r => r.viewAll).filter(Boolean)).toEqual(['featured=true'])
  })

  it('uses unique ids so each section has its own async-data key', () => {
    const ids = [...SCHOLARSHIP_HIGHLIGHT_RAILS, ...SCHOLARSHIP_FUNDING_RAILS, ...PROGRAM_RAILS].map(r => r.id)
    expect(new Set(ids).size).toBe(ids.length)
  })
})
