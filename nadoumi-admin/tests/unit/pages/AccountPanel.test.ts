import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({ listAccess: vi.fn(), deleteStudentAccount: vi.fn() }))
vi.mock('@/api/applicant', () => api)

const system = vi.hoisted(() => ({ getUser: vi.fn(), changeUserStatus: vi.fn() }))
vi.mock('@/api/system', () => system)

const confirmAnswer = vi.hoisted(() => ({ value: true }))
vi.mock('@/composables/useConfirm', () => ({ useConfirm: () => ({ confirm: vi.fn(async () => confirmAnswer.value) }) }))

import AccountPanel from '@/views/applicants/AccountPanel.vue'
import { useUserStore } from '@/stores/user'

const owner = { id: 1, userId: 42, applicantId: 7, accessRole: 'OWNER', status: 'ACTIVE' }
const student = (over = {}) => ({
  userId: 42, userName: 'amina', nickName: 'Amina', email: 'amina@example.com', status: '0', userType: '10', ...over,
})

const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/applicants', component: { template: '<div />' } }, { path: '/', component: { template: '<div />' } }],
})

async function mountPanel() {
  const w = mount(AccountPanel, {
    props: { id: '7' },
    global: {
      ...mountOpts().global,
      plugins: [...mountOpts().global.plugins, router],
      stubs: { StudentContactDialog: true },
    },
  })
  await flushPromises()
  return w
}

describe('Applicant account panel', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    useUserStore().permissions = ['*:*:*']
    confirmAnswer.value = true
    api.listAccess.mockReset().mockResolvedValue([owner])
    api.deleteStudentAccount.mockReset().mockResolvedValue(undefined)
    system.getUser.mockReset().mockResolvedValue({ code: 200, data: student() })
    system.changeUserStatus.mockReset().mockResolvedValue({})
    vi.spyOn(router, 'push').mockResolvedValue(undefined)
  })

  it('shows the owning student account with its status', async () => {
    const w = await mountPanel()
    expect(w.find('[data-test="account-panel"]').exists()).toBe(true)
    expect(w.text()).toContain('@amina')
    expect(system.getUser).toHaveBeenCalledWith(42)
  })

  it('stays hidden when the owner is staff rather than a student', async () => {
    system.getUser.mockResolvedValue({ code: 200, data: student({ userType: '00' }) })
    const w = await mountPanel()
    expect(w.find('[data-test="account-panel"]').exists()).toBe(false)
  })

  it('stays hidden when the applicant has no active owner account', async () => {
    api.listAccess.mockResolvedValue([{ ...owner, status: 'REVOKED' }])
    const w = await mountPanel()
    expect(w.find('[data-test="account-panel"]').exists()).toBe(false)
    expect(system.getUser).not.toHaveBeenCalled()
  })

  it('suspends an active account after confirmation', async () => {
    const w = await mountPanel()
    await w.find('[data-test="account-suspend"]').trigger('click')
    await flushPromises()
    expect(system.changeUserStatus).toHaveBeenCalledWith(42, '1')
    expect(w.find('[data-test="account-activate"]').exists()).toBe(true)
  })

  it('does not change the status when the confirmation is declined', async () => {
    confirmAnswer.value = false
    const w = await mountPanel()
    await w.find('[data-test="account-block"]').trigger('click')
    await flushPromises()
    expect(system.changeUserStatus).not.toHaveBeenCalled()
  })

  it('deletes the student after confirmation and returns to the applicants list', async () => {
    const w = await mountPanel()
    await w.find('[data-test="account-delete"]').trigger('click')
    await flushPromises()
    expect(api.deleteStudentAccount).toHaveBeenCalledWith(42)
    expect(router.push).toHaveBeenCalledWith('/applicants')
  })

  it('keeps the page when the server refuses the deletion', async () => {
    api.deleteStudentAccount.mockRejectedValue(new Error('This student has 1 application on record'))
    const w = await mountPanel()
    await w.find('[data-test="account-delete"]').trigger('click')
    await flushPromises()
    expect(router.push).not.toHaveBeenCalled()
    expect(w.find('[data-test="account-panel"]').exists()).toBe(true)
  })

  it('offers no delete button without the delete permission', async () => {
    useUserStore().permissions = ['system:user:edit', 'nad:applicant:access:view']
    const w = await mountPanel()
    expect(w.find('[data-test="account-delete"]').exists()).toBe(false)
    expect(w.find('[data-test="account-suspend"]').exists()).toBe(true)
  })
})
