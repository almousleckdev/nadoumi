import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import AiAssistant from '~/components/marketing/AiAssistant.vue'

const publicGet = vi.fn()
const studentFetch = vi.fn()
vi.mock('~/composables/useApi', () => ({
  useApi: () => ({ publicGet, studentFetch }),
}))

const scholarship = { id: 1, slug: 'csc-master', title: 'CSC Master', country: 'CN', fundingModel: 'FULLY', deadline: '2026-04-01' }

async function openAssistant() {
  const w = await mountSuspended(AiAssistant)
  await w.find('button.ai-fab').trigger('click')
  return w
}

beforeEach(() => {
  publicGet.mockReset().mockResolvedValue({ content: [scholarship] })
  studentFetch.mockReset()
  localStorage.clear()
})

describe('AiAssistant', () => {
  it('starts closed and opens from the floating button', async () => {
    const w = await mountSuspended(AiAssistant)
    expect(w.find('[role="dialog"]').exists()).toBe(false)
    await w.find('button.ai-fab').trigger('click')
    expect(w.find('[role="dialog"]').exists()).toBe(true)
    expect(w.text()).toContain('Nadoumi Assistant')
  })

  it('closes on Escape and from the close button', async () => {
    const w = await openAssistant()
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await flushPromises()
    expect(w.find('[role="dialog"]').exists()).toBe(false)

    await w.find('button.ai-fab').trigger('click')
    await w.find('button.ai-panel__close').trigger('click')
    await flushPromises()
    expect(w.find('[role="dialog"]').exists()).toBe(false)
  })

  it('remembers that the assistant was opened', async () => {
    await openAssistant()
    expect(localStorage.getItem('nad.assistant.seen')).toBe('1')
  })

  it('searches scholarships and lists the matches with a link to the full results', async () => {
    const w = await openAssistant()
    await w.find('input[type="search"]').setValue('engineering')
    await w.find('form.ai-form').trigger('submit')
    await flushPromises()

    expect(publicGet).toHaveBeenCalledWith('scholarships', { size: 5, q: 'engineering' })
    expect(w.text()).toContain('CSC Master')
    const hrefs = w.findAll('a').map(a => a.attributes('href') ?? '')
    expect(hrefs.some(h => h.includes('/scholarships/csc-master'))).toBe(true)
    expect(hrefs.some(h => h.includes('/scholarships?q=engineering'))).toBe(true)
  })

  it('says so when nothing matches and when the search fails', async () => {
    publicGet.mockResolvedValueOnce({ content: [] })
    const w = await openAssistant()
    await w.find('form.ai-form').trigger('submit')
    await flushPromises()
    expect(w.text()).toContain('No scholarships matched')

    publicGet.mockRejectedValueOnce(new Error('boom'))
    await w.find('form.ai-form').trigger('submit')
    await flushPromises()
    expect(w.text()).toContain('could not be loaded')
  })

  it('tracks an application by id from the second tab', async () => {
    studentFetch.mockResolvedValue({ id: 7, status: 'IN_REVIEW', opportunityTitle: 'CSC Master', stage: 'Documents' })
    const w = await openAssistant()
    await w.findAll('button.ai-tab')[1]!.trigger('click')
    await w.find('input[type="text"]').setValue('7')
    await w.find('form.ai-form').trigger('submit')
    await flushPromises()

    expect(studentFetch).toHaveBeenCalledWith('applications/7')
    expect(w.text()).toContain('CSC Master')
    expect(w.text()).toContain('IN_REVIEW')
    expect(w.text()).toContain('Documents')
  })

  it('explains when an application does not exist and when tracking is unavailable', async () => {
    studentFetch.mockRejectedValueOnce({ statusCode: 404 })
    const w = await openAssistant()
    await w.findAll('button.ai-tab')[1]!.trigger('click')
    await w.find('input[type="text"]').setValue('999')
    await w.find('form.ai-form').trigger('submit')
    await flushPromises()
    expect(w.text()).toContain("couldn't find an application")

    studentFetch.mockRejectedValueOnce({ statusCode: 503 })
    await w.find('form.ai-form').trigger('submit')
    await flushPromises()
    expect(w.text()).toContain('Application tracking is being connected')
  })
})
