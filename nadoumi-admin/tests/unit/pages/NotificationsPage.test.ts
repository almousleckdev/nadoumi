import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({
  listStaffNotifications: vi.fn(),
  getStaffNotification: vi.fn(),
  listMyNotifications: vi.fn(),
  markNotificationRead: vi.fn(),
  markAllNotificationsRead: vi.fn(),
}))
vi.mock('@/api/notification', () => api)

const hr = vi.hoisted(() => ({ getTask: vi.fn() }))
vi.mock('@/api/hr', () => hr)

const message = vi.hoisted(() => ({ success: vi.fn() }))
vi.mock('element-plus', async (orig) => {
  const actual = await orig<typeof import('element-plus')>()
  return { ...actual, ElMessage: { ...actual.ElMessage, success: message.success } }
})

import Notifications from '@/views/notifications/index.vue'
import { useUserStore } from '@/stores/user'

const inquiry = { id: 1, type: 'CONTACT_INQUIRY_RECEIVED', title: 'New contact inquiry', body: 'Amina asked about visas', read: false, createdAt: '2026-09-01 10:00:00' }
const progress = { id: 2, type: 'TASK_PROGRESS', title: 'Task moved', body: 'In progress', read: true, createdAt: '2026-09-02 11:00:00' }

const detail = (row: typeof inquiry, dataJson: string | null = null, deliveries: unknown[] = []) => ({ ...row, dataJson, deliveries })

async function mountPage() {
  const w = mount(Notifications, mountOpts())
  await flushPromises()
  return w
}

const row = (w: ReturnType<typeof mount>, text: string) => w.findAll('tbody tr').find(r => r.text().includes(text))!

describe('Notifications page', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    Object.values(api).forEach(fn => fn.mockReset())
    hr.getTask.mockReset()
    message.success.mockReset()
    api.listStaffNotifications.mockResolvedValue({ content: [inquiry, progress], totalElements: 2 })
    api.listMyNotifications.mockResolvedValue({ content: [inquiry, progress], totalElements: 2 })
    api.markNotificationRead.mockResolvedValue(undefined)
    api.markAllNotificationsRead.mockResolvedValue(undefined)
  })

  it('shows the oversight list to a caller who may list every notification', async () => {
    useUserStore().permissions = ['nad:notification:list']
    const w = await mountPage()
    expect(api.listStaffNotifications).toHaveBeenCalledWith({ type: undefined, page: 0, size: 15 })
    expect(api.listMyNotifications).not.toHaveBeenCalled()
    expect(w.text()).toContain('New contact inquiry')
    expect(w.find('.el-radio-group').exists()).toBe(true)
  })

  it('shows only the callers own feed, without switching or filtering, when they cannot oversee', async () => {
    useUserStore().permissions = []
    const w = await mountPage()
    expect(api.listMyNotifications).toHaveBeenCalledWith({ page: 0, size: 15 })
    expect(api.listStaffNotifications).not.toHaveBeenCalled()
    expect(w.find('.el-radio-group').exists()).toBe(false)
    expect(w.findAll('button').some(b => b.text() === 'Mark all read')).toBe(true)
  })

  it('opens a notification for oversight with its stored payload and delivery errors', async () => {
    useUserStore().permissions = ['nad:notification:list']
    api.getStaffNotification.mockResolvedValue(detail(inquiry, JSON.stringify({ email: 'a@x.com', recipientUserIds: [4], empty: '' }),
      [{ id: 1, channel: 'EMAIL', status: 'FAILED', lastError: 'mailbox full' }]))
    const w = await mountPage()
    await row(w, 'New contact inquiry').trigger('click')
    await flushPromises()

    expect(api.getStaffNotification).toHaveBeenCalledWith(1)
    expect(w.text()).toContain('a@x.com')
    expect(w.text()).not.toContain('recipientUserIds')
    expect(w.text()).toContain('mailbox full')
  })

  it('marks an unread notification as read when the caller opens it from their own feed', async () => {
    useUserStore().permissions = []
    const w = await mountPage()
    await row(w, 'New contact inquiry').trigger('click')
    await flushPromises()

    expect(api.markNotificationRead).toHaveBeenCalledWith(1)
    expect(api.getStaffNotification).not.toHaveBeenCalled()
    expect(w.text()).toContain('Amina asked about visas')
  })

  it('does not mark an already read notification again', async () => {
    useUserStore().permissions = []
    const w = await mountPage()
    await row(w, 'Task moved').trigger('click')
    await flushPromises()
    expect(api.markNotificationRead).not.toHaveBeenCalled()
  })

  it('pulls the full task for a task-progress notification only for someone who may query tasks', async () => {
    useUserStore().permissions = ['nad:notification:list', 'nad:task:query']
    api.getStaffNotification.mockResolvedValue(detail(progress as never, JSON.stringify({ taskId: 33, status: 'IN_PROGRESS' })))
    hr.getTask.mockResolvedValue({ id: 33, title: 'Collect passports', priority: 'HIGH', status: 'IN_PROGRESS', createdByUserId: 1, events: [] })
    const w = await mountPage()
    await row(w, 'Task moved').trigger('click')
    await flushPromises()
    expect(hr.getTask).toHaveBeenCalledWith(33)
    expect(w.text()).toContain('Collect passports')
  })

  it('never fetches the task when the caller lacks the task permission', async () => {
    useUserStore().permissions = ['nad:notification:list']
    api.getStaffNotification.mockResolvedValue(detail(progress as never, JSON.stringify({ taskId: 33 })))
    const w = await mountPage()
    await row(w, 'Task moved').trigger('click')
    await flushPromises()
    expect(hr.getTask).not.toHaveBeenCalled()
  })

  it('copes with a payload that is not valid JSON', async () => {
    useUserStore().permissions = ['nad:notification:list']
    api.getStaffNotification.mockResolvedValue(detail(inquiry, '{not json'))
    const w = await mountPage()
    await row(w, 'New contact inquiry').trigger('click')
    await flushPromises()
    expect(w.text()).toContain('Amina asked about visas')
  })

  it('marks everything read and reloads the feed', async () => {
    useUserStore().permissions = []
    const w = await mountPage()
    await w.findAll('button').find(b => b.text() === 'Mark all read')!.trigger('click')
    await flushPromises()
    expect(api.markAllNotificationsRead).toHaveBeenCalledOnce()
    expect(message.success).toHaveBeenCalled()
    expect(api.listMyNotifications).toHaveBeenCalledTimes(2)
  })
})
