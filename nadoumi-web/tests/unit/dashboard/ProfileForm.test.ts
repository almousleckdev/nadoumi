import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import ProfileForm from '~/components/dashboard/ProfileForm.vue'

describe('ProfileForm', () => {
  it('requires given and family name before emitting submit', async () => {
    const w = await mountSuspended(ProfileForm, { props: { modelValue: null, busy: false } })
    await w.find('form').trigger('submit')
    expect(w.emitted('submit')).toBeUndefined()
    expect(w.text()).toContain('required')
  })

  it('emits a clean body on valid submit', async () => {
    const w = await mountSuspended(ProfileForm, { props: { modelValue: null, busy: false } })
    await w.find('#givenName').setValue('Ada')
    await w.find('#familyName').setValue('Byron')
    await w.find('#nationality').setValue('GB')
    await w.find('form').trigger('submit')
    expect(w.emitted('submit')?.[0]?.[0]).toMatchObject({ givenName: 'Ada', familyName: 'Byron', nationality: 'GB' })
  })

  it('prefills from modelValue', async () => {
    const w = await mountSuspended(ProfileForm, {
      props: { busy: false, modelValue: { id: 1, givenName: 'Sam', familyName: 'Lee', dob: '2000-01-01', nationality: 'MY', passportNo: 'X1', email: 's@x.io', phone: '123', status: 'ACTIVE' } },
    })
    expect((w.find('#givenName').element as HTMLInputElement).value).toBe('Sam')
    expect((w.find('#nationality').element as HTMLInputElement).value).toBe('MY')
  })
})
