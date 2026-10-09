import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { defineComponent, h } from 'vue'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({ getApplicant: vi.fn(), listAccess: vi.fn(), deleteStudentAccount: vi.fn() }))
vi.mock('@/api/applicant', () => api)

const system = vi.hoisted(() => ({ changeUserStatus: vi.fn(), getUser: vi.fn() }))
vi.mock('@/api/system', () => system)

import ApplicantDetail from '@/views/applicants/detail.vue'
import { useUserStore } from '@/stores/user'

const applicant = {
  id: 7, givenName: 'Amina', familyName: 'Benali', dob: null, nationality: 'MA', passportNo: null,
  email: 'amina@example.com', phone: null, status: 'ACTIVE', createdAt: '2026-01-01 10:00:00',
}

const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/applicants/:id', component: { template: '<div />' } }],
})

const tabStub = (name: string) => defineComponent({
  name,
  props: { canEdit: { type: Boolean, default: undefined } },
  setup: (props) => () => h('div', { 'data-test': name, 'data-can-edit': String(props.canEdit) }),
})

async function mountDetail() {
  await router.push('/applicants/7')
  const w = mount(ApplicantDetail, {
    global: {
      ...mountOpts().global,
      plugins: [...mountOpts().global.plugins, router],
      stubs: {
        OverviewTab: tabStub('overview'), EducationTab: tabStub('education'), ScoresTab: tabStub('scores'),
        ContactsTab: tabStub('contacts'), InterestsTab: true, LocationTab: true, WorkTab: true, AccessTab: true,
      },
    },
  })
  await flushPromises()
  return w
}

describe('Applicant detail', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    useUserStore().permissions = ['*:*:*']
    api.getApplicant.mockReset().mockResolvedValue(applicant)
    api.listAccess.mockReset().mockResolvedValue([])
    system.changeUserStatus.mockReset()
    system.getUser.mockReset()
  })

  it('shows the applicant and the tabs, including test scores', async () => {
    const w = await mountDetail()
    expect(w.text()).toContain('Amina Benali')
    for (const label of ['Overview', 'Education', 'Test scores', 'Contacts']) expect(w.text()).toContain(label)
  })

  it('shows no account section for an applicant that has no student account', async () => {
    const w = await mountDetail()
    expect(w.find('[data-test="account-panel"]').exists()).toBe(false)
    expect(system.getUser).not.toHaveBeenCalled()
  })

  it('lets the overview edit only when the caller may edit applicants', async () => {
    expect((await mountDetail()).find('[data-test="overview"]').attributes('data-can-edit')).toBe('true')
    useUserStore().permissions = ['nad:applicant:list']
    expect((await mountDetail()).find('[data-test="overview"]').attributes('data-can-edit')).toBe('false')
  })

  it('shows an error state when the applicant cannot be loaded', async () => {
    api.getApplicant.mockRejectedValue(new Error('boom'))
    const w = await mountDetail()
    expect(w.text()).toContain('boom')
  })
})
