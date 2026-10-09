import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({ listApplicants: vi.fn(), deleteApplicant: vi.fn(), createApplicant: vi.fn() }))
vi.mock('@/api/applicant', () => api)

const confirmAnswer = vi.hoisted(() => ({ value: true }))
vi.mock('@/composables/useConfirm', () => ({ useConfirm: () => ({ confirm: vi.fn(async () => confirmAnswer.value) }) }))

import Applicants from '@/views/applicants/index.vue'
import { useUserStore } from '@/stores/user'

const row = (id: number, over = {}) => ({
  id, givenName: 'Amina', familyName: 'Benali', nationality: 'MA', email: 'a@x.io', passportNo: null,
  status: 'ACTIVE', createdAt: '2026-01-01 10:00:00', photoUrl: null, ...over,
})

const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:x(.*)*', component: { template: '<div />' } }] })

// el-table keeps a hidden copy of each column to register it; only the body rows are real.
const body = (w: Awaited<ReturnType<typeof mountList>>) => w.find('.el-table__body')

async function mountList() {
  const w = mount(Applicants, { global: { ...mountOpts().global, plugins: [...mountOpts().global.plugins, router] } })
  await flushPromises()
  return w
}

describe('Applicants list', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    useUserStore().permissions = ['*:*:*']
    confirmAnswer.value = true
    api.listApplicants.mockReset().mockResolvedValue({ content: [row(7, { photoUrl: '/api/public/avatars/applicants/7/sig' }), row(8)], totalElements: 2 })
    api.deleteApplicant.mockReset().mockResolvedValue(undefined)
  })

  it('shows the profile photo of an applicant that has one, and initials for the rest', async () => {
    const w = await mountList()
    const sources = w.findAllComponents({ name: 'Avatar' }).map(a => a.props('src'))
    expect(sources).toContain('/api/public/avatars/applicants/7/sig')
    expect(sources.filter(Boolean)).toHaveLength(1) // the applicant without a photo keeps its initials
  })

  it('offers Delete, not Archive, on every row', async () => {
    const w = await mountList()
    expect(body(w).findAll('[data-test="delete-applicant"]')).toHaveLength(2)
    expect(w.text()).not.toContain('Archive')
  })

  it('deletes after a confirmation and reloads the list', async () => {
    const w = await mountList()
    await body(w).find('[data-test="delete-applicant"]').trigger('click')
    await flushPromises()
    expect(api.deleteApplicant).toHaveBeenCalledWith(7)
    expect(api.listApplicants).toHaveBeenCalledTimes(2)
  })

  it('keeps the applicant when the confirmation is declined', async () => {
    confirmAnswer.value = false
    const w = await mountList()
    await body(w).find('[data-test="delete-applicant"]').trigger('click')
    await flushPromises()
    expect(api.deleteApplicant).not.toHaveBeenCalled()
  })

  it('shows no Delete without the delete permission', async () => {
    useUserStore().permissions = ['nad:applicant:list']
    const w = await mountList()
    expect(body(w).find('[data-test="delete-applicant"]').exists()).toBe(false)
  })
})
