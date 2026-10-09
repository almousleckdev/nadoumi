import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { defineComponent, h } from 'vue'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({ getApplicant: vi.fn(), listAccess: vi.fn(), deleteApplicant: vi.fn() }))
const confirmAnswer = vi.hoisted(() => ({ value: true }))
vi.mock('@/composables/useConfirm', () => ({ useConfirm: () => ({ confirm: vi.fn(async () => confirmAnswer.value) }) }))
vi.mock('@/api/applicant', () => api)

const system = vi.hoisted(() => ({ changeUserStatus: vi.fn(), getUser: vi.fn() }))
vi.mock('@/api/system', () => system)

import ApplicantDetail from '@/views/applicants/detail.vue'
import { useUserStore } from '@/stores/user'

const applicant = {
  id: 7, publicId: '00000000-0000-4000-8000-000000000007', givenName: 'Amina', familyName: 'Benali', dob: null, nationality: 'MA', passportNo: null,
  email: 'amina@example.com', phone: null, status: 'ACTIVE', createdAt: '2026-01-01 10:00:00',
}

const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/applicants/:id', component: { template: '<div />' } }, { path: '/applicants', component: { template: '<div />' } }],
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
        OverviewTab: tabStub('overview'), EducationTab: tabStub('education'),
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
    api.deleteApplicant.mockReset().mockResolvedValue(undefined)
    confirmAnswer.value = true
    system.changeUserStatus.mockReset()
    system.getUser.mockReset()
  })

  it('shows the applicant and the tabs, with no test scores tab', async () => {
    const w = await mountDetail()
    expect(w.text()).toContain('Amina Benali')
    for (const label of ['Overview', 'Education', 'Contacts']) expect(w.text()).toContain(label)
    expect(w.text()).not.toContain('Test scores')
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

  it('deletes the applicant after a confirmation and returns to the list', async () => {
    const w = await mountDetail()
    await w.find('[data-test="delete-applicant"]').trigger('click')
    await flushPromises()
    expect(api.deleteApplicant).toHaveBeenCalledWith('7')
    expect(router.currentRoute.value.path).toBe('/applicants')
  })

  it('keeps the applicant when the confirmation is declined', async () => {
    confirmAnswer.value = false
    const w = await mountDetail()
    await w.find('[data-test="delete-applicant"]').trigger('click')
    await flushPromises()
    expect(api.deleteApplicant).not.toHaveBeenCalled()
  })

  it('offers no delete without the delete permission', async () => {
    useUserStore().permissions = ['nad:applicant:view']
    const w = await mountDetail()
    expect(w.find('[data-test="delete-applicant"]').exists()).toBe(false)
  })
})
