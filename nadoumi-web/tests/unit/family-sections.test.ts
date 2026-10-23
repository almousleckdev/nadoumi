import { describe, expect, it } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import FamilyMemberForm from '~/components/applicant/FamilyMemberForm.vue'

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
