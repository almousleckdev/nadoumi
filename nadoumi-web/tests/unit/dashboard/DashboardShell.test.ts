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

  it('asks before signing out from the account menu, and does not sign out yet', async () => {
    useSignOutConfirm().dismiss()
    const w = await mountSuspended(DashboardShell, { slots: { default: () => 'body' } })
    await w.find('[data-test="account-menu"]').trigger('click')
    await w.find('[data-test="sign-out"]').trigger('click')
    expect(useSignOutConfirm().open.value).toBe(true)
    expect(signOut).not.toHaveBeenCalled()
  })

  it('asks before signing out from the sidebar too', async () => {
    useSignOutConfirm().dismiss()
    const w = await mountSuspended(DashboardShell, { slots: { default: () => 'body' } })
    await w.find('[data-test="sidebar-sign-out"]').trigger('click')
    expect(useSignOutConfirm().open.value).toBe(true)
    expect(signOut).not.toHaveBeenCalled()
  })

  it('shows the real, buildable sidebar sections — nothing linking to an unbuilt feature', async () => {
    const w = await mountSuspended(DashboardShell, { slots: { default: () => 'body' } })
    for (const label of ['Overview', 'My Applications', 'Documents', 'Messages', 'Notifications', 'Help & Support', 'Education', 'Settings']) {
      expect(w.text()).toContain(label)
    }
  })

  it('does not link to the public Scholarships page from the dashboard sidebar', async () => {
    const w = await mountSuspended(DashboardShell, { slots: { default: () => 'body' } })
    expect(w.text()).not.toContain('Scholarships')
  })
})
