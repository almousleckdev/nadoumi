import type { ApplicantDto } from '~/types/catalog'

/**
 * Onboarding completion state for the signed-in student.
 *
 * Interim heuristic until the backend carries an explicit `onboarding_state`
 * on the applicant (PLATFORM_ARCHITECTURE.md Step 6): the primary applicant
 * profile counts as complete once the core identity fields are all present —
 * the same field set the admin uses for "incomplete profiles".
 */
export const ONBOARDING_REQUIRED: (keyof ApplicantDto)[] = [
  'givenName', 'familyName', 'dob', 'nationality', 'passportNo', 'phone',
]

export function isProfileComplete(a: ApplicantDto | null | undefined): boolean {
  return Boolean(a) && ONBOARDING_REQUIRED.every(k => Boolean(a![k]))
}

export function useOnboarding() {
  const state = useState<'unknown' | 'incomplete' | 'complete'>('onboarding-state', () => 'unknown')
  const { listMine } = useApplicant()

  /** Load once per session (or when forced); returns the resolved state. */
  async function ensure(force = false) {
    if (state.value !== 'unknown' && !force) return state.value
    try {
      const mine = await listMine()
      const primary = mine[0] ?? null
      state.value = isProfileComplete(primary) ? 'complete' : 'incomplete'
    }
    catch {
      // don't trap the user if the check itself fails
      state.value = 'complete'
    }
    return state.value
  }

  /** Call after a profile save so the gate re-evaluates on next navigation. */
  function invalidate() {
    state.value = 'unknown'
  }

  return { state, ensure, invalidate }
}
