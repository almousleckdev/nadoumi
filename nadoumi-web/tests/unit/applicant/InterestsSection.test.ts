import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import InterestsSection from '~/components/applicant/InterestsSection.vue'

const { api } = vi.hoisted(() => ({
  api: { getInterests: vi.fn(), saveInterests: vi.fn() },
}))
vi.mock('~/composables/useApplicant', () => ({ useApplicant: () => api }))

const record = {
  desiredLevel: 'BACHELOR', fields: ['ENGINEERING'], cities: ['Beijing', 'Sichuan', 'Shanghai'], scholarshipInterest: 'PREFERRED',
  intakeYear: 2026, intakeTerm: 'FALL', teachingLanguage: 'EN_ZH', notes: null,
}

async function mountSection() {
  const w = await mountSuspended(InterestsSection, { props: { applicantId: 1 } })
  await flushPromises()
  return w
}

async function addTag(w: Awaited<ReturnType<typeof mountSection>>, selector: string, value: string) {
  const input = w.find(selector)
  await input.setValue(value)
  await input.trigger('keydown', { key: 'Enter' })
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
    await addTag(w, '#int-fields', 'Computer Science')
    await addTag(w, '#int-fields', 'Law')
    await addTag(w, '#int-cities', 'Beijing')
    await addTag(w, '#int-cities', 'Sichuan')
    await addTag(w, '#int-cities', 'Shanghai')
    await w.find('#int-teaching').setValue('EN_ZH')
    await w.find('form').trigger('submit')
    await flushPromises()

    expect(api.saveInterests).toHaveBeenCalledWith(1, expect.objectContaining({
      desiredLevel: 'MASTER', fields: ['Computer Science', 'Law'], cities: ['Beijing', 'Sichuan', 'Shanghai'], teachingLanguage: 'EN_ZH',
    }))
    expect(w.emitted('changed')).toBeTruthy()
  })

  it('does not offer a preset list of cities or fields: the student types their own', async () => {
    const w = await mountSection()
    const labels = w.findAll('button').map(b => b.text())
    expect(labels).not.toContain('Beijing')
    expect(labels).not.toContain('Engineering')
  })

  it('offers only English, Chinese, or both as the teaching language', async () => {
    const w = await mountSection()
    const options = w.findAll('#int-teaching option').map(o => o.text()).filter(Boolean)
    expect(options.filter(o => o !== 'Select…')).toEqual(['English', 'Chinese', 'English and Chinese'])
  })

  it('needs at least three provinces or cities before it will save', async () => {
    const w = await mountSection()
    await w.find('#int-level').setValue('MASTER')
    await addTag(w, '#int-fields', 'Law')
    await addTag(w, '#int-cities', 'Beijing')
    await addTag(w, '#int-cities', 'Shanghai')
    await w.find('form').trigger('submit')
    await flushPromises()

    expect(api.saveInterests).not.toHaveBeenCalled()
    expect(w.text()).toContain('at least three provinces or cities')
  })
})
