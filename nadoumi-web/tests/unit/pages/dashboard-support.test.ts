import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended, mockNuxtImport } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import SupportList from '~/pages/dashboard/support/index.vue'
import type { TicketSummary } from '~/types/support'

const T = (over: Partial<TicketSummary> = {}): TicketSummary => ({
  id: 1, subject: 'Visa question', category: 'APPLICATION', status: 'OPEN',
  createTime: '2026-01-01T10:00:00', updateTime: '2026-01-02T10:00:00', ...over,
})

const list = vi.fn()
const create = vi.fn()
vi.mock('~/composables/useSupport', () => ({ useSupport: () => ({ list, create }) }))

const { navigate } = vi.hoisted(() => ({ navigate: vi.fn() }))
mockNuxtImport('navigateTo', () => navigate)

async function mountPage() {
  const w = await mountSuspended(SupportList)
  await flushPromises()
  return w
}

beforeEach(() => {
  list.mockReset().mockResolvedValue([T()])
  create.mockReset().mockResolvedValue({ ticket: T({ id: 42 }), conversationId: 9, messages: [] })
  navigate.mockReset()
})

describe('student support list', () => {
  it('shows a real empty state, never fake tickets', async () => {
    list.mockResolvedValueOnce([])
    const w = await mountPage()
    expect(w.text()).toContain('no support tickets yet')
  })

  it('lists the student tickets with a status badge', async () => {
    const w = await mountPage()
    expect(w.text()).toContain('Visa question')
    expect(w.text()).toContain('Open')
  })

  it('shows a retryable error when the list fails to load', async () => {
    list.mockRejectedValueOnce(new Error('boom'))
    const w = await mountPage()
    expect(w.text()).toContain('could not be loaded')
    list.mockResolvedValueOnce([T({ subject: 'Recovered' })])
    await w.findAll('button').find(b => b.text() === 'Try again')!.trigger('click')
    await flushPromises()
    expect(w.text()).toContain('Recovered')
  })

  it('filters by status through the API', async () => {
    const w = await mountPage()
    await w.find('#ticket-status-filter').setValue('RESOLVED')
    await flushPromises()
    expect(list).toHaveBeenLastCalledWith({ status: 'RESOLVED', page: 0, size: 20 })
  })

  it('loads the next page when a full page came back', async () => {
    list.mockResolvedValueOnce(Array.from({ length: 20 }, (_, i) => T({ id: i + 1, subject: `Ticket ${i + 1}` })))
    list.mockResolvedValueOnce([T({ id: 99, subject: 'Older ticket' })])
    const w = await mountPage()
    await w.findAll('button').find(b => b.text() === 'Load more')!.trigger('click')
    await flushPromises()
    expect(w.text()).toContain('Older ticket')
    expect(list).toHaveBeenLastCalledWith({ status: undefined, page: 1, size: 20 })
  })

  it('validates the new-ticket form before calling the API', async () => {
    const w = await mountPage()
    await w.find('[data-test="new-ticket"]').trigger('click')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(create).not.toHaveBeenCalled()
    expect(w.text()).toContain('Please enter a subject')
    expect(w.text()).toContain('Please choose a category')
    expect(w.text()).toContain('Please describe your question')
  })

  it('creates a ticket and opens it', async () => {
    const w = await mountPage()
    await w.find('[data-test="new-ticket"]').trigger('click')
    await w.find('#ticket-subject').setValue('Passport help')
    await w.find('#ticket-category').setValue('DOCUMENT')
    await w.find('#ticket-body').setValue('My scan was rejected')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(create).toHaveBeenCalledWith({ subject: 'Passport help', category: 'DOCUMENT', body: 'My scan was rejected' })
    expect(navigate).toHaveBeenCalledWith(expect.stringContaining('/dashboard/support/42'))
  })
})
