import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import InterestsSection from '~/components/applicant/InterestsSection.vue'

const { api } = vi.hoisted(() => ({
  api: { getInterests: vi.fn(), saveInterests: vi.fn() },
}))
vi.mock('~/composables/useApplicant', () => ({ useApplicant: () => api }))

const record = {
  desiredLevel: 'BACHELOR', fields: ['ENGINEERING'], cities: ['Beijing'], scholarshipInterest: 'PREFERRED',
  intakeYear: 2026, intakeTerm: 'FALL', teachingLanguage: 'en', notes: null,
}

async function mountSection() {
  const w = await mountSuspended(InterestsSection, { props: { applicantId: 1 } })
  await flushPromises()
  return w
}

beforeEach(() => {
  api.getInterests.mockReset().mockResolvedValue(null)
  api.saveInterests.mockReset().mockResolvedValue(record)
})

describe('InterestsSection', () => {
  it('starts empty when nothing was saved yet', async () => {
    const w = await mountSection()
    expect((w.find('#int-level').element as HTMLSelectElement).value).toBe('')
  })

  it('prefills from the saved record', async () => {
    api.getInterests.mockResolvedValue(record)
    const w = await mountSection()
    expect((w.find('#int-level').element as HTMLSelectElement).value).toBe('BACHELOR')
  })

  it('saves the form and reports the change', async () => {
    const w = await mountSection()

    await w.find('#int-level').setValue('MASTER')
    await w.findAll('button').find(b => b.text() === 'Engineering')!.trigger('click')
    await w.findAll('button').find(b => b.text() === 'Beijing')!.trigger('click')
    await w.find('form').trigger('submit')
    await flushPromises()

    expect(api.saveInterests).toHaveBeenCalledWith(1, expect.objectContaining({ desiredLevel: 'MASTER' }))
    expect(w.emitted('changed')).toBeTruthy()
  })
})
