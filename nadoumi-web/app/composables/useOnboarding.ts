/**
 * Onboarding completion state for the signed-in student.
 *
 * The source of truth is the server: `ApplicantDto.onboardingComplete` mirrors
 * `nad_applicant.onboarded_at`, which only the onboarding-complete endpoint can set.
 * The gate fails CLOSED: if the state cannot be read, the student is treated as
 * not onboarded and kept out of the dashboard.
 *
 * The student's own applicant is the first one returned for the account.
 */
export type OnboardingState = 'unknown' | 'incomplete' | 'complete'

export function useOnboarding() {
  const state = useState<OnboardingState>('onboarding-state', () => 'unknown')
  const { listMine } = useApplicant()

  /** Load once per session (or when forced); returns the resolved state. */
  async function ensure(force = false): Promise<OnboardingState> {
    if (state.value !== 'unknown' && !force) return state.value
    try {
      const mine = await listMine()
      state.value = mine[0]?.onboardingComplete === true ? 'complete' : 'incomplete'
    }
    catch {
      state.value = 'incomplete'
    }
    return state.value
  }

  /** Call after anything that can change completion so the gate re-evaluates. */
  function invalidate() {
    state.value = 'unknown'
  }

  /** Record a known result without another request (e.g. right after Finish). */
  function markComplete() {
    state.value = 'complete'
  }

  return { state, ensure, invalidate, markComplete }
}
