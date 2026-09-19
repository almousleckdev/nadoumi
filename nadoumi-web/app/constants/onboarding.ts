export type DocumentStatus = 'done' | 'pending' | 'attention'

/** Badge tone for each document status. */
export const DOCUMENT_STATUS_TONES = { done: 'success', pending: 'neutral', attention: 'warning' } as const

/** Onboarding section keys as the server reports them. */
export const SECTION_PROFILE = 'PROFILE'
export const SECTION_PHOTO = 'PHOTO'
export const SECTION_PASSPORT = 'PASSPORT'
export const SECTION_EDUCATION = 'EDUCATION'
export const SECTION_INTERESTS = 'INTERESTS'
export const SECTION_LOCATION = 'LOCATION'
export const SECTION_CONTACT = 'CONTACT'

/** The wizard's steps, in order. `review` is the last and is not a server section. */
export const ONBOARDING_STEPS = [
  'personal', 'identity', 'education', 'interests', 'location', 'contact', 'work', 'review',
] as const
export type OnboardingStep = typeof ONBOARDING_STEPS[number]

/**
 * The server sections each step must complete before the student may move on. Work is optional,
 * so it has none; the review step is gated by Finish itself.
 */
export const STEP_SECTIONS: Record<OnboardingStep, readonly string[]> = {
  personal: [SECTION_PROFILE],
  identity: [SECTION_PHOTO, SECTION_PASSPORT],
  education: [SECTION_EDUCATION],
  interests: [SECTION_INTERESTS],
  location: [SECTION_LOCATION],
  contact: [SECTION_CONTACT],
  work: [],
  review: [],
}

/** How long the "setting up your dashboard" screen holds after Finish (a UX pause; the server is already done). */
export const FINISH_HOLD_MS = 15_000
