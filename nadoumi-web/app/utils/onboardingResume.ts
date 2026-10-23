import { ONBOARDING_STEPS, STEP_SECTIONS } from '~/constants/onboarding'

/**
 * Where a returning student continues: the first step whose required sections the server still reports
 * incomplete, or the review step once everything required is done. A student with no profile yet starts at
 * the first step. Derived from the server, so it survives a closed browser and a different device.
 */
export function resumeStepIndex(hasProfile: boolean, isComplete: (section: string) => boolean): number {
  if (!hasProfile) return 0
  const open = ONBOARDING_STEPS.findIndex(step => STEP_SECTIONS[step].some(section => !isComplete(section)))
  return open === -1 ? ONBOARDING_STEPS.length - 1 : open
}
