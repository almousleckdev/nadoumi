import {
  SECTION_CONTACT, SECTION_EDUCATION, SECTION_INTERESTS, SECTION_LOCATION, SECTION_PASSPORT, SECTION_PHOTO, SECTION_PROFILE,
} from '~/constants/onboarding'

/**
 * The application wizard's steps, in order. It runs outside the dashboard: first what and who the application is for,
 * then everything the Nadoumi application form needs (the onboarding sections plus the form's own extra details),
 * then the documents, the signature, and last the preview, the payment and the submit.
 */
export const APPLY_STEPS = [
  'start', 'profile', 'identity', 'address', 'education', 'work', 'family', 'support', 'study', 'documents',
  'signature', 'review',
] as const
export type ApplyStep = typeof APPLY_STEPS[number]

/** Codes of the application form's own sections, as `GET .../form-readiness` reports what is still missing. */
export const FORM_PERSONAL_DETAILS = 'PERSONAL_DETAILS'
export const FORM_FAMILY = 'FAMILY'
export const FORM_SUPPORTER = 'SUPPORTER'
export const FORM_CHINA_EMERGENCY = 'CHINA_EMERGENCY'

/** The onboarding sections each step needs complete, as the server reports them. Work is optional. */
export const STEP_ONBOARDING_SECTIONS: Partial<Record<ApplyStep, readonly string[]>> = {
  profile: [SECTION_PROFILE],
  identity: [SECTION_PHOTO, SECTION_PASSPORT],
  address: [SECTION_LOCATION, SECTION_CONTACT],
  education: [SECTION_EDUCATION],
  study: [SECTION_INTERESTS],
}

/** The application form's own sections each step needs complete. */
export const STEP_FORM_SECTIONS: Partial<Record<ApplyStep, readonly string[]>> = {
  address: [FORM_PERSONAL_DETAILS],
  family: [FORM_FAMILY],
  support: [FORM_SUPPORTER, FORM_CHINA_EMERGENCY],
}

/** The steps a student may skip: they add to the application but never block it. */
export const OPTIONAL_APPLY_STEPS: readonly ApplyStep[] = ['work']

export const isApplyStep = (value: unknown): value is ApplyStep =>
  typeof value === 'string' && (APPLY_STEPS as readonly string[]).includes(value)
