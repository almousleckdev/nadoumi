import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { defineComponent, h } from 'vue'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({ getApplicant: vi.fn() }))
vi.mock('@/api/applicant', () => api)

const system = vi.hoisted(() => ({ changeUserStatus: vi.fn() }))
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
    system.changeUserStatus.mockReset()
  })

  it('shows the applicant and the tabs, including test scores', async () => {
    const w = await mountDetail()
    expect(w.text()).toContain('Amina Benali')
    for (const label of ['Overview', 'Education', 'Test scores', 'Contacts']) expect(w.text()).toContain(label)
  })

  it('never offers to suspend or block a user account from an applicant page', async () => {
    const w = await mountDetail()
    const labels = w.findAll('button').map(b => b.text())
    expect(labels).not.toContain('Suspend')
    expect(labels).not.toContain('Block')
    expect(system.changeUserStatus).not.toHaveBeenCalled()
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
