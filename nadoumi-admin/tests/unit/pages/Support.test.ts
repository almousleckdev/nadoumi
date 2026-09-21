import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises, type VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({
  listTickets: vi.fn(),
  getTicket: vi.fn(),
  changeTicketStatus: vi.fn(),
  changeTicketPriority: vi.fn(),
  changeTicketCategory: vi.fn(),
  assignTicket: vi.fn(),
  replyToTicket: vi.fn(),
}))
vi.mock('@/api/support', async (orig) => ({ ...(await orig<typeof import('@/api/support')>()), ...api }))
vi.mock('@/api/system', async (orig) => ({
  ...(await orig<typeof import('@/api/system')>()),
  listUsers: vi.fn().mockResolvedValue({ rows: [{ userId: 7, userName: 'ada', nickName: 'Ada' }], total: 1 }),
}))

import Support from '@/views/support/index.vue'
import SupportTicketDrawer from '@/views/support/SupportTicketDrawer.vue'
import { useUserStore } from '@/stores/user'
import type { StaffTicketDetail, StaffTicketSummary, TicketStatus } from '@/api/support'

const ticket = (over: Partial<StaffTicketSummary> = {}): StaffTicketSummary => ({
  id: 5, subject: 'Visa question', category: 'APPLICATION', priority: 'NORMAL', status: 'OPEN',
  openedByUserId: 100, applicantId: null, assignedStaffId: null,
  createTime: '2026-01-01 10:00:00', updateTime: '2026-01-02 10:00:00', ...over,
})
const detail = (status: TicketStatus = 'OPEN'): StaffTicketDetail => ({
  ticket: ticket({ status }),
  conversationId: 9,
  messages: [{
    id: 1, conversationId: 9, senderUserId: 100, senderName: 'Sam', body: 'I need help', createdAt: '2026-01-01 10:00:00',
    editedAt: null, attachments: [],
  }],
  events: [{ eventType: 'OPENED', oldValue: null, newValue: null, actorUserId: 100, createdAt: '2026-01-01 10:00:00' }],
})

beforeEach(() => {
  setActivePinia(createPinia())
  useUserStore().permissions = ['*:*:*']
  Object.values(api).forEach(fn => fn.mockReset())
  api.listTickets.mockResolvedValue([ticket()])
  api.getTicket.mockResolvedValue(detail())
  api.changeTicketStatus.mockResolvedValue(ticket({ status: 'IN_PROGRESS' }))
  api.replyToTicket.mockResolvedValue({})
  api.assignTicket.mockResolvedValue(ticket({ assignedStaffId: 7 }))
})

describe('Support tickets queue', () => {
  it('loads the queue on mount and shows real rows', async () => {
    const w = mount(Support, mountOpts())
    await flushPromises()
    expect(api.listTickets).toHaveBeenCalledWith(expect.objectContaining({ page: 0, size: 20 }))
    expect(w.text()).toContain('Visa question')
    expect(w.text()).toContain('Unassigned')
  })

  it('shows an honest empty state', async () => {
    api.listTickets.mockResolvedValue([])
    const w = mount(Support, mountOpts())
    await flushPromises()
    expect(w.text()).toContain('No support tickets')
  })

  it('offers a next page only when a full page came back (the API has no total)', async () => {
    api.listTickets.mockResolvedValue(Array.from({ length: 20 }, (_, i) => ticket({ id: i + 1, subject: `T${i + 1}` })))
    const w = mount(Support, mountOpts())
    await flushPromises()
    expect(w.findComponent({ name: 'ElPagination' }).props('total')).toBeGreaterThan(20)
  })

  it('reloads with the chosen filters', async () => {
    const w = mount(Support, mountOpts())
    await flushPromises()
    const selects = w.findAllComponents({ name: 'ElSelect' })
    await selects[0]!.vm.$emit('update:modelValue', 'RESOLVED')
    await selects[0]!.vm.$emit('change', 'RESOLVED')
    await flushPromises()
    expect(api.listTickets).toHaveBeenLastCalledWith(expect.objectContaining({ status: 'RESOLVED', page: 0 }))
  })
})

// el-drawer teleports to <body>; unmount and clear it so one test never sees another's drawer.
const mounted: VueWrapper[] = []
afterEach(() => {
  mounted.splice(0).forEach(w => w.unmount())
  document.body.innerHTML = ''
})

function mountDrawer(status: TicketStatus = 'OPEN') {
  api.getTicket.mockResolvedValue(detail(status))
  const w = mount(SupportTicketDrawer, {
    ...mountOpts(),
    props: { modelValue: true, ticketId: 5, staff: [{ userId: 7, userName: 'ada', nickName: 'Ada' } as never] },
    attachTo: document.body,
  })
  mounted.push(w)
  return w
}
const body = () => document.body.textContent ?? ''

describe('Support ticket drawer', () => {
  it('shows the thread and history from the real detail', async () => {
    mountDrawer()
    await flushPromises()
    expect(api.getTicket).toHaveBeenCalledWith(5)
    expect(body()).toContain('I need help')
    expect(body()).toContain('Ticket opened')
  })

  it('only offers the status moves the backend allows', async () => {
    mountDrawer('OPEN')
    await flushPromises()
    const actions = document.body.querySelector('[data-test="status-actions"]')!.textContent ?? ''
    expect(actions).toContain('Start work')
    expect(actions).not.toContain('Mark resolved')
    expect(actions).not.toContain('Close ticket')
  })

  it('offers no moves on a closed ticket and hides the reply box', async () => {
    mountDrawer('CLOSED')
    await flushPromises()
    expect(document.body.querySelector('[data-test="status-actions"]')!.textContent).toContain('No further status changes')
    expect(document.body.querySelector('[data-test="reply"]')).toBeNull()
  })

  it('runs a status change then reloads the detail and tells the queue', async () => {
    const w = mountDrawer('OPEN')
    await flushPromises()
    const start = [...document.body.querySelectorAll('[data-test="status-actions"] button')].find(b => b.textContent?.includes('Start work'))!
    ;(start as HTMLButtonElement).click()
    await flushPromises()
    expect(api.changeTicketStatus).toHaveBeenCalledWith(5, 'IN_PROGRESS')
    expect(api.getTicket).toHaveBeenCalledTimes(2)
    expect(w.emitted('changed')).toHaveLength(1)
  })

  it('sends a reply and clears the draft', async () => {
    mountDrawer('IN_PROGRESS')
    await flushPromises()
    const ta = document.body.querySelector('[data-test="reply"] textarea') as HTMLTextAreaElement
    ta.value = 'We are looking into it'
    ta.dispatchEvent(new Event('input'))
    await flushPromises()
    ;(document.body.querySelector('[data-test="send-reply"]') as HTMLButtonElement).click()
    await flushPromises()
    expect(api.replyToTicket).toHaveBeenCalledWith(5, 'We are looking into it')
    expect((document.body.querySelector('[data-test="reply"] textarea') as HTMLTextAreaElement).value).toBe('')
  })

  it('keeps the draft when the reply is refused', async () => {
    api.replyToTicket.mockRejectedValue(new Error('ticket is CLOSED'))
    mountDrawer('IN_PROGRESS')
    await flushPromises()
    const ta = document.body.querySelector('[data-test="reply"] textarea') as HTMLTextAreaElement
    ta.value = 'still typing'
    ta.dispatchEvent(new Event('input'))
    await flushPromises()
    ;(document.body.querySelector('[data-test="send-reply"]') as HTMLButtonElement).click()
    await flushPromises()
    expect((document.body.querySelector('[data-test="reply"] textarea') as HTMLTextAreaElement).value).toBe('still typing')
  })

  it('hides every action from staff who may only view (server still enforces, UI just does not offer)', async () => {
    useUserStore().permissions = ['nad:support:ticket:view']
    mountDrawer('OPEN')
    await flushPromises()
    expect(document.body.querySelector('[data-test="status-actions"]')).toBeNull()
    expect(document.body.querySelector('[data-test="reply"]')).toBeNull()
    expect(document.body.querySelector('[data-test="priority-select"]')).toBeNull()
    expect(document.body.querySelector('[data-test="assign-select"]')).toBeNull()
  })

  it('shows assignment only with the assign permission', async () => {
    useUserStore().permissions = ['nad:support:ticket:view', 'nad:support:ticket:manage']
    mountDrawer('OPEN')
    await flushPromises()
    expect(document.body.querySelector('[data-test="priority-select"]')).not.toBeNull()
    expect(document.body.querySelector('[data-test="assign-select"]')).toBeNull()
  })
})
