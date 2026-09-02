import { describe, it, expect, vi } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import DashboardShell from '~/components/dashboard/DashboardShell.vue'

const signOut = vi.fn()
vi.mock('~/composables/useSession', () => ({
  useSession: () => ({
    user: ref({ userId: 1, username: 'sam', nickName: 'Sam' }),
    applicants: ref([{ applicantId: 1, accessRole: 'OWNER', capabilities: [] }]),
    activeApplicantId: ref(1),
    setActiveApplicant: vi.fn(),
    signOut,
  }),
}))

describe('DashboardShell', () => {
  it('renders the nav and hides the applicant switcher for a single applicant', async () => {
    const w = await mountSuspended(DashboardShell, { slots: { default: () => 'body' } })
    expect(w.text()).toContain('Overview')
    expect(w.text()).toContain('Profile')
    expect(w.text()).not.toContain('Switch applicant')
  })

  it('calls signOut from the account menu', async () => {
    const w = await mountSuspended(DashboardShell, { slots: { default: () => 'body' } })
    await w.find('button[aria-haspopup="menu"]').trigger('click')
    await w.find('[data-test="sign-out"]').trigger('click')
    expect(signOut).toHaveBeenCalled()
  })
})
