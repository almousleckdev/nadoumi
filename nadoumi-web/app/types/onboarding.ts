/** One row of the onboarding sidebar / mobile progress bar. */
export interface OnboardingStepView {
  key: string
  label: string
  optional?: boolean
  /** True once the server reports every section of the step complete; null for steps it does not track. */
  done: boolean | null
}
