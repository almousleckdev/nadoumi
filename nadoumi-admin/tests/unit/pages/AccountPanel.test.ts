import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({ listAccess: vi.fn() }))
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
    system.getUser.mockReset().mockResolvedValue({ code: 200, data: student() })
    system.changeUserStatus.mockReset().mockResolvedValue({})
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

  it('asks for a reason before suspending, and sends it with the change', async () => {
    const w = await mountPanel()
    await w.find('[data-test="account-suspend"]').trigger('click')
    await flushPromises()
    expect(system.changeUserStatus).not.toHaveBeenCalled()
    const dialog = w.findComponent({ name: 'StatusReasonDialog' })
    expect(dialog.props('modelValue')).toBe(true)

    dialog.vm.$emit('confirm', 'Repeated fake documents')
    await flushPromises()

    expect(system.changeUserStatus).toHaveBeenCalledWith(42, '1', 'Repeated fake documents')
    expect(w.find('[data-test="account-activate"]').exists()).toBe(true)
    expect(w.find('[data-test="account-reason"]').text()).toContain('Repeated fake documents')
  })

  it('asks for a reason before blocking', async () => {
    const w = await mountPanel()
    await w.find('[data-test="account-block"]').trigger('click')
    await flushPromises()
    w.findComponent({ name: 'StatusReasonDialog' }).vm.$emit('confirm', 'Abusive messages to staff')
    await flushPromises()
    expect(system.changeUserStatus).toHaveBeenCalledWith(42, '2', 'Abusive messages to staff')
  })

  it('reactivates after a plain confirmation, with no reason', async () => {
    system.getUser.mockResolvedValue({ code: 200, data: student({ status: '1', statusReason: 'Under review' }) })
    const w = await mountPanel()
    await w.find('[data-test="account-activate"]').trigger('click')
    await flushPromises()
    expect(system.changeUserStatus).toHaveBeenCalledWith(42, '0', undefined)
    expect(w.find('[data-test="account-reason"]').exists()).toBe(false)
  })

  it('does not reactivate when the confirmation is declined', async () => {
    confirmAnswer.value = false
    system.getUser.mockResolvedValue({ code: 200, data: student({ status: '1' }) })
    const w = await mountPanel()
    await w.find('[data-test="account-activate"]').trigger('click')
    await flushPromises()
    expect(system.changeUserStatus).not.toHaveBeenCalled()
  })

  it('offers no status buttons without the edit permission', async () => {
    useUserStore().permissions = ['nad:applicant:access:view']
    const w = await mountPanel()
    expect(w.find('[data-test="account-suspend"]').exists()).toBe(false)
    expect(w.find('[data-test="account-block"]').exists()).toBe(false)
  })
})
