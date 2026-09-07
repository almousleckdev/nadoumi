import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const sys = vi.hoisted(() => ({
  listUsers: vi.fn(),
  deleteUsers: vi.fn(),
  changeUserStatus: vi.fn(),
  resetUserPwd: vi.fn(),
  listRoles: vi.fn(),
  deleteRoles: vi.fn(),
  changeRoleStatus: vi.fn(),
  listConfigs: vi.fn(),
  deleteConfigs: vi.fn(),
  getConfig: vi.fn(),
  createConfig: vi.fn(),
  updateConfig: vi.fn(),
  refreshConfigCache: vi.fn(),
}))
vi.mock('@/api/system', () => sys)

const notif = vi.hoisted(() => ({
  listStaffNotifications: vi.fn(),
  getStaffNotification: vi.fn(),
}))
vi.mock('@/api/notification', () => notif)

const confirm = vi.hoisted(() => vi.fn())
vi.mock('@/composables/useConfirm', () => ({ useConfirm: () => ({ confirm }) }))

import Users from '@/views/users/index.vue'
import Roles from '@/views/roles/index.vue'
import Config from '@/views/config/index.vue'
import Notifications from '@/views/notifications/index.vue'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: '/staff', component: { template: '<div />' }, meta: { userType: '00' } },
    { path: '/students', component: { template: '<div />' }, meta: { userType: '10' } },
    { path: '/:x(.*)*', component: { template: '<div />' } },
  ],
})

function mountView(cmp: unknown) {
  return mount(cmp as never, {
    global: { ...mountOpts().global, plugins: [...mountOpts().global.plugins, router] },
  })
}

beforeEach(async () => {
  setActivePinia(createPinia())
  useUserStore().permissions = ['*:*:*']
  Object.values(sys).forEach(fn => fn.mockReset())
  Object.values(notif).forEach(fn => fn.mockReset())
  confirm.mockReset()
})

describe('Users screen', () => {
  const page = {
    rows: [{ userId: 5, userName: 'jdoe', nickName: 'J Doe', userType: '00', email: 'j@x.io', phonenumber: null, sex: '0', status: '0', deptId: null, createTime: null, loginDate: null }],
    total: 1,
    code: 200,
  }

  it('queries with the route userType and renders rows', async () => {
    sys.listUsers.mockResolvedValue(page)
    await router.push('/staff')
    const w = mountView(Users)
    await flushPromises()
    expect(sys.listUsers).toHaveBeenCalledWith(expect.objectContaining({ userType: '00' }))
    expect(w.text()).toContain('jdoe')
  })

  it('deletes a user only after the confirm resolves true', async () => {
    sys.listUsers.mockResolvedValue(page)
    sys.deleteUsers.mockResolvedValue(undefined)
    await router.push('/staff')
    const w = mountView(Users)
    await flushPromises()
    const vm = w.vm as unknown as { doDelete: (r: unknown) => Promise<void> }

    confirm.mockResolvedValueOnce(false)
    await vm.doDelete(page.rows[0])
    expect(sys.deleteUsers).not.toHaveBeenCalled()

    confirm.mockResolvedValueOnce(true)
    await vm.doDelete(page.rows[0])
    await flushPromises()
    expect(sys.deleteUsers).toHaveBeenCalledWith([5])
  })
})

describe('Roles screen', () => {
  it('loads roles on mount', async () => {
    sys.listRoles.mockResolvedValue({
      rows: [{ roleId: 2, roleName: 'Ops', roleKey: 'ops', roleSort: 1, dataScope: '1', status: '0', remark: null, createTime: null, admin: false }],
      total: 1, code: 200,
    })
    const w = mountView(Roles)
    await flushPromises()
    expect(sys.listRoles).toHaveBeenCalled()
    expect(w.text()).toContain('Ops')
  })
})

describe('Config screen', () => {
  it('refreshes the cache through the api', async () => {
    sys.listConfigs.mockResolvedValue({ rows: [], total: 0, code: 200 })
    sys.refreshConfigCache.mockResolvedValue(undefined)
    const w = mountView(Config)
    await flushPromises()
    const vm = w.vm as unknown as { refreshCache: () => Promise<void> }
    await vm.refreshCache()
    expect(sys.refreshConfigCache).toHaveBeenCalled()
  })
})

describe('Notifications oversight screen', () => {
  it('lists notifications and opens a detail with deliveries', async () => {
    notif.listStaffNotifications.mockResolvedValue({
      content: [{ id: 9, type: 'CONTACT_INQUIRY_RECEIVED', title: 'New contact inquiry', body: 'x', dataJson: null, applicationId: null, conversationId: null, messageId: null, createdAt: '2026-09-06', readAt: null, read: false }],
      page: 0, size: 15, totalElements: 1, totalPages: 1,
    })
    notif.getStaffNotification.mockResolvedValue({
      id: 9, recipientUserId: 3, type: 'CONTACT_INQUIRY_RECEIVED', title: 'New contact inquiry', body: 'x',
      dataJson: null, createdAt: '2026-09-06', readAt: null,
      deliveries: [{ id: 1, channel: 'EMAIL', provider: 'mail', providerMessageId: null, status: 'SENT', attempts: 1, lastError: null, sentAt: '2026-09-06' }],
    })
    const w = mountView(Notifications)
    await flushPromises()
    expect(notif.listStaffNotifications).toHaveBeenCalled()
    const vm = w.vm as unknown as { openDetail: (r: unknown) => Promise<void> }
    await vm.openDetail({ id: 9 })
    await flushPromises()
    expect(notif.getStaffNotification).toHaveBeenCalledWith(9)
    expect(w.text()).toContain('EMAIL')
  })
})
