import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import FormReadinessCard from '~/components/applicant/FormReadinessCard.vue'
import FamilyMemberForm from '~/components/applicant/FamilyMemberForm.vue'

const api = vi.hoisted(() => ({ readiness: vi.fn() }))
mockNuxtImport('useApplicantFamily', () => () => api)

beforeEach(() => api.readiness.mockReset())

describe('FormReadinessCard', () => {
  it('lists what the application form still lacks and links to the family page', async () => {
    api.readiness.mockResolvedValue({ missing: ['FAMILY', 'SUPPORTER'] })
    const w = await mountSuspended(FormReadinessCard, { props: { applicantId: 3 } })
    await flushPromises()

    expect(w.find('[data-test="readiness"]').exists()).toBe(true)
    expect(w.text()).toContain('Family members')
    expect(w.text()).toContain('Financial supporter')
    expect(w.text()).not.toContain('Personal details')
    expect(w.find('a[href="/dashboard/family"]').exists()).toBe(true)
  })

  it('shows nothing when the form is complete', async () => {
    api.readiness.mockResolvedValue({ missing: [] })
    const w = await mountSuspended(FormReadinessCard, { props: { applicantId: 3 } })
    await flushPromises()

    expect(w.find('[data-test="readiness"]').exists()).toBe(false)
  })
})

describe('FamilyMemberForm', () => {
  it('asks for a note when the person has died and submits once it is given', async () => {
    const w = await mountSuspended(FamilyMemberForm, { props: { record: null, busy: false } })
    await w.find('#fm-relation').setValue('MOTHER')
    await w.find('#fm-name').setValue('Fatoumata Diallo')
    await w.find('#fm-deceased').setValue(true)

    await w.find('form').trigger('submit')
    expect(w.emitted('submit')).toBeUndefined()

    await w.find('#fm-note').setValue('Passed away in 2020')
    await w.find('form').trigger('submit')
    expect(w.emitted('submit')?.[0]?.[0]).toMatchObject({ relation: 'MOTHER', deceased: true, note: 'Passed away in 2020' })
  })

  it('hides the employer fields for a person who has died', async () => {
    const w = await mountSuspended(FamilyMemberForm, { props: { record: null, busy: false } })
    expect(w.find('#fm-company').exists()).toBe(true)

    await w.find('#fm-deceased').setValue(true)
    expect(w.find('#fm-company').exists()).toBe(false)
  })
})
