import { describe, it, expect } from 'vitest'
import {
  MAX_GALLERY, blankUniversityForm, buildUniversityPayload, formFromUniversity,
} from '@/views/universities/universityForm'
import type { University } from '@/api/university'

const university = {
  id: 5, name: 'Fudan University', nameCn: null, country: 'CN', type: 'PUBLIC',
  city: 'Shanghai', province: null, foundedYear: 1905, totalStudents: 35000,
  internationalStudents: null, facultyCount: 3000, website: null, rankingTier: 'Top 50',
  introduction: 'A leading university.', history: null, campusInfo: null, accommodationInfo: null,
  nearbyInfo: null, admissionsEmail: null, officePhone: null,
  logoImageUrl: null, coverImageUrl: null, logoMediaId: 101, bannerMediaId: 102,
  recommended: true, featured: false, publicPartner: null, partnerStatus: null,
  status: 'ACTIVE', publishStatus: 'PUBLISHED', remark: null,
  rankings: [{ id: 1, source: 'QS', rankPosition: 34, rankYear: 2026, note: null }],
  highlights: [{ id: 1, kind: 'HIGHLIGHT', text: 'C9 League member' }],
  gallery: [{ id: 1, imageUrl: 'https://img.example/campus.jpg', mediaId: 103, url: 'https://cdn/campus.jpg', caption: 'Main campus' }],
} as unknown as University

describe('blankUniversityForm', () => {
  it('starts as an active draft with no partnership and no children', () => {
    expect(blankUniversityForm()).toMatchObject({
      name: '', country: '', partnerStatus: 'NONE', status: 'ACTIVE', publishStatus: 'DRAFT',
      highlights: [], rankings: [], gallery: [], departments: [],
    })
  })

  it('returns independent objects on every call', () => {
    const a = blankUniversityForm()
    a.gallery.push({ imageUrl: null, mediaId: 1, url: null, caption: null })
    expect(blankUniversityForm().gallery).toEqual([])
  })
})

describe('buildUniversityPayload', () => {
  it('trims text, uppercases the country and turns blanks into null', () => {
    const form = blankUniversityForm()
    Object.assign(form, { name: '  Fudan ', nameCn: '  ', country: ' cn ', city: ' Shanghai ', website: '' })
    expect(buildUniversityPayload(form)).toMatchObject({
      name: 'Fudan', nameCn: null, country: 'CN', city: 'Shanghai', website: null,
    })
  })

  it('keeps uploaded gallery rows that carry only a media id', () => {
    const form = blankUniversityForm()
    form.gallery.push({ imageUrl: null, mediaId: 71, url: null, caption: null })
    form.gallery.push({ imageUrl: ' https://img/a.jpg ', mediaId: null, url: null, caption: ' Hall ' })
    expect(buildUniversityPayload(form).gallery).toEqual([
      { mediaId: 71, imageUrl: undefined, caption: null },
      { mediaId: null, imageUrl: 'https://img/a.jpg', caption: 'Hall' },
    ])
  })

  it('drops gallery rows with neither a media id nor an image url', () => {
    const form = blankUniversityForm()
    form.gallery.push({ imageUrl: '  ', mediaId: null, url: null, caption: 'x' })
    form.gallery.push({ imageUrl: null, mediaId: null, url: 'blob:held', caption: null })
    expect(buildUniversityPayload(form).gallery).toEqual([])
  })

  it('caps the gallery at the backend limit', () => {
    const form = blankUniversityForm()
    for (let i = 1; i <= MAX_GALLERY + 3; i++) form.gallery.push({ imageUrl: null, mediaId: i, url: null, caption: null })
    expect(buildUniversityPayload(form).gallery).toHaveLength(MAX_GALLERY)
  })

  it('drops blank highlights and incomplete rankings', () => {
    const form = blankUniversityForm()
    form.highlights.push({ kind: 'HIGHLIGHT', text: '  ' } as never, { kind: 'ADVANTAGE', text: ' Great ' } as never)
    form.rankings.push(
      { source: 'QS', rankPosition: null, rankYear: null, note: null } as never,
      { source: ' THE ', rankPosition: '12', rankYear: '2026', note: '  ' } as never,
    )
    const p = buildUniversityPayload(form)
    expect(p.highlights).toEqual([{ kind: 'ADVANTAGE', text: 'Great' }])
    expect(p.rankings).toEqual([{ source: 'THE', rankPosition: 12, rankYear: 2026, note: null }])
  })
})

describe('formFromUniversity', () => {
  it('copies the record into the form and turns missing text into empty strings', () => {
    expect(formFromUniversity(university)).toMatchObject({
      id: 5, name: 'Fudan University', nameCn: '', country: 'CN', province: '', history: '',
      logoMediaId: 101, bannerMediaId: 102, publicPartner: false, partnerStatus: 'NONE', remark: '',
    })
  })

  it('copies the child collections', () => {
    const form = formFromUniversity(university)
    expect(form.rankings).toEqual([{ id: 1, source: 'QS', rankPosition: 34, rankYear: 2026, note: null }])
    expect(form.gallery).toEqual([
      { imageUrl: 'https://img.example/campus.jpg', mediaId: 103, url: 'https://cdn/campus.jpg', caption: 'Main campus' },
    ])
  })

  it('survives a round trip through buildUniversityPayload', () => {
    const p = buildUniversityPayload(formFromUniversity(university))
    expect(p).toMatchObject({ name: 'Fudan University', country: 'CN', logoMediaId: 101, partnerStatus: 'NONE' })
    expect(p.gallery).toEqual([{ mediaId: 103, imageUrl: 'https://img.example/campus.jpg', caption: 'Main campus' }])
    expect(p.highlights).toEqual([{ kind: 'HIGHLIGHT', text: 'C9 League member' }])
  })
})
