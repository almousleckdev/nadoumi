import { describe, expect, it } from 'vitest'
import { ONBOARDING_STEPS } from '~/constants/onboarding'
import { resumeStepIndex } from '~/utils/onboardingResume'

const completeOnly = (...sections: string[]) => (section: string) => sections.includes(section)

describe('resumeStepIndex', () => {
  it('starts at the first step when there is no profile yet', () => {
    expect(resumeStepIndex(false, () => true)).toBe(0)
  })

  it('resumes at the first step the server reports incomplete', () => {
    expect(resumeStepIndex(true, completeOnly('PROFILE'))).toBe(ONBOARDING_STEPS.indexOf('identity'))
    expect(resumeStepIndex(true, completeOnly('PROFILE', 'PHOTO', 'PASSPORT'))).toBe(ONBOARDING_STEPS.indexOf('education'))
  })

  it('resumes at identity while only one of photo and passport is done', () => {
    expect(resumeStepIndex(true, completeOnly('PROFILE', 'PHOTO'))).toBe(ONBOARDING_STEPS.indexOf('identity'))
  })

  it('skips over the steps that are complete, even when an earlier one was left open', () => {
    expect(resumeStepIndex(true, completeOnly('PROFILE', 'PHOTO', 'PASSPORT', 'EDUCATION', 'INTERESTS', 'LOCATION')))
      .toBe(ONBOARDING_STEPS.indexOf('contact'))
  })

  it('lands on review once every required section is complete', () => {
    const all = completeOnly('PROFILE', 'PHOTO', 'PASSPORT', 'EDUCATION', 'INTERESTS', 'LOCATION', 'CONTACT')
    expect(resumeStepIndex(true, all)).toBe(ONBOARDING_STEPS.indexOf('review'))
  })
})
