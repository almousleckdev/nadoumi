import { describe, expect, it } from 'vitest'
import { firstOpenStep, openSteps, stepDone, type ProgressFacts } from '~/utils/applyProgress'

const done = (over: Partial<ProgressFacts> = {}): ProgressFacts => ({
  hasApplicant: true,
  sectionComplete: () => true,
  missingFormSections: [],
  documentsReady: true,
  signatureReady: true,
  feeSettled: false,
  ...over,
})

describe('application progress', () => {
  it('counts a step done only when the server says all its sections are complete', () => {
    const facts = done({ sectionComplete: key => key !== 'PASSPORT' })
    expect(stepDone('profile', facts)).toBe(true)
    expect(stepDone('identity', facts)).toBe(false) // photo and passport
    expect(stepDone('education', facts)).toBe(true)
  })

  it('needs the application form sections too: address, family and the supporter and China contacts', () => {
    expect(stepDone('address', done({ missingFormSections: ['PERSONAL_DETAILS'] }))).toBe(false)
    expect(stepDone('family', done({ missingFormSections: ['FAMILY'] }))).toBe(false)
    expect(stepDone('support', done({ missingFormSections: ['CHINA_EMERGENCY'] }))).toBe(false)
    expect(stepDone('support', done({ missingFormSections: ['SIGNATURE'] }))).toBe(true) // not this step's concern
    expect(stepDone('family', done({ missingFormSections: null }))).toBe(false) // not known yet
  })

  it('never blocks on work experience, which is optional', () => {
    expect(stepDone('work', done())).toBe(true)
    expect(openSteps(done({ sectionComplete: () => false }))).not.toContain('work')
  })

  it('follows the documents, the signature and the fee reported by their own cards', () => {
    expect(stepDone('documents', done({ documentsReady: false }))).toBe(false)
    expect(stepDone('signature', done({ signatureReady: false }))).toBe(false)
    expect(stepDone('review', done({ feeSettled: true }))).toBe(true)
  })

  it('continues at the first required step that is not done, or the review when all are', () => {
    expect(firstOpenStep(done({ sectionComplete: key => key !== 'EDUCATION' }))).toBe('education')
    expect(firstOpenStep(done({ missingFormSections: ['FAMILY'] }))).toBe('family')
    expect(firstOpenStep(done())).toBe('review')
  })

  it('lists what stands between the student and Submit, in step order', () => {
    const facts = done({ sectionComplete: key => key !== 'PHOTO', documentsReady: false, signatureReady: false })
    expect(openSteps(facts)).toEqual(['identity', 'documents', 'signature'])
  })
})
