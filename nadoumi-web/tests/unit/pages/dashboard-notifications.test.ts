import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import Notifications from '~/pages/dashboard/notifications.vue'
import type { NotificationView } from '~/types/catalog'

const N = (over: Partial<NotificationView> = {}): NotificationView => ({
  id: 1, type: 'GENERAL', title: 'Welcome to Nadoumi', body: 'Glad to have you.', dataJson: null,
  applicationId: null, conversationId: null, messageId: null, createdAt: '2026-01-01T10:00:00Z',
  readAt: null, read: false, ...over,
})

const list = vi.fn()
const markRead = vi.fn().mockResolvedValue({ updated: true })
const markAllRead = vi.fn().mockResolvedValue({ updated: 1 })

vi.mock('~/composables/useNotifications', () => ({
  useNotifications: () => ({ list, markRead, markAllRead }),
}))

async function mountPage() {
  const w = await mountSuspended(Notifications)
  await flushPromises()
  return w
}

beforeEach(() => {
  list.mockReset().mockResolvedValue({ content: [N()], totalElements: 1 })
  markRead.mockClear()
  markAllRead.mockClear()
})

describe('dashboard notifications', () => {
  it('shows the empty state — never fake notifications', async () => {
    list.mockResolvedValueOnce({ content: [], totalElements: 0 })
    const w = await mountPage()
    expect(w.text()).toContain("all caught up")
  })

  it('lists real notifications, unread ones bolded', async () => {
    const w = await mountPage()
    expect(w.text()).toContain('Welcome to Nadoumi')
    expect(w.text()).toContain('Glad to have you.')
  })

  it('marks a notification read on click', async () => {
    const w = await mountPage()
    await w.findAll('button').find(b => b.text().includes('Welcome to Nadoumi'))!.trigger('click')
    await flushPromises()
    expect(markRead).toHaveBeenCalledWith(1)
  })

  it('marks all as read and hides the button once nothing is unread', async () => {
    const w = await mountPage()
    expect(w.text()).toContain('Mark all as read')
    await w.findAll('button').find(b => b.text() === 'Mark all as read')!.trigger('click')
    await flushPromises()
    expect(markAllRead).toHaveBeenCalledOnce()
    expect(w.text()).not.toContain('Mark all as read')
  })

  it('loads more when there are further pages', async () => {
    list.mockResolvedValueOnce({ content: [N({ id: 1 })], totalElements: 2 })
    list.mockResolvedValueOnce({ content: [N({ id: 2, title: 'Second' })], totalElements: 2 })
    const w = await mountPage()
    expect(w.text()).toContain('Load more')
    await w.findAll('button').find(b => b.text() === 'Load more')!.trigger('click')
    await flushPromises()
    expect(w.text()).toContain('Second')
    expect(list).toHaveBeenLastCalledWith({ page: 1, size: 20 })
  })

  it('shows an error with retry when the feed fails to load', async () => {
    list.mockReset().mockRejectedValue(new Error('boom'))
    const w = await mountPage()
    expect(w.text()).toContain('This section could not be loaded')
    list.mockResolvedValueOnce({ content: [N()], totalElements: 1 })
    await w.findAll('button').find(b => b.text() === 'Try again')!.trigger('click')
    await flushPromises()
    expect(w.text()).toContain('Welcome to Nadoumi')
  })
})
