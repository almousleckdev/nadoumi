import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended, mockNuxtImport } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import SupportDetail from '~/pages/dashboard/support/[id].vue'
import type { TicketDetail, TicketMessage } from '~/types/support'

const M = (over: Partial<TicketMessage> = {}): TicketMessage => ({
  id: 1, conversationId: 9, senderUserId: 1, senderName: 'Sam', body: 'I need help', createdAt: '2026-01-01T10:00:00',
  editedAt: null, attachments: [], ...over,
})
const D = (status: TicketDetail['ticket']['status'] = 'OPEN', messages = [M()]): TicketDetail => ({
  ticket: { id: 5, subject: 'Visa question', category: 'APPLICATION', status, createTime: '2026-01-01T10:00:00', updateTime: '2026-01-01T10:00:00' },
  conversationId: 9,
  messages,
})

const get = vi.fn()
const reply = vi.fn()
vi.mock('~/composables/useSupport', () => ({ useSupport: () => ({ get, reply }) }))

mockNuxtImport('useRoute', () => () => ({ params: { id: '5' } }))
vi.mock('~/composables/useSession', () => ({
  useSession: () => ({ user: ref({ userId: 1, username: 'stu', nickName: 'Sam', email: null }) }),
}))

async function mountPage() {
  const w = await mountSuspended(SupportDetail)
  await flushPromises()
  return w
}

beforeEach(() => {
  get.mockReset().mockResolvedValue(D())
  reply.mockReset().mockResolvedValue(M({ id: 2, body: 'thanks' }))
})

describe('student support ticket detail', () => {
  it('shows the subject, student-safe status and the thread', async () => {
    get.mockResolvedValue(D('WAITING_ON_STUDENT', [M(), M({ id: 2, senderUserId: 77, senderName: 'Ada', body: 'Please send a scan' })]))
    const w = await mountPage()
    expect(w.find('[data-test="subject"]').text()).toBe('Visa question')
    expect(w.find('[data-test="status"]').text()).toBe('Waiting for you')
    expect(w.text()).toContain('I need help')
    expect(w.text()).toContain('Please send a scan')
    expect(w.text()).toContain('Ada')
  })

  it('never renders internal triage data', async () => {
    const w = await mountPage()
    expect(w.text().toLowerCase()).not.toContain('priority')
    expect(w.text().toLowerCase()).not.toContain('assignee')
  })

  it('sends a reply and refreshes the thread', async () => {
    const w = await mountPage()
    await w.find('#support-reply').setValue('thanks')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(reply).toHaveBeenCalledWith(5, 'thanks')
    expect(get).toHaveBeenCalledTimes(2)
  })

  it('does not send an empty reply', async () => {
    const w = await mountPage()
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(reply).not.toHaveBeenCalled()
  })

  it.each(['RESOLVED', 'CLOSED'] as const)('replaces the composer with a notice when the ticket is %s', async (status) => {
    get.mockResolvedValue(D(status))
    const w = await mountPage()
    expect(w.find('[data-test="closed-notice"]').exists()).toBe(true)
    expect(w.find('#support-reply').exists()).toBe(false)
  })

  it('shows a clear message for a ticket that is not the student\'s', async () => {
    get.mockRejectedValue({ statusCode: 404 })
    const w = await mountPage()
    expect(w.text()).toContain('could not be found')
  })

  it('shows a load error for other failures', async () => {
    get.mockRejectedValue({ statusCode: 500 })
    const w = await mountPage()
    expect(w.text()).toContain('could not be loaded')
  })

  it('shows the reply error without losing the draft', async () => {
    reply.mockRejectedValue({ statusCode: 400, data: { detail: 'ticket is CLOSED, open a new ticket for further help' } })
    const w = await mountPage()
    await w.find('#support-reply').setValue('one more thing')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect((w.find('#support-reply').element as HTMLTextAreaElement).value).toBe('one more thing')
    expect(w.find('[role="alert"]').exists()).toBe(true)
  })
})
