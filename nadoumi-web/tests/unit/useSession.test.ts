import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockNuxtImport } from '@nuxt/test-utils/runtime'
import { useSession } from '~/composables/useSession'
import { useOnboarding } from '~/composables/useOnboarding'

// `vi.mock('#imports', ...)` is inert in this project's nuxt test env (same as the
// Task 10 server tests), so Nuxt auto-imports are stubbed with `mockNuxtImport`.
const { nav } = vi.hoisted(() => ({ nav: vi.fn() }))
mockNuxtImport('navigateTo', () => nav)
mockNuxtImport('useLocalePath', () => () => (p: string) => p)

const fetchImpl = vi.fn()
vi.stubGlobal('$fetch', fetchImpl)

beforeEach(() => {
  fetchImpl.mockReset(); nav.mockReset()
  // reset shared useState between tests
  useSession().status.value = 'unknown'
  useSession().user.value = null
  useSession().applicants.value = []
  useSession().activeApplicantId.value = null
  useOnboarding().state.value = 'unknown'
})

describe('useSession', () => {
  it('refresh() → guest when unauthenticated', async () => {
    fetchImpl.mockResolvedValueOnce({ authenticated: false })
    await useSession().refresh()
    expect(useSession().status.value).toBe('guest')
  })

  it('refresh() → authed, populates user + default active applicant', async () => {
    fetchImpl.mockResolvedValueOnce({
      authenticated: true,
      user: { userId: 1, username: 'a', nickName: 'A' },
      applicants: [{ applicantId: 5, accessRole: 'OWNER', capabilities: [] }],
    })
    await useSession().refresh()
    expect(useSession().status.value).toBe('authed')
    expect(useSession().activeApplicantId.value).toBe(5)
  })

  it('signOut() resets to guest and navigates home', async () => {
    fetchImpl.mockResolvedValueOnce({ signedIn: false })
    await useSession().signOut()
    expect(useSession().status.value).toBe('guest')
    expect(nav).toHaveBeenCalledWith('/')
  })

  it('signOut() invalidates onboarding state so the next account re-evaluates', async () => {
    useOnboarding().state.value = 'complete'
    fetchImpl.mockResolvedValueOnce({ signedIn: false })
    await useSession().signOut()
    expect(useOnboarding().state.value).toBe('unknown')
  })

  it('refresh() reselects the active applicant when the previous one is gone', async () => {
    useSession().activeApplicantId.value = 999 // stale id from a different account
    fetchImpl.mockResolvedValueOnce({
      authenticated: true,
      user: { userId: 1, username: 'a', nickName: 'A' },
      applicants: [{ applicantId: 5, accessRole: 'OWNER', capabilities: [] }],
    })
    await useSession().refresh()
    expect(useSession().activeApplicantId.value).toBe(5)
  })

  it('refresh() keeps the active applicant when it is still present', async () => {
    useSession().activeApplicantId.value = 5
    fetchImpl.mockResolvedValueOnce({
      authenticated: true,
      user: { userId: 1, username: 'a', nickName: 'A' },
      applicants: [
        { applicantId: 5, accessRole: 'OWNER', capabilities: [] },
        { applicantId: 6, accessRole: 'OWNER', capabilities: [] },
      ],
    })
    await useSession().refresh()
    expect(useSession().activeApplicantId.value).toBe(5)
  })

  it('refresh() clears the active applicant when the account has none', async () => {
    useSession().activeApplicantId.value = 999
    fetchImpl.mockResolvedValueOnce({
      authenticated: true,
      user: { userId: 1, username: 'a', nickName: 'A' },
      applicants: [],
    })
    await useSession().refresh()
    expect(useSession().activeApplicantId.value).toBeNull()
  })
})
