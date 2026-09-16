import type { SessionDto } from '~/types/catalog'

type Status = 'unknown' | 'guest' | 'authed'
type Applicant = NonNullable<SessionDto['applicants']>[number]

export function useSession() {
  const status = useState<Status>('nad-session-status', () => 'unknown')
  const user = useState<SessionDto['user'] | null>('nad-session-user', () => null)
  const applicants = useState<Applicant[]>('nad-session-applicants', () => [])
  const activeApplicantId = useState<number | null>('nad-session-active', () => null)
  const localePath = useLocalePath()

  async function refresh() {
    const s = await $fetch<SessionDto>('/api/student-session')
    if (s.authenticated) {
      status.value = 'authed'
      user.value = s.user ?? null
      applicants.value = s.applicants ?? []
      // reselect if the previously active id is gone (e.g. a different
      // account signed in on this device) — never keep a stale selection
      if (!applicants.value.some(a => a.applicantId === activeApplicantId.value)) {
        activeApplicantId.value = applicants.value[0]?.applicantId ?? null
      }
    }
    else {
      status.value = 'guest'
      user.value = null
      applicants.value = []
      activeApplicantId.value = null
    }
  }

  async function signOut() {
    await $fetch('/api/student-session', { method: 'DELETE' }).catch(() => undefined)
    status.value = 'guest'
    user.value = null
    applicants.value = []
    activeApplicantId.value = null
    // so the next account signed in on this device re-evaluates from scratch,
    // not this account's cached onboarding-complete state
    useOnboarding().invalidate()
    await navigateTo(localePath('/'))
  }

  function setActiveApplicant(id: number) {
    activeApplicantId.value = id
  }

  return { status, user, applicants, activeApplicantId, refresh, signOut, setActiveApplicant }
}
