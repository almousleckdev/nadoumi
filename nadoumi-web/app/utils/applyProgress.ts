import type { ApplyStep } from '~/constants/apply'
import { APPLY_STEPS, OPTIONAL_APPLY_STEPS, STEP_FORM_SECTIONS, STEP_ONBOARDING_SECTIONS } from '~/constants/apply'

/** Everything the wizard knows about where an application stands, gathered from the server and the step cards. */
export interface ProgressFacts {
  hasApplicant: boolean
  /** Whether the server reports an onboarding section (`PROFILE`, `PHOTO`, ...) complete. */
  sectionComplete: (key: string) => boolean
  /** The application form's sections still missing (`FAMILY`, ...); null until the server has answered. */
  missingFormSections: readonly string[] | null
  documentsReady: boolean
  signatureReady: boolean
  feeSettled: boolean
}

/** True once everything this step asks for is in. A step that asks for nothing of its own is always done. */
export function stepDone(step: ApplyStep, facts: ProgressFacts): boolean {
  if (step === 'start') return facts.hasApplicant
  if (step === 'documents') return facts.documentsReady
  if (step === 'signature') return facts.signatureReady
  if (step === 'review') return facts.feeSettled
  const sections = STEP_ONBOARDING_SECTIONS[step] ?? []
  const formSections = STEP_FORM_SECTIONS[step] ?? []
  if (!sections.length && !formSections.length) return OPTIONAL_APPLY_STEPS.includes(step)
  return sections.every(facts.sectionComplete)
    && formSections.every(code => facts.missingFormSections !== null && !facts.missingFormSections.includes(code))
}

const isRequiredBeforeReview = (step: ApplyStep) => step !== 'review' && !OPTIONAL_APPLY_STEPS.includes(step)

/** The required steps before the review that still need work: what stands between the student and Submit. */
export function openSteps(facts: ProgressFacts): ApplyStep[] {
  return APPLY_STEPS.filter(step => isRequiredBeforeReview(step) && !stepDone(step, facts))
}

/** Where a returning student continues: the first required step not done yet, or the review when all are. */
export function firstOpenStep(facts: ProgressFacts): ApplyStep {
  return openSteps(facts)[0] ?? 'review'
}
